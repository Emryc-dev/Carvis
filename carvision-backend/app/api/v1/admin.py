from datetime import datetime, timedelta, timezone

from typing import Any
from uuid import UUID

import httpx
from fastapi import APIRouter, Query
from sqlalchemy import String, cast, func, or_, select

from app.core.config import get_settings
from app.core.errors import AppError
from app.dependencies.auth import Admin, DB
from app.models.entities import AIRequest, Favorite, GarageEntry, UserProfile, Vehicle, VehicleScan

router = APIRouter(prefix="/admin", tags=["admin"])


async def supabase_auth_users() -> list[dict[str, Any]]:
    settings = get_settings()
    if not settings.supabase_secret_key:
        raise AppError(503, "supabase_admin_unavailable", "Supabase admin credentials are not configured")
    headers = {"Authorization": f"Bearer {settings.supabase_secret_key}", "apikey": settings.supabase_secret_key}
    users: list[dict[str, Any]] = []
    async with httpx.AsyncClient(timeout=15) as client:
        for page in range(1, 101):
            response = await client.get(
                f"{settings.supabase_url.rstrip('/')}/auth/v1/admin/users",
                headers=headers,
                params={"page": page, "per_page": 1000},
            )
            if response.status_code >= 400:
                raise AppError(502, "supabase_admin_error", "Unable to load Supabase Auth users")
            batch = response.json().get("users", [])
            users.extend(batch)
            if len(batch) < 1000:
                break
    return users


def auth_user_payload(user: dict[str, Any]) -> dict[str, Any]:
    identities = user.get("identities") or []
    providers = sorted({identity.get("provider") for identity in identities if identity.get("provider")})
    return {
        "auth_user_id": user.get("id"), "email": user.get("email"), "phone": user.get("phone"),
        "created_at": user.get("created_at"), "updated_at": user.get("updated_at"),
        "confirmed_at": user.get("confirmed_at") or user.get("email_confirmed_at"),
        "last_sign_in_at": user.get("last_sign_in_at"), "providers": providers,
        "app_metadata": user.get("app_metadata") or {}, "user_metadata": user.get("user_metadata") or {},
    }


def is_admin_auth_user(user: dict[str, Any]) -> bool:
    metadata = user.get("app_metadata") or {}
    roles = metadata.get("roles") or []
    return metadata.get("role") == "admin" or (isinstance(roles, list) and "admin" in roles)


def scan_payload(scan: VehicleScan, user_name: str | None, vehicle: Vehicle | None) -> dict:
    identification = scan.identification or {}
    return {
        "id": str(scan.id),
        "status": scan.status.value,
        "confidence": scan.confidence,
        "created_at": scan.created_at,
        "completed_at": scan.completed_at,
        "error_code": scan.error_code,
        "user_id": str(scan.user_id),
        "user_name": user_name,
        "vehicle": {
            "id": str(vehicle.id),
            "brand": vehicle.brand,
            "model": vehicle.model,
            "year": vehicle.year,
        } if vehicle else {
            "id": None,
            "brand": identification.get("brand"),
            "model": identification.get("model"),
            "year": identification.get("year"),
        },
    }


@router.get("/overview")
async def overview(_: Admin, db: DB) -> dict:
    since = datetime.now(timezone.utc) - timedelta(days=30)
    user_count = sum(1 for user in await supabase_auth_users() if not is_admin_auth_user(user))
    vehicle_count = await db.scalar(select(func.count(Vehicle.id))) or 0
    scan_count = await db.scalar(select(func.count(VehicleScan.id))) or 0
    recent_scan_count = await db.scalar(select(func.count(VehicleScan.id)).where(VehicleScan.created_at >= since)) or 0
    completed_count = await db.scalar(select(func.count(VehicleScan.id)).where(VehicleScan.status == "completed")) or 0
    failed_count = await db.scalar(select(func.count(VehicleScan.id)).where(VehicleScan.status == "failed")) or 0
    ai_request_count = await db.scalar(select(func.count(AIRequest.id))) or 0
    favorite_count = await db.scalar(select(func.count(Favorite.id))) or 0
    average_confidence = await db.scalar(select(func.avg(VehicleScan.confidence)).where(VehicleScan.confidence.is_not(None)))

    trend_rows = (await db.execute(
        select(func.date(VehicleScan.created_at).label("day"), func.count(VehicleScan.id).label("count"))
        .where(VehicleScan.created_at >= since)
        .group_by(func.date(VehicleScan.created_at))
        .order_by(func.date(VehicleScan.created_at))
    )).all()
    recent_rows = (await db.execute(
        select(VehicleScan, UserProfile.name, Vehicle)
        .join(UserProfile, UserProfile.id == VehicleScan.user_id)
        .outerjoin(Vehicle, Vehicle.id == VehicleScan.vehicle_id)
        .order_by(VehicleScan.created_at.desc())
        .limit(8)
    )).all()

    return {
        "metrics": {
            "users": user_count,
            "vehicles": vehicle_count,
            "scans": scan_count,
            "scans_last_30_days": recent_scan_count,
            "completed_scans": completed_count,
            "failed_scans": failed_count,
            "ai_requests": ai_request_count,
            "favorites": favorite_count,
            "average_confidence": float(average_confidence) if average_confidence is not None else None,
        },
        "scan_trend": [{"day": str(day), "count": count} for day, count in trend_rows],
        "recent_scans": [scan_payload(scan, name, vehicle) for scan, name, vehicle in recent_rows],
    }


@router.get("/scans")
async def scans(
    _: Admin,
    db: DB,
    q: str = Query("", max_length=120),
    status: str | None = Query(None),
    limit: int = Query(50, ge=1, le=100),
    offset: int = Query(0, ge=0),
) -> dict:
    filters = []
    if status:
        filters.append(VehicleScan.status == status)
    if q.strip():
        term = f"%{q.strip()}%"
        filters.append(or_(UserProfile.name.ilike(term), Vehicle.brand.ilike(term), Vehicle.model.ilike(term), cast(VehicleScan.identification, String).ilike(term)))
    base = select(VehicleScan, UserProfile.name, Vehicle).join(UserProfile, UserProfile.id == VehicleScan.user_id).outerjoin(Vehicle, Vehicle.id == VehicleScan.vehicle_id).where(*filters)
    count_query = select(func.count(VehicleScan.id)).join(UserProfile, UserProfile.id == VehicleScan.user_id).outerjoin(Vehicle, Vehicle.id == VehicleScan.vehicle_id).where(*filters)
    total = await db.scalar(count_query) or 0
    rows = (await db.execute(base.order_by(VehicleScan.created_at.desc()).limit(limit).offset(offset))).all()
    return {"items": [scan_payload(scan, name, vehicle) for scan, name, vehicle in rows], "total": total, "limit": limit, "offset": offset}


@router.get("/users")
async def users(_: Admin, db: DB, q: str = Query("", max_length=120), limit: int = Query(50, ge=1, le=100), offset: int = Query(0, ge=0)) -> dict:
    auth_users = [user for user in await supabase_auth_users() if not is_admin_auth_user(user)]
    auth_ids = [UUID(user["id"]) for user in auth_users if user.get("id")]
    profiles = (await db.scalars(select(UserProfile).where(UserProfile.auth_user_id.in_(auth_ids)))).all() if auth_ids else []
    profiles_by_auth_id = {str(profile.auth_user_id): profile for profile in profiles}
    scan_counts = select(VehicleScan.user_id, func.count(VehicleScan.id).label("scan_count")).group_by(VehicleScan.user_id).subquery()
    count_rows = (await db.execute(select(UserProfile.auth_user_id, func.coalesce(scan_counts.c.scan_count, 0)).outerjoin(scan_counts, scan_counts.c.user_id == UserProfile.id))).all()
    counts_by_auth_id = {str(auth_user_id): int(scan_count) for auth_user_id, scan_count in count_rows}
    items = []
    for auth_user in auth_users:
        auth = auth_user_payload(auth_user)
        profile = profiles_by_auth_id.get(str(auth_user.get("id")))
        metadata = auth["user_metadata"]
        items.append({
            **auth,
            "id": str(profile.id) if profile else None,
            "name": profile.name if profile else metadata.get("full_name") or metadata.get("name"),
            "avatar_url": profile.avatar_url if profile else metadata.get("avatar_url") or metadata.get("picture"),
            "profile_created_at": profile.created_at if profile else None,
            "scan_count": counts_by_auth_id.get(str(auth_user.get("id")), 0),
        })
    needle = q.strip().lower()
    if needle:
        items = [item for item in items if needle in f"{item.get('name') or ''} {item.get('email') or ''} {item.get('phone') or ''}".lower()]
    items.sort(key=lambda item: item.get("created_at") or "", reverse=True)
    total = len(items)
    return {
        "items": items[offset:offset + limit],
        "total": total, "limit": limit, "offset": offset,
    }


@router.get("/users/{auth_user_id}")
async def user_detail(auth_user_id: UUID, _: Admin, db: DB) -> dict:
    auth_users = await supabase_auth_users()
    auth_user = next((user for user in auth_users if user.get("id") == str(auth_user_id)), None)
    if auth_user is None or is_admin_auth_user(auth_user):
        raise AppError(404, "user_not_found", "User was not found")

    auth = auth_user_payload(auth_user)
    profile = await db.scalar(select(UserProfile).where(UserProfile.auth_user_id == auth_user_id))
    scans: list[dict[str, Any]] = []
    favorites: list[dict[str, Any]] = []
    garage: list[dict[str, Any]] = []
    ai_requests: list[dict[str, Any]] = []
    if profile:
        scan_rows = (await db.execute(
            select(VehicleScan, Vehicle).outerjoin(Vehicle, Vehicle.id == VehicleScan.vehicle_id)
            .where(VehicleScan.user_id == profile.id).order_by(VehicleScan.created_at.desc())
        )).all()
        scans = [scan_payload(scan, profile.name, vehicle) for scan, vehicle in scan_rows]
        scan_ids = [scan.id for scan, _ in scan_rows]
        if scan_ids:
            request_rows = (await db.scalars(select(AIRequest).where(AIRequest.scan_id.in_(scan_ids)).order_by(AIRequest.created_at.desc()))).all()
            ai_requests = [{
                "id": str(request.id), "scan_id": str(request.scan_id), "provider": request.provider,
                "model": request.model, "status": request.status, "latency_ms": request.latency_ms,
                "error_code": request.error_code, "created_at": request.created_at,
            } for request in request_rows]
        favorite_rows = (await db.execute(
            select(Favorite, Vehicle).join(Vehicle, Vehicle.id == Favorite.vehicle_id)
            .where(Favorite.user_id == profile.id).order_by(Favorite.created_at.desc())
        )).all()
        favorites = [{
            "id": str(favorite.id), "created_at": favorite.created_at,
            "vehicle": {"id": str(vehicle.id), "brand": vehicle.brand, "model": vehicle.model, "year": vehicle.year},
        } for favorite, vehicle in favorite_rows]
        garage_rows = (await db.execute(
            select(GarageEntry, Vehicle).join(Vehicle, Vehicle.id == GarageEntry.vehicle_id)
            .where(GarageEntry.user_id == profile.id).order_by(GarageEntry.captured_at.desc())
        )).all()
        garage = [{
            "id": str(entry.id), "scan_id": str(entry.scan_id) if entry.scan_id else None,
            "captured_at": entry.captured_at, "xp_earned": entry.xp_earned, "removed_at": entry.removed_at,
            "vehicle": {"id": str(vehicle.id), "brand": vehicle.brand, "model": vehicle.model, "year": vehicle.year},
        } for entry, vehicle in garage_rows]

    metadata = auth["user_metadata"]
    return {
        **auth,
        "profile": {
            "id": str(profile.id) if profile else None,
            "name": profile.name if profile else metadata.get("full_name") or metadata.get("name"),
            "avatar_url": profile.avatar_url if profile else metadata.get("avatar_url") or metadata.get("picture"),
            "settings": profile.settings if profile else {},
            "created_at": profile.created_at if profile else None,
            "updated_at": profile.updated_at if profile else None,
        },
        "summary": {"scans": len(scans), "favorites": len(favorites), "garage_entries": len(garage), "ai_requests": len(ai_requests)},
        "scans": scans, "favorites": favorites, "garage": garage, "ai_requests": ai_requests,
    }


@router.get("/vehicles")
async def vehicles(_: Admin, db: DB, q: str = Query("", max_length=120), limit: int = Query(50, ge=1, le=100), offset: int = Query(0, ge=0)) -> dict:
    filters = [or_(Vehicle.brand.ilike(f"%{q.strip()}%"), Vehicle.model.ilike(f"%{q.strip()}%"))] if q.strip() else []
    query = select(Vehicle).where(*filters).order_by(Vehicle.created_at.desc()).limit(limit).offset(offset)
    total = await db.scalar(select(func.count(Vehicle.id)).where(*filters)) or 0
    rows = (await db.scalars(query)).all()
    return {
        "items": [{"id": str(vehicle.id), "brand": vehicle.brand, "model": vehicle.model, "generation": vehicle.generation, "year": vehicle.year, "vehicle_type": vehicle.vehicle_type, "image_url": vehicle.image_url, "created_at": vehicle.created_at} for vehicle in rows],
        "total": total, "limit": limit, "offset": offset,
    }

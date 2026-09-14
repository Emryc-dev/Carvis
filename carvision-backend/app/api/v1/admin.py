from datetime import datetime, timedelta, timezone

from fastapi import APIRouter, Query
from sqlalchemy import String, cast, func, or_, select

from app.dependencies.auth import Admin, DB
from app.models.entities import AIRequest, Favorite, UserProfile, Vehicle, VehicleScan

router = APIRouter(prefix="/admin", tags=["admin"])


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
    user_count = await db.scalar(select(func.count(UserProfile.id))) or 0
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
    scan_counts = select(VehicleScan.user_id, func.count(VehicleScan.id).label("scan_count")).group_by(VehicleScan.user_id).subquery()
    filters = [UserProfile.name.ilike(f"%{q.strip()}%")] if q.strip() else []
    query = select(UserProfile, func.coalesce(scan_counts.c.scan_count, 0)).outerjoin(scan_counts, scan_counts.c.user_id == UserProfile.id).where(*filters)
    total = await db.scalar(select(func.count(UserProfile.id)).where(*filters)) or 0
    rows = (await db.execute(query.order_by(UserProfile.created_at.desc()).limit(limit).offset(offset))).all()
    return {
        "items": [{"id": str(user.id), "auth_user_id": str(user.auth_user_id), "name": user.name, "avatar_url": user.avatar_url, "created_at": user.created_at, "scan_count": scan_count} for user, scan_count in rows],
        "total": total, "limit": limit, "offset": offset,
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

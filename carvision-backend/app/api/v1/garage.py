import uuid
from datetime import datetime, timezone

from fastapi import APIRouter, Query, Response

from app.core.config import get_settings
from app.core.errors import AppError
from app.dependencies.auth import DB, Profile
from app.repositories.garage import garage_totals, get_by_vehicle, get_entry, list_entries
from app.schemas.domain import (
    GarageCheckRead, GarageCreate, GarageEntryRead, GarageListRead, GarageStatsRead, GarageVehicleRead,
)
from app.services.garage_service import GarageService
from app.services.storage_service import StorageService
from app.services.xp_service import XPService

router = APIRouter(prefix="/garage", tags=["garage"])
storage = StorageService(get_settings())
xp_service = XPService()
service = GarageService(xp_service)


async def present(entry, *, awarded: int = 0, already: bool = False) -> GarageEntryRead:
    image_url = await storage.signed_url(entry.captured_image_path) if entry.captured_image_path else entry.vehicle.image_url
    return GarageEntryRead(
        id=entry.id,
        vehicle=GarageVehicleRead.model_validate(entry.vehicle, from_attributes=True),
        rarity=entry.vehicle.rarity,
        xp_earned=entry.xp_earned,
        captured_at=entry.captured_at,
        captured_image_url=image_url,
        newly_awarded_xp=awarded,
        already_collected=already,
    )


@router.get("", response_model=GarageListRead)
async def garage_list(profile: Profile, db: DB, limit: int = Query(20, ge=1, le=100), offset: int = Query(0, ge=0)) -> GarageListRead:
    rows, total = await list_entries(db, profile, limit, offset)
    return GarageListRead(items=[await present(row) for row in rows], total=total, limit=limit, offset=offset)


@router.get("/stats", response_model=GarageStatsRead)
async def garage_stats(profile: Profile, db: DB) -> GarageStatsRead:
    count, total_xp = await garage_totals(db, profile)
    level, next_level_xp, progress = xp_service.level(total_xp)
    return GarageStatsRead(cars_collected=count, total_xp=total_xp, level=level, next_level_xp=next_level_xp, progress_to_next_level=progress)


@router.get("/check/{vehicle_id}", response_model=GarageCheckRead)
async def garage_check(vehicle_id: uuid.UUID, profile: Profile, db: DB) -> GarageCheckRead:
    entry = await get_by_vehicle(db, vehicle_id, profile)
    return GarageCheckRead(collected=entry is not None, entry_id=entry.id if entry else None)


@router.get("/{entry_id}", response_model=GarageEntryRead)
async def garage_detail(entry_id: uuid.UUID, profile: Profile, db: DB) -> GarageEntryRead:
    entry = await get_entry(db, entry_id, profile)
    if entry is None:
        raise AppError(404, "garage_entry_not_found", "Garage entry was not found")
    return await present(entry)


@router.post("", response_model=GarageEntryRead, status_code=201)
async def garage_add(body: GarageCreate, profile: Profile, db: DB) -> GarageEntryRead:
    entry, awarded, already = await service.add(db, profile, body.vehicle_id, body.scan_id)
    return await present(entry, awarded=awarded, already=already)


@router.delete("/{entry_id}", status_code=204)
async def garage_remove(entry_id: uuid.UUID, profile: Profile, db: DB) -> Response:
    entry = await get_entry(db, entry_id, profile)
    if entry is None:
        raise AppError(404, "garage_entry_not_found", "Garage entry was not found")
    entry.removed_at = datetime.now(timezone.utc)
    await db.commit()
    return Response(status_code=204)

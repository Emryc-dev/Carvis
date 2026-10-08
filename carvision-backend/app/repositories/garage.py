import uuid

from sqlalchemy import func, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.entities import GarageEntry, UserProfile, Vehicle, VehicleScan


def garage_options():
    return selectinload(GarageEntry.vehicle)


async def get_entry(db: AsyncSession, entry_id: uuid.UUID, profile: UserProfile, *, include_removed: bool = False) -> GarageEntry | None:
    query = select(GarageEntry).options(garage_options()).where(GarageEntry.id == entry_id, GarageEntry.user_id == profile.id)
    if not include_removed:
        query = query.where(GarageEntry.removed_at.is_(None))
    return await db.scalar(query)


async def get_by_vehicle(db: AsyncSession, vehicle_id: uuid.UUID, profile: UserProfile, *, include_removed: bool = False) -> GarageEntry | None:
    query = select(GarageEntry).options(garage_options()).where(GarageEntry.vehicle_id == vehicle_id, GarageEntry.user_id == profile.id)
    if not include_removed:
        query = query.where(GarageEntry.removed_at.is_(None))
    return await db.scalar(query)


async def list_entries(db: AsyncSession, profile: UserProfile, limit: int, offset: int) -> tuple[list[GarageEntry], int]:
    active = (GarageEntry.user_id == profile.id, GarageEntry.removed_at.is_(None))
    total = await db.scalar(select(func.count(GarageEntry.id)).where(*active)) or 0
    rows = (await db.scalars(
        select(GarageEntry).options(garage_options()).where(*active)
        .order_by(GarageEntry.captured_at.desc()).limit(limit).offset(offset)
    )).all()
    return list(rows), total


async def owned_scan_for_vehicle(db: AsyncSession, scan_id: uuid.UUID, vehicle_id: uuid.UUID, profile: UserProfile) -> VehicleScan | None:
    return await db.scalar(select(VehicleScan).where(
        VehicleScan.id == scan_id,
        VehicleScan.user_id == profile.id,
        VehicleScan.vehicle_id == vehicle_id,
    ))


async def garage_totals(db: AsyncSession, profile: UserProfile) -> tuple[int, int]:
    count = await db.scalar(select(func.count(GarageEntry.id)).where(
        GarageEntry.user_id == profile.id, GarageEntry.removed_at.is_(None)
    )) or 0
    # XP is a permanent discovery award. Soft deletion cannot reset it for farming.
    xp = await db.scalar(select(func.coalesce(func.sum(GarageEntry.xp_earned), 0)).where(GarageEntry.user_id == profile.id)) or 0
    return int(count), int(xp)

import uuid

from sqlalchemy import func, or_, select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.entities import Favorite, UserProfile, Vehicle, VehicleScan


async def get_vehicle(db: AsyncSession, vehicle_id: uuid.UUID) -> Vehicle | None:
    return await db.scalar(select(Vehicle).options(selectinload(Vehicle.specifications)).where(Vehicle.id == vehicle_id))


async def find_vehicle_match(db: AsyncSession, brand: str, model: str, year: int | None) -> Vehicle | None:
    query = select(Vehicle).options(selectinload(Vehicle.specifications)).where(
        func.lower(Vehicle.brand) == brand.lower(), func.lower(Vehicle.model) == model.lower()
    )
    if year is not None:
        query = query.where(or_(Vehicle.year == year, Vehicle.year.is_(None)))
    return await db.scalar(query.limit(1))


async def owned_scan(db: AsyncSession, scan_id: uuid.UUID, profile: UserProfile) -> VehicleScan | None:
    return await db.scalar(select(VehicleScan).options(selectinload(VehicleScan.vehicle).selectinload(Vehicle.specifications)).where(VehicleScan.id == scan_id, VehicleScan.user_id == profile.id))

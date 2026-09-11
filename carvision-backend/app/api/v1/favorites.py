import uuid

from fastapi import APIRouter, Response
from sqlalchemy import delete, select
from sqlalchemy.orm import selectinload

from app.core.errors import AppError
from app.dependencies.auth import DB, Profile
from app.models.entities import Favorite, Vehicle
from app.repositories.catalog import get_vehicle
from app.schemas.domain import VehicleRead

router = APIRouter(prefix="/favorites", tags=["favorites"])


@router.get("", response_model=list[VehicleRead])
async def list_favorites(profile: Profile, db: DB) -> list[VehicleRead]:
    rows = (await db.scalars(select(Favorite).options(selectinload(Favorite.vehicle).selectinload(Vehicle.specifications)).where(Favorite.user_id == profile.id).order_by(Favorite.created_at.desc()))).all()
    return [VehicleRead.model_validate(row.vehicle) for row in rows]


@router.put("/{vehicle_id}", response_model=VehicleRead)
async def add_favorite(vehicle_id: uuid.UUID, profile: Profile, db: DB) -> VehicleRead:
    vehicle = await get_vehicle(db, vehicle_id)
    if not vehicle: raise AppError(404, "vehicle_not_found", "Vehicle was not found")
    exists = await db.scalar(select(Favorite).where(Favorite.user_id == profile.id, Favorite.vehicle_id == vehicle_id))
    if not exists:
        db.add(Favorite(user_id=profile.id, vehicle_id=vehicle_id)); await db.commit()
    return VehicleRead.model_validate(vehicle)


@router.delete("/{vehicle_id}", status_code=204)
async def remove_favorite(vehicle_id: uuid.UUID, profile: Profile, db: DB) -> Response:
    await db.execute(delete(Favorite).where(Favorite.user_id == profile.id, Favorite.vehicle_id == vehicle_id)); await db.commit()
    return Response(status_code=204)

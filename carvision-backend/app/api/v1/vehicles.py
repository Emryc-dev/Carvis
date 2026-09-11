import uuid

from fastapi import APIRouter, Query
from sqlalchemy import func, or_, select
from sqlalchemy.orm import selectinload

from app.core.errors import AppError
from app.dependencies.auth import DB, Profile
from app.models.entities import Vehicle
from app.repositories.catalog import get_vehicle
from app.schemas.domain import VehicleRead

router = APIRouter(prefix="/vehicles", tags=["vehicles"])


@router.get("", response_model=dict)
async def list_vehicles(profile: Profile, db: DB, q: str | None = None, limit: int = Query(20, ge=1, le=100), offset: int = Query(0, ge=0)) -> dict:
    filters = []
    if q:
        pattern = f"%{q.strip()}%"
        filters.append(or_(Vehicle.brand.ilike(pattern), Vehicle.model.ilike(pattern), Vehicle.generation.ilike(pattern)))
    total = await db.scalar(select(func.count(Vehicle.id)).where(*filters))
    rows = (await db.scalars(select(Vehicle).options(selectinload(Vehicle.specifications)).where(*filters).order_by(Vehicle.brand, Vehicle.model).limit(limit).offset(offset))).all()
    return {"items": [VehicleRead.model_validate(v) for v in rows], "total": total or 0, "limit": limit, "offset": offset}


@router.get("/compare", response_model=list[VehicleRead])
async def compare_vehicles(profile: Profile, db: DB, ids: list[uuid.UUID] = Query(min_length=2, max_length=4)) -> list[VehicleRead]:
    rows = (await db.scalars(select(Vehicle).options(selectinload(Vehicle.specifications)).where(Vehicle.id.in_(ids)))).all()
    by_id = {row.id: row for row in rows}
    if len(by_id) != len(set(ids)): raise AppError(404, "vehicle_not_found", "One or more vehicles were not found")
    return [VehicleRead.model_validate(by_id[item]) for item in ids]


@router.get("/{vehicle_id}", response_model=VehicleRead)
async def vehicle_detail(vehicle_id: uuid.UUID, profile: Profile, db: DB) -> VehicleRead:
    vehicle = await get_vehicle(db, vehicle_id)
    if not vehicle: raise AppError(404, "vehicle_not_found", "Vehicle was not found")
    return VehicleRead.model_validate(vehicle)

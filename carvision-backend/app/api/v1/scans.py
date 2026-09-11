import uuid

from fastapi import APIRouter, File, Form, Query, Response, UploadFile
from sqlalchemy import delete, func, select
from sqlalchemy.orm import selectinload

from app.core.config import get_settings
from app.core.errors import AppError
from app.dependencies.auth import DB, Profile
from app.models.entities import Vehicle, VehicleScan
from app.repositories.catalog import owned_scan
from app.schemas.domain import ScanRead
from app.services.ai.gemini import GeminiService
from app.services.image_service import validate_image
from app.services.redis_service import RedisService
from app.services.scan_service import ScanService
from app.services.storage_service import StorageService

router = APIRouter(prefix="/scans", tags=["scans"])
settings = get_settings()
storage = StorageService(settings)
service = ScanService(settings, GeminiService(settings), RedisService(settings), storage)


async def present(scan: VehicleScan) -> ScanRead:
    dto = ScanRead.model_validate(scan)
    dto.image_url = await storage.signed_url(scan.image_path) if scan.image_path else None
    return dto


@router.post("", response_model=ScanRead, status_code=201)
async def create_scan(profile: Profile, db: DB, image: UploadFile = File(...), context: str | None = Form(None, max_length=500)) -> ScanRead:
    content = await image.read(settings.max_image_bytes + 1)
    valid = validate_image(content, image.content_type, settings)
    scan = await service.create(db, profile, valid, context)
    loaded = await owned_scan(db, scan.id, profile)
    return await present(loaded or scan)


@router.get("", response_model=dict)
async def list_scans(profile: Profile, db: DB, limit: int = Query(20, ge=1, le=100), offset: int = Query(0, ge=0)) -> dict:
    total = await db.scalar(select(func.count(VehicleScan.id)).where(VehicleScan.user_id == profile.id))
    rows = (await db.scalars(select(VehicleScan).options(selectinload(VehicleScan.vehicle).selectinload(Vehicle.specifications)).where(VehicleScan.user_id == profile.id).order_by(VehicleScan.created_at.desc()).limit(limit).offset(offset))).all()
    return {"items": [await present(row) for row in rows], "total": total or 0, "limit": limit, "offset": offset}


@router.get("/{scan_id}", response_model=ScanRead)
async def scan_detail(scan_id: uuid.UUID, profile: Profile, db: DB) -> ScanRead:
    scan = await owned_scan(db, scan_id, profile)
    if not scan: raise AppError(404, "scan_not_found", "Scan was not found")
    return await present(scan)


@router.delete("/{scan_id}", status_code=204)
async def delete_scan(scan_id: uuid.UUID, profile: Profile, db: DB) -> Response:
    scan = await owned_scan(db, scan_id, profile)
    if not scan: raise AppError(404, "scan_not_found", "Scan was not found")
    if scan.image_path: await storage.delete([scan.image_path])
    await db.delete(scan); await db.commit()
    return Response(status_code=204)

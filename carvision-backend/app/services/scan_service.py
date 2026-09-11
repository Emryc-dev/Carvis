import time
import uuid
from datetime import datetime, timezone

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import Settings
from app.core.errors import AppError
from app.models.entities import AIRequest, ScanStatus, UserProfile, VehicleScan
from app.repositories.catalog import find_vehicle_match
from app.schemas.ai import AIIdentification
from app.services.ai.base import AIService
from app.services.image_service import ValidImage
from app.services.redis_service import RedisService
from app.services.storage_service import StorageService


class ScanService:
    def __init__(self, settings: Settings, ai: AIService, cache: RedisService, storage: StorageService):
        self.settings, self.ai, self.cache, self.storage = settings, ai, cache, storage

    async def create(self, db: AsyncSession, user: UserProfile, image: ValidImage, context: str | None) -> VehicleScan:
        scan_id = uuid.uuid4()
        path = f"{user.auth_user_id}/{scan_id}/original.{image.extension}"
        await self.storage.upload(path, image.content, image.mime_type)
        scan = VehicleScan(id=scan_id, user_id=user.id, status=ScanStatus.processing, image_path=path,
                           image_hash=image.sha256, mime_type=image.mime_type, width=image.width, height=image.height)
        db.add(scan)
        await db.flush()
        cache_key = f"ai_result:v1:{image.sha256}"
        cached = await self.cache.get_json(cache_key)
        started = time.perf_counter()
        request = AIRequest(scan_id=scan.id, provider="cache" if cached else "gemini", model="sha256" if cached else self.settings.gemini_model, status="started")
        db.add(request)
        try:
            result = AIIdentification.model_validate(cached) if cached else await self.ai.identify_vehicle(image.content, image.mime_type, context)
            request.status = "completed"
            request.latency_ms = int((time.perf_counter() - started) * 1000)
            request.response = result.model_dump(mode="json")
            vehicle = await find_vehicle_match(db, result.brand, result.model, result.approximate_year)
            scan.vehicle_id = vehicle.id if vehicle else None
            scan.identification = result.model_dump(mode="json")
            scan.confidence = result.confidence
            scan.status = ScanStatus.completed
            scan.completed_at = datetime.now(timezone.utc)
            if not cached:
                await self.cache.set_json(cache_key, scan.identification, 7 * 24 * 3600)
            await db.commit()
            return scan
        except AppError as exc:
            request.status, request.error_code = "failed", exc.code
            request.latency_ms = int((time.perf_counter() - started) * 1000)
            scan.status, scan.error_code = ScanStatus.failed, exc.code
            scan.completed_at = datetime.now(timezone.utc)
            await db.commit()
            raise

import time
import uuid
from datetime import datetime, timezone

from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import Settings
from app.core.errors import AppError
from app.models.entities import AIRequest, ScanStatus, UserProfile, VehicleScan
from app.repositories.catalog import enrich_catalogue_from_ai, find_vehicle_match
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
        started = time.perf_counter()
        request = AIRequest(scan_id=scan.id, provider=self.settings.recognition_engine, model=self.settings.ml_model_version if self.settings.recognition_engine != "gemini" else self.settings.gemini_model, status="started")
        db.add(request)
        try:
            result, recognition = await self._identify(db, image, context, str(scan.id))
            request.status = "completed"
            request.latency_ms = int((time.perf_counter() - started) * 1000)
            request.response = recognition if recognition is not None else result.model_dump(mode="json") if result else None
            vehicle = await find_vehicle_match(db, result.brand, result.model, result.approximate_year) if result else None
            if result and vehicle is None and result.confidence >= self.settings.catalogue_enrichment_min_confidence:
                vehicle = await enrich_catalogue_from_ai(db, result)
            scan.vehicle_id = vehicle.id if vehicle else None
            scan.identification = result.model_dump(mode="json") if result else None
            scan.confidence = result.confidence if result else (recognition or {}).get("final_confidence")
            scan.recognition_metadata = recognition
            scan.status = ScanStatus.completed
            scan.completed_at = datetime.now(timezone.utc)
            await db.commit()
            return scan
        except AppError as exc:
            request.status, request.error_code = "failed", exc.code
            request.latency_ms = int((time.perf_counter() - started) * 1000)
            scan.status, scan.error_code = ScanStatus.failed, exc.code
            scan.completed_at = datetime.now(timezone.utc)
            await db.commit()
            raise

    async def _identify(self, db: AsyncSession, image: ValidImage, context: str | None, scan_id: str) -> tuple[AIIdentification | None, dict | None]:
        """Use Gemini until the separately developed ML package is deployed."""
        if self.settings.recognition_engine != "gemini":
            raise AppError(
                503,
                "recognition_engine_not_deployed",
                "This deployment supports Gemini recognition only.",
            )
        result = await self._gemini_cached(image, context)
        return result, {"engine": "gemini"}

    async def _gemini_cached(self, image: ValidImage, context: str | None) -> AIIdentification:
        cache_key = f"ai_result:v1:{image.sha256}"
        cached = await self.cache.get_json(cache_key)
        result = AIIdentification.model_validate(cached) if cached else await self.ai.identify_vehicle(image.content, image.mime_type, context)
        if not cached:
            await self.cache.set_json(cache_key, result.model_dump(mode="json"), 7 * 24 * 3600)
        return result

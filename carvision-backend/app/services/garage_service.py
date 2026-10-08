from datetime import datetime, timezone
import uuid

from sqlalchemy import select
from sqlalchemy.exc import IntegrityError
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.errors import AppError
from app.models.entities import GarageEntry, UserProfile, Vehicle
from app.repositories.garage import get_by_vehicle, owned_scan_for_vehicle
from app.services.xp_service import XPService


class GarageService:
    def __init__(self, xp: XPService | None = None):
        self.xp = xp or XPService()

    async def add(
        self, db: AsyncSession, profile: UserProfile, vehicle_id: uuid.UUID, scan_id: uuid.UUID | None
    ) -> tuple[GarageEntry, int, bool]:
        vehicle = await db.scalar(select(Vehicle).where(Vehicle.id == vehicle_id))
        if vehicle is None:
            raise AppError(404, "vehicle_not_found", "Vehicle was not found")

        scan = None
        if scan_id is not None:
            scan = await owned_scan_for_vehicle(db, scan_id, vehicle_id, profile)
            if scan is None:
                raise AppError(404, "scan_not_found", "A matching scan owned by this user was not found")

        existing = await get_by_vehicle(db, vehicle_id, profile, include_removed=True)
        if existing is not None:
            already_active = existing.removed_at is None
            if not already_active:
                existing.removed_at = None
                existing.captured_at = scan.completed_at or scan.created_at if scan else datetime.now(timezone.utc)
                existing.scan_id = scan.id if scan else existing.scan_id
                existing.captured_image_path = scan.image_path if scan else existing.captured_image_path
                await db.commit()
            return existing, 0, already_active

        award = self.xp.calculate(vehicle).total
        entry = GarageEntry(
            user_id=profile.id,
            vehicle_id=vehicle.id,
            scan_id=scan.id if scan else None,
            captured_image_path=scan.image_path if scan else None,
            captured_at=(scan.completed_at or scan.created_at) if scan else datetime.now(timezone.utc),
            xp_earned=award,
        )
        db.add(entry)
        try:
            await db.commit()
        except IntegrityError:
            await db.rollback()
            winner = await get_by_vehicle(db, vehicle_id, profile, include_removed=True)
            if winner is None:
                raise
            return winner, 0, winner.removed_at is None
        await db.refresh(entry)
        entry.vehicle = vehicle
        return entry, award, False

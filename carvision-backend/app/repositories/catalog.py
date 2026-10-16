import uuid
import re
import unicodedata

from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession
from sqlalchemy.orm import selectinload

from app.models.entities import Favorite, UserProfile, Vehicle, VehicleScan, VehicleSpecification, VerificationStatus
from app.schemas.ai import AIIdentification


def _normalise_identity(value: str) -> str:
    """Match harmless case, punctuation, and accent differences from AI output."""
    decomposed = unicodedata.normalize("NFKD", value)
    ascii_value = "".join(char for char in decomposed if not unicodedata.combining(char))
    return re.sub(r"[^a-z0-9]+", " ", ascii_value.lower()).strip()


def _same_model_family(recognised: str, catalogued: str) -> bool:
    return recognised == catalogued or recognised.startswith(f"{catalogued} ") or catalogued.startswith(f"{recognised} ")


async def get_vehicle(db: AsyncSession, vehicle_id: uuid.UUID) -> Vehicle | None:
    return await db.scalar(select(Vehicle).options(selectinload(Vehicle.specifications)).where(Vehicle.id == vehicle_id))


async def find_vehicle_match(db: AsyncSession, brand: str, model: str, year: int | None) -> Vehicle | None:
    recognised_brand = _normalise_identity(brand)
    recognised_model = _normalise_identity(model)
    rows = await db.scalars(select(Vehicle).options(selectinload(Vehicle.specifications)))
    candidates = [
        vehicle for vehicle in rows.unique().all()
        if _normalise_identity(vehicle.brand) == recognised_brand
        and _same_model_family(recognised_model, _normalise_identity(vehicle.model))
    ]
    if not candidates:
        return None

    # Prefer the exact model and the requested year. A variant-to-base-model
    # fallback is accepted only when it maps to one unambiguous catalogue model.
    candidates.sort(
        key=lambda vehicle: (
            _normalise_identity(vehicle.model) == recognised_model,
            year is not None and vehicle.year == year,
            vehicle.year is None,
        ),
        reverse=True,
    )
    identities = {(_normalise_identity(vehicle.brand), _normalise_identity(vehicle.model)) for vehicle in candidates}
    best = candidates[0]
    return best if len(identities) == 1 or _normalise_identity(best.model) == recognised_model else None


async def enrich_catalogue_from_ai(db: AsyncSession, identification: AIIdentification) -> Vehicle:
    """Create one explicitly AI-inferred catalogue entry when no match exists.

    This preserves the useful identification while making its provenance visible
    to admins; it does not claim that Gemini-supplied specifications are verified.
    """
    existing = await find_vehicle_match(
        db,
        identification.brand,
        identification.model,
        identification.approximate_year,
    )
    if existing:
        return existing

    vehicle = Vehicle(
        brand=identification.brand.strip(),
        model=identification.model.strip(),
        generation=identification.generation,
        year=identification.approximate_year,
        vehicle_type=identification.vehicle_type,
    )
    vehicle.specifications.append(
        VehicleSpecification(
            engine=identification.engine,
            horsepower=identification.horsepower,
            torque=identification.torque,
            transmission=identification.transmission,
            drivetrain=identification.drivetrain,
            fuel_type=identification.fuel_type,
            consumption=identification.consumption,
            source_name="Gemini AI (inferred from scan)",
            verification_status=VerificationStatus.ai_inferred,
            extra={
                "confidence": identification.confidence,
                "visual_evidence": identification.visual_evidence,
                "uncertainties": identification.uncertainties,
            },
        )
    )
    db.add(vehicle)
    await db.flush()
    return vehicle


async def owned_scan(db: AsyncSession, scan_id: uuid.UUID, profile: UserProfile) -> VehicleScan | None:
    return await db.scalar(select(VehicleScan).options(selectinload(VehicleScan.vehicle).selectinload(Vehicle.specifications)).where(VehicleScan.id == scan_id, VehicleScan.user_id == profile.id))

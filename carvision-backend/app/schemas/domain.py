import uuid
from datetime import datetime

from pydantic import BaseModel, ConfigDict, Field

from app.models.entities import ScanStatus, VehicleRarity, VerificationStatus
from app.schemas.ai import AIIdentification


class UserRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: uuid.UUID
    auth_user_id: uuid.UUID
    name: str | None
    avatar_url: str | None
    settings: dict
    created_at: datetime
    updated_at: datetime


class UserUpdate(BaseModel):
    name: str | None = Field(None, min_length=1, max_length=120)
    avatar_url: str | None = Field(None, max_length=2048)
    settings: dict | None = None


class SpecificationRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    engine: str | None
    horsepower: int | None
    torque: str | None
    transmission: str | None
    drivetrain: str | None
    fuel_type: str | None
    consumption: str | None
    zero_to_sixty: float | None
    top_speed_mph: int | None
    curb_weight_lbs: int | None
    source_name: str | None
    source_url: str | None
    verification_status: VerificationStatus
    extra: dict


class VehicleRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: uuid.UUID
    brand: str
    model: str
    generation: str | None
    year: int | None
    vehicle_type: str | None
    image_url: str | None
    rarity: VehicleRarity
    base_xp: int
    rarity_score: int
    market_value: int | None
    specifications: list[SpecificationRead] = []


class ScanRead(BaseModel):
    model_config = ConfigDict(from_attributes=True)
    id: uuid.UUID
    status: ScanStatus
    image_hash: str
    mime_type: str
    width: int
    height: int
    identification: AIIdentification | None
    confidence: float | None
    vehicle: VehicleRead | None
    created_at: datetime
    completed_at: datetime | None
    image_url: str | None = None
    collection_xp: int | None = None


class GarageCreate(BaseModel):
    model_config = ConfigDict(extra="forbid")
    vehicle_id: uuid.UUID
    scan_id: uuid.UUID | None = None


class GarageVehicleRead(BaseModel):
    id: uuid.UUID
    brand: str
    model: str
    generation: str | None
    year: int | None
    image_url: str | None


class GarageEntryRead(BaseModel):
    id: uuid.UUID
    vehicle: GarageVehicleRead
    rarity: VehicleRarity
    xp_earned: int
    captured_at: datetime
    captured_image_url: str | None = None
    newly_awarded_xp: int = 0
    already_collected: bool = False


class GarageListRead(BaseModel):
    items: list[GarageEntryRead]
    total: int
    limit: int
    offset: int


class GarageStatsRead(BaseModel):
    cars_collected: int
    total_xp: int
    level: int
    next_level_xp: int | None
    progress_to_next_level: float


class GarageCheckRead(BaseModel):
    collected: bool
    entry_id: uuid.UUID | None = None

import enum
import uuid
from datetime import datetime

from sqlalchemy import Boolean, DateTime, Enum, Float, ForeignKey, Index, Integer, JSON, String, Text, UniqueConstraint, func
from sqlalchemy.orm import Mapped, mapped_column, relationship

from app.database.base import Base


class ScanStatus(str, enum.Enum):
    processing = "processing"
    completed = "completed"
    failed = "failed"


class VerificationStatus(str, enum.Enum):
    ai_inferred = "ai_inferred"
    partially_verified = "partially_verified"
    verified = "verified"


class VehicleRarity(str, enum.Enum):
    common = "COMMON"
    uncommon = "UNCOMMON"
    rare = "RARE"
    epic = "EPIC"
    mythic = "MYTHIC"


class UserProfile(Base):
    __tablename__ = "user_profiles"
    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    auth_user_id: Mapped[uuid.UUID] = mapped_column(unique=True, index=True)
    name: Mapped[str | None] = mapped_column(String(120))
    avatar_url: Mapped[str | None] = mapped_column(Text)
    settings: Mapped[dict] = mapped_column(JSON, default=dict)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    updated_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now(), onupdate=func.now())
    scans: Mapped[list["VehicleScan"]] = relationship(back_populates="user", cascade="all, delete-orphan")
    garage_entries: Mapped[list["GarageEntry"]] = relationship(back_populates="user", cascade="all, delete-orphan")


class Vehicle(Base):
    __tablename__ = "vehicles"
    __table_args__ = (Index("ix_vehicle_identity", "brand", "model", "generation", "year"),)
    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    brand: Mapped[str] = mapped_column(String(80), index=True)
    model: Mapped[str] = mapped_column(String(120), index=True)
    generation: Mapped[str | None] = mapped_column(String(80))
    year: Mapped[int | None] = mapped_column(Integer)
    vehicle_type: Mapped[str | None] = mapped_column(String(80))
    image_url: Mapped[str | None] = mapped_column(Text)
    rarity: Mapped[VehicleRarity] = mapped_column(
        Enum(VehicleRarity, values_callable=lambda items: [item.value for item in items]),
        default=VehicleRarity.common,
        server_default=VehicleRarity.common.value,
    )
    base_xp: Mapped[int] = mapped_column(Integer, default=50, server_default="50")
    rarity_score: Mapped[int] = mapped_column(Integer, default=0, server_default="0")
    market_value: Mapped[int | None] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    specifications: Mapped[list["VehicleSpecification"]] = relationship(back_populates="vehicle", cascade="all, delete-orphan")


class VehicleSpecification(Base):
    __tablename__ = "vehicle_specifications"
    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    vehicle_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("vehicles.id", ondelete="CASCADE"), index=True)
    engine: Mapped[str | None] = mapped_column(String(200))
    horsepower: Mapped[int | None]
    torque: Mapped[str | None] = mapped_column(String(100))
    transmission: Mapped[str | None] = mapped_column(String(160))
    drivetrain: Mapped[str | None] = mapped_column(String(120))
    fuel_type: Mapped[str | None] = mapped_column(String(80))
    consumption: Mapped[str | None] = mapped_column(String(120))
    zero_to_sixty: Mapped[float | None] = mapped_column(Float)
    top_speed_mph: Mapped[int | None]
    curb_weight_lbs: Mapped[int | None]
    source_name: Mapped[str | None] = mapped_column(String(160))
    source_url: Mapped[str | None] = mapped_column(Text)
    verification_status: Mapped[VerificationStatus] = mapped_column(Enum(VerificationStatus), default=VerificationStatus.ai_inferred)
    extra: Mapped[dict] = mapped_column(JSON, default=dict)
    vehicle: Mapped[Vehicle] = relationship(back_populates="specifications")


class VehicleScan(Base):
    __tablename__ = "vehicle_scans"
    __table_args__ = (Index("ix_scan_user_created", "user_id", "created_at"),)
    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("user_profiles.id", ondelete="CASCADE"), index=True)
    vehicle_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("vehicles.id", ondelete="SET NULL"), index=True)
    status: Mapped[ScanStatus] = mapped_column(Enum(ScanStatus), default=ScanStatus.processing)
    image_path: Mapped[str | None] = mapped_column(Text)
    image_hash: Mapped[str] = mapped_column(String(64), index=True)
    mime_type: Mapped[str] = mapped_column(String(40))
    width: Mapped[int]
    height: Mapped[int]
    identification: Mapped[dict | None] = mapped_column(JSON)
    confidence: Mapped[float | None] = mapped_column(Float)
    error_code: Mapped[str | None] = mapped_column(String(80))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    completed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    user: Mapped[UserProfile] = relationship(back_populates="scans")
    vehicle: Mapped[Vehicle | None] = relationship()
    ai_requests: Mapped[list["AIRequest"]] = relationship(back_populates="scan", cascade="all, delete-orphan")


class AIRequest(Base):
    __tablename__ = "ai_requests"
    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    scan_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("vehicle_scans.id", ondelete="CASCADE"), index=True)
    provider: Mapped[str] = mapped_column(String(40))
    model: Mapped[str] = mapped_column(String(100))
    status: Mapped[str] = mapped_column(String(30))
    latency_ms: Mapped[int | None]
    response: Mapped[dict | None] = mapped_column(JSON)
    error_code: Mapped[str | None] = mapped_column(String(80))
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    scan: Mapped[VehicleScan] = relationship(back_populates="ai_requests")


class Favorite(Base):
    __tablename__ = "favorites"
    __table_args__ = (UniqueConstraint("user_id", "vehicle_id"),)
    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("user_profiles.id", ondelete="CASCADE"), index=True)
    vehicle_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("vehicles.id", ondelete="CASCADE"), index=True)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    vehicle: Mapped[Vehicle] = relationship()


class GarageEntry(Base):
    __tablename__ = "garage_entries"
    __table_args__ = (
        UniqueConstraint("user_id", "vehicle_id", name="uq_garage_user_vehicle"),
        Index("ix_garage_user_captured", "user_id", "captured_at"),
    )
    id: Mapped[uuid.UUID] = mapped_column(primary_key=True, default=uuid.uuid4)
    user_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("user_profiles.id", ondelete="CASCADE"), index=True)
    vehicle_id: Mapped[uuid.UUID] = mapped_column(ForeignKey("vehicles.id", ondelete="RESTRICT"), index=True)
    scan_id: Mapped[uuid.UUID | None] = mapped_column(ForeignKey("vehicle_scans.id", ondelete="SET NULL"), index=True)
    captured_image_path: Mapped[str | None] = mapped_column(Text)
    captured_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    xp_earned: Mapped[int] = mapped_column(Integer)
    created_at: Mapped[datetime] = mapped_column(DateTime(timezone=True), server_default=func.now())
    removed_at: Mapped[datetime | None] = mapped_column(DateTime(timezone=True))
    user: Mapped[UserProfile] = relationship(back_populates="garage_entries")
    vehicle: Mapped[Vehicle] = relationship()
    scan: Mapped[VehicleScan | None] = relationship()

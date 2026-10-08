import uuid
from datetime import datetime, timezone

import pytest
from pydantic import ValidationError
from sqlalchemy import select
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from app.core.errors import AppError
from app.database.base import Base
from app.models.entities import GarageEntry, UserProfile, Vehicle, VehicleRarity, VehicleScan, ScanStatus
from app.repositories.garage import garage_totals, get_entry, list_entries
from app.schemas.domain import GarageCreate
from app.services.garage_service import GarageService
from app.services.xp_service import XPPolicy, XPService


@pytest.fixture
async def db():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    sessions = async_sessionmaker(engine, expire_on_commit=False)
    async with sessions() as session:
        yield session
    await engine.dispose()


async def make_catalog(db, rarity=VehicleRarity.epic, base_xp=500, market_value=900_000):
    user = UserProfile(auth_user_id=uuid.uuid4(), name="Collector")
    other = UserProfile(auth_user_id=uuid.uuid4(), name="Other")
    vehicle = Vehicle(brand="BMW", model="M4 Competition", generation="G82", year=2021,
                      rarity=rarity, base_xp=base_xp, rarity_score=82, market_value=market_value)
    db.add_all([user, other, vehicle])
    await db.flush()
    scan = VehicleScan(user_id=user.id, vehicle_id=vehicle.id, status=ScanStatus.completed,
                       image_path=f"{user.auth_user_id}/scan/original.jpg", image_hash="a" * 64,
                       mime_type="image/jpeg", width=1280, height=720, completed_at=datetime.now(timezone.utc))
    db.add(scan)
    await db.commit()
    return user, other, vehicle, scan


def test_xp_rarity_is_dominant_and_level_is_centralized():
    service = XPService()
    common = Vehicle(brand="A", model="A", rarity=VehicleRarity.common, base_xp=50, market_value=20_000_000)
    mythic = Vehicle(brand="B", model="B", rarity=VehicleRarity.mythic, base_xp=1000, market_value=0)
    assert service.calculate(common).total == 60
    assert service.calculate(mythic).total == 1000
    assert service.calculate(common).total < service.calculate(mythic).total
    assert service.level(1_200) == (3, 2_500, 0.0)


def test_xp_policy_is_configurable():
    policy = XPPolicy(rarity_base_xp={rarity: 77 for rarity in VehicleRarity}, market_bonus_cap_ratio=0)
    vehicle = Vehicle(brand="A", model="B", rarity=VehicleRarity.common, base_xp=1, market_value=1_000_000)
    assert XPService(policy).calculate(vehicle).total == 77


@pytest.mark.asyncio
async def test_create_retrieve_and_duplicate_is_idempotent(db):
    user, _, vehicle, scan = await make_catalog(db)
    service = GarageService()
    first, awarded, already = await service.add(db, user, vehicle.id, scan.id)
    duplicate, duplicate_award, duplicate_already = await service.add(db, user, vehicle.id, scan.id)
    assert first.id == duplicate.id
    assert awarded == first.xp_earned > 0
    assert already is False
    assert duplicate_award == 0
    assert duplicate_already is True
    assert len((await db.scalars(select(GarageEntry))).all()) == 1
    assert await garage_totals(db, user) == (1, first.xp_earned)


@pytest.mark.asyncio
async def test_user_isolation_and_owned_scan_validation(db):
    user, other, vehicle, scan = await make_catalog(db)
    entry, _, _ = await GarageService().add(db, user, vehicle.id, scan.id)
    assert await get_entry(db, entry.id, other) is None
    rows, total = await list_entries(db, other, 20, 0)
    assert rows == [] and total == 0
    with pytest.raises(AppError) as exc:
        await GarageService().add(db, other, vehicle.id, scan.id)
    assert exc.value.code == "scan_not_found"


@pytest.mark.asyncio
async def test_invalid_vehicle_is_rejected(db):
    user = UserProfile(auth_user_id=uuid.uuid4())
    db.add(user)
    await db.commit()
    with pytest.raises(AppError) as exc:
        await GarageService().add(db, user, uuid.uuid4(), None)
    assert exc.value.code == "vehicle_not_found"


@pytest.mark.asyncio
async def test_delete_then_reactivate_does_not_award_again(db):
    user, _, vehicle, scan = await make_catalog(db)
    service = GarageService()
    entry, awarded, _ = await service.add(db, user, vehicle.id, scan.id)
    entry.removed_at = datetime.now(timezone.utc)
    await db.commit()
    restored, second_award, already = await service.add(db, user, vehicle.id, scan.id)
    assert restored.id == entry.id
    assert second_award == 0
    assert already is False
    assert await garage_totals(db, user) == (1, awarded)


def test_client_cannot_send_xp_or_rarity():
    with pytest.raises(ValidationError):
        GarageCreate.model_validate({"vehicle_id": str(uuid.uuid4()), "xp_earned": 999_999, "rarity": "MYTHIC"})

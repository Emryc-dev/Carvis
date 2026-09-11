import uuid

import pytest
from sqlalchemy import select
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from app.database.base import Base
from app.models.entities import Favorite, UserProfile, Vehicle


@pytest.mark.asyncio
async def test_profile_vehicle_and_favorite_relationships():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as connection: await connection.run_sync(Base.metadata.create_all)
    sessions = async_sessionmaker(engine, expire_on_commit=False)
    async with sessions() as db:
        user = UserProfile(auth_user_id=uuid.uuid4(), name="Learner")
        vehicle = Vehicle(brand="BMW", model="M4", generation="G82", year=2023)
        db.add_all([user, vehicle]); await db.flush()
        db.add(Favorite(user_id=user.id, vehicle_id=vehicle.id)); await db.commit()
        assert await db.scalar(select(Favorite).where(Favorite.user_id == user.id))
    await engine.dispose()

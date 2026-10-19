import uuid
from datetime import datetime, timezone

import httpx
import pytest
from sqlalchemy.ext.asyncio import async_sessionmaker, create_async_engine

from app.database.base import Base
from app.database.connection import get_db
from app.dependencies.auth import current_profile
from app.main import app
from app.models.entities import GarageEntry, UserProfile, Vehicle


@pytest.mark.asyncio
async def test_leaderboard_ranks_database_xp_and_current_user():
    engine = create_async_engine("sqlite+aiosqlite:///:memory:")
    async with engine.begin() as connection:
        await connection.run_sync(Base.metadata.create_all)
    sessions = async_sessionmaker(engine, expire_on_commit=False)
    async with sessions() as db:
        first = UserProfile(auth_user_id=uuid.uuid4(), name="First")
        second = UserProfile(auth_user_id=uuid.uuid4(), name="Second")
        empty = UserProfile(auth_user_id=uuid.uuid4(), name="Empty")
        vehicle = Vehicle(brand="Test", model="Vehicle")
        db.add_all([first, second, empty, vehicle])
        await db.flush()
        db.add_all([
            GarageEntry(user_id=first.id, vehicle_id=vehicle.id, xp_earned=200),
            GarageEntry(user_id=second.id, vehicle_id=vehicle.id, xp_earned=50,
                        removed_at=datetime.now(timezone.utc)),
        ])
        await db.commit()

        async def override_db():
            yield db

        app.dependency_overrides[get_db] = override_db
        app.dependency_overrides[current_profile] = lambda: second
        try:
            async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
                response = await client.get("/api/v1/leaderboard?limit=1")
                assert response.status_code == 200
                payload = response.json()
                assert payload["current_user_rank"] == 2
                assert len(payload["entries"]) == 1
                assert payload["entries"][0]["name"] == "First"
                assert payload["entries"][0]["total_xp"] == 200
                assert payload["entries"][0]["cars_collected"] == 1
                response = await client.get("/api/v1/leaderboard")
                assert response.json()["entries"][1]["cars_collected"] == 0
        finally:
            app.dependency_overrides.clear()
    await engine.dispose()


@pytest.mark.asyncio
async def test_leaderboard_requires_authentication():
    async with httpx.AsyncClient(transport=httpx.ASGITransport(app=app), base_url="http://test") as client:
        response = await client.get("/api/v1/leaderboard")
    assert response.status_code == 401

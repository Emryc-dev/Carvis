import uuid

from fastapi import APIRouter, Query
from pydantic import BaseModel
from sqlalchemy import func, select

from app.dependencies.auth import DB, Profile
from app.models.entities import GarageEntry, UserProfile


class LeaderboardEntryRead(BaseModel):
    rank: int
    user_id: uuid.UUID
    name: str | None
    avatar_url: str | None
    total_xp: int
    cars_collected: int


class LeaderboardRead(BaseModel):
    entries: list[LeaderboardEntryRead]
    current_user_rank: int | None

router = APIRouter(prefix="/leaderboard", tags=["leaderboard"])


@router.get("", response_model=LeaderboardRead)
async def leaderboard(profile: Profile, db: DB, limit: int = Query(50, ge=1, le=100)) -> LeaderboardRead:
    # XP remains earned after a Garage entry is removed, matching /garage/stats
    # and preventing a user from changing their rank by toggling entries.
    totals = (
        select(
            GarageEntry.user_id.label("user_id"),
            func.coalesce(func.sum(GarageEntry.xp_earned), 0).label("total_xp"),
            func.count(GarageEntry.id).filter(GarageEntry.removed_at.is_(None)).label("cars_collected"),
        )
        .group_by(GarageEntry.user_id)
        .subquery()
    )
    ranked = (
        select(
            UserProfile.id.label("user_id"),
            UserProfile.name,
            UserProfile.avatar_url,
            totals.c.total_xp,
            totals.c.cars_collected,
            func.dense_rank().over(order_by=totals.c.total_xp.desc()).label("rank"),
        )
        .join(totals, totals.c.user_id == UserProfile.id)
        .subquery()
    )
    rows = (await db.execute(select(ranked).order_by(ranked.c.rank, ranked.c.name).limit(limit))).mappings().all()
    current_rank = await db.scalar(select(ranked.c.rank).where(ranked.c.user_id == profile.id))
    return LeaderboardRead(
        entries=[LeaderboardEntryRead(**dict(row)) for row in rows],
        current_user_rank=int(current_rank) if current_rank is not None else None,
    )

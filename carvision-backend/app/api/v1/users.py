from fastapi import APIRouter

from app.dependencies.auth import DB, Profile
from app.schemas.domain import UserRead, UserUpdate

router = APIRouter(prefix="/users", tags=["users"])


@router.get("/me", response_model=UserRead)
async def me(profile: Profile) -> UserRead:
    return UserRead.model_validate(profile)


@router.patch("/me", response_model=UserRead)
async def update_me(body: UserUpdate, profile: Profile, db: DB) -> UserRead:
    for field, value in body.model_dump(exclude_unset=True).items():
        setattr(profile, field, value)
    await db.commit()
    await db.refresh(profile)
    return UserRead.model_validate(profile)

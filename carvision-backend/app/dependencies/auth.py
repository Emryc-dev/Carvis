from typing import Annotated

from fastapi import Depends
from fastapi.security import HTTPAuthorizationCredentials, HTTPBearer
from sqlalchemy import select
from sqlalchemy.ext.asyncio import AsyncSession

from app.core.config import get_settings
from app.core.errors import AppError
from app.core.security import AuthUser, SupabaseTokenVerifier
from app.database.connection import get_db
from app.models.entities import UserProfile

bearer = HTTPBearer(auto_error=False)
verifier = SupabaseTokenVerifier(get_settings())


async def current_auth_user(credentials: Annotated[HTTPAuthorizationCredentials | None, Depends(bearer)]) -> AuthUser:
    if not credentials or credentials.scheme.lower() != "bearer":
        raise AppError(401, "authentication_required", "A Supabase access token is required")
    return verifier.verify(credentials.credentials)


async def current_profile(
    auth: Annotated[AuthUser, Depends(current_auth_user)], db: Annotated[AsyncSession, Depends(get_db)]
) -> UserProfile:
    profile = await db.scalar(select(UserProfile).where(UserProfile.auth_user_id == auth.id))
    if profile is None:
        profile = UserProfile(auth_user_id=auth.id, name=auth.metadata.get("full_name"), avatar_url=auth.metadata.get("avatar_url"))
        db.add(profile)
        await db.commit()
        await db.refresh(profile)
    return profile

DB = Annotated[AsyncSession, Depends(get_db)]
Profile = Annotated[UserProfile, Depends(current_profile)]

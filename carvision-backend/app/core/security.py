import time
import uuid
from dataclasses import dataclass

import httpx
import jwt
from jwt import PyJWKClient

from app.core.config import Settings
from app.core.errors import AppError


@dataclass(frozen=True)
class AuthUser:
    id: uuid.UUID
    email: str | None
    metadata: dict
    app_metadata: dict
    role: str


class SupabaseTokenVerifier:
    """Verifies Supabase JWTs locally using the project's rotating JWKS."""

    def __init__(self, settings: Settings):
        self.settings = settings
        self.issuer = f"{settings.supabase_url.rstrip('/')}/auth/v1"
        self.jwks = PyJWKClient(f"{self.issuer}/.well-known/jwks.json", cache_jwk_set=True, lifespan=300)

    def verify(self, token: str) -> AuthUser:
        try:
            key = self.jwks.get_signing_key_from_jwt(token)
            claims = jwt.decode(
                token,
                key.key,
                algorithms=["RS256", "ES256"],
                audience=self.settings.supabase_jwt_audience,
                issuer=self.issuer,
                options={"require": ["exp", "sub"]},
            )
            return AuthUser(uuid.UUID(claims["sub"]), claims.get("email"), claims.get("user_metadata", {}), claims.get("app_metadata", {}), claims.get("role", "authenticated"))
        except Exception as exc:
            raise AppError(401, "invalid_token", "Authentication token is invalid or expired") from exc


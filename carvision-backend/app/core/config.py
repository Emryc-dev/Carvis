from functools import lru_cache
from typing import Annotated, Literal

from pydantic import Field, field_validator
from pydantic_settings import BaseSettings, NoDecode, SettingsConfigDict


class Settings(BaseSettings):
    model_config = SettingsConfigDict(env_file=".env", env_file_encoding="utf-8", extra="ignore")

    app_env: Literal["development", "test", "production"] = "development"
    debug: bool = False
    api_v1_prefix: str = "/api/v1"
    database_url: str = "postgresql+asyncpg://user:password@localhost.invalid/postgres"
    direct_url: str = "postgresql+asyncpg://user:password@localhost.invalid/postgres"
    supabase_url: str = "https://example.supabase.co"
    supabase_publishable_key: str = ""
    supabase_secret_key: str = ""
    supabase_jwt_audience: str = "authenticated"
    supabase_storage_bucket: str = "vehicle-scans"
    upstash_redis_rest_url: str = ""
    upstash_redis_rest_token: str = ""
    gemini_api_key: str = ""
    gemini_model: str = "gemini-2.5-flash"
    cors_origins: Annotated[list[str], NoDecode] = Field(default_factory=list)
    max_image_bytes: int = 10 * 1024 * 1024
    min_image_width: int = 224
    min_image_height: int = 224
    max_image_dimension: int = 12_000
    ai_timeout_seconds: float = 45

    @field_validator("cors_origins", mode="before")
    @classmethod
    def split_origins(cls, value: object) -> object:
        if isinstance(value, str):
            return [item.strip() for item in value.split(",") if item.strip()]
        return value

    @field_validator("database_url", "direct_url")
    @classmethod
    def async_driver(cls, value: str) -> str:
        return value.replace("postgresql://", "postgresql+asyncpg://", 1)


@lru_cache
def get_settings() -> Settings:
    return Settings()

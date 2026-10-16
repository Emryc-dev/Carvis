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
    gemini_fallback_model: str = "gemini-3.5-flash-lite"
    cors_origins: Annotated[list[str], NoDecode] = Field(default_factory=list)
    max_image_bytes: int = 10 * 1024 * 1024
    min_image_width: int = 224
    min_image_height: int = 224
    max_image_dimension: int = 12_000
    ai_timeout_seconds: float = 45
    catalogue_enrichment_min_confidence: float = Field(0.65, ge=0, le=1)
    recognition_engine: Literal["gemini", "ml", "hybrid"] = "gemini"
    ml_shadow_mode: bool = False
    ml_model_path: str = "ml/artifacts/vehicle_classifier_v1.pt"
    ml_model_metadata_path: str = "ml/artifacts/vehicle_classifier_v1.json"
    ml_model_version: str = "vehicle_classifier_v1"
    ml_device: str = "cpu"
    ml_top_k: int = Field(5, ge=1, le=20)
    ml_min_confidence: float = Field(0.70, ge=0, le=1)
    ml_gemini_validation_threshold: float = Field(0.82, ge=0, le=1)
    ml_detector_min_confidence: float = Field(0.55, ge=0, le=1)
    ml_enable_pretrained_detector: bool = True

    @field_validator("cors_origins", mode="before")
    @classmethod
    def split_origins(cls, value: object) -> object:
        if isinstance(value, str):
            return [item.strip() for item in value.split(",") if item.strip()]
        return value

    @field_validator("debug", mode="before")
    @classmethod
    def normalise_debug(cls, value: object) -> object:
        """Accept the legacy deployment value DEBUG=release as false."""
        if isinstance(value, str) and value.strip().lower() in {"release", "production"}:
            return False
        return value

    @field_validator("database_url", "direct_url")
    @classmethod
    def async_driver(cls, value: str) -> str:
        return value.replace("postgresql://", "postgresql+asyncpg://", 1)


@lru_cache
def get_settings() -> Settings:
    return Settings()

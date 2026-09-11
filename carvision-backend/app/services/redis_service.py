import json
from typing import Any

import httpx

from app.core.config import Settings


class RedisService:
    """Small fail-open adapter for Upstash's REST pipeline API."""
    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None):
        self.settings, self.client = settings, client

    @property
    def configured(self) -> bool:
        return bool(self.settings.upstash_redis_rest_url and self.settings.upstash_redis_rest_token)

    async def _command(self, command: list[Any]) -> Any:
        if not self.configured:
            return None
        owns = self.client is None
        client = self.client or httpx.AsyncClient(timeout=3)
        try:
            response = await client.post(self.settings.upstash_redis_rest_url.rstrip("/") + "/pipeline", headers={"Authorization": f"Bearer {self.settings.upstash_redis_rest_token}"}, json=[command])
            response.raise_for_status()
            return response.json()[0].get("result")
        except (httpx.HTTPError, ValueError, KeyError, IndexError):
            return None  # Cache failure must not make durable application data unavailable.
        finally:
            if owns:
                await client.aclose()

    async def get_json(self, key: str) -> dict | None:
        value = await self._command(["GET", key])
        try:
            return json.loads(value) if value else None
        except (TypeError, json.JSONDecodeError):
            return None

    async def set_json(self, key: str, value: dict, ttl_seconds: int) -> None:
        await self._command(["SET", key, json.dumps(value, separators=(",", ":")), "EX", ttl_seconds])

    async def delete(self, key: str) -> None:
        await self._command(["DEL", key])

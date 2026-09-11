import httpx
import pytest

from app.core.config import Settings
from app.services.redis_service import RedisService


@pytest.mark.asyncio
async def test_cache_round_trip_decoding():
    async def handler(request): return httpx.Response(200, json=[{"result": '{"brand":"BMW"}'}])
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        cache = RedisService(Settings(upstash_redis_rest_url="https://redis.example", upstash_redis_rest_token="token"), client)
        assert await cache.get_json("key") == {"brand": "BMW"}


@pytest.mark.asyncio
async def test_cache_fails_open():
    async def handler(request): raise httpx.ConnectError("offline", request=request)
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        cache = RedisService(Settings(upstash_redis_rest_url="https://redis.example", upstash_redis_rest_token="token"), client)
        assert await cache.get_json("key") is None

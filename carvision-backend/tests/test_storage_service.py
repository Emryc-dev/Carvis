import httpx
import pytest

from app.core.config import Settings
from app.services.storage_service import StorageService


@pytest.mark.asyncio
async def test_storage_upload_uses_private_server_credentials():
    seen = {}
    async def handler(request):
        seen["authorization"] = request.headers["authorization"]
        return httpx.Response(200, json={})
    settings = Settings(supabase_url="https://project.supabase.co", supabase_secret_key="server-secret")
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        await StorageService(settings, client).upload("user/scan/original.jpg", b"jpg", "image/jpeg")
    assert seen["authorization"] == "Bearer server-secret"

import httpx
import pytest

from app.core.config import Settings
from app.core.errors import AppError
from app.services.ai.gemini import GeminiService


@pytest.mark.asyncio
async def test_gemini_validates_structured_response():
    payload = {"candidates": [{"content": {"parts": [{"text": '{"brand":"BMW","model":"M4","confidence":0.8,"visual_evidence":[],"uncertainties":[]}' }]}}]}
    async def handler(request): return httpx.Response(200, json=payload)
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        result = await GeminiService(Settings(gemini_api_key="test"), client).identify_vehicle(b"image", "image/jpeg")
    assert result.brand == "BMW" and result.confidence == 0.8


@pytest.mark.asyncio
async def test_gemini_uses_fallback_when_primary_is_busy():
    valid = {"candidates": [{"content": {"parts": [{"text": '{"brand":"BMW","model":"M4","confidence":0.8,"visual_evidence":[],"uncertainties":[]}' }]}}]}
    async def handler(request):
        if "gemini-primary" in str(request.url):
            return httpx.Response(503, request=request, json={"error": {"message": "busy"}})
        return httpx.Response(200, request=request, json=valid)
    settings = Settings(gemini_api_key="test", gemini_model="gemini-primary", gemini_fallback_model="gemini-fallback")
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        result = await GeminiService(settings, client).identify_vehicle(b"image", "image/jpeg")
    assert result.model == "M4"


@pytest.mark.asyncio
async def test_gemini_reports_temporary_unavailability():
    async def handler(request): return httpx.Response(503, request=request, json={"error": {"message": "busy"}})
    settings = Settings(gemini_api_key="test", gemini_model="gemini-primary", gemini_fallback_model="gemini-fallback")
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        with pytest.raises(AppError) as raised:
            await GeminiService(settings, client).identify_vehicle(b"image", "image/jpeg")
    assert raised.value.code == "ai_temporarily_unavailable"

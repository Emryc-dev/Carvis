import httpx
import pytest

from app.core.config import Settings
from app.services.ai.gemini import GeminiService


@pytest.mark.asyncio
async def test_gemini_validates_structured_response():
    payload = {"candidates": [{"content": {"parts": [{"text": '{"brand":"BMW","model":"M4","confidence":0.8,"visual_evidence":[],"uncertainties":[]}' }]}}]}
    async def handler(request): return httpx.Response(200, json=payload)
    async with httpx.AsyncClient(transport=httpx.MockTransport(handler)) as client:
        result = await GeminiService(Settings(gemini_api_key="test"), client).identify_vehicle(b"image", "image/jpeg")
    assert result.brand == "BMW" and result.confidence == 0.8

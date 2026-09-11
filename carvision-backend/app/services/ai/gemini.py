import base64
import json

import httpx

from app.core.config import Settings
from app.core.errors import AppError
from app.schemas.ai import AIIdentification
from app.services.ai.base import AIService


class GeminiService(AIService):
    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None):
        self.settings = settings
        self.client = client

    async def identify_vehicle(self, image: bytes, mime_type: str, context: str | None = None) -> AIIdentification:
        if not self.settings.gemini_api_key:
            raise AppError(503, "ai_not_configured", "Vehicle recognition is not configured")
        schema = AIIdentification.model_json_schema()
        prompt = (
            "Identify only what is supportable from the vehicle image. Return JSON matching the supplied schema. "
            "Use null for unsupported specifications, list visual evidence and uncertainties, and never invent VIN, packages, prices, or verification. "
            f"Optional user context: {context or 'none'}"
        )
        body = {"contents": [{"parts": [{"text": prompt}, {"inline_data": {"mime_type": mime_type, "data": base64.b64encode(image).decode()}}]}],
                "generationConfig": {"temperature": 0.1, "responseMimeType": "application/json", "responseJsonSchema": schema}}
        url = f"https://generativelanguage.googleapis.com/v1beta/models/{self.settings.gemini_model}:generateContent"
        owns_client = self.client is None
        client = self.client or httpx.AsyncClient(timeout=self.settings.ai_timeout_seconds)
        try:
            response = await client.post(url, params={"key": self.settings.gemini_api_key}, json=body)
            response.raise_for_status()
            raw = response.json()["candidates"][0]["content"]["parts"][0]["text"]
            return AIIdentification.model_validate(json.loads(raw))
        except httpx.TimeoutException as exc:
            raise AppError(504, "ai_timeout", "Vehicle recognition timed out") from exc
        except (httpx.HTTPError, KeyError, IndexError, json.JSONDecodeError, ValueError) as exc:
            raise AppError(502, "ai_invalid_response", "The AI provider returned an unusable response") from exc
        finally:
            if owns_client:
                await client.aclose()

import httpx

from app.core.config import Settings
from app.core.errors import AppError


class StorageService:
    def __init__(self, settings: Settings, client: httpx.AsyncClient | None = None):
        self.settings, self.client = settings, client

    def _headers(self) -> dict[str, str]:
        key = self.settings.supabase_secret_key
        return {"Authorization": f"Bearer {key}", "apikey": key}

    async def upload(self, path: str, content: bytes, mime_type: str) -> None:
        if not self.settings.supabase_secret_key:
            raise AppError(503, "storage_not_configured", "Image storage is not configured")
        owns = self.client is None
        client = self.client or httpx.AsyncClient(timeout=20)
        try:
            url = f"{self.settings.supabase_url}/storage/v1/object/{self.settings.supabase_storage_bucket}/{path}"
            response = await client.post(url, headers={**self._headers(), "Content-Type": mime_type, "x-upsert": "false"}, content=content)
            response.raise_for_status()
        except httpx.HTTPError as exc:
            raise AppError(502, "storage_failure", "The image could not be stored") from exc
        finally:
            if owns: await client.aclose()

    async def signed_url(self, path: str, expires_in: int = 900) -> str | None:
        if not path or not self.settings.supabase_secret_key: return None
        owns = self.client is None
        client = self.client or httpx.AsyncClient(timeout=10)
        try:
            url = f"{self.settings.supabase_url}/storage/v1/object/sign/{self.settings.supabase_storage_bucket}/{path}"
            response = await client.post(url, headers=self._headers(), json={"expiresIn": expires_in})
            response.raise_for_status()
            signed = response.json()["signedURL"]
            return signed if signed.startswith("http") else f"{self.settings.supabase_url}/storage/v1{signed}"
        except (httpx.HTTPError, KeyError, ValueError): return None
        finally:
            if owns: await client.aclose()

    async def delete(self, paths: list[str]) -> None:
        if not paths: return
        owns = self.client is None
        client = self.client or httpx.AsyncClient(timeout=10)
        try:
            url = f"{self.settings.supabase_url}/storage/v1/object/{self.settings.supabase_storage_bucket}"
            response = await client.request("DELETE", url, headers=self._headers(), json={"prefixes": paths})
            response.raise_for_status()
        except httpx.HTTPError as exc: raise AppError(502, "storage_failure", "The stored image could not be deleted") from exc
        finally:
            if owns: await client.aclose()

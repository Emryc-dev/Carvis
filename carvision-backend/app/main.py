import uuid

from fastapi import FastAPI, Request
from fastapi.middleware.cors import CORSMiddleware
from fastapi.responses import ORJSONResponse

from app.api.v1.router import router
from app.core.config import get_settings
from app.core.errors import install_error_handlers

settings = get_settings()
app = FastAPI(title="CarVision AI API", version="0.1.0", default_response_class=ORJSONResponse)
app.add_middleware(CORSMiddleware, allow_origins=settings.cors_origins, allow_credentials=True, allow_methods=["GET", "POST", "PUT", "PATCH", "DELETE"], allow_headers=["Authorization", "Content-Type", "X-Request-ID"])


@app.middleware("http")
async def request_id(request: Request, call_next):
    request.state.request_id = request.headers.get("X-Request-ID", str(uuid.uuid4()))[:128]
    response = await call_next(request)
    response.headers["X-Request-ID"] = request.state.request_id
    response.headers["X-Content-Type-Options"] = "nosniff"
    return response


@app.get("/health", tags=["operations"])
async def health() -> dict:
    return {"status": "ok"}


@app.get("/ready", tags=["operations"])
async def ready() -> dict:
    configured = {"database": "localhost.invalid" not in settings.database_url, "supabase": settings.supabase_url != "https://example.supabase.co", "storage": bool(settings.supabase_secret_key), "redis": bool(settings.upstash_redis_rest_url and settings.upstash_redis_rest_token), "gemini": bool(settings.gemini_api_key)}
    return {"status": "ready" if all(configured.values()) else "configuration_required", "dependencies": configured}


app.include_router(router, prefix=settings.api_v1_prefix)
install_error_handlers(app)

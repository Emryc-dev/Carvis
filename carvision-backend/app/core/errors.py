from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import ORJSONResponse

from app.core.config import get_settings


class AppError(Exception):
    def __init__(self, status_code: int, code: str, message: str, details: Any = None):
        self.status_code, self.code, self.message, self.details = status_code, code, message, details


def payload(code: str, message: str, request_id: str, details: Any = None) -> dict:
    return {"error": {"code": code, "message": message, "details": details, "request_id": request_id}}


def cors_headers(request: Request) -> dict[str, str]:
    """Keep an allowed browser origin visible even when the API fails unexpectedly."""
    origin = request.headers.get("origin")
    if origin and origin in get_settings().cors_origins:
        return {
            "Access-Control-Allow-Origin": origin,
            "Access-Control-Allow-Credentials": "true",
            "Vary": "Origin",
        }
    return {}


def install_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppError)
    async def app_error(request: Request, exc: AppError) -> ORJSONResponse:
        return ORJSONResponse(status_code=exc.status_code, content=payload(exc.code, exc.message, request.state.request_id, exc.details), headers=cors_headers(request))

    @app.exception_handler(RequestValidationError)
    async def invalid_request(request: Request, exc: RequestValidationError) -> ORJSONResponse:
        return ORJSONResponse(status_code=422, content=payload("validation_error", "Request validation failed", request.state.request_id, exc.errors()), headers=cors_headers(request))

    @app.exception_handler(Exception)
    async def unexpected(request: Request, exc: Exception) -> ORJSONResponse:
        return ORJSONResponse(status_code=500, content=payload("internal_error", "An unexpected error occurred", request.state.request_id), headers=cors_headers(request))


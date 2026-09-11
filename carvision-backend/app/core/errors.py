from typing import Any

from fastapi import FastAPI, Request
from fastapi.exceptions import RequestValidationError
from fastapi.responses import ORJSONResponse


class AppError(Exception):
    def __init__(self, status_code: int, code: str, message: str, details: Any = None):
        self.status_code, self.code, self.message, self.details = status_code, code, message, details


def payload(code: str, message: str, request_id: str, details: Any = None) -> dict:
    return {"error": {"code": code, "message": message, "details": details, "request_id": request_id}}


def install_error_handlers(app: FastAPI) -> None:
    @app.exception_handler(AppError)
    async def app_error(request: Request, exc: AppError) -> ORJSONResponse:
        return ORJSONResponse(status_code=exc.status_code, content=payload(exc.code, exc.message, request.state.request_id, exc.details))

    @app.exception_handler(RequestValidationError)
    async def invalid_request(request: Request, exc: RequestValidationError) -> ORJSONResponse:
        return ORJSONResponse(status_code=422, content=payload("validation_error", "Request validation failed", request.state.request_id, exc.errors()))

    @app.exception_handler(Exception)
    async def unexpected(request: Request, exc: Exception) -> ORJSONResponse:
        return ORJSONResponse(status_code=500, content=payload("internal_error", "An unexpected error occurred", request.state.request_id))


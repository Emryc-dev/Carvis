from typing import Any

from pydantic import BaseModel


class Page(BaseModel):
    items: list[Any]
    limit: int
    offset: int
    total: int


class Message(BaseModel):
    message: str

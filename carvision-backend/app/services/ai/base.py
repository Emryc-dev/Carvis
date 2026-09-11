from abc import ABC, abstractmethod

from app.schemas.ai import AIIdentification


class AIService(ABC):
    @abstractmethod
    async def identify_vehicle(self, image: bytes, mime_type: str, context: str | None = None) -> AIIdentification:
        raise NotImplementedError

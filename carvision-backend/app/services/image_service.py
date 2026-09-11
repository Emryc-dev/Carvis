import hashlib
import io
from dataclasses import dataclass

from PIL import Image, UnidentifiedImageError

from app.core.config import Settings
from app.core.errors import AppError

ALLOWED = {"image/jpeg": "JPEG", "image/png": "PNG", "image/webp": "WEBP"}


@dataclass(frozen=True)
class ValidImage:
    content: bytes
    mime_type: str
    width: int
    height: int
    sha256: str
    extension: str


def validate_image(content: bytes, declared_type: str | None, settings: Settings) -> ValidImage:
    if not content:
        raise AppError(422, "empty_image", "The image file is empty")
    if len(content) > settings.max_image_bytes:
        raise AppError(413, "image_too_large", f"Image exceeds {settings.max_image_bytes} bytes")
    if declared_type not in ALLOWED:
        raise AppError(415, "unsupported_image", "Only JPEG, PNG, and WebP images are accepted")
    try:
        with Image.open(io.BytesIO(content)) as image:
            image.verify()
        with Image.open(io.BytesIO(content)) as image:
            width, height, detected = image.width, image.height, image.format
    except (UnidentifiedImageError, OSError, Image.DecompressionBombError) as exc:
        raise AppError(422, "invalid_image", "The uploaded file is not a valid image") from exc
    if detected != ALLOWED[declared_type]:
        raise AppError(422, "image_type_mismatch", "The file content does not match its MIME type")
    if min(width, height) < min(settings.min_image_width, settings.min_image_height) or max(width, height) > settings.max_image_dimension:
        raise AppError(422, "invalid_dimensions", "Image dimensions are outside the accepted range")
    return ValidImage(content, declared_type, width, height, hashlib.sha256(content).hexdigest(), detected.lower().replace("jpeg", "jpg"))

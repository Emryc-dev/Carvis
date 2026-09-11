import io

import pytest
from PIL import Image

from app.core.config import Settings
from app.core.errors import AppError
from app.services.image_service import validate_image


def png(size=(300, 300)) -> bytes:
    out = io.BytesIO(); Image.new("RGB", size, "red").save(out, "PNG"); return out.getvalue()


def test_valid_image_has_dimensions_and_stable_hash():
    result = validate_image(png(), "image/png", Settings())
    assert (result.width, result.height, len(result.sha256)) == (300, 300, 64)


@pytest.mark.parametrize("content,mime,code", [(b"", "image/png", "empty_image"), (b"not-image", "image/png", "invalid_image"), (b"x", "image/gif", "unsupported_image")])
def test_rejects_bad_uploads(content, mime, code):
    with pytest.raises(AppError) as caught: validate_image(content, mime, Settings())
    assert caught.value.code == code

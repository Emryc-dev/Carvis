# Libraries guide

## FastAPI

Domain: web APIs. It maps typed Python functions to HTTP routes, dependency injection and OpenAPI. Here it validates inputs, injects auth/database dependencies and serializes responses. Basic use: `@app.get("/health") async def health(): return {"status":"ok"}`. Conceptually it is ASGI routing plus type-driven validation. Professionals use it for APIs and inference services. Alternatives include Django REST Framework, Flask and Litestar. Do not use it for a static site or when a full Django admin/ORM ecosystem is the primary need.

## Pydantic and pydantic-settings

Domain: runtime data/configuration validation. Here schemas protect HTTP and untrusted AI boundaries; settings load environment secrets. `AIIdentification.model_validate(data)` converts or rejects data. Pydantic builds validators from type hints and emits JSON Schema. It is common in APIs, ETL and configuration. Alternatives: attrs, dataclasses, Marshmallow. Do not mistake validation for sanitization or database constraints.

## SQLAlchemy

Domain: relational persistence. Its async ORM maps models, relationships and transactions to Supabase PostgreSQL. `await db.scalar(select(Vehicle).where(Vehicle.id == id))` is a typed query. The unit of work tracks changes and flushes SQL. Used in transactional business systems. Alternatives: SQLModel, Django ORM, psycopg, encode/databases. Prefer raw SQL for specialized analytics or tiny one-off scripts.

## Alembic

Domain: schema migrations. Revisions apply ordered `upgrade`/`downgrade` operations to cloud PostgreSQL. `alembic upgrade head` advances a database. It records the current revision in a version table. Alternatives include Flyway and Liquibase. Do not auto-apply unreviewed destructive migrations in every app replica.

## asyncpg

Domain: PostgreSQL driver. SQLAlchemy uses it for non-blocking database I/O. The URL begins `postgresql+asyncpg://`. It implements the PostgreSQL wire protocol efficiently. Alternative: psycopg 3 async. Do not use async merely for offline scripts where synchronous code is simpler.

## HTTPX

Domain: HTTP client. It calls Gemini, Upstash and Supabase Storage asynchronously and is easy to mock. `await client.post(url, json=body)`. It manages pooling, timeouts and HTTP semantics. Alternatives: Requests (sync), aiohttp. Do not create an unbounded new client per high-volume request in mature deployments; inject a lifespan client.

## PyJWT with cryptography

Domain: authentication/cryptography. It parses and verifies Supabase JWT signatures, issuer, audience and expiry using JWKS. `jwt.decode(token, key, algorithms=[...], audience=..., issuer=...)`. Alternatives: Authlib and python-jose. Never use decode-without-verification for authorization and never accept algorithms from an untrusted token.

## Pillow

Domain: raster image processing. It verifies that uploaded bytes are a supported image and reads dimensions without heavyweight ML dependencies. `Image.open(stream).verify()`. Alternatives: OpenCV, ImageMagick bindings. Pillow is not a malware scanner and should not process unlimited pixels; retain size/dimension limits.

## python-multipart

Domain: HTTP form parsing. FastAPI needs it for streamed multipart image uploads. It parses boundaries and form fields. Alternatives are lower-level ASGI parsers. Do not base64 images inside JSON when multipart is available; it costs memory and bandwidth.

## Uvicorn

Domain: ASGI server. It runs `app.main:app` locally and in production. `uvicorn app.main:app --reload`. It converts network requests into ASGI events. Alternatives: Hypercorn, Daphne, Gunicorn with an ASGI worker. Do not use reload in production.

## pytest, pytest-asyncio, HTTPX MockTransport

Domain: testing. Pytest discovers expressive tests; pytest-asyncio runs coroutines; MockTransport isolates cloud calls. `pytest` executes the suite. Alternatives: unittest and respx. Mocks verify contracts but cannot replace live staging integration tests.

## Deliberately not installed

NumPy/OpenCV are unnecessary for hashing, integrity and dimensions; add them only for array-level transforms or vision algorithms. Pandas is for tabular analysis, not request handling. PyTorch, TensorFlow and Transformers are valuable for training/local inference but would add large binaries, model operations and GPU concerns while Gemini performs inference remotely.

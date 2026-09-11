# CarVision AI backend

Production-oriented, educational FastAPI modular monolith for the CarVision Android client. It validates Supabase sessions, stores application data in Supabase PostgreSQL through async SQLAlchemy, keeps scan images private in Supabase Storage, caches recognition through Upstash Redis, and calls Gemini only from the server. It contains no billing or premium functionality.

> The supplied mobile project is Kotlin/Jetpack Compose, not Flutter. See [`../FRONTEND_ANALYSIS.md`](../FRONTEND_ANALYSIS.md) before integrating it.

## Architecture

`Android/Flutter -> HTTPS + Supabase JWT -> FastAPI -> SQLAlchemy/Supabase PostgreSQL | Upstash | Supabase Storage | AIService/Gemini`

AI output is recorded as `ai_inferred`. Only sourced catalog records may be `partially_verified` or `verified`.

## Requirements and installation

Python 3.12+ is required. No local PostgreSQL or Redis is used.

```bash
cd carvision-backend
python -m venv .venv
# Windows: .venv\Scripts\activate
pip install -e ".[test]"
copy .env.example .env
```

Fill `.env` with cloud credentials. Use Supabase's transaction pooler URL (asyncpg format) for deployed/API traffic. The service-role key, database password, Gemini key and Upstash token are server-only.

## Cloud setup

1. Create a Supabase project and a **private** `vehicle-scans` bucket.
2. Copy the project URL, anon key, service-role key, and pooler database URL into `.env`.
3. In Google Cloud create an OAuth web client. Set the authorized redirect URI to `https://PROJECT_REF.supabase.co/auth/v1/callback`. Enable Google under Supabase Dashboard → Authentication → Providers, then configure the Android app's custom URL/deep-link callback in Supabase redirect URLs and its manifest. The mobile Supabase SDK receives the session; FastAPI only validates its access token.
4. Create an Upstash Redis database and copy its REST URL/token.
5. Create a Gemini API key and choose an available multimodal model in `GEMINI_MODEL`.

The anon key is suitable for client-side Supabase Auth initialization. The service-role key bypasses RLS and is used only by the backend for private storage operations. Never ship it in the APK.

## Database and run commands

```bash
alembic upgrade head
uvicorn app.main:app --reload
pytest
```

OpenAPI is at `http://127.0.0.1:8000/docs`; readiness/configuration status is at `/ready`. Live cloud checks require real credentials and network access.

## Main endpoints

- Profile: `GET/PATCH /api/v1/users/me`
- Catalog: `GET /api/v1/vehicles`, `GET /api/v1/vehicles/{id}`, `GET /api/v1/vehicles/compare`
- Recognition/history: `POST/GET /api/v1/scans`, `GET/DELETE /api/v1/scans/{id}`
- Garage/favorites: `GET /api/v1/favorites`, `PUT/DELETE /api/v1/favorites/{vehicle_id}`

All `/api/v1` endpoints require `Authorization: Bearer <Supabase access token>`.

## Structure

`app/api` routes; `core` configuration/security/errors; `database` engine/session; `models` ORM; `schemas` Pydantic contracts; `repositories` queries; `services` AI/cache/storage/scanning; `alembic` migrations; `tests` isolated validation; `documentation` architecture, API and learning guides.

See [API documentation](documentation/API_DOCUMENTATION.md), [database guide](documentation/DATABASE_GUIDE.md), [deployment](DEPLOYMENT.md), and the generated OpenAPI UI.

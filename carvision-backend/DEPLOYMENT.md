# Deployment

Deploy the ASGI application as `app.main:app` on Railway, Render, Fly.io, or a Python 3.12 VPS. Build with `pip install .`; release with `alembic upgrade head`; start with `uvicorn app.main:app --host 0.0.0.0 --port $PORT --proxy-headers`. Use one migration job, not one migration per web replica.

Configure every `.env.example` variable in the host's encrypted secret manager. Use the Supabase transaction pooler on port 6543 for horizontally scaled services and keep SQLAlchemy's pool modest (the default here is 5 + 10 overflow per process). If a provider offers persistent processes, use 2–4 workers according to CPU and measure Gemini/DB concurrency. Restrict CORS to real client/admin origins, terminate TLS at the platform, and never log tokens or uploaded images.

Readiness is `/ready`; liveness is `/health`. The readiness response reports configuration presence without exposing values. Before promotion, run migrations, `pytest`, an authenticated profile request, one disposable scan, cache-hit repetition, signed-image retrieval, and deletion. Add centralized logs/metrics, error reporting, database backups, cost alerts, key rotation, and a retention policy. Docker is optional; no Docker database or Redis is needed.

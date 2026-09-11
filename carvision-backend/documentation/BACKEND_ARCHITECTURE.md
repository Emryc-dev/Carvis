# Backend architecture

CarVision is a modular monolith: one deployable FastAPI application with isolated HTTP, authentication, persistence, cache, storage, and AI boundaries. This keeps the MVP understandable and transactional while allowing a provider or background worker to be replaced later.

The mobile client authenticates with Supabase Auth and sends an access token. FastAPI validates that JWT against project JWKS and maps `sub` to `user_profiles.auth_user_id`. Supabase Auth owns credentials; the application database never stores passwords. Profiles are separate because authentication lifecycle/security and product data have different owners.

For a scan, FastAPI bounds the stream, verifies MIME and actual image bytes with Pillow, checks dimensions, computes SHA-256, uploads to a private bucket, persists a processing row, checks `ai_result:v1:{sha256}`, calls `AIService` on a miss, validates output, searches (but does not invent) a catalog match, records the AI attempt, commits, and returns a short-lived signed image URL. Provider/storage errors are safe and consistent; Redis fails open because it is an optimization.

An `AIService` interface isolates Gemini. A future `OpenAIService` can implement the same method and return the same `AIIdentification`. The schema disallows extra fields and bounds year, power and confidence. Inferred data stays on the scan. Vehicle specifications carry source and verification status.

Trust boundaries: anon key may exist in the client; service role, database password, Upstash token and AI key remain server-side. Object paths are user-namespaced. Every scan query includes application user ownership. Admin APIs are intentionally absent until a real server-side role policy exists.

Background jobs are unnecessary for the current synchronous MVP. If scan latency becomes unacceptable, keep the same scan state machine, enqueue its UUID, return 202, and have a worker execute the existing pipeline idempotently.

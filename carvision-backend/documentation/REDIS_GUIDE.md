# Redis and Upstash guide

Redis is an in-memory key/value system. Upstash supplies it as a managed REST service; no local Redis is installed. The adapter uses REST pipeline commands and deliberately fails open.

Current cache: `ai_result:v1:{sha256}` stores only validated AI identification JSON for 7 days. The version permits schema invalidation; SHA-256 makes identical bytes reusable. Natural TTL removes stale model output. Changing prompts/schema/model semantics requires incrementing `v1` or deleting matching keys. Do not cache signed URLs, tokens, or user-private profiles.

Planned keys: `vehicle:v1:{vehicle_id}` (1 hour; invalidate after catalog/spec update), `scan_state:v1:{scan_id}` (15 minutes for async progress; delete on terminal state), and `rate:v1:{user_id}:{minute}` (two minutes; atomic increment/expiry). Rate limiting must fail safely and should return 429 with retry guidance. Redis is not the source of truth: PostgreSQL remains authoritative.

TTL limits memory and stale data. Cache-aside means read cache, query/compute on miss, then populate. Invalidation is easy for identity keys, harder for queries; prefer versioning and short TTLs over wildcard deletion.

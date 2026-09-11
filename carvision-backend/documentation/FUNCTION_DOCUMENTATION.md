# Major function documentation

## `validate_image(content, declared_type, settings)`

Purpose: establish a safe, consistent upload boundary. Parameters: raw bytes, request MIME type and validated settings. Returns: `ValidImage` with bytes, actual type, dimensions, SHA-256 and extension. Exceptions: `AppError` for empty, oversized, unsupported, corrupt, mismatched or invalid-dimension images. Logic: bound bytes before call, check allowlist, ask Pillow to verify/decode headers, compare detected format, enforce dimensions, hash bytes. Example: `validate_image(data, "image/jpeg", settings)`. Common mistakes: trusting filename/MIME, reading unlimited data, or using hash as proof that an image depicts a car. Related: content sniffing, decompression bombs, deduplication.

## `SupabaseTokenVerifier.verify(token)`

Purpose: convert a signed Supabase access token to trusted identity. Parameter: compact JWT string. Returns: `AuthUser`. Exceptions: 401 `AppError`. Logic: select rotating public key from JWKS, require supported asymmetric algorithm, signature, issuer, audience, expiry and UUID subject. Example: used by `current_auth_user`. Common mistakes: using the anon/service key as a user token, disabling audience checks, or trusting unverified claims. Related: OIDC, JWKS, asymmetric signatures.

## `current_profile(auth, db)`

Purpose: map an auth identity to application profile data. Returns an existing or newly created profile. Database failures propagate to the global safe handler. It never stores passwords. Concurrent first requests rely on the unique `auth_user_id` constraint; a production hardening step may retry a unique-conflict race. Related: just-in-time provisioning.

## `GeminiService.identify_vehicle(image, mime_type, context)`

Purpose: obtain constrained, uncertain visual identification. Returns validated `AIIdentification`. Exceptions: 503 missing key, 504 timeout, 502 HTTP/shape/validation failure. Logic: encode bytes, provide schema and conservative prompt, request JSON, validate every field. Common mistakes: defaults that fabricate facts, logging image/key, treating confidence as truth. Related: structured output and provider adapters.

## `RedisService.get_json/set_json/delete`

Purpose: cache reusable validated output through Upstash REST. Parameters: namespaced key, JSON dict and TTL. Returns decoded dict or none. Network/parser failures deliberately return none so the cache is not a system dependency. Common mistakes: caching secrets, omitting key versions/TTL, and treating Redis as durable storage.

## `StorageService.upload/signed_url/delete`

Purpose: manage private Supabase Storage objects with a server-only service role. Paths are user/scan namespaced. Upload/delete raise safe storage errors; signed URL creation returns none on transient failure. Common mistakes: public scan buckets, persisting signed URLs, exposing the service role, or trusting user-provided object paths.

## `ScanService.create(db, user, image, context)`

Purpose: orchestrate the scan transaction. Returns a completed `VehicleScan`; provider failures are recorded then re-raised. Logic: allocate UUID/path, store image, persist processing state, read cache, call/validate AI on miss, catalog-match exact normalized identity/year, record attempt, cache result and commit. It never promotes inference to verified specs. Common mistakes: holding fabricated fallbacks, losing failed-attempt audit data, cross-user paths, or linking fuzzy matches without thresholds.

## Route functions

Profile routes read/update only the injected profile. Vehicle routes paginate/search sourced catalog rows. Scan routes bound uploads and filter every lookup by `user_id`. Favorite routes implement idempotent garage actions. Each returns Pydantic response models. Common route mistakes are missing ownership predicates, unbounded pages, lazy-loading during serialization, and leaking exception details.

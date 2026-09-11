# CarVision AI Frontend Analysis

## Executive finding

The supplied client is **not Flutter/Dart**. It is a native Android application written in Kotlin with Jetpack Compose. No `.dart`, `pubspec.yaml`, Flutter widget, or Flutter state-management code exists. The backend contract below therefore follows the actual Kotlin models while remaining JSON-friendly for a future Flutter client.

## 1. Existing architecture

- Single Android Gradle module (`app`), Kotlin, Jetpack Compose and Material 3.
- `MainActivity` owns a Compose `NavHost`; routes are a sealed `Screen` class.
- `CarVisionViewModel` is the application state holder using `StateFlow`.
- `VehicleRepository` combines a Room database with a direct Gemini HTTP client.
- Room has `vehicles` and `scan_telemetry` tables. Seed data is inserted on every repository initialization using replace semantics.
- There is no dependency injection, remote backend client, authenticated session persistence, pagination, domain/use-case layer, or network-state model.

## 2. Screens and navigation

Routes: `onboarding`, `auth`, `dashboard`, `scanner`, `vehicle_result`, `compare`, `garage`, and `admin`.

- **Onboarding:** marketing/feature introduction; “get started” bypasses auth and opens the dashboard.
- **Auth:** toggles sign-in/sign-up UI, accepts name/email/password, but performs no real authentication. Google/social authentication is not implemented.
- **Dashboard:** greeting, recent vehicles, scan entry points, comparison and garage navigation.
- **Scanner:** accepts a selected image or a built-in sample and displays a four-stage simulated pipeline.
- **Vehicle result:** shows identification, performance, valuation, packages, dyno/aero/history tabs, compare, and garage actions.
- **Compare:** compares two locally loaded vehicles.
- **Garage/profile:** combines saved vehicles, scan history, profile statistics, local settings, admin link, and sign-out.
- **Admin monitoring:** hardcoded KPIs and queue items; “verify” only changes Compose state. It has no role check.

## 3. Existing functionality

Room provides local vehicle listing, garage filtering, scan history, vehicle lookup, and garage toggling. Gemini is called directly from the mobile process. If the key/call/parse fails, the repository silently returns a seeded BMW. Scan stages, latency, depth, confidence, verification status, prices, drivetrain, consumption, weight and reliability may be fixed or randomly generated. Settings are memory-only. The camera permission exists, but CameraX dependencies are disabled; the scanner primarily uses image selection/sample URLs.

## 4. Existing data models

`Vehicle`: id, brand, model, generation, year, category, engine, horsepower, torque, transmission, drivetrain, fuel, fuelConsumption, zeroToSixty, topSpeedMph, estimatedPrice, newPrice, usedPrice, curbWeightLbs, reliabilityScore, description, colorName, chassisCode, factoryPackages, imageUrl, isSavedInGarage, lastScannedTimestamp.

`ScanTelemetry`: scanId, timestamp, vehicleName, confidence, latencyMs, lidarDepth, trimVerified, imageUrl, status.

`VehicleIdentificationResult` mirrors a subset of `Vehicle`. `ScanWorkflowState` and `UserProfileState` are presentation-only models. Room stores packages as a comma-separated string, which is lossy.

## 5. Required backend functionality

- Validate Supabase access tokens and maintain an application profile keyed by the Supabase user UUID.
- Validate, hash, optionally normalize, and privately store uploaded images.
- Deduplicate/cache recognition by image hash; call Gemini through a server-side provider abstraction.
- Validate uncertain AI output and distinguish inferred fields from verified catalog specifications.
- Persist scans, AI attempts, vehicles/specifications, favorites, and user settings.
- Return paginated history/catalog data and consistent errors.
- Keep service-role, Gemini, database, and Redis credentials off the client.

## 6. Required API endpoints

- `GET /health`, `GET /ready`
- `GET /api/v1/users/me`, `PATCH /api/v1/users/me`
- `GET /api/v1/vehicles`, `GET /api/v1/vehicles/{id}`
- `GET /api/v1/vehicles/compare?ids=id1&ids=id2`
- `POST /api/v1/scans` (multipart field `image`, optional `context`)
- `GET /api/v1/scans`, `GET /api/v1/scans/{id}`, `DELETE /api/v1/scans/{id}`
- `GET /api/v1/favorites`, `PUT /api/v1/favorites/{vehicle_id}`, `DELETE /api/v1/favorites/{vehicle_id}`

Supabase itself owns email/password and Google OAuth endpoints; FastAPI must not duplicate them.

## 7. Authentication flow

The client signs up/signs in with Supabase Auth (email/password or Google), receives a session, and sends `Authorization: Bearer <access-token>` to FastAPI. FastAPI verifies signature, issuer, audience and expiry using Supabase JWKS, then upserts/loads `UserProfile`. Sign-out occurs in the Supabase client. The current `signIn`/`signOut` methods must be replaced with this flow; “skip” may only remain if an explicitly designed anonymous mode is added later.

## 8. Database entities

- `user_profiles`: application data linked 1:1 to `auth.users.id`, no password.
- `vehicles`: normalized vehicle identity/catalog row.
- `vehicle_specifications`: one or more sourced spec sets with verification/source metadata.
- `vehicle_scans`: user-owned upload, hash, status, AI identification and linked catalog match.
- `ai_requests`: provider/model/status/latency/error and validated output for auditability.
- `favorites`: user-to-vehicle association (the UI calls this garage).

## 9. Storage operations

Create a private Supabase Storage bucket named `vehicle-scans`. Upload to `{auth_user_id}/{scan_id}/original.{ext}` with the service-role key. Store only the object path in PostgreSQL. Return short-lived signed URLs when display is required. Delete the object when a scan is deleted, subject to retention policy.

## 10. Frontend problems affecting integration

- The requested Flutter client is absent; Android Retrofit/Supabase wiring remains client work.
- Gemini API key is packaged in the app and must be removed.
- Auth and admin authorization are cosmetic; default profile claims the user is logged in.
- Failures become a confident BMW result, and many technical values are fabricated.
- “VERIFIED”, LiDAR depth, VIN/OEM cross-reference and precision claims are not backed by sensors or sources.
- UI expects non-null values while honest recognition often produces unknowns.
- Local IDs and timestamp milliseconds differ from backend UUID/ISO-8601 conventions.
- Room seed replacement can overwrite local state; no sync/conflict strategy exists.
- Garage status is embedded on `Vehicle`, but it is user-specific and belongs in a join table.
- Admin UI lacks server data, roles, pagination and authorization.

## 11. Integration recommendations

Add a Supabase Android client and a Retrofit API client. Map backend snake_case DTOs explicitly (or configure Moshi naming), model nullable/unknown values, ISO timestamps, confidence in `[0,1]`, provenance (`ai_inferred` vs `verified`), scan status, and structured errors. Upload multipart images from content streams, not full in-memory base64. Treat HTTP 401 as session refresh/sign-in, 422 as user-correctable input, 429 as retryable, and 5xx as unavailable. Keep Room only as an optional offline cache with backend UUIDs as stable keys. Hide admin navigation unless a server-validated role permits it.

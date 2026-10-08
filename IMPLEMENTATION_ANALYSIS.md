# CARVIS Garage — implementation analysis

## 1. Existing Android architecture

CARVIS is a native Android application using Kotlin, Gradle, Jetpack Compose, Material 3, Navigation Compose, Coil, Room, OkHttp and a single `CarVisionViewModel`. `MainActivity` owns the Compose `NavHost`; screens receive state and callbacks. Network calls are centralized in `CarVisionApi`, then wrapped by `VehicleRepository`, then exposed as `StateFlow` by the ViewModel. No Flutter code is present or required.

Room contains legacy `VehicleEntity` and `ScanTelemetryEntity` tables, but the active repository currently keeps API results in memory. The Garage implementation will preserve this architecture and keep the backend as the source of truth rather than creating a second local collection database.

## 2. Existing backend architecture

The backend is a FastAPI modular monolith using async SQLAlchemy 2, Pydantic v2, Alembic, Supabase Auth/JWT, Supabase Storage, Gemini and an optional fail-open Upstash REST cache. API modules live under `app/api/v1`, durable models in `app/models/entities.py`, schemas in `app/schemas`, and application services in `app/services`.

The current scan service uploads the image to private Supabase Storage, asks Gemini for structured identification, matches that identification against the catalog, and persists a `VehicleScan`. The existing `favorites` API is only a bookmark list and has no rarity, XP, collection statistics or scan provenance.

## 3. Existing database architecture

Alembic revision `0001` creates `user_profiles`, `vehicles`, `vehicle_specifications`, `vehicle_scans`, `ai_requests` and `favorites`. UUID primary keys and user-scoped indexes are already used. PostgreSQL/Supabase is production truth; SQLite is used only for isolated automated tests.

The Garage requires a new Alembic revision that adds catalog-owned collection metadata to `vehicles` and creates `garage_entries`. `favorites` will remain temporarily for backward compatibility but will no longer drive the Android Garage.

## 4. Existing authentication architecture

Android authenticates directly with Supabase Auth (email/password or Google ID token exchange), stores the access token in process memory, and sends it as a Bearer token to FastAPI. FastAPI verifies the Supabase JWT and resolves or creates a `UserProfile`. Every Garage query must filter by that resolved profile ID. No service-role credential is shipped to Android.

## 5. Existing vehicle identification flow

`ScannerScreen` or the dashboard photo picker provides a bitmap to `CarVisionViewModel.analyzePhoto`. The repository uploads it to `POST /scans`. The backend validates and stores the image, calls/caches Gemini, matches the result to `vehicles`, and returns a `ScanRead` with the matched vehicle and a signed image URL. Android then navigates to `VehicleResultScreen`.

Only catalog-matched vehicles can be collected because rarity and XP must come from database fields, not from AI output.

## 6. Existing scan flow

Scans and their images are already user-owned. A scan may complete without a catalog vehicle match. Today the result screen toggles `/favorites/{vehicle_id}` and the repository reloads a broad snapshot. There is no XP award. The new flow will retain scan history, explicitly call `POST /garage` only after the user presses “Ajouter au garage”, and never award XP during `POST /scans`.

## 7. Existing navigation

Routes are Splash, Onboarding, Auth, Dashboard, History, Scanner, VehicleResult, Compare, Garage and Admin. The bottom bar already contains Home, History, a prominent central Scanner action, Compare and Garage. Garage and History currently share one tabbed `GarageProfileScreen`; Garage will become a dedicated collection screen while History remains separate. A Garage detail route will be added.

## 8. Existing UI components

The app has reusable top/bottom bars and a simplified dark automotive theme. `DashboardScreen` is already focused on scanning and a recent real result. `VehicleResultScreen` is the current discovery/detail surface. `GarageProfileScreen` is a dense list and profile/logout hybrid. The new UI will add focused `VehicleDiscoveryCard`, `GarageVehicleCard`, Garage loading/empty/error content, a two-column grid, a compact statistics header and a dedicated detail view. It will retain the current palette, navigation template and spacing direction rather than redesigning unrelated screens.

## 9. Files that must be modified

- Backend: `app/models/entities.py`, `app/models/__init__.py`, `app/schemas/domain.py`, `app/api/v1/router.py` and tests.
- Android: `data/model/Vehicle.kt`, `data/remote/CarVisionApi.kt`, `data/repository/VehicleRepository.kt`, `ui/viewmodel/CarVisionViewModel.kt`, `MainActivity.kt`, `ui/navigation/Screen.kt`, `ui/screens/VehicleResultScreen.kt` and the existing Garage/History screen wiring.
- Documentation: backend/root API and feature documentation where the old favorites-as-garage contract is described.

## 10. New files that should be created

- Alembic revision `0002_garage_collection.py`.
- Backend `app/repositories/garage.py`, `app/services/xp_service.py`, `app/services/garage_service.py`, `app/api/v1/garage.py` and Garage tests.
- Android `GarageScreen.kt`, `GarageDetailScreen.kt` and reusable Garage/discovery components as focused files.
- `GARAGE_FEATURE.md`, `XP_SYSTEM.md` and `GARAGE_API.md`.

## 11. Potential conflicts and risks

- The dirty worktree contains ongoing Gradle, Google Auth, API and UI changes; all Garage edits must preserve them.
- `favorites` is currently named and presented as Garage. Removing it would break older clients, so it should remain as a deprecated compatibility API while Android moves to `/garage`.
- The active `.env` currently has `DEBUG=release`, which prevents test collection unless overridden with `DEBUG=false`.
- The backend venv currently lacks `pytest`; the system Python 3.11 test installation is the available baseline runner.
- Existing catalog rows will need rarity/base XP values. Migration defaults make rows valid, but production-quality rarity classification must be curated in Supabase; AI must never invent it.
- Signed scan image URLs expire. `garage_entries` should store a durable Storage object path/reference when derived from an owned scan, and the API should mint a fresh signed URL for display. A client URL may be accepted only as fallback metadata; XP never depends on it.
- Concurrency can bypass an application-only duplicate check. A database unique constraint plus transactional conflict handling is mandatory.
- Deleting and re-adding must not farm XP. Entries should be soft-removed and reactivated without granting XP again, preserving the unique user/vehicle collection ledger.

## 12. Recommended implementation strategy

1. Extend `Vehicle` with database-owned `rarity`, `base_xp`, `rarity_score` and optional `market_value`.
2. Add `GarageEntry` with a permanent unique `(user_id, vehicle_id)` ledger, captured scan/image provenance, awarded XP and optional removal timestamp.
3. Centralize configurable rarity XP, capped market bonus and level thresholds in a pure backend XP service. Derive total XP from the immutable awards ledger; count only active entries.
4. Build a transactional, idempotent Garage service. On duplicate retries return the existing entry with zero newly awarded XP. On reactivation, restore the card without awarding again.
5. Expose authenticated, user-isolated list/stats/detail/create/delete/check routes. Keep PostgreSQL authoritative and do not add Garage caching until usage data justifies its invalidation complexity.
6. Update Android through the existing UI → ViewModel → Repository → API chain. Replace the favorites snapshot with real Garage entries and stats. Keep the scan result independent from collection creation.
7. Implement a dedicated two-column Garage grid using real API images/data, rarity treatment, loading/empty/error states, retry, detail navigation and subtle add-XP feedback.
8. Add unit/service/API tests for calculation, duplicate requests, reactivation, isolation, invalid vehicles, authentication and client XP tampering; then run backend and Android builds/tests.
9. Audit production code for mock Garage/XP/rarity data and document the required Supabase migration and catalog curation. Do not claim the cloud migration was applied unless it is actually verified against Supabase.

# REST API

Base URL: `/api/v1`. All routes below require a Supabase bearer access token. Success is JSON except 204 deletes. Errors use `{"error":{"code":"...","message":"...","details":null,"request_id":"..."}}`. Common responses: 401 invalid/missing token, 404 absent/foreign resource, 413 oversized upload, 415 unsupported type, 422 invalid request/image, 429 future rate limit, 502 provider/storage failure, 503 missing configuration, 504 timeout.

## Profile

`GET /users/me` returns the current profile. Example: `curl -H "Authorization: Bearer $TOKEN" $API/api/v1/users/me`. Response contains UUIDs, optional name/avatar, settings and ISO timestamps.

`PATCH /users/me` accepts any subset of `name`, `avatar_url`, `settings`; returns the updated profile. Example JSON: `{"name":"Alex","settings":{"sound":true}}`.

## Vehicles

`GET /vehicles?q=&limit=20&offset=0` returns `{items,total,limit,offset}`. Each item has identity fields and sourced specifications. `GET /vehicles/{uuid}` returns one. `GET /vehicles/compare?ids=UUID&ids=UUID` returns 2–4 vehicles in requested order.

## Scans

`POST /scans` accepts multipart `image` (JPEG/PNG/WebP, default max 10 MiB) and optional `context` (max 500 chars). Example: `curl -H "Authorization: Bearer $TOKEN" -F image=@car.jpg -F context="rear view" $API/api/v1/scans`. It returns 201 with status, validated `identification`, optional verified catalog `vehicle`, confidence `[0,1]`, metadata, and expiring `image_url`.

`GET /scans?limit=20&offset=0` returns the user's paginated history. `GET /scans/{uuid}` returns only an owned scan. `DELETE /scans/{uuid}` removes the private object and record, returning 204.

## Favorites / garage

`GET /favorites` lists catalog vehicles saved by the current user. `PUT /favorites/{vehicle_uuid}` is idempotent and returns the vehicle. `DELETE /favorites/{vehicle_uuid}` is idempotent and returns 204.

## Operations

Unauthenticated `GET /health` is liveness. `GET /ready` reports whether required configuration is present; it does not perform billable external calls or expose secrets. OpenAPI examples and exact schemas are available at `/docs` and `/openapi.json`.

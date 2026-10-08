# Garage API

Base path: `/api/v1/garage`. Every endpoint requires `Authorization: Bearer <supabase_access_token>`.

## List collection

`GET /garage?limit=20&offset=0`

Returns `{ items, total, limit, offset }`. Items contain entry ID, minimal vehicle identity, rarity, immutable XP, captured timestamp and a fresh image URL. Only active entries belonging to the current user are returned.

## Statistics

`GET /garage/stats`

```json
{
  "cars_collected": 4,
  "total_xp": 920,
  "level": 2,
  "next_level_xp": 1200,
  "progress_to_next_level": 0.6
}
```

## Check a vehicle

`GET /garage/check/{vehicle_id}` returns `collected` and the active `entry_id` when present.

## Entry detail

`GET /garage/{entry_id}` returns one active entry owned by the current user. A missing, removed or foreign entry returns `garage_entry_not_found`.

## Add to Garage

`POST /garage`

```json
{
  "vehicle_id": "2eb7c0f5-cf75-4df7-a2bb-e1cc36681858",
  "scan_id": "4ddf7016-c694-41d4-807f-73c9a8764337"
}
```

The backend validates the catalog vehicle and optional owned matching scan, calculates XP, and creates the ledger entry. The client cannot submit XP or rarity.

```json
{
  "id": "5738a77d-9d4c-48ca-bb77-03e63a642b87",
  "vehicle": {
    "id": "2eb7c0f5-cf75-4df7-a2bb-e1cc36681858",
    "brand": "BMW",
    "model": "M4 Competition",
    "generation": "G82",
    "year": 2021,
    "image_url": "https://catalog.example/bmw-m4.jpg"
  },
  "rarity": "EPIC",
  "xp_earned": 600,
  "captured_at": "2026-09-11T10:30:00Z",
  "captured_image_url": "https://signed-storage-url.example/...",
  "newly_awarded_xp": 600,
  "already_collected": false
}
```

Repeated requests are successful and idempotent: they return the same entry with `newly_awarded_xp: 0` and `already_collected: true`. A previously removed entry is reactivated with no new XP.

## Remove from active Garage

`DELETE /garage/{entry_id}` returns `204`. Removal is soft so re-adding cannot farm XP.

## Error envelope

Errors use the existing backend envelope, for example:

```json
{
  "error": {
    "code": "vehicle_not_found",
    "message": "Vehicle was not found",
    "request_id": "..."
  }
}
```

Relevant status codes: `401 authentication_required`, `404 vehicle_not_found`, `404 scan_not_found`, `404 garage_entry_not_found`, and `422` for invalid or client-manipulated request fields.

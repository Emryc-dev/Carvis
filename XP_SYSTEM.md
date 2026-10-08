# CARVIS XP system

## Principles

XP represents a unique catalog discovery, not a scan count. It is awarded once when the user explicitly collects a vehicle. Repeated scans, retries, refreshes, reinstalls, duplicate API calls and delete/re-add cycles award no additional XP.

All calculations are performed by `XPService` on the backend. Android submits no XP or rarity value.

## Rarity base values

| Rarity | Base XP |
|---|---:|
| COMMON | 50 |
| UNCOMMON | 100 |
| RARE | 250 |
| EPIC | 500 |
| MYTHIC | 1000 |

The centralized `XPPolicy` owns these values and can be injected in tests or changed without touching API/UI code. A curated `vehicles.base_xp` may raise the rarity floor for a special catalog entry, but cannot lower it.

## Market value bonus

The initial formula is:

`rarity_xp = max(policy rarity base, vehicle.base_xp)`

`raw_market_bonus = floor(market_value / 50,000) × 10`

`market_bonus = min(raw_market_bonus, floor(rarity_xp × 0.20))`

`final_xp = rarity_xp + market_bonus`

A missing or negative value contributes zero. The 20% cap guarantees that market price remains a secondary signal; an expensive COMMON vehicle cannot overtake the next rarity tier through price alone.

## Levels

The centralized thresholds are `0, 500, 1,200, 2,500, 5,000, 9,000, 15,000, 24,000, 36,000, 52,000`. The API returns the current level, next threshold and normalized progress. Thresholds are policy data, not UI constants.

## Source of truth

Total XP is derived from the immutable sum of `garage_entries.xp_earned`, including soft-removed entries because the discovery award is permanent. Active Garage count includes only entries whose `removed_at` is null. This avoids a duplicated mutable `user_profiles.total_xp` field and eliminates synchronization drift.

## Anti-abuse controls

- Unique database constraint on `(user_id, vehicle_id)`.
- Transactional server-side create with `IntegrityError` recovery for concurrent requests.
- Permanent soft-delete ledger; reactivation awards zero XP.
- Catalog-owned rarity/base XP/market value.
- Request schema forbids extra fields such as `xp_earned` or `rarity`.
- Optional scan provenance must be owned by the authenticated user and match the vehicle.

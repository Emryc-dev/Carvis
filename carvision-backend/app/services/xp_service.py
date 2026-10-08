from dataclasses import dataclass, field

from app.models.entities import Vehicle, VehicleRarity


@dataclass(frozen=True)
class XPPolicy:
    rarity_base_xp: dict[VehicleRarity, int] = field(default_factory=lambda: {
        VehicleRarity.common: 50,
        VehicleRarity.uncommon: 100,
        VehicleRarity.rare: 250,
        VehicleRarity.epic: 500,
        VehicleRarity.mythic: 1000,
    })
    market_value_step: int = 50_000
    market_bonus_per_step: int = 10
    market_bonus_cap_ratio: float = 0.20
    level_thresholds: tuple[int, ...] = (0, 500, 1_200, 2_500, 5_000, 9_000, 15_000, 24_000, 36_000, 52_000)


@dataclass(frozen=True)
class XPBreakdown:
    rarity_xp: int
    market_bonus: int

    @property
    def total(self) -> int:
        return self.rarity_xp + self.market_bonus


class XPService:
    """Authoritative, deterministic Garage progression calculations."""

    def __init__(self, policy: XPPolicy | None = None):
        self.policy = policy or XPPolicy()

    def calculate(self, vehicle: Vehicle) -> XPBreakdown:
        configured_base = self.policy.rarity_base_xp[vehicle.rarity]
        # Catalog base_xp may be curated upward, but never below the rarity floor.
        rarity_xp = max(configured_base, vehicle.base_xp)
        value = max(0, vehicle.market_value or 0)
        uncapped = (value // self.policy.market_value_step) * self.policy.market_bonus_per_step
        cap = int(rarity_xp * self.policy.market_bonus_cap_ratio)
        return XPBreakdown(rarity_xp=rarity_xp, market_bonus=min(uncapped, cap))

    def level(self, total_xp: int) -> tuple[int, int | None, float]:
        total_xp = max(0, total_xp)
        thresholds = self.policy.level_thresholds
        index = max(i for i, threshold in enumerate(thresholds) if threshold <= total_xp)
        level = index + 1
        if index == len(thresholds) - 1:
            return level, None, 1.0
        current, following = thresholds[index], thresholds[index + 1]
        progress = (total_xp - current) / (following - current)
        return level, following, round(progress, 4)

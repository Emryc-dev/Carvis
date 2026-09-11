from pydantic import BaseModel, ConfigDict, Field, field_validator


class AIIdentification(BaseModel):
    model_config = ConfigDict(extra="forbid")
    brand: str = Field(min_length=1, max_length=80)
    model: str = Field(min_length=1, max_length=120)
    generation: str | None = Field(None, max_length=80)
    approximate_year: int | None = Field(None, ge=1886, le=2100)
    vehicle_type: str | None = Field(None, max_length=80)
    engine: str | None = Field(None, max_length=200)
    horsepower: int | None = Field(None, ge=1, le=5000)
    torque: str | None = Field(None, max_length=100)
    transmission: str | None = Field(None, max_length=160)
    drivetrain: str | None = Field(None, max_length=120)
    fuel_type: str | None = Field(None, max_length=80)
    consumption: str | None = Field(None, max_length=120)
    color: str | None = Field(None, max_length=80)
    chassis_code: str | None = Field(None, max_length=80)
    confidence: float = Field(ge=0, le=1)
    visual_evidence: list[str] = Field(default_factory=list, max_length=12)
    uncertainties: list[str] = Field(default_factory=list, max_length=12)

    @field_validator("brand", "model")
    @classmethod
    def no_unknown_identity(cls, value: str) -> str:
        value = value.strip()
        if value.lower() in {"unknown", "n/a", "none"}:
            raise ValueError("identity must be a meaningful value")
        return value

"""Initial normalized CarVision schema."""
from alembic import op
import sqlalchemy as sa
from sqlalchemy.dialects import postgresql

revision = "0001"
down_revision = None
branch_labels = None
depends_on = None


def upgrade() -> None:
    verification = sa.Enum("ai_inferred", "partially_verified", "verified", name="verificationstatus")
    scanstatus = sa.Enum("processing", "completed", "failed", name="scanstatus")
    op.create_table("user_profiles", sa.Column("id", sa.Uuid(), primary_key=True), sa.Column("auth_user_id", sa.Uuid(), nullable=False), sa.Column("name", sa.String(120)), sa.Column("avatar_url", sa.Text()), sa.Column("settings", sa.JSON(), nullable=False), sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False), sa.Column("updated_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False), sa.UniqueConstraint("auth_user_id"))
    op.create_index("ix_user_profiles_auth_user_id", "user_profiles", ["auth_user_id"], unique=True)
    op.create_table("vehicles", sa.Column("id", sa.Uuid(), primary_key=True), sa.Column("brand", sa.String(80), nullable=False), sa.Column("model", sa.String(120), nullable=False), sa.Column("generation", sa.String(80)), sa.Column("year", sa.Integer()), sa.Column("vehicle_type", sa.String(80)), sa.Column("image_url", sa.Text()), sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False))
    op.create_index("ix_vehicle_identity", "vehicles", ["brand", "model", "generation", "year"]); op.create_index("ix_vehicles_brand", "vehicles", ["brand"]); op.create_index("ix_vehicles_model", "vehicles", ["model"])
    op.create_table("vehicle_specifications", sa.Column("id", sa.Uuid(), primary_key=True), sa.Column("vehicle_id", sa.Uuid(), sa.ForeignKey("vehicles.id", ondelete="CASCADE"), nullable=False), sa.Column("engine", sa.String(200)), sa.Column("horsepower", sa.Integer()), sa.Column("torque", sa.String(100)), sa.Column("transmission", sa.String(160)), sa.Column("drivetrain", sa.String(120)), sa.Column("fuel_type", sa.String(80)), sa.Column("consumption", sa.String(120)), sa.Column("zero_to_sixty", sa.Float()), sa.Column("top_speed_mph", sa.Integer()), sa.Column("curb_weight_lbs", sa.Integer()), sa.Column("source_name", sa.String(160)), sa.Column("source_url", sa.Text()), sa.Column("verification_status", verification, nullable=False), sa.Column("extra", sa.JSON(), nullable=False))
    op.create_index("ix_vehicle_specifications_vehicle_id", "vehicle_specifications", ["vehicle_id"])
    op.create_table("vehicle_scans", sa.Column("id", sa.Uuid(), primary_key=True), sa.Column("user_id", sa.Uuid(), sa.ForeignKey("user_profiles.id", ondelete="CASCADE"), nullable=False), sa.Column("vehicle_id", sa.Uuid(), sa.ForeignKey("vehicles.id", ondelete="SET NULL")), sa.Column("status", scanstatus, nullable=False), sa.Column("image_path", sa.Text()), sa.Column("image_hash", sa.String(64), nullable=False), sa.Column("mime_type", sa.String(40), nullable=False), sa.Column("width", sa.Integer(), nullable=False), sa.Column("height", sa.Integer(), nullable=False), sa.Column("identification", sa.JSON()), sa.Column("confidence", sa.Float()), sa.Column("error_code", sa.String(80)), sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False), sa.Column("completed_at", sa.DateTime(timezone=True)))
    op.create_index("ix_scan_user_created", "vehicle_scans", ["user_id", "created_at"]); op.create_index("ix_vehicle_scans_user_id", "vehicle_scans", ["user_id"]); op.create_index("ix_vehicle_scans_vehicle_id", "vehicle_scans", ["vehicle_id"]); op.create_index("ix_vehicle_scans_image_hash", "vehicle_scans", ["image_hash"])
    op.create_table("ai_requests", sa.Column("id", sa.Uuid(), primary_key=True), sa.Column("scan_id", sa.Uuid(), sa.ForeignKey("vehicle_scans.id", ondelete="CASCADE"), nullable=False), sa.Column("provider", sa.String(40), nullable=False), sa.Column("model", sa.String(100), nullable=False), sa.Column("status", sa.String(30), nullable=False), sa.Column("latency_ms", sa.Integer()), sa.Column("response", sa.JSON()), sa.Column("error_code", sa.String(80)), sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False)); op.create_index("ix_ai_requests_scan_id", "ai_requests", ["scan_id"])
    op.create_table("favorites", sa.Column("id", sa.Uuid(), primary_key=True), sa.Column("user_id", sa.Uuid(), sa.ForeignKey("user_profiles.id", ondelete="CASCADE"), nullable=False), sa.Column("vehicle_id", sa.Uuid(), sa.ForeignKey("vehicles.id", ondelete="CASCADE"), nullable=False), sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False), sa.UniqueConstraint("user_id", "vehicle_id")); op.create_index("ix_favorites_user_id", "favorites", ["user_id"]); op.create_index("ix_favorites_vehicle_id", "favorites", ["vehicle_id"])


def downgrade() -> None:
    for table in ("favorites", "ai_requests", "vehicle_scans", "vehicle_specifications", "vehicles", "user_profiles"): op.drop_table(table)
    sa.Enum(name="scanstatus").drop(op.get_bind()); sa.Enum(name="verificationstatus").drop(op.get_bind())

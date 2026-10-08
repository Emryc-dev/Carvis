"""Add durable Garage collection, rarity and XP metadata."""
from alembic import op
import sqlalchemy as sa

revision = "0002"
down_revision = "0001"
branch_labels = None
depends_on = None


def upgrade() -> None:
    rarity = sa.Enum("COMMON", "UNCOMMON", "RARE", "EPIC", "MYTHIC", name="vehiclerarity")
    rarity.create(op.get_bind(), checkfirst=True)
    op.add_column("vehicles", sa.Column("rarity", rarity, nullable=False, server_default="COMMON"))
    op.add_column("vehicles", sa.Column("base_xp", sa.Integer(), nullable=False, server_default="50"))
    op.add_column("vehicles", sa.Column("rarity_score", sa.Integer(), nullable=False, server_default="0"))
    op.add_column("vehicles", sa.Column("market_value", sa.Integer(), nullable=True))
    op.create_check_constraint("ck_vehicle_base_xp_positive", "vehicles", "base_xp > 0")
    op.create_check_constraint("ck_vehicle_rarity_score_range", "vehicles", "rarity_score >= 0 AND rarity_score <= 100")
    op.create_check_constraint("ck_vehicle_market_value_nonnegative", "vehicles", "market_value IS NULL OR market_value >= 0")

    op.create_table(
        "garage_entries",
        sa.Column("id", sa.Uuid(), primary_key=True),
        sa.Column("user_id", sa.Uuid(), sa.ForeignKey("user_profiles.id", ondelete="CASCADE"), nullable=False),
        sa.Column("vehicle_id", sa.Uuid(), sa.ForeignKey("vehicles.id", ondelete="RESTRICT"), nullable=False),
        sa.Column("scan_id", sa.Uuid(), sa.ForeignKey("vehicle_scans.id", ondelete="SET NULL"), nullable=True),
        sa.Column("captured_image_path", sa.Text(), nullable=True),
        sa.Column("captured_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("xp_earned", sa.Integer(), nullable=False),
        sa.Column("created_at", sa.DateTime(timezone=True), server_default=sa.func.now(), nullable=False),
        sa.Column("removed_at", sa.DateTime(timezone=True), nullable=True),
        sa.UniqueConstraint("user_id", "vehicle_id", name="uq_garage_user_vehicle"),
        sa.CheckConstraint("xp_earned > 0", name="ck_garage_xp_positive"),
    )
    op.create_index("ix_garage_entries_user_id", "garage_entries", ["user_id"])
    op.create_index("ix_garage_entries_vehicle_id", "garage_entries", ["vehicle_id"])
    op.create_index("ix_garage_entries_scan_id", "garage_entries", ["scan_id"])
    op.create_index("ix_garage_user_captured", "garage_entries", ["user_id", "captured_at"])


def downgrade() -> None:
    op.drop_table("garage_entries")
    op.drop_constraint("ck_vehicle_market_value_nonnegative", "vehicles", type_="check")
    op.drop_constraint("ck_vehicle_rarity_score_range", "vehicles", type_="check")
    op.drop_constraint("ck_vehicle_base_xp_positive", "vehicles", type_="check")
    op.drop_column("vehicles", "market_value")
    op.drop_column("vehicles", "rarity_score")
    op.drop_column("vehicles", "base_xp")
    op.drop_column("vehicles", "rarity")
    sa.Enum(name="vehiclerarity").drop(op.get_bind(), checkfirst=True)

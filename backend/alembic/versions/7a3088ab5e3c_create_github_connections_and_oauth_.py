"""create github_connections and oauth_states tables

Revision ID: 7a3088ab5e3c
Revises: 4366d86b35c5
Create Date: 2026-10-17 06:30:38.350488

"""
from typing import Sequence, Union

from alembic import op
import sqlalchemy as sa


# revision identifiers, used by Alembic.
revision: str = '7a3088ab5e3c'
down_revision: Union[str, Sequence[str], None] = '4366d86b35c5'
branch_labels: Union[str, Sequence[str], None] = None
depends_on: Union[str, Sequence[str], None] = None


def upgrade() -> None:
    conn = op.get_bind()
    inspector = sa.inspect(conn)
    tables = inspector.get_table_names()

    if "github_connections" not in tables:
        op.create_table(
            "github_connections",
            sa.Column("id", sa.String(length=36), nullable=False),
            sa.Column("user_id", sa.String(length=36), nullable=False),
            sa.Column("github_user_id", sa.String(length=100), nullable=False),
            sa.Column("github_username", sa.String(length=150), nullable=False),
            sa.Column("encrypted_access_token", sa.Text(), nullable=False),
            sa.Column("scope", sa.String(length=255), nullable=False),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
            sa.Column("updated_at", sa.DateTime(timezone=True), nullable=False),
            sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
            sa.PrimaryKeyConstraint("id"),
        )
        op.create_index(op.f("ix_github_connections_user_id"), "github_connections", ["user_id"], unique=True)

    if "oauth_states" not in tables:
        op.create_table(
            "oauth_states",
            sa.Column("state", sa.String(length=64), nullable=False),
            sa.Column("user_id", sa.String(length=36), nullable=False),
            sa.Column("expires_at", sa.DateTime(timezone=True), nullable=False),
            sa.Column("created_at", sa.DateTime(timezone=True), nullable=False),
            sa.ForeignKeyConstraint(["user_id"], ["users.id"], ondelete="CASCADE"),
            sa.PrimaryKeyConstraint("state"),
        )
        op.create_index(op.f("ix_oauth_states_user_id"), "oauth_states", ["user_id"], unique=False)


def downgrade() -> None:
    conn = op.get_bind()
    inspector = sa.inspect(conn)
    tables = inspector.get_table_names()

    if "oauth_states" in tables:
        op.drop_table("oauth_states")
    if "github_connections" in tables:
        op.drop_index(op.f("ix_github_connections_user_id"), table_name="github_connections")
        op.drop_table("github_connections")

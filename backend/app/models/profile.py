import uuid
from datetime import datetime, timezone
from sqlalchemy import String, DateTime, ForeignKey, Text
from sqlalchemy.orm import Mapped, mapped_column, relationship
from app.db.base import Base

class CareerProfile(Base):
    __tablename__ = "career_profiles"

    id: Mapped[str] = mapped_column(String(36), primary_key=True, default=lambda: str(uuid.uuid4()))
    user_id: Mapped[str] = mapped_column(
        String(36),
        ForeignKey("users.id", ondelete="CASCADE"),
        unique=True,
        index=True,
        nullable=False,
    )
    
    # Career attributes
    level: Mapped[str] = mapped_column(String(100), default="", nullable=False)
    target_role: Mapped[str] = mapped_column(String(150), default="", nullable=False)
    location: Mapped[str] = mapped_column(String(150), default="", nullable=False)
    work_mode: Mapped[str] = mapped_column(String(50), default="NO_PREFERENCE", nullable=False)
    
    # Multi-valued fields stored as JSON arrays in Text for SQLite/PostgreSQL compatibility
    skills_json: Mapped[str] = mapped_column(Text, default="[]", nullable=False)
    languages_json: Mapped[str] = mapped_column(Text, default="[]", nullable=False)
    frameworks_json: Mapped[str] = mapped_column(Text, default="[]", nullable=False)
    interests_json: Mapped[str] = mapped_column(Text, default="[]", nullable=False)
    
    created_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        nullable=False,
    )
    updated_at: Mapped[datetime] = mapped_column(
        DateTime(timezone=True),
        default=lambda: datetime.now(timezone.utc),
        onupdate=lambda: datetime.now(timezone.utc),
        nullable=False,
    )

    user = relationship("User", back_populates="career_profile")

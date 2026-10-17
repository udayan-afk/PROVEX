import json
from typing import Optional
from sqlalchemy.orm import Session
from sqlalchemy import select
from fastapi import HTTPException, status

from app.models.user import User
from app.models.profile import CareerProfile
from app.schemas.profile import CareerProfileUpdate, CareerProfileResponse

class ProfileService:
    @staticmethod
    def _to_response(profile: CareerProfile, user: User) -> CareerProfileResponse:
        skills = json.loads(profile.skills_json) if profile.skills_json else []
        languages = json.loads(profile.languages_json) if profile.languages_json else []
        frameworks = json.loads(profile.frameworks_json) if profile.frameworks_json else []
        interests = json.loads(profile.interests_json) if profile.interests_json else []

        return CareerProfileResponse(
            id=profile.id,
            user_id=profile.user_id,
            name=user.full_name,
            email=user.email,
            level=profile.level,
            target_role=profile.target_role,
            location=profile.location,
            work_mode=profile.work_mode,
            skills=skills,
            languages=languages,
            frameworks=frameworks,
            interests=interests,
            created_at=profile.created_at,
            updated_at=profile.updated_at,
        )

    @staticmethod
    def get_profile(db: Session, user: User) -> Optional[CareerProfileResponse]:
        stmt = select(CareerProfile).where(CareerProfile.user_id == user.id)
        profile = db.execute(stmt).scalar_one_or_none()
        if not profile:
            return None
        return ProfileService._to_response(profile, user)

    @staticmethod
    def upsert_profile(db: Session, user: User, data: CareerProfileUpdate) -> CareerProfileResponse:
        stmt = select(CareerProfile).where(CareerProfile.user_id == user.id)
        profile = db.execute(stmt).scalar_one_or_none()

        skills_json = json.dumps(data.skills)
        languages_json = json.dumps(data.languages)
        frameworks_json = json.dumps(data.frameworks)
        interests_json = json.dumps(data.interests)

        if not profile:
            profile = CareerProfile(
                user_id=user.id,
                level=data.level.strip(),
                target_role=data.target_role.strip(),
                location=data.location.strip(),
                work_mode=data.work_mode.strip() or "NO_PREFERENCE",
                skills_json=skills_json,
                languages_json=languages_json,
                frameworks_json=frameworks_json,
                interests_json=interests_json,
            )
            db.add(profile)
        else:
            profile.level = data.level.strip()
            profile.target_role = data.target_role.strip()
            profile.location = data.location.strip()
            profile.work_mode = data.work_mode.strip() or "NO_PREFERENCE"
            profile.skills_json = skills_json
            profile.languages_json = languages_json
            profile.frameworks_json = frameworks_json
            profile.interests_json = interests_json

        db.commit()
        db.refresh(profile)
        return ProfileService._to_response(profile, user)

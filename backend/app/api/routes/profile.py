from fastapi import APIRouter, Depends, status, HTTPException
from sqlalchemy.orm import Session
from typing import Optional

from app.db.session import get_db
from app.models.user import User
from app.api.deps import get_current_user
from app.schemas.profile import CareerProfileUpdate, CareerProfileResponse
from app.services.profile_service import ProfileService

router = APIRouter(prefix="/profile", tags=["Career Profile"])

@router.get(
    "",
    response_model=CareerProfileResponse,
    status_code=status.HTTP_200_OK,
    summary="Get current user's career profile",
)
def get_my_profile(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> CareerProfileResponse:
    profile = ProfileService.get_profile(db, current_user)
    if not profile:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail="Career profile not found for this user.",
        )
    return profile

@router.put(
    "",
    response_model=CareerProfileResponse,
    status_code=status.HTTP_200_OK,
    summary="Create or update current user's career profile",
)
def upsert_my_profile(
    data: CareerProfileUpdate,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> CareerProfileResponse:
    return ProfileService.upsert_profile(db, current_user, data)

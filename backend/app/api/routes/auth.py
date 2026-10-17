from fastapi import APIRouter, Depends, status, Request
from sqlalchemy.orm import Session
from typing import Dict, Any

from app.db.session import get_db
from app.models.user import User
from app.schemas.auth import (
    RegisterRequest,
    LoginRequest,
    TokenResponse,
    RefreshTokenRequest,
    LogoutRequest,
)
from app.schemas.user import UserResponse
from app.services.auth_service import AuthService
from app.api.deps import get_current_user
from app.core.rate_limiter import check_auth_rate_limit

router = APIRouter(prefix="/auth", tags=["Authentication"])

@router.post(
    "/register",
    response_model=TokenResponse,
    status_code=status.HTTP_201_CREATED,
    summary="Register a new PROVEX user account",
)
def register(
    request: Request,
    req: RegisterRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    check_auth_rate_limit(request)
    return AuthService.register_user(db, req)

@router.post(
    "/login",
    response_model=TokenResponse,
    status_code=status.HTTP_200_OK,
    summary="Log into existing user account and issue tokens",
)
def login(
    request: Request,
    req: LoginRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    check_auth_rate_limit(request)
    return AuthService.authenticate_user(db, req)

@router.post(
    "/refresh",
    response_model=TokenResponse,
    status_code=status.HTTP_200_OK,
    summary="Rotate refresh token and issue a new access token",
)
def refresh_token(
    req: RefreshTokenRequest,
    db: Session = Depends(get_db),
) -> TokenResponse:
    return AuthService.refresh_session(db, req.refresh_token)

@router.post(
    "/logout",
    status_code=status.HTTP_200_OK,
    summary="Revoke session and refresh token",
)
def logout(
    req: LogoutRequest,
    db: Session = Depends(get_db),
) -> Dict[str, str]:
    AuthService.logout(db, refresh_token_str=req.refresh_token)
    return {"message": "Successfully logged out and session revoked."}

@router.get(
    "/me",
    response_model=UserResponse,
    status_code=status.HTTP_200_OK,
    summary="Get current authenticated user profile",
)
def get_me(
    current_user: User = Depends(get_current_user),
) -> UserResponse:
    return UserResponse.model_validate(current_user)

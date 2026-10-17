from sqlalchemy.orm import Session
from sqlalchemy import select
from fastapi import HTTPException, status
from datetime import datetime, timezone
import jwt

from app.models.user import User
from app.models.refresh_token import RefreshToken
from app.schemas.auth import RegisterRequest, LoginRequest, TokenResponse
from app.schemas.user import UserResponse
from app.core.security import (
    hash_password,
    verify_password,
    hash_token,
    create_access_token,
    create_refresh_token,
    decode_token,
)

class AuthService:
    @staticmethod
    def register_user(db: Session, req: RegisterRequest) -> TokenResponse:
        # Check existing user
        stmt = select(User).where(User.email == req.email)
        existing = db.execute(stmt).scalar_one_or_none()
        if existing:
            raise HTTPException(
                status_code=status.HTTP_409_CONFLICT,
                detail="An account with that email already exists.",
            )

        hashed = hash_password(req.password)
        new_user = User(
            email=req.email,
            full_name=req.full_name,
            hashed_password=hashed,
            is_active=True,
        )
        db.add(new_user)
        db.commit()
        db.refresh(new_user)

        # Issue access & refresh tokens
        access_token = create_access_token(data={"sub": new_user.id, "email": new_user.email})
        refresh_token_str, refresh_expiry = create_refresh_token(user_id=new_user.id)
        
        # Save hashed refresh token
        db_token = RefreshToken(
            user_id=new_user.id,
            token_hash=hash_token(refresh_token_str),
            expires_at=refresh_expiry,
            revoked=False,
        )
        db.add(db_token)
        db.commit()

        user_resp = UserResponse.model_validate(new_user)
        return TokenResponse(
            access_token=access_token,
            refresh_token=refresh_token_str,
            user=user_resp,
            email=user_resp.email,
        )

    @staticmethod
    def authenticate_user(db: Session, req: LoginRequest) -> TokenResponse:
        stmt = select(User).where(User.email == req.email)
        user = db.execute(stmt).scalar_one_or_none()
        
        # Constant-time-like rejection for security
        if not user or not verify_password(req.password, user.hashed_password):
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Incorrect email or password.",
                headers={"WWW-Authenticate": "Bearer"},
            )

        if not user.is_active:
            raise HTTPException(
                status_code=status.HTTP_403_FORBIDDEN,
                detail="Account is inactive.",
            )

        # Issue access & refresh tokens
        access_token = create_access_token(data={"sub": user.id, "email": user.email})
        refresh_token_str, refresh_expiry = create_refresh_token(user_id=user.id)

        # Save hashed refresh token
        db_token = RefreshToken(
            user_id=user.id,
            token_hash=hash_token(refresh_token_str),
            expires_at=refresh_expiry,
            revoked=False,
        )
        db.add(db_token)
        db.commit()

        user_resp = UserResponse.model_validate(user)
        return TokenResponse(
            access_token=access_token,
            refresh_token=refresh_token_str,
            user=user_resp,
            email=user_resp.email,
        )

    @staticmethod
    def refresh_session(db: Session, refresh_token_str: str) -> TokenResponse:
        try:
            payload = decode_token(refresh_token_str, expected_type="refresh")
        except jwt.PyJWTError:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid or expired refresh token.",
            )

        user_id = payload.get("sub")
        if not user_id:
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Invalid refresh token claims.",
            )

        token_hash_val = hash_token(refresh_token_str)
        stmt = select(RefreshToken).where(RefreshToken.token_hash == token_hash_val)
        stored_token = db.execute(stmt).scalar_one_or_none()

        if not stored_token or stored_token.revoked:
            # Token reuse detection / revocation: If revoked token is used, invalidate all sessions for user
            if stored_token and stored_token.revoked:
                # Mark all tokens for this user revoked as precaution
                revoke_all = select(RefreshToken).where(RefreshToken.user_id == user_id)
                for t in db.execute(revoke_all).scalars():
                    t.revoked = True
                db.commit()
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Refresh token has been revoked.",
            )

        now = datetime.now(timezone.utc)
        if stored_token.expires_at.replace(tzinfo=timezone.utc) < now:
            stored_token.revoked = True
            db.commit()
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="Refresh token expired.",
            )

        # Rotate: Revoke old refresh token
        stored_token.revoked = True

        # Fetch user
        user = db.get(User, user_id)
        if not user or not user.is_active:
            db.commit()
            raise HTTPException(
                status_code=status.HTTP_401_UNAUTHORIZED,
                detail="User no longer active or exists.",
            )

        # Issue new pair
        new_access_token = create_access_token(data={"sub": user.id, "email": user.email})
        new_refresh_str, new_expiry = create_refresh_token(user_id=user.id)

        new_db_token = RefreshToken(
            user_id=user.id,
            token_hash=hash_token(new_refresh_str),
            expires_at=new_expiry,
            revoked=False,
        )
        db.add(new_db_token)
        db.commit()

        user_resp = UserResponse.model_validate(user)
        return TokenResponse(
            access_token=new_access_token,
            refresh_token=new_refresh_str,
            user=user_resp,
            email=user_resp.email,
        )

    @staticmethod
    def logout(db: Session, refresh_token_str: str | None, current_user_id: str | None = None) -> None:
        if refresh_token_str:
            token_hash_val = hash_token(refresh_token_str)
            stmt = select(RefreshToken).where(RefreshToken.token_hash == token_hash_val)
            stored_token = db.execute(stmt).scalar_one_or_none()
            if stored_token:
                stored_token.revoked = True
                db.commit()
                return

        # If refresh token not provided or not found, but user is known via access token
        if current_user_id:
            # Revoke all active refresh tokens for the user
            stmt = select(RefreshToken).where(
                RefreshToken.user_id == current_user_id,
                RefreshToken.revoked == False,
            )
            for t in db.execute(stmt).scalars():
                t.revoked = True
            db.commit()

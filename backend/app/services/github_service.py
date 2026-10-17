import secrets
import httpx
from datetime import datetime, timedelta, timezone
from typing import List, Optional
from urllib.parse import urlencode
from sqlalchemy.orm import Session
from sqlalchemy import select, delete
from fastapi import HTTPException, status

from app.core.config import settings
from app.core.security import encrypt_secret, decrypt_secret
from app.models.user import User
from app.models.github import GitHubConnection, OAuthState
from app.schemas.github import (
    GitHubAuthorizeResponse,
    GitHubStatusResponse,
    GitHubRepositoryResponse,
)

class GitHubService:
    @staticmethod
    def create_authorization_url(db: Session, user: User) -> GitHubAuthorizeResponse:
        if not settings.is_github_configured:
            # If not configured, raise informative exception
            raise HTTPException(
                status_code=status.HTTP_503_SERVICE_UNAVAILABLE,
                detail="GitHub OAuth client is not configured on the PROVEX backend. Set GITHUB_CLIENT_ID and GITHUB_CLIENT_SECRET in backend/.env.",
            )

        client_id = settings.GITHUB_CLIENT_ID.strip()

        # Generate secure random state
        state_token = secrets.token_urlsafe(32)
        now_utc = datetime.now(timezone.utc)
        expires_at = now_utc + timedelta(minutes=10)

        # Clean up any expired states in table to keep DB clean
        db.execute(delete(OAuthState).where(OAuthState.expires_at < now_utc))

        # Delete any existing states for this specific user
        db.execute(delete(OAuthState).where(OAuthState.user_id == user.id))
        
        # Save new state bound to user
        oauth_state = OAuthState(
            state=state_token,
            user_id=user.id,
            expires_at=expires_at,
        )
        db.add(oauth_state)
        db.commit()

        # Build GitHub OAuth URL with minimum required scopes (read:user, repo)
        # prompt=select_account forces account chooser so different PROVEX users can sign into their own GitHub accounts
        params = {
            "client_id": client_id,
            "redirect_uri": settings.GITHUB_CALLBACK_URL,
            "scope": "read:user,repo",
            "state": state_token,
            "allow_signup": "true",
            "prompt": "select_account",
        }
        url = f"https://github.com/login/oauth/authorize?{urlencode(params)}"
        return GitHubAuthorizeResponse(authorization_url=url, state=state_token)

    @staticmethod
    async def handle_callback(db: Session, code: str, state: str) -> GitHubConnection:
        # Validate state
        stmt = select(OAuthState).where(OAuthState.state == state)
        stored_state = db.execute(stmt).scalar_one_or_none()

        if not stored_state:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="Invalid or expired OAuth state token.",
            )

        now = datetime.now(timezone.utc)
        exp = stored_state.expires_at
        if exp.tzinfo is None:
            exp = exp.replace(tzinfo=timezone.utc)
        if exp < now:
            db.delete(stored_state)
            db.commit()
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="OAuth state has expired. Please initiate authorization again.",
            )

        user_id = stored_state.user_id
        # Consume single-use state token to prevent replay
        db.delete(stored_state)
        db.commit()

        # Validate that the bound user still exists and is active
        target_user = db.get(User, user_id)
        if not target_user or not target_user.is_active:
            raise HTTPException(
                status_code=status.HTTP_400_BAD_REQUEST,
                detail="User associated with OAuth authorization no longer exists or is inactive.",
            )

        # Exchange authorization code with GitHub API
        client_id = settings.GITHUB_CLIENT_ID.strip()
        client_secret = settings.GITHUB_CLIENT_SECRET.strip()

        token_url = "https://github.com/login/oauth/access_token"
        headers = {"Accept": "application/json"}
        payload = {
            "client_id": client_id,
            "client_secret": client_secret,
            "code": code,
            "redirect_uri": settings.GITHUB_CALLBACK_URL,
        }

        async with httpx.AsyncClient(timeout=15.0) as client:
            token_resp = await client.post(token_url, headers=headers, json=payload)
            if token_resp.status_code != 200:
                raise HTTPException(
                    status_code=status.HTTP_502_BAD_GATEWAY,
                    detail="Failed to exchange authorization code with GitHub.",
                )
            
            token_data = token_resp.json()
            access_token = token_data.get("access_token")
            scope = token_data.get("scope", "")

            if not access_token:
                err_desc = token_data.get("error_description", token_data.get("error", "Unknown OAuth error"))
                raise HTTPException(
                    status_code=status.HTTP_400_BAD_REQUEST,
                    detail=f"GitHub OAuth error: {err_desc}",
                )

            # Retrieve GitHub user profile
            user_url = "https://api.github.com/user"
            gh_headers = {
                "Authorization": f"Bearer {access_token}",
                "Accept": "application/vnd.github+json",
                "User-Agent": "PROVEX-Career-Intelligence",
            }
            user_resp = await client.get(user_url, headers=gh_headers)
            if user_resp.status_code != 200:
                raise HTTPException(
                    status_code=status.HTTP_502_BAD_GATEWAY,
                    detail="Failed to fetch GitHub user identity.",
                )
            
            user_info = user_resp.json()
            gh_user_id = str(user_info.get("id"))
            gh_username = user_info.get("login")

        # Save or update GitHubConnection with encrypted token
        encrypted_token = encrypt_secret(access_token)

        stmt = select(GitHubConnection).where(GitHubConnection.user_id == user_id)
        connection = db.execute(stmt).scalar_one_or_none()

        if not connection:
            connection = GitHubConnection(
                user_id=user_id,
                github_user_id=gh_user_id,
                github_username=gh_username,
                encrypted_access_token=encrypted_token,
                scope=scope,
            )
            db.add(connection)
        else:
            connection.github_user_id = gh_user_id
            connection.github_username = gh_username
            connection.encrypted_access_token = encrypted_token
            connection.scope = scope

        db.commit()
        db.refresh(connection)
        return connection

    @staticmethod
    def get_status(db: Session, user: User) -> GitHubStatusResponse:
        stmt = select(GitHubConnection).where(GitHubConnection.user_id == user.id)
        connection = db.execute(stmt).scalar_one_or_none()

        if not connection:
            return GitHubStatusResponse(connected=False)
        
        return GitHubStatusResponse(
            connected=True,
            github_username=connection.github_username,
            connected_at=connection.created_at,
            scope=connection.scope,
        )

    @staticmethod
    def disconnect(db: Session, user: User) -> None:
        stmt = select(GitHubConnection).where(GitHubConnection.user_id == user.id)
        connection = db.execute(stmt).scalar_one_or_none()
        if connection:
            db.delete(connection)
            db.commit()

    @staticmethod
    async def get_repositories(db: Session, user: User) -> List[GitHubRepositoryResponse]:
        stmt = select(GitHubConnection).where(GitHubConnection.user_id == user.id)
        connection = db.execute(stmt).scalar_one_or_none()

        if not connection:
            raise HTTPException(
                status_code=status.HTTP_404_NOT_FOUND,
                detail="No GitHub account connected. Please connect your GitHub account first.",
            )

        token = decrypt_secret(connection.encrypted_access_token)
        url = "https://api.github.com/user/repos"
        headers = {
            "Authorization": f"Bearer {token}",
            "Accept": "application/vnd.github+json",
            "User-Agent": "PROVEX-Career-Intelligence",
        }
        params = {
            "sort": "updated",
            "per_page": 50,
            "affiliation": "owner,collaborator",
        }

        async with httpx.AsyncClient(timeout=15.0) as client:
            resp = await client.get(url, headers=headers, params=params)
            if resp.status_code == 401:
                # Token revoked on GitHub side
                db.delete(connection)
                db.commit()
                raise HTTPException(
                    status_code=status.HTTP_401_UNAUTHORIZED,
                    detail="GitHub token revoked or expired. Please reconnect your account.",
                )
            if resp.status_code != 200:
                raise HTTPException(
                    status_code=status.HTTP_502_BAD_GATEWAY,
                    detail=f"GitHub API error (HTTP {resp.status_code}).",
                )
            
            items = resp.json()

        results = []
        for item in items:
            owner_obj = item.get("owner") or {}
            results.append(GitHubRepositoryResponse(
                id=str(item.get("id")),
                name=item.get("name", ""),
                full_name=item.get("full_name", ""),
                owner=owner_obj.get("login", ""),
                description=item.get("description"),
                language=item.get("language"),
                is_private=item.get("private", False),
                html_url=item.get("html_url", ""),
                default_branch=item.get("default_branch", "main"),
                updated_at=item.get("updated_at"),
            ))
        return results

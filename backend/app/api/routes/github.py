from fastapi import APIRouter, Depends, status, Query, HTTPException
from fastapi.responses import HTMLResponse
from sqlalchemy.orm import Session
from typing import List, Dict

from app.db.session import get_db
from app.models.user import User
from app.api.deps import get_current_user
from app.schemas.github import (
    GitHubAuthorizeResponse,
    GitHubStatusResponse,
    GitHubRepositoryResponse,
)
from app.services.github_service import GitHubService

router = APIRouter(prefix="/github", tags=["GitHub Integration"])

@router.post(
    "/authorize",
    response_model=GitHubAuthorizeResponse,
    status_code=status.HTTP_200_OK,
    summary="Generate GitHub OAuth authorization URL bound to current PROVEX user",
)
def begin_github_authorization(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> GitHubAuthorizeResponse:
    return GitHubService.create_authorization_url(db, current_user)

@router.get(
    "/callback",
    response_class=HTMLResponse,
    summary="GitHub OAuth callback URL handler",
)
async def github_callback(
    code: str | None = Query(None),
    state: str | None = Query(None),
    error: str | None = Query(None),
    error_description: str | None = Query(None),
    db: Session = Depends(get_db),
) -> HTMLResponse:
    if error:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=f"GitHub OAuth error: {error} - {error_description or 'No description'}",
        )
    if not code or not state:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail="Missing code or state in GitHub callback.",
        )
    connection = await GitHubService.handle_callback(db, code, state)
    html_content = f"""
    <!DOCTYPE html>
    <html>
    <head>
        <title>PROVEX — GitHub Connected</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0">
        <style>
            body {{
                background-color: #101827;
                color: #FFFFFF;
                font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, sans-serif;
                display: flex;
                flex-direction: column;
                align-items: center;
                justify-content: center;
                min-height: 100vh;
                margin: 0;
                padding: 24px;
                text-align: center;
            }}
            .card {{
                background-color: #1F2937;
                border-radius: 16px;
                padding: 32px;
                max-width: 400px;
                box-shadow: 0 10px 25px rgba(0,0,0,0.5);
                border: 1px solid #374151;
            }}
            .sparkle {{
                color: #54D6A1;
                font-size: 40px;
                margin-bottom: 16px;
            }}
            h2 {{ margin: 0 0 12px; color: #FFFFFF; font-size: 24px; }}
            p {{ color: #9CA3AF; font-size: 15px; line-height: 1.5; margin: 0 0 24px; }}
            .username {{ color: #54D6A1; font-weight: bold; }}
            .btn {{
                background-color: #54D6A1;
                color: #101827;
                padding: 12px 24px;
                border-radius: 8px;
                text-decoration: none;
                font-weight: 600;
                display: inline-block;
            }}
        </style>
    </head>
    <body>
        <div class="card">
            <div class="sparkle">✦</div>
            <h2>GitHub Connected!</h2>
            <p>Your account <span class="username">@{connection.github_username}</span> is now securely connected to PROVEX.</p>
            <p>You can return to the PROVEX application on your phone.</p>
            <a href="provex://github-callback" class="btn">Return to PROVEX</a>
            <script>
                setTimeout(function() {{
                    window.location.href = "provex://github-callback";
                }}, 800);
            </script>
        </div>
    </body>
    </html>
    """
    return HTMLResponse(content=html_content, status_code=200)

@router.get(
    "/status",
    response_model=GitHubStatusResponse,
    status_code=status.HTTP_200_OK,
    summary="Get current user's GitHub connection status",
)
def get_github_status(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> GitHubStatusResponse:
    return GitHubService.get_status(db, current_user)

@router.delete(
    "/disconnect",
    status_code=status.HTTP_200_OK,
    summary="Disconnect GitHub account and revoke stored credentials",
)
def disconnect_github(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> Dict[str, str]:
    GitHubService.disconnect(db, current_user)
    return {"message": "GitHub account successfully disconnected."}

@router.get(
    "/repositories",
    response_model=List[GitHubRepositoryResponse],
    status_code=status.HTTP_200_OK,
    summary="Get list of accessible GitHub repositories for the authenticated user",
)
async def get_github_repositories(
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
) -> List[GitHubRepositoryResponse]:
    return await GitHubService.get_repositories(db, current_user)

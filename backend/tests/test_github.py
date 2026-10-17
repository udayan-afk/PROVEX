import pytest
import httpx
from unittest.mock import patch, AsyncMock
from datetime import datetime, timezone, timedelta
from app.core.config import settings
from app.models.github import GitHubConnection, OAuthState
from app.core.security import encrypt_secret, decrypt_secret

def test_github_unauthenticated_access(client):
    assert client.post("/api/v1/github/authorize").status_code == 401
    assert client.get("/api/v1/github/status").status_code == 401
    assert client.get("/api/v1/github/repositories").status_code == 401
    assert client.delete("/api/v1/github/disconnect").status_code == 401

def test_github_authorize_url_generation(client):
    # Register user
    reg = client.post("/api/v1/auth/register", json={
        "full_name": "GitHub User",
        "email": "ghuser@oauth.org",
        "password": "Password123!",
    })
    token = reg.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Set mock client id & secret in settings
    with patch.object(settings, "GITHUB_CLIENT_ID", "test_client_id_123"), \
         patch.object(settings, "GITHUB_CLIENT_SECRET", "test_client_secret_456"):
        resp = client.post("/api/v1/github/authorize", headers=headers)
        assert resp.status_code == 200
        data = resp.json()
        assert "authorization_url" in data
        assert "state" in data
        assert "client_id=test_client_id_123" in data["authorization_url"]
        assert f"state={data['state']}" in data["authorization_url"]

def test_github_authorize_missing_configuration_returns_503(client):
    reg = client.post("/api/v1/auth/register", json={
        "full_name": "Unconfigured Tester",
        "email": "unconfigured@test.org",
        "password": "Password123!",
    })
    token = reg.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Case 1: Empty client ID
    with patch.object(settings, "GITHUB_CLIENT_ID", ""), patch.object(settings, "GITHUB_CLIENT_SECRET", ""):
        resp = client.post("/api/v1/github/authorize", headers=headers)
        assert resp.status_code == 503
        assert "not configured" in resp.json()["detail"].lower()

    # Case 2: Placeholder client ID
    with patch.object(settings, "GITHUB_CLIENT_ID", "PASTE_YOUR_CLIENT_ID"), \
         patch.object(settings, "GITHUB_CLIENT_SECRET", "PASTE_YOUR_CLIENT_SECRET"):
        resp = client.post("/api/v1/github/authorize", headers=headers)
        assert resp.status_code == 503
        assert "not configured" in resp.json()["detail"].lower()

def test_github_callback_invalid_and_expired_state(client, db):
    # Invalid state
    resp1 = client.get("/api/v1/github/callback?code=abc&state=nonexistent_state")
    assert resp1.status_code == 400
    assert "invalid" in resp1.json()["detail"].lower()

    # Expired state
    user_state = OAuthState(
        state="expired_state_123",
        user_id="dummy_user_id",
        expires_at=datetime.now(timezone.utc) - timedelta(minutes=5),
    )
    db.add(user_state)
    db.commit()

    resp2 = client.get("/api/v1/github/callback?code=abc&state=expired_state_123")
    assert resp2.status_code == 400
    assert "expired" in resp2.json()["detail"].lower()

@pytest.mark.asyncio
async def test_github_callback_success_and_replay_protection(client, db):
    # Register user
    reg = client.post("/api/v1/auth/register", json={
        "full_name": "OAuth Success User",
        "email": "oauthsuccess@test.org",
        "password": "Password123!",
    })
    token = reg.json()["access_token"]
    user_id = reg.json()["user"]["id"]
    headers = {"Authorization": f"Bearer {token}"}

    # Insert valid state bound to this user
    state_token = "valid_test_state_999"
    db_state = OAuthState(
        state=state_token,
        user_id=user_id,
        expires_at=datetime.now(timezone.utc) + timedelta(minutes=10),
    )
    db.add(db_state)
    db.commit()
    # Mock GitHub responses
    mock_token_resp = httpx.Response(200, json={
        "access_token": "gho_test_token_secret_12345",
        "scope": "read:user,repo",
        "token_type": "bearer",
    })

    mock_user_resp = httpx.Response(200, json={
        "id": 1234567,
        "login": "octocat-engineer",
    })

    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, return_value=mock_token_resp), \
         patch("httpx.AsyncClient.get", new_callable=AsyncMock, return_value=mock_user_resp):
        
        callback_resp = client.get(f"/api/v1/github/callback?code=valid_code&state={state_token}")
        assert callback_resp.status_code == 200
        assert "GitHub Connected!" in callback_resp.text
        assert "octocat-engineer" in callback_resp.text

        # Verify state was consumed (replay protection)
        replay_resp = client.get(f"/api/v1/github/callback?code=valid_code&state={state_token}")
        assert replay_resp.status_code == 400

    # Verify status endpoint reflects connected account
    status_resp = client.get("/api/v1/github/status", headers=headers)
    assert status_resp.status_code == 200
    status_data = status_resp.json()
    assert status_data["connected"] is True
    assert status_data["github_username"] == "octocat-engineer"

    # Verify token is encrypted at rest in DB
    conn = db.query(GitHubConnection).filter(GitHubConnection.user_id == user_id).first()
    assert conn is not None
    assert conn.encrypted_access_token != "gho_test_token_secret_12345"
    assert decrypt_secret(conn.encrypted_access_token) == "gho_test_token_secret_12345"

@pytest.mark.asyncio
async def test_github_repositories_and_disconnect(client, db):
    # Register user
    reg = client.post("/api/v1/auth/register", json={
        "full_name": "Repo Tester",
        "email": "repotest@test.org",
        "password": "Password123!",
    })
    token = reg.json()["access_token"]
    user_id = reg.json()["user"]["id"]
    headers = {"Authorization": f"Bearer {token}"}

    # Add connected GitHub account directly in DB
    conn = GitHubConnection(
        user_id=user_id,
        github_user_id="998877",
        github_username="dev-jordan",
        encrypted_access_token=encrypt_secret("gho_sample_token"),
        scope="repo",
    )
    db.add(conn)
    db.commit()

    # Mock GitHub /user/repos API
    mock_repos_resp = httpx.Response(200, json=[
        {
            "id": 101,
            "name": "provex-mobile",
            "full_name": "dev-jordan/provex-mobile",
            "owner": {"login": "dev-jordan"},
            "description": "Native Android app in Kotlin",
            "language": "Kotlin",
            "private": False,
            "html_url": "https://github.com/dev-jordan/provex-mobile",
            "default_branch": "main",
            "updated_at": "2026-10-17T06:00:00Z",
        }
    ])

    with patch("httpx.AsyncClient.get", new_callable=AsyncMock, return_value=mock_repos_resp):
        repos_resp = client.get("/api/v1/github/repositories", headers=headers)
        assert repos_resp.status_code == 200
        repos = repos_resp.json()
        assert len(repos) == 1
        assert repos[0]["name"] == "provex-mobile"
        assert repos[0]["language"] == "Kotlin"
        assert repos[0]["owner"] == "dev-jordan"

    # Disconnect
    disc_resp = client.delete("/api/v1/github/disconnect", headers=headers)
    assert disc_resp.status_code == 200

    # Status must now be disconnected
    st_resp = client.get("/api/v1/github/status", headers=headers)
    assert st_resp.status_code == 200
    assert st_resp.json()["connected"] is False

    # Repositories endpoint must return 404 when disconnected
    assert client.get("/api/v1/github/repositories", headers=headers).status_code == 404

def test_github_callback_cancellation(client):
    resp = client.get("/api/v1/github/callback?error=access_denied&error_description=The+user+has+denied+your+application+access.")
    assert resp.status_code == 400
    assert "access_denied" in resp.json()["detail"]

@pytest.mark.asyncio
async def test_github_failed_token_exchange(client, db):
    reg = client.post("/api/v1/auth/register", json={
        "full_name": "Failed Exchanger",
        "email": "failedexchange@test.org",
        "password": "Password123!",
    })
    user_id = reg.json()["user"]["id"]

    state_token = "state_failed_exchange"
    db_state = OAuthState(
        state=state_token,
        user_id=user_id,
        expires_at=datetime.now(timezone.utc) + timedelta(minutes=10),
    )
    db.add(db_state)
    db.commit()

    # Mock GitHub returning error on token exchange
    mock_bad_resp = httpx.Response(400, json={"error": "bad_verification_code"})
    with patch("httpx.AsyncClient.post", new_callable=AsyncMock, return_value=mock_bad_resp):
        callback_resp = client.get(f"/api/v1/github/callback?code=bad_code&state={state_token}")
        assert callback_resp.status_code == 502
        assert "failed" in callback_resp.json()["detail"].lower()

def test_github_user_isolation(client, db):
    # Register user A
    reg_a = client.post("/api/v1/auth/register", json={
        "full_name": "User Alpha",
        "email": "alpha@test.org",
        "password": "Password123!",
    })
    token_a = reg_a.json()["access_token"]
    user_id_a = reg_a.json()["user"]["id"]

    # Register user B
    reg_b = client.post("/api/v1/auth/register", json={
        "full_name": "User Beta",
        "email": "beta@test.org",
        "password": "Password123!",
    })
    token_b = reg_b.json()["access_token"]
    user_id_b = reg_b.json()["user"]["id"]

    # Connect User A
    conn = GitHubConnection(
        user_id=user_id_a,
        github_user_id="111111",
        github_username="alpha-coder",
        encrypted_access_token=encrypt_secret("gho_alpha_secret"),
        scope="read:user,repo",
    )
    db.add(conn)
    db.commit()

    # User A status is connected
    status_a = client.get("/api/v1/github/status", headers={"Authorization": f"Bearer {token_a}"}).json()
    assert status_a["connected"] is True
    assert status_a["github_username"] == "alpha-coder"

    # User B status is disconnected
    status_b = client.get("/api/v1/github/status", headers={"Authorization": f"Bearer {token_b}"}).json()
    assert status_b["connected"] is False
    assert status_b["github_username"] is None

    # User B cannot get repositories
    repos_b = client.get("/api/v1/github/repositories", headers={"Authorization": f"Bearer {token_b}"})
    assert repos_b.status_code == 404


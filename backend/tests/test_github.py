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

@pytest.mark.asyncio
async def test_github_scenarios_a_b_c_multi_user_isolation(client, db):
    """
    Explicit regression test for Scenarios A, B, and C:
    Scenario A: User A connects GitHub A -> User A connected, User B disconnected & cannot access A's repos.
    Scenario B: User B connects GitHub B -> User B sees B's repos, User A sees A's repos.
    Scenario C: User A disconnects -> User A disconnected, User B remains connected.
    """
    # Register User A
    reg_a = client.post("/api/v1/auth/register", json={
        "full_name": "User Alpha",
        "email": "scenario_alpha@test.org",
        "password": "Password123!",
    })
    token_a = reg_a.json()["access_token"]
    user_id_a = reg_a.json()["user"]["id"]
    headers_a = {"Authorization": f"Bearer {token_a}"}

    # Register User B
    reg_b = client.post("/api/v1/auth/register", json={
        "full_name": "User Beta",
        "email": "scenario_beta@test.org",
        "password": "Password123!",
    })
    token_b = reg_b.json()["access_token"]
    user_id_b = reg_b.json()["user"]["id"]
    headers_b = {"Authorization": f"Bearer {token_b}"}

    # --- SCENARIO A ---
    # 1. User A initiates OAuth
    with patch.object(settings, "GITHUB_CLIENT_ID", "test_client_id"), \
         patch.object(settings, "GITHUB_CLIENT_SECRET", "test_client_secret"):
        auth_resp_a = client.post("/api/v1/github/authorize", headers=headers_a)
        assert auth_resp_a.status_code == 200
        state_a = auth_resp_a.json()["state"]

        # Ensure state is bound to user A
        db_state_a = db.query(OAuthState).filter(OAuthState.state == state_a).first()
        assert db_state_a is not None
        assert db_state_a.user_id == user_id_a

        # Mock token exchange and GitHub user profile for User A
        token_resp_a = httpx.Response(200, json={
            "access_token": "gho_user_a_token",
            "scope": "read:user,repo",
            "token_type": "bearer",
        })
        profile_resp_a = httpx.Response(200, json={
            "id": 111111,
            "login": "octocat-alpha",
        })

        with patch("httpx.AsyncClient.post", new_callable=AsyncMock, return_value=token_resp_a), \
             patch("httpx.AsyncClient.get", new_callable=AsyncMock, return_value=profile_resp_a):
            cb_a = client.get(f"/api/v1/github/callback?code=code_a&state={state_a}")
            assert cb_a.status_code == 200
            assert "octocat-alpha" in cb_a.text

    # 2. User A's status reports connected
    status_a = client.get("/api/v1/github/status", headers=headers_a).json()
    assert status_a["connected"] is True
    assert status_a["github_username"] == "octocat-alpha"

    # 3 & 4. User B's status reports disconnected
    status_b = client.get("/api/v1/github/status", headers=headers_b).json()
    assert status_b["connected"] is False
    assert status_b["github_username"] is None

    # 5. User B's repository endpoint must not return User A's repositories
    repos_b = client.get("/api/v1/github/repositories", headers=headers_b)
    assert repos_b.status_code == 404

    # --- SCENARIO B ---
    # 1. User B connects GitHub account B
    with patch.object(settings, "GITHUB_CLIENT_ID", "test_client_id"), \
         patch.object(settings, "GITHUB_CLIENT_SECRET", "test_client_secret"):
        auth_resp_b = client.post("/api/v1/github/authorize", headers=headers_b)
        assert auth_resp_b.status_code == 200
        state_b = auth_resp_b.json()["state"]

        assert state_b != state_a
        db_state_b = db.query(OAuthState).filter(OAuthState.state == state_b).first()
        assert db_state_b.user_id == user_id_b

        token_resp_b = httpx.Response(200, json={
            "access_token": "gho_user_b_token",
            "scope": "read:user,repo",
            "token_type": "bearer",
        })
        profile_resp_b = httpx.Response(200, json={
            "id": 222222,
            "login": "octocat-beta",
        })

        with patch("httpx.AsyncClient.post", new_callable=AsyncMock, return_value=token_resp_b), \
             patch("httpx.AsyncClient.get", new_callable=AsyncMock, return_value=profile_resp_b):
            cb_b = client.get(f"/api/v1/github/callback?code=code_b&state={state_b}")
            assert cb_b.status_code == 200
            assert "octocat-beta" in cb_b.text

    # 2. User B sees account B and its repositories
    status_b2 = client.get("/api/v1/github/status", headers=headers_b).json()
    assert status_b2["connected"] is True
    assert status_b2["github_username"] == "octocat-beta"

    repos_mock_b = httpx.Response(200, json=[{
        "id": 201,
        "name": "beta-system",
        "full_name": "octocat-beta/beta-system",
        "owner": {"login": "octocat-beta"},
        "description": "Beta repo",
        "language": "Python",
        "private": False,
        "html_url": "https://github.com/octocat-beta/beta-system",
        "default_branch": "main",
        "updated_at": "2026-10-17T08:00:00Z",
    }])
    with patch("httpx.AsyncClient.get", new_callable=AsyncMock, return_value=repos_mock_b):
        b_repos = client.get("/api/v1/github/repositories", headers=headers_b).json()
        assert len(b_repos) == 1
        assert b_repos[0]["name"] == "beta-system"
        assert b_repos[0]["owner"] == "octocat-beta"

    # 3. User A continues to see account A and its repositories
    status_a2 = client.get("/api/v1/github/status", headers=headers_a).json()
    assert status_a2["connected"] is True
    assert status_a2["github_username"] == "octocat-alpha"

    repos_mock_a = httpx.Response(200, json=[{
        "id": 101,
        "name": "alpha-lib",
        "full_name": "octocat-alpha/alpha-lib",
        "owner": {"login": "octocat-alpha"},
        "description": "Alpha repo",
        "language": "Kotlin",
        "private": False,
        "html_url": "https://github.com/octocat-alpha/alpha-lib",
        "default_branch": "main",
        "updated_at": "2026-10-17T08:00:00Z",
    }])
    with patch("httpx.AsyncClient.get", new_callable=AsyncMock, return_value=repos_mock_a):
        a_repos = client.get("/api/v1/github/repositories", headers=headers_a).json()
        assert len(a_repos) == 1
        assert a_repos[0]["name"] == "alpha-lib"
        assert a_repos[0]["owner"] == "octocat-alpha"

    # --- SCENARIO C ---
    # 1. User A disconnects GitHub
    disc_a = client.delete("/api/v1/github/disconnect", headers=headers_a)
    assert disc_a.status_code == 200

    # 2. User A becomes disconnected
    status_a3 = client.get("/api/v1/github/status", headers=headers_a).json()
    assert status_a3["connected"] is False
    assert status_a3["github_username"] is None
    assert client.get("/api/v1/github/repositories", headers=headers_a).status_code == 404

    # 3. User B's connection remains unchanged
    status_b3 = client.get("/api/v1/github/status", headers=headers_b).json()
    assert status_b3["connected"] is True
    assert status_b3["github_username"] == "octocat-beta"
    with patch("httpx.AsyncClient.get", new_callable=AsyncMock, return_value=repos_mock_b):
        b_repos_after = client.get("/api/v1/github/repositories", headers=headers_b).json()
        assert len(b_repos_after) == 1
        assert b_repos_after[0]["name"] == "beta-system"

@pytest.mark.asyncio
async def test_github_concurrent_oauth_states_different_users(client, db):
    """
    Ensure concurrent OAuth attempts from different PROVEX users cannot overwrite
    or attach connections to the wrong account.
    """
    # User 1
    u1 = client.post("/api/v1/auth/register", json={
        "full_name": "Concurrent 1",
        "email": "c1@test.org",
        "password": "Password123!",
    }).json()
    token1 = u1["access_token"]
    user_id_1 = u1["user"]["id"]

    # User 2
    u2 = client.post("/api/v1/auth/register", json={
        "full_name": "Concurrent 2",
        "email": "c2@test.org",
        "password": "Password123!",
    }).json()
    token2 = u2["access_token"]
    user_id_2 = u2["user"]["id"]

    with patch.object(settings, "GITHUB_CLIENT_ID", "test_client_id"), \
         patch.object(settings, "GITHUB_CLIENT_SECRET", "test_client_secret"):
        # Both start auth concurrently
        r1 = client.post("/api/v1/github/authorize", headers={"Authorization": f"Bearer {token1}"}).json()
        r2 = client.post("/api/v1/github/authorize", headers={"Authorization": f"Bearer {token2}"}).json()

        state1 = r1["state"]
        state2 = r2["state"]
        assert state1 != state2

        # State 2 callback executes first
        token_resp2 = httpx.Response(200, json={"access_token": "gho_tok2", "scope": "repo", "token_type": "bearer"})
        prof_resp2 = httpx.Response(200, json={"id": 2002, "login": "gh-user-two"})
        with patch("httpx.AsyncClient.post", new_callable=AsyncMock, return_value=token_resp2), \
             patch("httpx.AsyncClient.get", new_callable=AsyncMock, return_value=prof_resp2):
            cb2 = client.get(f"/api/v1/github/callback?code=code2&state={state2}")
            assert cb2.status_code == 200

        # User 2 is connected to gh-user-two
        s2 = client.get("/api/v1/github/status", headers={"Authorization": f"Bearer {token2}"}).json()
        assert s2["connected"] is True
        assert s2["github_username"] == "gh-user-two"

        # User 1 is still NOT connected
        s1 = client.get("/api/v1/github/status", headers={"Authorization": f"Bearer {token1}"}).json()
        assert s1["connected"] is False

        # State 1 callback executes next
        token_resp1 = httpx.Response(200, json={"access_token": "gho_tok1", "scope": "repo", "token_type": "bearer"})
        prof_resp1 = httpx.Response(200, json={"id": 1001, "login": "gh-user-one"})
        with patch("httpx.AsyncClient.post", new_callable=AsyncMock, return_value=token_resp1), \
             patch("httpx.AsyncClient.get", new_callable=AsyncMock, return_value=prof_resp1):
            cb1 = client.get(f"/api/v1/github/callback?code=code1&state={state1}")
            assert cb1.status_code == 200

        # Now User 1 is connected to gh-user-one
        s1_after = client.get("/api/v1/github/status", headers={"Authorization": f"Bearer {token1}"}).json()
        assert s1_after["connected"] is True
        assert s1_after["github_username"] == "gh-user-one"

        # And User 2's connection was completely unaffected
        s2_after = client.get("/api/v1/github/status", headers={"Authorization": f"Bearer {token2}"}).json()
        assert s2_after["connected"] is True
        assert s2_after["github_username"] == "gh-user-two"



import pytest

def test_register_success(client):
    payload = {
        "full_name": "Alex Chen",
        "email": "alex.chen@university.edu",
        "password": "Password123!",
    }
    response = client.post("/api/v1/auth/register", json=payload)
    assert response.status_code == 201
    data = response.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["token_type"] == "bearer"
    assert data["user"]["email"] == "alex.chen@university.edu"
    assert data["user"]["full_name"] == "Alex Chen"
    assert "password" not in data["user"]
    assert "hashed_password" not in data["user"]

def test_register_duplicate_email(client):
    payload = {
        "full_name": "Alex Chen",
        "email": "duplicate@university.edu",
        "password": "Password123!",
    }
    resp1 = client.post("/api/v1/auth/register", json=payload)
    assert resp1.status_code == 201

    resp2 = client.post("/api/v1/auth/register", json=payload)
    assert resp2.status_code == 409
    assert "already exists" in resp2.json()["detail"].lower()

def test_register_invalid_email_and_short_password(client):
    # Invalid email
    resp1 = client.post("/api/v1/auth/register", json={
        "full_name": "Test User",
        "email": "not-an-email",
        "password": "ValidPassword123",
    })
    assert resp1.status_code == 422

    # Short password (< 8 chars)
    resp2 = client.post("/api/v1/auth/register", json={
        "full_name": "Test User",
        "email": "valid@email.com",
        "password": "short",
    })
    assert resp2.status_code == 422

def test_login_success(client):
    # Register first
    client.post("/api/v1/auth/register", json={
        "full_name": "Sam Login",
        "email": "sam.login@university.edu",
        "password": "SecretPassword123",
    })

    # Login
    resp = client.post("/api/v1/auth/login", json={
        "email": "sam.login@university.edu",
        "password": "SecretPassword123",
    })
    assert resp.status_code == 200
    data = resp.json()
    assert "access_token" in data
    assert "refresh_token" in data
    assert data["user"]["email"] == "sam.login@university.edu"

def test_login_invalid_credentials(client):
    resp = client.post("/api/v1/auth/login", json={
        "email": "nonexistent@university.edu",
        "password": "WrongPassword123",
    })
    assert resp.status_code == 401
    assert "incorrect email or password" in resp.json()["detail"].lower()

def test_get_me_with_valid_and_invalid_token(client):
    # Register
    reg_resp = client.post("/api/v1/auth/register", json={
        "full_name": "Taylor Swift",
        "email": "taylor@swift.com",
        "password": "ValidPassword123",
    })
    access_token = reg_resp.json()["access_token"]
    refresh_token = reg_resp.json()["refresh_token"]

    # Valid access token
    headers = {"Authorization": f"Bearer {access_token}"}
    me_resp = client.get("/api/v1/auth/me", headers=headers)
    assert me_resp.status_code == 200
    assert me_resp.json()["email"] == "taylor@swift.com"

    # Missing token
    assert client.get("/api/v1/auth/me").status_code == 401

    # Invalid token string
    bad_headers = {"Authorization": "Bearer not-a-valid-jwt-token"}
    assert client.get("/api/v1/auth/me", headers=bad_headers).status_code == 401

    # Refresh token MUST NOT be accepted as access token
    rf_headers = {"Authorization": f"Bearer {refresh_token}"}
    assert client.get("/api/v1/auth/me", headers=rf_headers).status_code == 401

def test_refresh_token_rotation_and_revocation(client):
    reg_resp = client.post("/api/v1/auth/register", json={
        "full_name": "Jordan Dev",
        "email": "jordan@dev.org",
        "password": "ComplexPassword99!",
    })
    initial_refresh = reg_resp.json()["refresh_token"]

    # Rotate refresh token
    ref_resp = client.post("/api/v1/auth/refresh", json={"refresh_token": initial_refresh})
    assert ref_resp.status_code == 200
    new_tokens = ref_resp.json()
    new_refresh = new_tokens["refresh_token"]
    assert new_refresh != initial_refresh

    # Using the OLD refresh token again must fail (revoked)
    reuse_resp = client.post("/api/v1/auth/refresh", json={"refresh_token": initial_refresh})
    assert reuse_resp.status_code == 401
    assert "revoked" in reuse_resp.json()["detail"].lower()

    # The new refresh token should also be revoked because token reuse triggers revocation
    reuse_new = client.post("/api/v1/auth/refresh", json={"refresh_token": new_refresh})
    assert reuse_new.status_code == 401

def test_logout_revocation(client):
    reg_resp = client.post("/api/v1/auth/register", json={
        "full_name": "Logout Tester",
        "email": "logout@test.com",
        "password": "Password123!",
    })
    refresh_token = reg_resp.json()["refresh_token"]

    # Logout
    logout_resp = client.post("/api/v1/auth/logout", json={"refresh_token": refresh_token})
    assert logout_resp.status_code == 200

    # Refresh token is revoked now
    ref_resp = client.post("/api/v1/auth/refresh", json={"refresh_token": refresh_token})
    assert ref_resp.status_code == 401
    assert "revoked" in ref_resp.json()["detail"].lower()

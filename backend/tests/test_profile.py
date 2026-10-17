import pytest

def test_get_profile_unauthenticated(client):
    response = client.get("/api/v1/profile")
    assert response.status_code == 401

def test_get_profile_not_found_initially(client):
    # Register a new user
    reg = client.post("/api/v1/auth/register", json={
        "full_name": "New Developer",
        "email": "newdev@provex.org",
        "password": "Password123!",
    })
    token = reg.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Getting profile before creating one returns 404
    resp = client.get("/api/v1/profile", headers=headers)
    assert resp.status_code == 404
    assert "not found" in resp.json()["detail"].lower()

def test_create_and_get_profile(client):
    reg = client.post("/api/v1/auth/register", json={
        "full_name": "Sarah Connor",
        "email": "sarah@resistance.org",
        "password": "SecurePassword123!",
    })
    token = reg.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    profile_payload = {
        "level": "Undergraduate",
        "target_role": "Android Engineer",
        "location": "San Francisco, CA",
        "work_mode": "HYBRID",
        "skills": ["Kotlin", "Jetpack Compose", "Coroutines"],
        "languages": ["Kotlin", "Java", "Python"],
        "frameworks": ["Android SDK", "Room", "Retrofit"],
        "interests": ["Mobile AI", "Open Source"],
    }

    # PUT creates profile
    put_resp = client.put("/api/v1/profile", headers=headers, json=profile_payload)
    assert put_resp.status_code == 200
    created = put_resp.json()
    assert created["target_role"] == "Android Engineer"
    assert created["name"] == "Sarah Connor"
    assert created["email"] == "sarah@resistance.org"
    assert "Jetpack Compose" in created["skills"]
    assert "Room" in created["frameworks"]
    assert created["work_mode"] == "HYBRID"

    # GET retrieves the created profile
    get_resp = client.get("/api/v1/profile", headers=headers)
    assert get_resp.status_code == 200
    fetched = get_resp.json()
    assert fetched["id"] == created["id"]
    assert fetched["target_role"] == "Android Engineer"
    assert fetched["skills"] == profile_payload["skills"]

def test_update_existing_profile(client):
    reg = client.post("/api/v1/auth/register", json={
        "full_name": "John Doe",
        "email": "johndoe@test.com",
        "password": "Password123!",
    })
    token = reg.json()["access_token"]
    headers = {"Authorization": f"Bearer {token}"}

    # Initial profile
    client.put("/api/v1/profile", headers=headers, json={
        "level": "Junior",
        "target_role": "Junior Android Dev",
        "location": "Boston",
        "work_mode": "REMOTE",
        "skills": ["Kotlin"],
        "languages": ["Kotlin"],
        "frameworks": [],
        "interests": [],
    })

    # Updated profile
    update_payload = {
        "level": "Mid-level",
        "target_role": "Senior Android Engineer",
        "location": "Remote",
        "work_mode": "REMOTE",
        "skills": ["Kotlin", "Compose", "Architecture"],
        "languages": ["Kotlin", "Swift"],
        "frameworks": ["Android SDK", "KMP"],
        "interests": ["Cross-platform"],
    }
    put_resp = client.put("/api/v1/profile", headers=headers, json=update_payload)
    assert put_resp.status_code == 200
    updated = put_resp.json()
    assert updated["level"] == "Mid-level"
    assert updated["target_role"] == "Senior Android Engineer"
    assert updated["languages"] == ["Kotlin", "Swift"]

def test_profile_isolation_between_two_users(client):
    # User 1
    u1_reg = client.post("/api/v1/auth/register", json={
        "full_name": "User One",
        "email": "user1@isolation.org",
        "password": "Password123!",
    })
    u1_token = u1_reg.json()["access_token"]
    u1_headers = {"Authorization": f"Bearer {u1_token}"}

    client.put("/api/v1/profile", headers=u1_headers, json={
        "level": "Student",
        "target_role": "Backend Engineer",
        "location": "New York",
        "work_mode": "REMOTE",
        "skills": ["Python", "FastAPI"],
        "languages": ["Python"],
        "frameworks": ["FastAPI", "SQLAlchemy"],
        "interests": ["Databases"],
    })

    # User 2
    u2_reg = client.post("/api/v1/auth/register", json={
        "full_name": "User Two",
        "email": "user2@isolation.org",
        "password": "Password123!",
    })
    u2_token = u2_reg.json()["access_token"]
    u2_headers = {"Authorization": f"Bearer {u2_token}"}

    client.put("/api/v1/profile", headers=u2_headers, json={
        "level": "Graduate",
        "target_role": "Data Scientist",
        "location": "Seattle",
        "work_mode": "ON_SITE",
        "skills": ["Pandas", "PyTorch"],
        "languages": ["Python", "R"],
        "frameworks": ["PyTorch"],
        "interests": ["Deep Learning"],
    })

    # Verify User 1 cannot see User 2's profile
    u1_get = client.get("/api/v1/profile", headers=u1_headers).json()
    assert u1_get["target_role"] == "Backend Engineer"
    assert u1_get["email"] == "user1@isolation.org"

    # Verify User 2 cannot see User 1's profile
    u2_get = client.get("/api/v1/profile", headers=u2_headers).json()
    assert u2_get["target_role"] == "Data Scientist"
    assert u2_get["email"] == "user2@isolation.org"

import json
import pytest
from unittest.mock import AsyncMock, patch, MagicMock
from fastapi.testclient import TestClient
from app.main import app
from app.services.url_reader import validate_url_and_resolve, is_ip_blocked
import ipaddress

client = TestClient(app)

SAMPLE_JOB_TEXT = """
Job Title: Android Development Intern
Company: TechNova Solutions

About the Role:
We are looking for an ambitious Android Development Intern to join our mobile team.

Responsibilities:
- Build modern, intuitive user interfaces using Jetpack Compose and Material Design 3.
- Integrate RESTful APIs using Retrofit and Kotlin Coroutines for network communication.
- Collaborate with cross-functional product and design teams in an Agile environment.

Requirements & Qualifications:
- Proficiency in Kotlin and Java programming languages.
- Solid understanding of the Android SDK and Android architecture components (MVVM).
- Experience working with SQLite or Room Database for local data storage.
- Familiarity with Git version control and GitHub workflows.
- Strong problem solving and communication skills.
- Currently pursuing a Bachelor's degree in Computer Science or related technical field.

Preferred Qualifications:
- Prior experience with CI/CD pipelines and unit testing with JUnit or MockK is a plus.
- Familiarity with Python, FastAPI or backend systems is preferred.
- Familiarity with Figma design tools.
"""


@pytest.fixture
def auth_headers():
    # Register and login a unique user to test authenticated endpoints
    import uuid
    email = f"job_tester_{uuid.uuid4().hex[:6]}@example.com"
    reg_resp = client.post(
        "/api/v1/auth/register",
        json={"email": email, "password": "Password123!", "full_name": "Job Tester"},
    )
    assert reg_resp.status_code == 201
    token = reg_resp.json()["access_token"]
    return {"Authorization": f"Bearer {token}"}


def test_job_analyze_unauthenticated():
    resp = client.post("/api/v1/jobs/analyze", json={"description": SAMPLE_JOB_TEXT})
    assert resp.status_code == 401
    assert "Missing authorization header" in resp.json()["detail"]


def test_job_analyze_empty_request(auth_headers):
    resp = client.post("/api/v1/jobs/analyze", json={}, headers=auth_headers)
    assert resp.status_code == 422

    resp2 = client.post("/api/v1/jobs/analyze", json={"description": "", "url": ""}, headers=auth_headers)
    assert resp2.status_code == 422


def test_job_analyze_short_description(auth_headers):
    resp = client.post("/api/v1/jobs/analyze", json={"description": "Too short"}, headers=auth_headers)
    assert resp.status_code == 400
    assert "too short" in resp.json()["detail"].lower()


def test_job_analyze_pasted_description(auth_headers):
    resp = client.post(
        "/api/v1/jobs/analyze",
        json={"description": SAMPLE_JOB_TEXT},
        headers=auth_headers,
    )
    assert resp.status_code == 200
    data = resp.json()

    assert data["job_title"] == "Android Development Intern"
    assert data["company"] == "TechNova Solutions"
    assert data["source_type"] == "pasted_description"
    assert len(data["skills"]) > 5

    # Verify skill extraction
    skill_names = {s["name"]: s for s in data["skills"]}
    assert "Kotlin" in skill_names
    assert "Jetpack Compose" in skill_names
    assert "Retrofit" in skill_names
    assert "Git / Version Control" in skill_names

    # Check importance classification
    assert skill_names["Kotlin"]["importance"] == "Required"
    assert skill_names["Kotlin"]["category"] == "Programming Language"
    assert len(skill_names["Kotlin"]["evidence"]) > 0

    # Preferred skill verification
    assert "Python" in skill_names
    assert skill_names["Python"]["importance"] == "Preferred"

    # Category counts
    assert "Programming Language" in data["skill_counts_by_category"]
    assert "Mobile Framework" in data["skill_counts_by_category"]

    # Responsibilities and qualifications extracted
    assert len(data["responsibilities"]) >= 2
    assert len(data["qualifications"]) >= 2
    assert len(data["education_requirements"]) >= 1
    assert "Bachelor" in data["education_requirements"][0]
    assert len(data["resume_keywords"]) >= 5


def test_job_analyze_both_description_and_url(auth_headers):
    resp = client.post(
        "/api/v1/jobs/analyze",
        json={
            "description": SAMPLE_JOB_TEXT,
            "url": "https://example.com/careers/android-intern",
        },
        headers=auth_headers,
    )
    assert resp.status_code == 200
    data = resp.json()
    assert data["source_url"] == "https://example.com/careers/android-intern"
    assert any("Both description and URL" in w for w in data["warnings"])


def test_profile_comparison(auth_headers):
    # Set up user's career profile first
    prof_resp = client.put(
        "/api/v1/profile",
        json={
            "level": "STUDENT",
            "target_role": "Android Developer",
            "skills": ["Kotlin", "Git", "Problem Solving"],
            "languages": ["Kotlin"],
            "frameworks": ["Jetpack Compose"],
            "interests": ["Mobile"],
        },
        headers=auth_headers,
    )
    assert prof_resp.status_code == 200

    resp = client.post(
        "/api/v1/jobs/analyze",
        json={"description": SAMPLE_JOB_TEXT},
        headers=auth_headers,
    )
    assert resp.status_code == 200
    comp = resp.json().get("profile_comparison")
    assert comp is not None
    assert comp["user_has_profile"] is True
    assert "Kotlin" in comp["matching_skills"]
    assert len(comp["missing_skills"]) > 0
    assert comp["match_percentage"] > 0
    assert "self-reported" in comp["disclaimer"]


# SSRF & URL Protection Tests
def test_ssrf_blocking_localhost_and_internal_ips():
    # Loopback
    assert is_ip_blocked(ipaddress.ip_address("127.0.0.1")) is True
    assert is_ip_blocked(ipaddress.ip_address("127.0.0.2")) is True
    assert is_ip_blocked(ipaddress.ip_address("::1")) is True

    # Private ranges (RFC 1918)
    assert is_ip_blocked(ipaddress.ip_address("10.0.0.5")) is True
    assert is_ip_blocked(ipaddress.ip_address("172.16.1.1")) is True
    assert is_ip_blocked(ipaddress.ip_address("192.168.1.1")) is True
    assert is_ip_blocked(ipaddress.ip_address("192.168.137.246")) is True

    # Cloud metadata / link-local
    assert is_ip_blocked(ipaddress.ip_address("169.254.169.254")) is True

    # Public IP should NOT be blocked
    assert is_ip_blocked(ipaddress.ip_address("8.8.8.8")) is False
    assert is_ip_blocked(ipaddress.ip_address("142.250.190.46")) is False


def test_validate_url_and_resolve_ssrf():
    # Unsupported scheme
    valid, err, _ = validate_url_and_resolve("ftp://example.com/job")
    assert not valid
    assert "scheme" in err.lower()

    # Blocked hostname
    valid, err, _ = validate_url_and_resolve("http://localhost:8000/api/v1/health")
    assert not valid
    assert "blocked" in err.lower()

    # Direct private IP literal
    valid, err, _ = validate_url_and_resolve("http://127.0.0.1:8000/job")
    assert not valid

    valid, err, _ = validate_url_and_resolve("http://192.168.1.1/job")
    assert not valid

    # Cloud metadata
    valid, err, _ = validate_url_and_resolve("http://169.254.169.254/latest/meta-data")
    assert not valid


@pytest.mark.asyncio
async def test_job_url_reader_mock_success(auth_headers):
    mock_html = """
    <html>
      <head>
        <title>Junior Backend Engineer | Acme Corp</title>
        <meta property="og:title" content="Junior Backend Engineer" />
        <meta property="og:site_name" content="Acme Corp" />
      </head>
      <body>
        <main>
          <h1>Junior Backend Engineer</h1>
          <p>Acme Corp is hiring a Junior Backend Engineer.</p>
          <h2>Requirements</h2>
          <ul>
            <li>Strong proficiency in Python and FastAPI.</li>
            <li>Experience designing relational databases with PostgreSQL or SQLAlchemy.</li>
            <li>Familiarity with Docker and Linux containers.</li>
          </ul>
        </main>
      </body>
    </html>
    """

    with patch("app.services.url_reader.validate_url_and_resolve", return_value=(True, "example.com", "93.184.216.34")):
        with patch("httpx.AsyncClient.get") as mock_get:
            mock_resp = MagicMock()
            mock_resp.status_code = 200
            mock_resp.headers = {"Content-Type": "text/html; charset=utf-8"}
            mock_resp.content = mock_html.encode("utf-8")
            mock_resp.encoding = "utf-8"
            mock_get.return_value = mock_resp

            resp = client.post(
                "/api/v1/jobs/analyze",
                json={"url": "https://example.com/careers/backend-engineer"},
                headers=auth_headers,
            )
            assert resp.status_code == 200
            data = resp.json()
            assert data["job_title"] == "Junior Backend Engineer"
            assert data["company"] == "Acme Corp"
            assert data["source_type"] == "url_extracted"
            skill_names = {s["name"] for s in data["skills"]}
            assert "Python" in skill_names
            assert "FastAPI" in skill_names
            assert "PostgreSQL" in skill_names
            assert "Docker" in skill_names


def test_job_url_reader_ssrf_via_endpoint(auth_headers):
    # Attempting to query localhost through the analyze endpoint must return HTTP 400 with SSRF warning
    resp = client.post(
        "/api/v1/jobs/analyze",
        json={"url": "http://127.0.0.1:8000/api/v1/health"},
        headers=auth_headers,
    )
    assert resp.status_code == 400
    assert "prohibited" in resp.json()["detail"].lower() or "blocked" in resp.json()["detail"].lower()


def test_deterministic_match_formula():
    from app.services.github_skill_matcher import GitHubSkillMatcher
    from app.schemas.job_analyzer import ExtractedSkill
    from app.schemas.github import GitHubRepositoryResponse

    job_skills = [
        ExtractedSkill(name="Kotlin", category="Language", importance="Required", evidence="Req"),
        ExtractedSkill(name="Python", category="Language", importance="Preferred", evidence="Pref"),
        ExtractedSkill(name="Rust", category="Language", importance="Mentioned", evidence="Ment"),
    ]

    repos = [
        GitHubRepositoryResponse(
            id="1",
            name="provex-app",
            full_name="testuser/provex-app",
            owner="testuser",
            language="Kotlin",
            description="Android app in Kotlin",
            html_url="https://github.com/testuser/provex-app",
        ),
        GitHubRepositoryResponse(
            id="2",
            name="backend-scripts",
            full_name="testuser/backend-scripts",
            owner="testuser",
            language="Python",
            description="Tools in Python",
            html_url="https://github.com/testuser/backend-scripts",
        ),
    ]

    result = GitHubSkillMatcher.match(job_skills, repos)

    # Weights: Required = 3, Preferred = 2, Mentioned = 1 => Total = 6
    # Kotlin (Req) Strong = 1.0 => 3 * 1.0 = 3.0
    # Python (Pref) Strong = 1.0 => 2 * 1.0 = 2.0
    # Rust (Ment) Not Found = 0.0 => 1 * 0.0 = 0.0
    # Expected Score = 100 * (3.0 + 2.0 + 0.0) / 6 = 500 / 6 = 83.3
    assert result["score"] == 83.3
    assert result["score_label"] == "Repository Evidence Match"
    assert result["strong_count"] == 1  # Kotlin (Required strong)
    assert result["preferred_match_count"] == 1  # Python (Preferred match)
    assert result["not_found_count"] == 1  # Rust

    # Check symbols
    skill_map = {s["name"]: s for s in result["matched_skills"]}
    assert skill_map["Kotlin"]["symbol"] == "✓"
    assert skill_map["Kotlin"]["match_level"] == "STRONG"
    assert "Kotlin" in skill_map["Kotlin"]["evidence_reason"]
    assert skill_map["Python"]["symbol"] == "★"
    assert skill_map["Python"]["match_level"] == "PREFERRED_MATCH"
    assert skill_map["Rust"]["symbol"] == "○"
    assert skill_map["Rust"]["match_level"] == "NOT_FOUND"


def test_job_analyze_with_github_matching(auth_headers):
    from app.schemas.github import GitHubRepositoryResponse

    fake_repos = [
        GitHubRepositoryResponse(
            id="101",
            name="provex-mobile",
            full_name="octocat/provex-mobile",
            owner="octocat",
            language="Kotlin",
            description="Android app with Jetpack Compose and Retrofit",
            html_url="https://github.com/octocat/provex-mobile",
        )
    ]

    with patch("app.services.github_service.GitHubService.get_repositories", return_value=fake_repos):
        with patch("sqlalchemy.orm.Query.first") as mock_conn:
            mock_conn.return_value = MagicMock(github_username="octocat")

            resp = client.post(
                "/api/v1/jobs/analyze",
                json={"description": SAMPLE_JOB_TEXT},
                headers=auth_headers,
            )
            assert resp.status_code == 200
            data = resp.json()

            gh_match = data.get("github_matching")
            assert gh_match is not None
            assert gh_match["connected"] is True
            assert gh_match["github_username"] == "octocat"
            assert gh_match["repositories_analyzed"] == 1
            assert gh_match["score"] is not None
            assert gh_match["score"] > 0
            assert gh_match["score_label"] == "Repository Evidence Match"
            assert len(gh_match["matched_skills"]) > 0


def test_match_github_endpoint(auth_headers):
    from app.schemas.github import GitHubRepositoryResponse

    fake_repos = [
        GitHubRepositoryResponse(
            id="202",
            name="ml-pipeline",
            full_name="octocat/ml-pipeline",
            owner="octocat",
            language="Python",
            description="FastAPI service with PostgreSQL",
            html_url="https://github.com/octocat/ml-pipeline",
        )
    ]

    with patch("app.services.github_service.GitHubService.get_repositories", return_value=fake_repos):
        with patch("sqlalchemy.orm.Query.first") as mock_conn:
            mock_conn.return_value = MagicMock(github_username="octocat")

            payload = {
                "skills": [
                    {"name": "Python", "category": "Programming Language", "importance": "Required", "evidence": "Must know Python"},
                    {"name": "FastAPI", "category": "Backend Framework", "importance": "Preferred", "evidence": "FastAPI experience"},
                    {"name": "Kotlin", "category": "Programming Language", "importance": "Required", "evidence": "Kotlin bonus"},
                ]
            }

            resp = client.post("/api/v1/jobs/match-github", json=payload, headers=auth_headers)
            assert resp.status_code == 200
            data = resp.json()
            assert data["connected"] is True
            assert data["repositories_analyzed"] == 1
            assert data["score"] is not None
            assert len(data["skills_to_strengthen"]) > 0

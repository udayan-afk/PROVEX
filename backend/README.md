# PROVEX Backend & Authentication Service

Backend services for **PROVEX: Turn Code Into Credibility**.

This service provides:
- Secure user registration, login, and password hashing via Argon2id.
- JWT access & refresh tokens with rotation and revocation.
- Persistent Career Profile management (`/api/v1/profile`) with level, target role, self-reported skills, location, work mode, and career interests.
- User profile introspection (`/me`) and health checks (`/health`).

---

## 1. Prerequisites & Environment Setup

- Python 3.12+ (or 3.13)
- PowerShell (Windows) or Bash (macOS/Linux)

### Create and Activate Virtual Environment

From the project root:

```powershell
# Windows PowerShell
py -3.12 -m venv backend/venv
.\backend\venv\Scripts\Activate.ps1
```

```bash
# macOS / Linux
python3 -m venv backend/venv
source backend/venv/bin/activate
```

---

## 2. Install Dependencies

```powershell
pip install --upgrade pip
pip install -r backend/requirements.txt
```

---

## 3. Environment Configuration

Copy the example configuration to `.env` in the `backend/` directory or project root:

```powershell
Copy-Item backend\.env.example backend\.env
```

Key configuration variables:
- `PROVEX_ENV`: `development` or `production`
- `DATABASE_URL`: `sqlite:///./provex_dev.db` (or PostgreSQL URL for production)
- `JWT_SECRET_KEY`: 256-bit secret key (generated via `python -c "import secrets; print(secrets.token_urlsafe(32))"`)
- `ACCESS_TOKEN_EXPIRE_MINUTES`: Default 30 minutes
- `REFRESH_TOKEN_EXPIRE_DAYS`: Default 14 days
- `AUTH_RATE_LIMIT_PER_MINUTE`: Default 20 requests per minute

---

## 4. Database Migrations

PROVEX uses Alembic for database migrations.

To apply migrations to the current database:

```powershell
.\backend\venv\Scripts\alembic.exe -c backend/alembic.ini upgrade head
```

To create a new migration after editing SQLAlchemy models in `app/models/`:

```powershell
.\backend\venv\Scripts\alembic.exe -c backend/alembic.ini revision --autogenerate -m "describe changes"
```

---

## 5. Starting the FastAPI Server

Run the development server with hot-reload enabled:

```powershell
.\backend\venv\Scripts\python.exe -m uvicorn app.main:app --app-dir backend --host 0.0.0.0 --port 8000 --reload
```

### Interactive API Documentation
- Swagger UI: [http://localhost:8000/docs](http://localhost:8000/docs)
- ReDoc: [http://localhost:8000/redoc](http://localhost:8000/redoc)
- Health Check: [http://localhost:8000/api/v1/health](http://localhost:8000/api/v1/health)

---

## 6. Running Automated Tests

Run the backend test suite (includes authentication tests, password validation, token rotation, logout revocation, and persistence verification):

```powershell
.\backend\venv\Scripts\pytest.exe -v backend/tests
```

---

## 7. Android App Connectivity Settings

The PROVEX Android application connects to the backend using Retrofit. Set `provex.apiBaseUrl` in `local.properties` or as a Gradle property:

### Android Emulator
The Android emulator accesses your host machine's localhost via `10.0.2.2`:
```properties
# Add to local.properties or gradle.properties:
provex.apiBaseUrl=http://10.0.2.2:8000/
```

### Physical Android Device
Ensure your phone and development computer are connected to the same Wi-Fi network. Find your computer's local IP (e.g. `192.168.1.100` via `ipconfig`) and configure:
```properties
provex.apiBaseUrl=http://192.168.1.100:8000/
```

### Production Deployment
In production, point to your verified HTTPS endpoint:
```properties
provex.apiBaseUrl=https://api.provex.app/
```
Cleartext HTTP is permitted only in debug builds scoped to local network domains (`10.0.2.2`, `localhost`, `127.0.0.1`) in `app/src/debug/res/xml/network_security_config.xml`.

---

## 8. Building the Android Application

To build the debug APK with the Gradle wrapper:

```powershell
.\gradlew.bat assembleDebug
```

The APK will be generated at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 9. GitHub OAuth App Configuration & Integration (Phase 3)

PROVEX implements secure, backend-owned GitHub OAuth Web Flow to turn self-reported skills into code-supported evidence without ever storing GitHub client secrets or unencrypted tokens on client devices.

### 9.1 Creating a GitHub OAuth App

1. Go to [GitHub Developer Settings](https://github.com/settings/developers).
2. Select **OAuth Apps** -> **New OAuth App**.
3. Fill in the application details:
   - **Application name**: `PROVEX`
   - **Homepage URL**: `http://localhost:8000` (or your production domain `https://api.provex.app`)
   - **Application description**: `Turn Code Into Credibility — Skill evidence verification`
   - **Authorization callback URL**:
     - For local development / emulator: `http://localhost:8000/api/v1/github/callback`
     - For testing on physical phone via LAN: `http://<your-lan-ip>:8000/api/v1/github/callback` (e.g. `http://192.168.137.246:8000/api/v1/github/callback`)
4. Click **Register application**.
5. Copy the **Client ID**.
6. Generate and copy a new **Client Secret**.

### 9.2 Configure Environment Variables in `backend/.env`

```ini
# GitHub OAuth Configuration
GITHUB_CLIENT_ID=your_github_client_id_here
GITHUB_CLIENT_SECRET=your_github_client_secret_here
GITHUB_CALLBACK_URL=http://192.168.137.246:8000/api/v1/github/callback

# Server-Side Token Encryption Key (Fernet 32-byte urlsafe base64)
# Generate via: python -c "from cryptography.fernet import Fernet; print(Fernet.generate_key().decode())"
GITHUB_TOKEN_ENCRYPTION_KEY=your_generated_fernet_key_here
```

### 9.3 Endpoints

| Method | Endpoint | Auth Required | Description |
|---|---|---|---|
| `POST` | `/api/v1/github/authorize` | Yes (Bearer) | Creates a single-use expiring CSRF state bound to the user session and returns GitHub's authorization URL. |
| `GET` | `/api/v1/github/callback` | No (Validates OAuth state) | Receives authorization code from GitHub, validates one-time state, exchanges code for access token server-side, encrypts token at rest, and renders a return confirmation. |
| `GET` | `/api/v1/github/status` | Yes (Bearer) | Returns connection status (`connected: true/false`), GitHub username, and connection timestamp. |
| `GET` | `/api/v1/github/repositories` | Yes (Bearer) | Fetches the user's accessible GitHub repositories with metadata (name, owner, description, language, visibility, default branch, updated date, HTML URL). |
| `DELETE` | `/api/v1/github/disconnect` | Yes (Bearer) | Revokes/deletes stored server-side GitHub credentials and clears connection. |


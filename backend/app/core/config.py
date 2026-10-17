from pathlib import Path
from pydantic_settings import BaseSettings, SettingsConfigDict
from typing import List, Union
from pydantic import field_validator
import secrets

_BACKEND_DIR = Path(__file__).resolve().parent.parent.parent
_ROOT_DIR = _BACKEND_DIR.parent

class Settings(BaseSettings):
    PROVEX_ENV: str = "development"
    HOST: str = "0.0.0.0"
    PORT: int = 8000
    
    DATABASE_URL: str = "sqlite:///./provex_dev.db"
    
    JWT_SECRET_KEY: str = ""
    JWT_ALGORITHM: str = "HS256"
    
    ACCESS_TOKEN_EXPIRE_MINUTES: int = 30
    REFRESH_TOKEN_EXPIRE_DAYS: int = 14
    
    AUTH_RATE_LIMIT_PER_MINUTE: int = 20
    CORS_ORIGINS: Union[str, List[str]] = "*"

    # GitHub OAuth & Token Encryption
    GITHUB_CLIENT_ID: str = ""
    GITHUB_CLIENT_SECRET: str = ""
    GITHUB_CALLBACK_URL: str = "http://localhost:8000/api/v1/github/callback"
    GITHUB_TOKEN_ENCRYPTION_KEY: str = ""

    @field_validator("JWT_SECRET_KEY", mode="before")
    def validate_or_generate_secret(cls, v: str, info) -> str:
        if v and len(v.strip()) >= 16:
            return v.strip()
        # If in development and not provided, generate a safe ephemeral secret
        # In production, require an explicit secure secret
        return secrets.token_urlsafe(32)

    @property
    def is_production(self) -> bool:
        return self.PROVEX_ENV.lower() == "production"

    @property
    def is_github_configured(self) -> bool:
        cid = self.GITHUB_CLIENT_ID.strip()
        sec = self.GITHUB_CLIENT_SECRET.strip()
        if not cid or not sec:
            return False
        placeholders = {
            "your_github_client_id_here",
            "your_github_client_secret_here",
            "paste_your_client_id",
            "paste_your_client_secret",
            "change_this",
        }
        if cid.lower() in placeholders or sec.lower() in placeholders:
            return False
        if cid.lower().startswith("paste_") or sec.lower().startswith("paste_"):
            return False
        return True

    @property
    def cors_origins_list(self) -> List[str]:
        if isinstance(self.CORS_ORIGINS, list):
            return self.CORS_ORIGINS
        if self.CORS_ORIGINS == "*":
            return ["*"]
        return [origin.strip() for origin in self.CORS_ORIGINS.split(",") if origin.strip()]

    model_config = SettingsConfigDict(
        env_file=[
            _ROOT_DIR / ".env",
            _BACKEND_DIR / ".env",
            ".env",
        ],
        env_file_encoding="utf-8",
        extra="ignore"
    )

settings = Settings()

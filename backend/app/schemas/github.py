from pydantic import BaseModel, ConfigDict
from typing import Optional, List
from datetime import datetime

class GitHubAuthorizeResponse(BaseModel):
    authorization_url: str
    state: str

class GitHubStatusResponse(BaseModel):
    connected: bool
    github_username: Optional[str] = None
    connected_at: Optional[datetime] = None
    scope: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

class GitHubRepositoryResponse(BaseModel):
    id: str
    name: str
    full_name: str
    owner: str
    description: Optional[str] = None
    language: Optional[str] = None
    is_private: bool = False
    html_url: str
    default_branch: str = "main"
    updated_at: Optional[str] = None

    model_config = ConfigDict(from_attributes=True)

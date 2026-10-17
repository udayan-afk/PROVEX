from pydantic import BaseModel, Field, ConfigDict
from typing import List, Optional
from datetime import datetime

class CareerProfileBase(BaseModel):
    level: str = Field(default="", max_length=100)
    target_role: str = Field(default="", max_length=150)
    location: str = Field(default="", max_length=150)
    work_mode: str = Field(default="NO_PREFERENCE", max_length=50)
    skills: List[str] = Field(default_factory=list)
    languages: List[str] = Field(default_factory=list)
    frameworks: List[str] = Field(default_factory=list)
    interests: List[str] = Field(default_factory=list)

class CareerProfileUpdate(CareerProfileBase):
    pass

class CareerProfileResponse(CareerProfileBase):
    id: str
    user_id: str
    name: str = ""
    email: str = ""
    created_at: datetime
    updated_at: datetime

    model_config = ConfigDict(from_attributes=True)

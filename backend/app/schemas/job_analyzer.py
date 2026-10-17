from pydantic import BaseModel, Field, field_validator
from typing import List, Optional, Dict

class JobAnalysisRequest(BaseModel):
    description: Optional[str] = Field(default=None, description="Pasted job or internship description text")
    url: Optional[str] = Field(default=None, description="Public job posting URL")

    @field_validator("description", "url")
    def clean_strings(cls, v: Optional[str]) -> Optional[str]:
        if v is not None:
            stripped = v.strip()
            return stripped if stripped else None
        return None


class ExtractedSkill(BaseModel):
    name: str
    category: str
    importance: str = Field(default="Mentioned", description="Required, Preferred, or Mentioned")
    evidence: str = Field(default="", description="Short supporting quote from the job posting")
    confidence: Optional[float] = None


class ProfileComparison(BaseModel):
    user_has_profile: bool = False
    matching_skills: List[str] = Field(default_factory=list)
    missing_skills: List[str] = Field(default_factory=list)
    next_skills_to_learn: List[str] = Field(default_factory=list)
    match_percentage: int = 0
    disclaimer: str = (
        "Skills comparison is based on self-reported profile data. "
        "PROVEX requires verified code citations to establish technical credibility."
    )


class SkillMatchItem(BaseModel):
    name: str
    category: str
    importance: str = "Mentioned"
    match_level: str = "NOT_FOUND"  # STRONG, PARTIAL, NOT_FOUND, PREFERRED_MATCH
    symbol: str = "○"              # ✓, ◐, ○, ★
    credit: float = 0.0
    supporting_repo: Optional[str] = None
    evidence_reason: Optional[str] = None


class GitHubMatchResult(BaseModel):
    connected: bool = True
    github_username: Optional[str] = None
    repositories_analyzed: int = 0
    score: Optional[float] = None
    score_label: str = "Repository Evidence Match"
    strong_count: int = 0
    partial_count: int = 0
    not_found_count: int = 0
    preferred_match_count: int = 0
    matched_skills: List[SkillMatchItem] = Field(default_factory=list)
    skills_to_strengthen: List[str] = Field(default_factory=list)
    warnings: List[str] = Field(default_factory=list)


class GitHubMatchRequest(BaseModel):
    skills: List[ExtractedSkill] = Field(default_factory=list)
    job_title: Optional[str] = None
    description: Optional[str] = None
    url: Optional[str] = None


class JobAnalysisResponse(BaseModel):
    job_title: Optional[str] = None
    company: Optional[str] = None
    source_type: str = "pasted_description"
    source_url: Optional[str] = None
    summary: str = ""
    skills: List[ExtractedSkill] = Field(default_factory=list)
    skill_counts_by_category: Dict[str, int] = Field(default_factory=dict)
    responsibilities: List[str] = Field(default_factory=list)
    qualifications: List[str] = Field(default_factory=list)
    education_requirements: List[str] = Field(default_factory=list)
    experience_requirements: List[str] = Field(default_factory=list)
    resume_keywords: List[str] = Field(default_factory=list)
    profile_comparison: Optional[ProfileComparison] = None
    github_matching: Optional[GitHubMatchResult] = None
    extraction_method: str = "deterministic_rules"
    warnings: List[str] = Field(default_factory=list)

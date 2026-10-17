from fastapi import APIRouter, Depends, HTTPException, status
from sqlalchemy.orm import Session

from app.api.deps import get_current_user
from app.db.session import get_db
from app.models.user import User
from app.schemas.job_analyzer import (
    JobAnalysisRequest,
    JobAnalysisResponse,
    GitHubMatchRequest,
    GitHubMatchResult,
)
from app.services.job_analyzer_service import JobAnalyzerService

router = APIRouter(prefix="/jobs", tags=["Jobs"])


@router.post(
    "/analyze",
    response_model=JobAnalysisResponse,
    status_code=status.HTTP_200_OK,
    summary="Analyze job or internship description to extract skills and compare with profile and GitHub repositories",
)
async def analyze_job(
    request: JobAnalysisRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Accepts either a pasted job description or a public job posting URL.
    Extracts structured technical and soft skills, qualifications, responsibilities,
    and compares the requirements against the authenticated user's persistent career profile and GitHub repositories.
    """
    if not request.description and not request.url:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="Please provide either a job description or a job posting URL.",
        )

    try:
        response = await JobAnalyzerService.analyze(
            description=request.description,
            url=request.url,
            user=current_user,
            db=db,
        )
        return response
    except ValueError as ve:
        raise HTTPException(
            status_code=status.HTTP_400_BAD_REQUEST,
            detail=str(ve),
        )
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Job analysis failed: {str(e)}",
        )


@router.post(
    "/match-github",
    response_model=GitHubMatchResult,
    status_code=status.HTTP_200_OK,
    summary="Match extracted job skills against user's connected GitHub repositories",
)
async def match_job_github(
    request: GitHubMatchRequest,
    current_user: User = Depends(get_current_user),
    db: Session = Depends(get_db),
):
    """
    Compares job skills directly with the user's actual GitHub repositories.
    Calculates deterministic match score based on requirement importance and evidence tier.
    """
    skills_to_match = request.skills

    # If description or url provided without skills list, analyze first
    if not skills_to_match and (request.description or request.url):
        analysis = await JobAnalyzerService.analyze(
            description=request.description,
            url=request.url,
            user=current_user,
            db=db,
        )
        skills_to_match = analysis.skills

    if not skills_to_match:
        raise HTTPException(
            status_code=status.HTTP_422_UNPROCESSABLE_ENTITY,
            detail="No skills provided to match against GitHub repositories.",
        )

    return await JobAnalyzerService.match_skills_with_github(
        user=current_user,
        skills=skills_to_match,
        db=db,
    )

from fastapi import APIRouter
from typing import Dict

router = APIRouter(tags=["Health"])

@router.get("/health")
def health_check() -> Dict[str, str]:
    return {
        "status": "healthy",
        "service": "PROVEX Backend API",
        "version": "0.1.0",
    }

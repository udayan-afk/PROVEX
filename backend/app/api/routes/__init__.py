from fastapi import APIRouter
from app.api.routes import auth, health, profile, github, jobs

api_router = APIRouter()

api_router.include_router(health.router)
api_router.include_router(auth.router)
api_router.include_router(profile.router)
api_router.include_router(github.router)
api_router.include_router(jobs.router)

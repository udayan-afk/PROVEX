from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware
from app.core.config import settings
from app.api.routes import api_router
from app.db.base import Base
from app.db.session import engine

# Import all models to ensure metadata registration
import app.models # noqa: F401

@asynccontextmanager
async def lifespan(app: FastAPI):
    # Create tables if not existing (especially for sqlite / local dev)
    Base.metadata.create_all(bind=engine)
    yield

app = FastAPI(
    title="PROVEX Career Intelligence API",
    description="Backend services for PROVEX: Turn Code Into Credibility.",
    version="0.1.0",
    lifespan=lifespan,
    docs_url="/docs",
    redoc_url="/redoc",
)

# CORS configuration
app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# API routes
app.include_router(api_router, prefix="/api/v1")

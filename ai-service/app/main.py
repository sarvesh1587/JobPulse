import logging

from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from app.config import get_settings
from app.routers import health, match, resume, skills

logging.basicConfig(level=logging.INFO)

settings = get_settings()

app = FastAPI(
    title="JobPulse AI Service",
    description=(
        "Semantic matching, skill extraction, and resume parsing for JobPulse. "
        "Called by the Java backend — not exposed directly to end users."
    ),
    version="0.1.0",
)

app.add_middleware(
    CORSMiddleware,
    allow_origins=settings.cors_origins_list,
    allow_methods=["GET", "POST"],
    allow_headers=["*"],
)

app.include_router(health.router)
app.include_router(skills.router)
app.include_router(resume.router)
app.include_router(match.router)

from fastapi import APIRouter

from app.models.schemas import ResumeParseRequest, ResumeParseResponse
from app.services.resume_parser import parse_resume

router = APIRouter(prefix="/api/v1/resume", tags=["resume"])


@router.post("/parse", response_model=ResumeParseResponse)
def parse(request: ResumeParseRequest) -> ResumeParseResponse:
    return parse_resume(request.text)

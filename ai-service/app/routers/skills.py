from fastapi import APIRouter

from app.models.schemas import SkillExtractionRequest, SkillExtractionResponse
from app.services.skill_extraction import extract_skills

router = APIRouter(prefix="/api/v1/skills", tags=["skills"])


@router.post("/extract", response_model=SkillExtractionResponse)
def extract(request: SkillExtractionRequest) -> SkillExtractionResponse:
    skills, method = extract_skills(request.text)
    return SkillExtractionResponse(skills=skills, method=method)

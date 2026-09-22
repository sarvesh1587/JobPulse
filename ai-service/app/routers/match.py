from fastapi import APIRouter

from app.models.schemas import SemanticMatchRequest, SemanticMatchResponse
from app.services.embeddings import semantic_similarity

router = APIRouter(prefix="/api/v1/match", tags=["match"])


@router.post("/semantic", response_model=SemanticMatchResponse)
def semantic(request: SemanticMatchRequest) -> SemanticMatchResponse:
    score, backend = semantic_similarity(request.resume_text, request.job_description)
    return SemanticMatchResponse(similarity=score, backend_used=backend)

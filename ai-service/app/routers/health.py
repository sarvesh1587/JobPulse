from fastapi import APIRouter

from app.models.schemas import HealthResponse
from app.services.embeddings import active_backend_name
from app.services.nlp_engine import get_nlp

router = APIRouter(tags=["health"])


@router.get("/health", response_model=HealthResponse)
def health() -> HealthResponse:
    return HealthResponse(
        status="ok",
        spacy_loaded=get_nlp() is not None,
        embedding_backend_available=active_backend_name(),
    )

from pydantic import BaseModel, Field


class SkillExtractionRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=50_000)


class SkillMatch(BaseModel):
    skill: str
    category: str | None = None


class SkillExtractionResponse(BaseModel):
    skills: list[SkillMatch]
    method: str  # "spacy-phrase-matcher" — always honest about how this was produced


class ResumeParseRequest(BaseModel):
    text: str = Field(..., min_length=1, max_length=100_000)


class ResumeParseResponse(BaseModel):
    detected_skills: list[SkillMatch]
    likely_education_level: str | None
    likely_graduation_year: int | None
    organizations_mentioned: list[str]  # raw spaCy ORG entities — often includes past employers/schools, unfiltered
    method: str


class SemanticMatchRequest(BaseModel):
    resume_text: str = Field(..., min_length=1, max_length=100_000)
    job_description: str = Field(..., min_length=1, max_length=50_000)


class SemanticMatchResponse(BaseModel):
    similarity: float = Field(..., ge=0.0, le=1.0)
    backend_used: str  # "transformer" or "tfidf" — always disclosed, never hidden


class HealthResponse(BaseModel):
    status: str
    spacy_loaded: bool
    embedding_backend_available: str

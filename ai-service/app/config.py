from functools import lru_cache
from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    """
    Reads from environment variables / a .env file. See .env.example.
    """
    model_config = SettingsConfigDict(env_file=".env", env_prefix="JOBPULSE_AI_")

    spacy_model: str = "en_core_web_sm"

    # "auto" tries sentence-transformers first and falls back to TF-IDF if
    # the transformer model can't be loaded (e.g. no network access to the
    # model hub). "transformer" or "tfidf" force one backend.
    embedding_backend: str = "auto"
    transformer_model_name: str = "all-MiniLM-L6-v2"

    cors_allowed_origins: str = "http://localhost:8080"

    @property
    def cors_origins_list(self) -> list[str]:
        return [o.strip() for o in self.cors_allowed_origins.split(",") if o.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()

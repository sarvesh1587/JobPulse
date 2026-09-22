import logging
from functools import lru_cache

from app.config import get_settings

logger = logging.getLogger(__name__)

TRANSFORMER = "transformer"
TFIDF = "tfidf"


@lru_cache
def _try_load_transformer():
    """
    Attempts to load the configured sentence-transformers model. Returns
    None on any failure — missing package, no network access to the model
    hub, out-of-disk, whatever — so the caller can fall back to TF-IDF
    rather than the whole service failing to start. This is a real,
    practical constraint in some deployment environments (e.g. air-gapped
    or network-restricted sandboxes), not a hypothetical one — this exact
    fallback path is what serves every request in this project's dev
    sandbox, where the model hub isn't reachable.
    """
    try:
        from sentence_transformers import SentenceTransformer
    except ImportError:
        logger.warning("sentence-transformers is not installed — using TF-IDF fallback for semantic matching")
        return None

    settings = get_settings()
    try:
        return SentenceTransformer(settings.transformer_model_name)
    except Exception as e:  # noqa: BLE001 — deliberately broad: any load failure should fall back, not crash
        logger.warning("Could not load transformer model '%s' (%s) — using TF-IDF fallback",
                        settings.transformer_model_name, e)
        return None


def active_backend_name() -> str:
    settings = get_settings()
    if settings.embedding_backend == "tfidf":
        return TFIDF
    if settings.embedding_backend == "transformer":
        return TRANSFORMER if _try_load_transformer() is not None else TFIDF
    # "auto"
    return TRANSFORMER if _try_load_transformer() is not None else TFIDF


def semantic_similarity(text_a: str, text_b: str) -> tuple[float, str]:
    backend = active_backend_name()

    if backend == TRANSFORMER:
        return _transformer_similarity(text_a, text_b), TRANSFORMER
    return _tfidf_similarity(text_a, text_b), TFIDF


def _transformer_similarity(text_a: str, text_b: str) -> float:
    import numpy as np

    model = _try_load_transformer()
    embeddings = model.encode([text_a, text_b])
    a, b = embeddings[0], embeddings[1]
    cosine = float(np.dot(a, b) / (np.linalg.norm(a) * np.linalg.norm(b)))
    return max(0.0, min(1.0, (cosine + 1) / 2))  # cosine in [-1,1] -> [0,1]


def _tfidf_similarity(text_a: str, text_b: str) -> float:
    """
    Deliberately weaker than a real embedding model — TF-IDF fit on just
    these two documents captures word overlap weighted by rarity between
    them, not meaning. It'll correctly see "Java Spring Boot" and "Java
    Spring Framework" as similar, but won't catch a resume that says
    "built REST APIs" matching a job that says "developed web services" the
    way a real sentence embedding would. Good enough to demonstrate the
    pipeline end-to-end; swap in the transformer backend for anything that
    needs to actually be good at this.
    """
    from sklearn.feature_extraction.text import TfidfVectorizer
    from sklearn.metrics.pairwise import cosine_similarity

    vectorizer = TfidfVectorizer(stop_words="english")
    try:
        matrix = vectorizer.fit_transform([text_a, text_b])
    except ValueError:
        # Happens if both texts are entirely stopwords/empty after cleaning.
        return 0.0

    score = cosine_similarity(matrix[0], matrix[1])[0][0]
    return max(0.0, min(1.0, float(score)))

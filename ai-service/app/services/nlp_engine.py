import logging

import spacy
from spacy.language import Language

from app.config import get_settings

logger = logging.getLogger(__name__)

_nlp: Language | None = None
_load_failed = False


def get_nlp() -> Language | None:
    """
    Lazily loads the configured spaCy model exactly once. Returns None if
    the model isn't installed rather than crashing the whole service —
    callers are expected to handle that (see skill_extraction.py's regex
    fallback) so one missing model download doesn't take down endpoints
    that don't need it.
    """
    global _nlp, _load_failed
    if _nlp is not None or _load_failed:
        return _nlp

    settings = get_settings()
    try:
        _nlp = spacy.load(settings.spacy_model)
        logger.info("Loaded spaCy model '%s'", settings.spacy_model)
    except OSError:
        _load_failed = True
        logger.warning(
            "spaCy model '%s' is not installed. Run: python -m spacy download %s",
            settings.spacy_model, settings.spacy_model,
        )
    return _nlp

import re
from functools import lru_cache

from spacy.matcher import PhraseMatcher

from app.models.schemas import SkillMatch
from app.services.nlp_engine import get_nlp
from app.services.skill_vocab import load_skill_vocabulary


@lru_cache
def _build_matcher():
    """
    Built once against the loaded spaCy model's vocab. Returns None if the
    model isn't available — extract_skills() falls back to plain regex in
    that case, so the endpoint still works, just with weaker matching
    (no lemmatization/case-folding from spaCy's pipeline).
    """
    nlp = get_nlp()
    if nlp is None:
        return None

    matcher = PhraseMatcher(nlp.vocab, attr="LOWER")
    vocab = load_skill_vocabulary()
    patterns = {item["name"]: nlp.make_doc(item["name"]) for item in vocab}
    for name, doc in patterns.items():
        matcher.add(name, [doc])
    return matcher


def extract_skills(text: str) -> tuple[list[SkillMatch], str]:
    """
    Whole-word / whole-phrase matching against the known skill vocabulary —
    this is still a fixed vocabulary, not open-ended entity recognition.
    spaCy buys case-insensitive, tokenization-aware matching over plain
    regex (so "Docker." at a sentence end or "Docker," in a list still
    matches cleanly), not semantic understanding of skills it's never seen
    named.
    """
    vocab = load_skill_vocabulary()
    vocab_by_name = {item["name"]: item for item in vocab}

    nlp = get_nlp()
    matcher = _build_matcher()

    if nlp is not None and matcher is not None:
        doc = nlp(text)
        matched_names = {doc.vocab.strings[match_id] for match_id, _, _ in matcher(doc)}
        method = "spacy-phrase-matcher"
    else:
        matched_names = {
            item["name"] for item in vocab
            if re.search(r"(?i)\b" + re.escape(item["name"]) + r"\b", text)
        }
        method = "regex-fallback"

    results = [
        SkillMatch(skill=name, category=vocab_by_name[name].get("category"))
        for name in sorted(matched_names)
    ]
    return results, method

import re

from app.models.schemas import ResumeParseResponse
from app.services.nlp_engine import get_nlp
from app.services.skill_extraction import extract_skills

YEAR_PATTERN = re.compile(r"\b(20\d{2})\b")

EDUCATION_KEYWORDS = [
    "B.Tech", "B.E.", "Bachelor of Engineering", "Bachelor of Technology",
    "M.Tech", "M.E.", "Master of Engineering", "Master of Technology",
    "B.Sc", "M.Sc", "MBA", "BCA", "MCA", "Ph.D",
]


def parse_resume(text: str) -> ResumeParseResponse:
    """
    Same honesty as the Java-side naive extractor, plus one real NLP step:
    spaCy's ORG entity recognition surfaces organization names (employers,
    schools) mentioned in the text. That list is returned raw and
    unfiltered — it's a lead for a human (or a future, better pipeline) to
    review, not a structured "this is your employer" / "this is your
    school" classification. Building that distinction properly needs more
    than a generic NER model; it's flagged as future work, not silently
    pretended to be solved.
    """
    skills, method = extract_skills(text)

    lower_text = text.lower()
    likely_education_level = next(
        (kw for kw in EDUCATION_KEYWORDS if kw.lower() in lower_text), None
    )

    years = [int(y) for y in YEAR_PATTERN.findall(text) if 2015 <= int(y) <= 2035]
    likely_graduation_year = max(years) if years else None

    organizations: list[str] = []
    nlp = get_nlp()
    if nlp is not None:
        doc = nlp(text)
        seen = set()
        for ent in doc.ents:
            if ent.label_ == "ORG" and ent.text not in seen:
                organizations.append(ent.text)
                seen.add(ent.text)

    return ResumeParseResponse(
        detected_skills=skills,
        likely_education_level=likely_education_level,
        likely_graduation_year=likely_graduation_year,
        organizations_mentioned=organizations[:15],  # cap — resumes can mention many orgs (projects, tools, clients)
        method=method,
    )

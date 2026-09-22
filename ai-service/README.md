# JobPulse AI Service

FastAPI service for skill extraction, resume parsing, and semantic
job-resume matching. Called by the Spring Boot backend — not meant to be
exposed directly to end users (no auth of its own; put it behind the Java
backend or an internal network boundary).

## Unlike the Java backend, this one I actually ran and tested

This sandbox's network allowlist includes PyPI, so — unlike Maven Central —
I could install dependencies here and verify things for real:

- The FastAPI app loads and every route registers correctly
- `en_core_web_sm` (spaCy's small English model) downloaded and loaded
  successfully — it's distributed as a pip-installable wheel via GitHub
  releases, which this sandbox can reach
- All 8 pytest tests pass (`pytest tests/ -v`)
- I started the server and hit every endpoint with real `curl` requests —
  skill extraction, resume parsing, and semantic matching all returned
  sensible results

**What I could not verify here:** `sentence-transformers` (and the `torch`
it depends on) could not be installed in this sandbox — partly disk space
(the full CUDA-enabled torch wheel is enormous), partly because the model
weights download from Hugging Face Hub, which isn't on this sandbox's
network allowlist. The code path is written and should work fine on your
own machine (`pip install -r requirements.txt` there has no such
restriction) — but I have not personally run it. The service is designed
to degrade gracefully if it can't load the transformer model: it falls
back to a TF-IDF-based similarity instead of crashing, and I *did* verify
that fallback path works correctly, including the automatic detection
logic (`embedding_backend=auto` in config).

## Endpoints

| Method | Path | What it does |
|---|---|---|
| GET | `/health` | Reports spaCy load status and which embedding backend is active |
| POST | `/api/v1/skills/extract` | `{"text": "..."}` → known skills found in the text |
| POST | `/api/v1/resume/parse` | `{"text": "..."}` → skills, likely education level, likely graduation year, organizations mentioned |
| POST | `/api/v1/match/semantic` | `{"resume_text": "...", "job_description": "..."}` → similarity score 0–1 |

Interactive docs at `/docs` once running.

## Honest limitations (on purpose, not by accident)

- **Skill extraction is vocabulary-matching, not open-ended NLP.** It only
  finds skills listed in `app/data/skills.json`, which is kept manually in
  sync with the Java backend's seed migration
  (`V2__seed_skills.sql`) — nothing enforces that sync automatically.
  spaCy's `PhraseMatcher` gets you clean tokenization-aware matching over
  that fixed list, not true entity recognition of skills it's never seen
  named.
- **Resume parsing's education/graduation-year detection is regex/keyword
  based**, same as the Java side's naive version — this service adds one
  genuinely different capability (spaCy's `ORG` named-entity recognition
  surfaces company/school names mentioned in the text), but that list is
  returned raw and unclassified. It doesn't know which org is "the
  candidate's employer" vs. "a client project" vs. "a school" — that
  requires more context than generic NER gives you.
- **The TF-IDF fallback is meaningfully weaker than real embeddings.** It's
  fit on just the two documents being compared, so it's really measuring
  weighted word overlap, not semantic meaning. "Built REST APIs" and
  "developed web services" mean nearly the same thing to a real sentence
  embedding and nothing alike to TF-IDF. `backend_used` in every response
  tells you honestly which one served that request — never hidden.

## Local setup

```bash
python -m venv venv
source venv/bin/activate        # Windows: venv\Scripts\activate
pip install -r requirements.txt
python -m spacy download en_core_web_sm
cp .env.example .env            # defaults are fine to start
uvicorn app.main:app --reload
```

Visit `http://localhost:8000/docs`.

## Testing

```bash
pytest tests/ -v
```

8 tests, covering health, skill extraction (positive/negative/validation),
resume parsing, and semantic matching (including a sanity check that
related texts score higher than unrelated ones).

## Wiring this into the Java backend

Nothing in the Spring Boot service calls this yet — that's the next piece.
The natural integration points:
- `ResumeParsingService` (Java) currently does its own naive extraction;
  it should call `POST /api/v1/resume/parse` here instead once this
  service has a real deployment target
- `MatchingService` (Java) could call `POST /api/v1/match/semantic` as an
  additional signal alongside its deterministic skill/experience/education/
  location/freshness scoring — the brief's intent (section 23) is for the
  deterministic engine to stay the primary, explainable score, with
  semantic matching as a supplementary signal, not a replacement

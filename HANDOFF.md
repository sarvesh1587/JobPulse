# JobPulse — Handoff / Context Document

This file exists so you can hand this project to another AI assistant
(DeepSeek, or anyone else) and have them understand the full state without
access to the original conversation. Read this before touching code.

## What JobPulse is

A job-intelligence platform (not another job board) for students and
early-career developers. Core idea: instead of showing thousands of
listings, score every job against the candidate's actual profile
(skills/experience/education/location/freshness) and **explain the score**
— never a bare number. Built in phases over one long session with Claude;
this document is the compressed version of everything decided along the way.

## Repo layout

```
jobpulse/
├── landing/          # static HTML landing page — no build step
├── frontend/         # React 18 + TS + Vite + Tailwind
├── backend/          # Spring Boot 3.3 / Java 21 API
├── ai-service/        # FastAPI (Python 3.12) — skills/resume/semantic match
└── README.md          # project-level overview (architecture diagrams, status)
```

Each subfolder has its own README with setup steps. This file is about
**decisions and constraints**, not step-by-step setup — the READMEs cover
that.

## The single most important constraint across this whole project

**Never fabricate.** This shows up everywhere in the code and copy:
- Company pages show `"Not enough data yet"` for historical hiring trends
  instead of inventing numbers
- Match scores always come with matched/missing skills, concerns, and
  positive signals — never a bare number
- Every "naive"/"placeholder" implementation (skill tagging via keyword
  match, education-level guessing via regex) is labeled as such in code
  comments and READMEs, with an explanation of what a *real* implementation
  would need — never silently passed off as more sophisticated than it is
- Demo/mock data is always clearly labeled as such

**Keep this convention if you continue this project.** It's not just a
style choice — it's load-bearing for the "explainable, not a black box"
product positioning the whole thing is built around.

## Design system (landing page + frontend — keep these in sync)

- Dark-first theme (near-black `#0A0B08` base), light theme is the toggle,
  not the default
- One accent split into three semantic colors: amber `#FF7A1E` (primary/
  CTA/scores), green `#4FE3A6` (verified/good), red `#FF6B6B` (missing/
  warning) — deliberately not a single flat accent color
- Typography: Plus Jakarta Sans (body/headings), IBM Plex Mono (metadata,
  scores, timestamps, technical labels only)
- Explicitly avoided: purple/blue gradients, glassmorphism, generic AI-SaaS
  hero sections, fake testimonials/stats/logos — the original brief was
  very insistent about not looking "AI-generated," and several rounds of
  revision happened for exactly this reason (grid background removed in
  favor of soft radial gradients, navbar/footer rebuilt from "too simple/
  casual" to structured+gradient-accented, etc.)
- Frontend Tailwind config pulls these as CSS variables (`--paper`, `--ink`,
  `--accent`, `--good`, `--miss`, etc.) — see `frontend/tailwind.config.js`
  and `frontend/src/index.css`

## Component status (as of handoff)

### Landing page — ✅ done
Single self-contained HTML file. Interactive hero demo (typing animation →
signal tags → job results with animated match scores), every section from
the original brief, dark/light toggle, mobile hamburger nav, scroll-reveal
animations. No known issues.

### Frontend — ✅ shell done, ❌ not wired to backend
Vite + React + TS + Tailwind + React Router. Verified with `npm run build`
(passes clean — this was actually run and checked, repeatedly, after every
change).

Built pages: **Overview** (dashboard/digest), **Discover** (search +
filters + job rows + "Should I Apply?" intelligence panel), **Applications**
(status pipeline tracker, not Kanban — brief was explicit about that),
**Skill Gap** (expandable priority cards with effort estimates, learning
resources, project ideas per missing skill).

Stub pages (placeholder "NOT BUILT YET" content, intentionally honest
rather than faked): Saved, Companies, Insights, Profile.

**Everything currently runs on mock data** in `frontend/src/data/*.ts`.
Wiring to the real backend API has not been started.

### Backend — ✅ code written, ⚠️ never compiled
Spring Boot 3.3 / Java 21. Full Flyway schema (`V1__init_schema.sql`,
`V2__seed_skills.sql`) covering every entity: users, candidate_profiles,
skills, candidate_skills, companies, job_sources, jobs, job_skills,
job_verifications, saved_jobs, applications, match_scores, skill_gaps,
resumes.

Built and (in principle) working:
- **Auth** — JWT, register/login (`/api/auth/*`)
- **Jobs** — search/filter/detail (`/api/jobs`, public)
- **Profile** — get/update (`/api/profile`, authenticated)
- **Ingestion** — real adapters for Greenhouse/Lever/Ashby's public job-
  board APIs (no auth needed on their side). Pipeline: fetch → normalize →
  validate → deduplicate (tier 1: external id per source; tier 2: company+
  title+location) → upsert → verify. Scheduler exists but is **disabled by
  default** (`jobpulse.ingestion.enabled=false`, empty target list) —
  nothing calls an external ATS until you explicitly configure a company
  slug. Manual trigger endpoint is ADMIN-role-gated (`/api/internal/
  ingestion/run`) but there's no admin-seeding mechanism yet.
- **Matching engine** — deterministic scoring in `MatchingService`, NOT an
  LLM call. Weights are configurable (`application.yml` →
  `jobpulse.matching.weights.*`). Education scoring is an honest
  placeholder (checks profile completeness, not actual fit — there's no
  structured "required education" field on Job, that needs NLP extraction
  the AI service doesn't do yet either). `GET /api/jobs/{id}/match`,
  authenticated, caches for 6 hours before recomputing.
- **Applications** — `/api/applications`, full CRUD-ish (create/list/
  patch status), maps to the same 7-state pipeline as the frontend
  (SAVED→PLANNING→APPLIED→ASSESSMENT→INTERVIEW→OFFER/REJECTED)
- **Resume upload** — `/api/resume/analyze`, multipart. **Privacy-critical
  detail: nothing is persisted unless the request includes
  `consentToStore=true`.** Without it, extraction still runs and results
  are returned, but nothing touches disk or the database. This was an
  explicit requirement from the original brief.
- **Skill gap** — `/api/skill-gap`, scans candidate's `preferredRoles`
  against the most recent 300 active job postings (capped on purpose — see
  `SkillGapService` class comment), ranks missing skills by frequency
- **Company intelligence** — `/api/companies`, `/api/companies/{id}`,
  computed strictly from currently-ACTIVE listings, `historicalHiringTrend`
  field is hardcoded to `"Not enough data yet"` — never fabricated

**Critical unresolved issue: this has never been compiled.** The sandbox
this was built in has no network access to Maven Central (only npm/PyPI
were reachable), so `mvn clean compile` has never actually been run. The
code was written carefully and a few real bugs were caught by manual
review along the way (see "Bugs already caught" below), but there could
easily be something that only shows up at compile time. **Run `mvn clean
compile` before doing anything else with this component.**

### AI service — ✅ done, and actually tested (unlike the backend)
FastAPI, Python 3.12. Unlike the Spring Boot backend, PyPI *was* reachable
in the build sandbox, so this one was actually installed, run, and hit with
real HTTP requests. 8/8 pytest tests pass.

Endpoints: `/health`, `POST /api/v1/skills/extract`, `POST /api/v1/resume/
parse`, `POST /api/v1/match/semantic`.

- Skill extraction: spaCy `PhraseMatcher` against a fixed vocabulary
  (`app/data/skills.json` — **manually kept in sync with the backend's
  `V2__seed_skills.sql`, nothing enforces this automatically**, worth
  fixing if you touch either list)
- Resume parsing: same skill extraction + regex-based education/graduation-
  year guessing + spaCy `ORG` named-entity recognition for
  employer/school names (returned raw/unclassified — it doesn't know which
  org is "the employer" vs. "a client project")
- Semantic matching: designed to use `sentence-transformers`
  (`all-MiniLM-L6-v2`), but **that could not be installed in the build
  sandbox** (disk space — full torch wheel is huge — plus Hugging Face Hub
  wasn't network-reachable there). The service auto-falls-back to a
  TF-IDF/scikit-learn cosine-similarity approach when the transformer
  model can't load, and **that fallback path is what was actually tested**.
  Every response includes `backend_used: "transformer" | "tfidf"` so it's
  never silently pretending to be the better one. On a normal dev machine
  with internet access, `pip install -r requirements.txt` should get the
  real transformer path working — that part is written but unverified.

**Not wired to the backend at all.** The Java `MatchingService` and
`ResumeParsingService` still use their own naive Java-side logic; nothing
calls this FastAPI service yet. That's the natural next integration point.

## Bugs already found and fixed during the build (context so they don't get reintroduced)

1. **Spring `@Transactional` self-invocation.** Originally had the ingestion
   upsert logic as a method on `IngestionService` calling itself — Spring's
   proxy-based AOP doesn't intercept that, so `@Transactional` would have
   silently done nothing. Fixed by extracting it into its own bean,
   `JobUpsertService`.
2. **Security config wildcard too broad.** `/api/jobs/**` as a public
   endpoint pattern accidentally also matched `/api/jobs/{id}/match`, which
   needs authentication. Fixed to `/api/jobs/*` (single segment only).
3. **Duplicate top-level YAML key.** Added a second top-level `spring:`
   block to `application.yml` for multipart config instead of merging into
   the existing one — would have silently broken datasource/JPA/Flyway
   config. Fixed by merging.
4. **N+1-ish dedup query.** First draft of tier-2 job deduplication did
   `jobRepository.findAll().stream().filter(...)` — replaced with a proper
   derived query method (`findFirstByCompanyIdAndNormalizedTitleAndNormalizedLocation`).
5. **Information disclosure in Applications API.** Updating another user's
   application by ID originally would have leaked "this ID exists" via a
   403. Changed to return 404 instead, same as if it didn't exist.
6. **Path traversal in resume upload.** Filename sanitization + an explicit
   check that the resolved storage path stays inside the user's directory
   before writing.

## What's next, in priority order

1. **Compile the backend.** This is the biggest unknown in the whole
   project. `cd backend && mvn clean compile`.
2. **Wire frontend → backend.** Replace the mock data in
   `frontend/src/data/*.ts` with real API calls (axios/fetch + TanStack
   Query would fit the existing dependency list). Auth token handling
   (store JWT, attach to requests) doesn't exist yet on the frontend side.
3. **Wire backend → AI service.** `ResumeParsingService` and
   `MatchingService` (Java) should call the FastAPI endpoints instead of
   their own naive logic. AI service has no auth of its own — needs to sit
   behind the Java backend or an internal network boundary, not be exposed
   directly.
4. **Build remaining frontend pages** — Saved, Companies, Insights, Profile
   are stubs.
5. **Redis caching** — dependency and Spring config exist
   (`spring.data.redis.*`), nothing actually uses it. Good candidates:
   company-page aggregates, popular searches.
6. **Docker Compose covering all four pieces** — current
   `backend/docker-compose.yml` only has Postgres + Redis + the Java
   backend; the AI service isn't in it yet.
7. **Admin-role seeding** — needed to actually use the manual ingestion
   trigger endpoint.

## A note on tone/process, if it matters to how you work

Every step in this build followed the same pattern: build something, then
say plainly what wasn't verified and why (network/sandbox limits, mostly),
rather than presenting untested code as tested. If you're picking this up
with another assistant, it's worth keeping that up — the READMEs
throughout this project are written the same way on purpose, and the
person you're handing this to will trust the whole thing more if the next
phase is documented with the same honesty about what's real vs. assumed.

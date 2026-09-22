# JobPulse Backend

Spring Boot 3 (Java 21) core API for JobPulse. Handles auth, candidate
profiles, job search, and ingesting listings from Greenhouse, Lever, and
Ashby's public job-board APIs.

This is one phase of a larger build — see the root brief for the full
product scope. The Python AI service (semantic matching, resume parsing,
embeddings) is a separate, not-yet-built component this API will call into.

## What's actually here right now

- **Auth** — register/login, JWT (stateless, no server-side sessions)
- **Jobs** — search/filter, detail view
- **Profile** — get/update candidate profile
- **Ingestion** — `JobSourceAdapter` implementations for Greenhouse, Lever,
  and Ashby; a pipeline that fetches → validates → deduplicates → upserts →
  records a verification row; a scheduler (disabled by default) and a manual
  admin-only trigger endpoint. Includes a naive keyword-based skill tagger
  (`JobUpsertService.tagSkillsNaively`) so ingested jobs actually end up
  with `job_skills` rows for the matching engine to score against — this is
  explicitly a placeholder for real NLP extraction, not a finished feature.
- **Matching engine** (`GET /api/jobs/{id}/match`) — deterministic,
  explainable scoring across skills/experience/education/location/
  freshness, using the weights in `application.yml`. Every score comes with
  matched/missing skills, concerns, and positive signals — see
  `MatchingService`'s class comment for the honest limitations (education
  scoring is a placeholder; the "job-type" weight is configured but not yet
  used since there's no reliable candidate signal for it).
- **Applications** (`/api/applications`) — save a job into the pipeline,
  list your applications (each with its match score if one's been
  computed), and PATCH status/next-action/notes as it moves through
  Saved → Planning → Applied → Assessment → Interview → Offer/Rejected
- **Resume upload/analysis** (`POST /api/resume/analyze`, multipart) —
  extracts text from PDF/DOCX (Apache PDFBox / POI), then a naive
  keyword-based pass detects skills against the known skill vocabulary plus
  a rough education-level/graduation-year guess. **Nothing is saved unless
  the request includes `consentToStore=true`** — per the brief's explicit
  privacy requirement, the default call just returns what would be
  detected, for review, and writes nothing to disk or the database. Same
  "this is a placeholder for real NLP" caveat as the job-skill tagger.
- **Skill gap** (`GET /api/skill-gap`) — scans the candidate's target roles
  (from `preferredRoles` on their profile, falling back to a general recent-
  postings scan if that's empty) against the most recent 300 active
  listings, ranks which required skills show up most often that the
  candidate doesn't have yet, and separately surfaces which of the
  candidate's *own* skills are actually in demand for those roles. See
  `SkillGapService`'s class comment for the scope simplifications.
- **Company intelligence** (`/api/companies`, `/api/companies/{id}`) — open
  roles, internship/entry-level counts, role distribution, top requested
  skills, and locations, all computed strictly from currently ACTIVE
  listings. No historical hiring trend is fabricated — the detail response
  always says `"Not enough data yet"` for that field, per the brief. Public
  endpoints, same as job search.
- **Schema** — full Flyway migration covering every entity from the brief
  (jobs, companies, skills, applications, match scores, skill gaps, resumes,
  job verifications), even though not every table has a service/controller
  wired up yet

## What's not here yet

- The **Python AI service** itself — real skill extraction, resume parsing,
  semantic matching
- **Redis caching** — dependency and config are in, nothing uses it yet
- Real **admin-role seeding** — the ingestion trigger endpoint checks for
  `role = ADMIN`, but nothing currently creates an admin user

## Important — this hasn't been compiled here

This sandbox's network allowlist doesn't include Maven Central, so I could
not run `mvn compile` to verify this builds, unlike the frontend (where npm
registry access let me verify every change with a real build). I've been
careful, but **please run a build yourself before trusting this**:

```bash
mvn clean compile
```

If it doesn't compile, tell me the error and I'll fix it — I'd rather know
than have this silently ship broken.

## Local setup

1. Copy `.env.example` to `.env` and fill in real values (a real
   `JWT_SECRET` especially — generate one with `openssl rand -base64 64`).
2. Start Postgres + Redis:
   ```bash
   docker compose up postgres redis -d
   ```
3. Run the app (it reads `.env` via your shell/IDE — Spring Boot doesn't
   load `.env` files itself, so either `export $(cat .env | xargs)` first or
   configure your IDE's run config with those environment variables):
   ```bash
   mvn spring-boot:run
   ```
4. Flyway runs the schema automatically on startup.
5. API docs: `http://localhost:8080/api/docs/ui`

## Turning ingestion on

Ingestion is **off by default** (`INGESTION_ENABLED=false`) and ships with
an empty target list — nothing calls an external ATS until you explicitly
configure a company slug you've verified yourself. Add targets under
`jobpulse.ingestion.targets` in `application.yml`, then set
`INGESTION_ENABLED=true`.

## Testing

```bash
mvn test
```

Uses H2 in-memory (Postgres-compatible mode) for tests, so no local
database is required to run the test suite. Currently covers: application
context loads, and `AuthService` registration logic. More coverage is a
follow-up.

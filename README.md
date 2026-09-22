\# JobPulse



\*\*Stop searching. Start targeting.\*\*



> \*\*Repo:\*\* \[github.com/sarvesh1587/JobPulse](https://github.com/sarvesh1587/JobPulse)

> \*\*Status:\*\* In active development · \*\*Last verified:\*\* 2026-09-23

> \*\*Stack:\*\* Java 21 · Spring Boot 3.3 · PostgreSQL · Redis · FastAPI · React 18 · TypeScript



Job intelligence for students and early-career developers. Instead of

showing thousands of listings, JobPulse scores every job against your

actual profile — skills, experience, education, location, freshness — and

explains \*why\*, so the question stops being "what jobs exist" and starts

being "which ones are actually worth my time."



This is a portfolio project, built in phases and documented honestly at

every stage — what's real, what's a placeholder, and what's still missing.

See \*\*Status\*\* below before assuming any piece is production-ready.



\---



\## Screenshots



!\[Discover — job list with match scores](docs/discover.png)



!\[Overview — dashboard](docs/overview.png)



!\[Applications — application tracker](docs/applications.png)



!\[Skill Gap — missing skills analysis](docs/skill-gap.png)



\---



\## Repo layout



Built as four separate pieces, unified into one repo:



```

jobpulse/

├── landing/          # marketing/landing page (static HTML)

├── frontend/         # React app — Overview, Discover, Applications, Skill Gap

├── backend/          # Spring Boot API — auth, jobs, matching, ingestion

├── ai-service/       # FastAPI — skill extraction, resume parsing, semantic match

└── README.md         # this file

```



Each subfolder has its own README with setup instructions specific to that

piece — this file is the map, not a replacement for those.



\---



\## Architecture



```mermaid

flowchart TB

&#x20;   subgraph Client

&#x20;       FE\[React Frontend<br/>Vite + TS + Tailwind]

&#x20;       LP\[Landing Page<br/>static HTML]

&#x20;   end



&#x20;   subgraph Core\["Spring Boot Backend"]

&#x20;       API\[REST API<br/>auth · jobs · profile · applications]

&#x20;       ING\[Ingestion Pipeline<br/>fetch → validate → dedupe → upsert → verify]

&#x20;       MATCH\[Matching Engine<br/>deterministic, explainable scoring]

&#x20;       SKILLGAP\[Skill Gap Service]

&#x20;       COMP\[Company Intelligence]

&#x20;   end



&#x20;   subgraph AI\["Python AI Service (FastAPI)"]

&#x20;       SKILLS\[Skill Extraction<br/>spaCy PhraseMatcher]

&#x20;       RESUME\[Resume Parsing<br/>spaCy NER + regex]

&#x20;       SEMANTIC\[Semantic Match<br/>sentence-transformers → TF-IDF fallback]

&#x20;   end



&#x20;   subgraph External\["External ATS Job Boards"]

&#x20;       GH\[Greenhouse]

&#x20;       LV\[Lever]

&#x20;       AB\[Ashby]

&#x20;   end



&#x20;   subgraph Data

&#x20;       PG\[(PostgreSQL)]

&#x20;       RD\[(Redis — configured,<br/>not yet wired)]

&#x20;   end



&#x20;   FE -->|JWT auth| API

&#x20;   LP -.->|planned| API

&#x20;   API --> PG

&#x20;   API -.->|not yet wired| RD

&#x20;   API -->|planned| AI

&#x20;   ING --> GH

&#x20;   ING --> LV

&#x20;   ING --> AB

&#x20;   ING --> PG

&#x20;   MATCH --> PG

&#x20;   SKILLGAP --> PG

&#x20;   COMP --> PG

&#x20;   API --> ING

&#x20;   API --> MATCH

&#x20;   API --> SKILLGAP

&#x20;   API --> COMP

```



Dotted lines mark connections that are designed but not implemented yet —

see Status.



\### Data flow: from a raw listing to a match score



```mermaid

sequenceDiagram

&#x20;   participant Source as Greenhouse/Lever/Ashby

&#x20;   participant Ing as IngestionService

&#x20;   participant DB as PostgreSQL

&#x20;   participant Match as MatchingService

&#x20;   participant User



&#x20;   Ing->>Source: GET public job board API

&#x20;   Source-->>Ing: raw jobs (source-specific shape)

&#x20;   Ing->>Ing: normalize into common Job shape

&#x20;   Ing->>Ing: validate (title, id, apply URL present)

&#x20;   Ing->>DB: dedupe check (external id, then company+title+location)

&#x20;   Ing->>DB: upsert Job + naive keyword skill tagging

&#x20;   Ing->>DB: record JobVerification



&#x20;   User->>Match: GET /api/jobs/{id}/match

&#x20;   Match->>DB: load candidate profile + skills

&#x20;   Match->>DB: load job + required skills

&#x20;   Match->>Match: score skills/experience/education/location/freshness

&#x20;   Match->>DB: persist MatchScore

&#x20;   Match-->>User: score + matched/missing skills + concerns + positive signals

```



\---



\## Tech stack



| Layer | Stack |

|---|---|

| Landing page | Static HTML/CSS/JS, no build step |

| Frontend | React 18, TypeScript, Vite, Tailwind CSS, React Router, lucide-react |

| Backend | Java 21, Spring Boot 3.3, Spring Security (JWT), Spring Data JPA, PostgreSQL, Flyway, Redis (client wired, unused) |

| AI service | Python 3.12, FastAPI, spaCy, sentence-transformers (with TF-IDF/scikit-learn fallback) |

| Ingestion | Greenhouse, Lever, and Ashby public job-board APIs — no auth, no scraping |



\---



\## Status



Honesty over optics — this is what actually exists, not what the brief

originally asked for. \*\*Last verified: 2026-09-23.\*\*



\### ✅ Built, runs, verified



\- \*\*Backend\*\* — compiles clean on Java 21, starts against real Postgres

&#x20; (Docker), Flyway applies V1 + V2 migrations, and

&#x20; `POST /api/auth/register` returns a valid JWT. The full chain

&#x20; Postgres → JPA → Flyway → Spring Security → JWT is proven working

&#x20; end to end.

\- \*\*AI service\*\* — server starts via `uvicorn`, all endpoints respond,

&#x20; 8/8 pytest tests pass. `/health` reports the `sentence-transformers`

&#x20; backend active (not just the TF-IDF fallback).

\- \*\*Frontend\*\* — builds clean (`npm run build` verified), runs on Vite

&#x20; dev server. Currently uses mock data.

\- \*\*Landing page\*\* — full design system, interactive hero demo, dark/light

&#x20; theme, mobile nav.

\- \*\*Ingestion pipeline\*\* — real adapters for Greenhouse / Lever / Ashby

&#x20; public APIs, fetch → validate → dedupe → upsert → verify, naive

&#x20; keyword-based skill tagging.

\- \*\*Matching engine\*\* — deterministic, explainable scoring

&#x20; (skills / experience / education / location / freshness) with

&#x20; configurable weights. Never a bare number without a reason.



\### ⚠️ Known issues



Being explicit about what's broken. These are all in active progress and

will be fixed in follow-up commits.



\- \*\*Flyway migration gap\*\* — `V1\_\_init\_schema.sql` omits `created\_at` and

&#x20; `updated\_at` on 9 tables that all extend `BaseEntity`. Currently worked

&#x20; around with `spring.jpa.hibernate.ddl-auto=none` (set via env var).

&#x20; A `V3\_\_add\_missing\_base\_entity\_columns.sql` migration to backfill the

&#x20; missing columns is in progress; once applied, `ddl-auto` can return to

&#x20; `validate`.

\- \*\*Backend test compilation\*\* — `AuthServiceTest` fails to compile

&#x20; because Lombok `@Builder` on `User` cannot see the inherited

&#x20; `BaseEntity.id` field. Temporarily bypassed with

&#x20; `-Dmaven.test.skip=true` when running. Fix in progress (either setter

&#x20; post-build or migrate to `@SuperBuilder`).

\- \*\*Redis\*\* — dependency and config exist, nothing uses it yet.

\- \*\*Admin role seeding\*\* — the ingestion trigger endpoint checks for

&#x20; `role = ADMIN`, but nothing currently creates an admin user.



\### ❌ Not built yet



\- \*\*Frontend ↔ backend wiring\*\* — the React app doesn't call the Spring

&#x20; Boot API at all right now

\- \*\*Backend ↔ AI service wiring\*\* — Java's `MatchingService` and

&#x20; `ResumeParsingService` still use their own local logic; they don't

&#x20; call the FastAPI service

\- \*\*Frontend pages\*\*: Saved, Companies, Insights, Profile (stubs only)

\- \*\*Docker Compose doesn't cover the AI service or the frontend yet\*\*

&#x20; (only Postgres, Redis, backend)



\---



\## Local setup



\### Prerequisites



\- \*\*Java 21\*\* (Temurin recommended)

\- \*\*Maven 3.9+\*\*

\- \*\*Node 18+\*\* — verified on Node 22

\- \*\*Python 3.11+\*\* — verified on Python 3.12

\- \*\*Docker Desktop\*\* — for Postgres + Redis



\### 1. Infra (Postgres + Redis via Docker)



```bash

cd backend

docker compose up postgres redis -d

docker compose ps

```



\### 2. Backend (Spring Boot)



```bash

cd backend

cp .env.example .env

\# Edit .env and set JWT\_SECRET to a long random string

```



Load `.env` into your shell, then run:



```bash

\# Unix / Git Bash

export $(cat .env | xargs)



\# Windows cmd

for /f "usebackq tokens=1,\* delims==" %i in (".env") do set "%i=%j"

```



Then:



```bash

mvn clean compile

mvn spring-boot:run -Dmaven.test.skip=true

```



Success looks like `Started JobpulseApplication in X.XXX seconds` plus

Flyway logging V1 and V2 migrations applied.



\*\*Smoke test:\*\*



```bash

curl -s -X POST http://localhost:8080/api/auth/register \\

&#x20; -H "Content-Type: application/json" \\

&#x20; -d '{"email":"test@example.com","password":"password123","fullName":"Test User"}'

```



Swagger UI: <http://localhost:8080/api/docs/ui>



\### 3. AI service (FastAPI)



```bash

cd ai-service

python -m venv venv

\# Windows: venv\\Scripts\\activate

source venv/bin/activate

pip install -r requirements.txt

python -m spacy download en\_core\_web\_sm

uvicorn app.main:app --reload

```



\*\*Health check:\*\*



```bash

curl -s http://localhost:8000/health

```



Swagger UI: <http://localhost:8000/docs>



\### 4. Frontend (Vite + React)



```bash

cd frontend

npm install

npm run dev

```



Opens on <http://localhost:5173>.



\---



\## What's next, in priority order



1\. `V3\_\_add\_missing\_base\_entity\_columns.sql` migration to fix the

&#x20;  schema-validation gap; restore `ddl-auto=validate`

2\. Fix `AuthServiceTest` compilation; get `mvn test` green

3\. Wire the frontend to the backend (replace mock data with real API calls)

4\. Wire `MatchingService` / `ResumeParsingService` (Java) to the AI service

5\. Build the remaining frontend pages (Saved, Companies, Insights, Profile)

6\. Redis caching for hot queries (popular searches, company pages)

7\. Docker Compose covering all four services together



\---



\## License



Portfolio project — no license granted for reuse without permission.


# JobPulse

**Stop searching. Start targeting.**

JobPulse is a job discovery and application-tracking platform for students and early-career developers. Candidates search real listings, see an *explainable* match score for each one (skills, experience, education, location, freshness, plus exactly what's missing), apply, and track every application in one pipeline. Employers post roles and move applicants through stages. Admins moderate users and listings.

**Stack:** Java 21 · Spring Boot 3.5 · Spring Security · Spring Data JPA (Hibernate) · PostgreSQL · Flyway · React 18 · TypeScript · Vite · Tailwind CSS · TanStack Query

---

## What works

| Area | What you can do |
| --- | --- |
| Discovery | Keyword and location search, filters (type, level, work mode, salary, posted date), sorting, pagination. All of it lives in the URL, so it survives refresh, sharing and the back button. |
| Job detail | Full description, skills, salary, company info, save/unsave, and an explainable match score for signed-in candidates. |
| Applying | **Employer-posted jobs:** apply in-app with a cover note. **Imported jobs:** apply on the company's own site, then track it here. |
| Tracker | Every application with its status, a progress bar, an audit-trail timeline, private notes, next step, interview time, and withdraw/re-apply. |
| Dashboard | Real counts per stage, upcoming interviews, skill-based recommendations, recently saved jobs, and a profile-completeness checklist. |
| Profile | Basics, target roles, skills (with autocomplete), education and experience entries, and resume upload. The resume is only stored if you tick consent. |
| Skill gap | The skills that come up most in your target roles that you don't have yet, computed from live listings. |
| Employers | Company profile, post/edit/close/reopen jobs, review applicants (profile + cover note), move them through the pipeline with interview times and notes. |
| Admin | Platform stats, search users and deactivate them (takes effect immediately), take down any listing, trigger ATS ingestion. |
| Ingestion | Pulls public job boards from Greenhouse, Lever and Ashby, with de-duplication and keyword skill tagging. Off until you configure a board. |

Nothing on screen is mocked. A new account shows empty states, not invented numbers.

---

## Repository layout

```
jobpulse/
├── backend/            Spring Boot API (layered: controller → service → repository)
│   └── src/main/resources/db/migration   Flyway SQL migrations (V1–V4)
├── frontend/           React SPA (Vite), served by nginx in Docker
├── docs/
│   ├── ARCHITECTURE.md     system design, ER diagram, auth flow, JPA, security review, testing, deployment
│   ├── API.md              every endpoint with auth rules and examples
│   └── INTERVIEW_GUIDE.md  project pitches, interview questions and answers, trade-offs
└── docker-compose.yml  PostgreSQL + backend + frontend
```

---

## Running it locally

### Prerequisites
- **Java 21** (Temurin recommended). The Maven wrapper (`./mvnw`) downloads Maven for you.
- **Node 18+** (tested on Node 22)
- **PostgreSQL 14+** running locally, *or* Docker

### 1. Database

Create a database and user (any PostgreSQL client):

```sql
CREATE ROLE jobpulse LOGIN PASSWORD 'change-me';
CREATE DATABASE jobpulse OWNER jobpulse;
```

Flyway creates every table when the backend starts. There's no manual schema step.

### 2. Backend

```bash
cd backend
cp .env.example .env        # then set DB_PASSWORD and JWT_SECRET
./mvnw spring-boot:run      # Windows: mvnw.cmd spring-boot:run
```

`backend/.env` is loaded automatically. The default profile is `dev`, which seeds **demo data** on an empty database: three fictional companies (all on `@example.com` addresses, marked as demo in their descriptions), 14 listings, and these accounts:

| Account | Email | Password |
| --- | --- | --- |
| Candidate | `candidate@example.com` | value of `DEV_SEED_PASSWORD` (default `jobpulse-demo`) |
| Employer | `hiring@northwind.example.com` | same |
| Admin | `admin@example.com` | same |

Run with `SPRING_PROFILES_ACTIVE=prod` for a clean database with no seed data.

- API: http://localhost:8080/api
- Swagger UI: http://localhost:8080/api/docs/ui (call `GET /api/auth/csrf` first, then `POST /api/auth/login`)
- Health: http://localhost:8080/actuator/health

### 3. Frontend

```bash
cd frontend
npm install
npm run dev
```

Open http://localhost:5173. Vite forwards `/api` to `localhost:8080`, so the browser only ever talks to one origin.

### Everything in Docker

```bash
cp .env.example .env    # set DB_PASSWORD and JWT_SECRET
docker compose up --build
```

Open http://localhost:8081. nginx serves the SPA and proxies `/api` to the backend, which isn't exposed directly.

### Deploying (Render + Neon, free)

The root `Dockerfile` builds one image that serves both the React app and the API. `render.yaml` sets up the Render service. Step-by-step guide: [docs/DEPLOYMENT.md](docs/DEPLOYMENT.md).

### Importing real listings (optional)

Add boards under `jobpulse.ingestion.targets` in `application.yml` (or pass them as arguments), then trigger a run as an admin:

```bash
./mvnw spring-boot:run -Dspring-boot.run.arguments="--jobpulse.ingestion.targets[0].source=GREENHOUSE --jobpulse.ingestion.targets[0].slug=gitlab"
# then, signed in as admin: POST /api/admin/ingestion/run
```

Set `INGESTION_ENABLED=true` to also run it on a schedule (every `INGESTION_INTERVAL_MINUTES`).

---

## Tests

```bash
cd backend && ./mvnw test        # 72 tests: unit + integration against a real embedded PostgreSQL
cd frontend && npm test          # 13 unit tests (Vitest)
cd frontend && npm run typecheck # strict TypeScript
cd frontend && npm run build     # production build
```

Backend integration tests start a real PostgreSQL binary (via `io.zonky.test:embedded-postgres`, no Docker needed), run every Flyway migration, and drive the full Spring context, security filters included, through MockMvc. See [docs/ARCHITECTURE.md](docs/ARCHITECTURE.md#testing-strategy).

---

## Environment variables

| Variable | Used by | Purpose |
| --- | --- | --- |
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | backend | PostgreSQL connection |
| `JWT_SECRET` | backend | HMAC key for auth tokens. **Required**, at least 32 bytes. The app refuses to start without it. |
| `AUTH_TOKEN_TTL` | backend | Session length (default `12h`) |
| `AUTH_COOKIE_SECURE` | backend | `true` behind HTTPS (the `prod` default), `false` for local http |
| `ADMIN_EMAIL`, `ADMIN_PASSWORD` | backend | Creates the first admin on startup if none exists (password ≥ 12 chars) |
| `CORS_ALLOWED_ORIGINS` | backend | Only needed if the SPA is hosted on a different origin |
| `RESUME_STORAGE_DIR`, `RESUME_MAX_SIZE_MB` | backend | Where consented resumes are stored, and the size cap |
| `INGESTION_ENABLED`, `INGESTION_INTERVAL_MINUTES` | backend | Scheduled ATS ingestion |
| `DEV_SEED_PASSWORD` | backend (dev) | Password for the demo accounts |
| `DEMO_DATA_ENABLED`, `DEMO_DATA_PASSWORD` | backend | Seed demo companies, jobs and accounts on a public deployment (no demo admin) |
| `PORT` | backend | Port injected by hosts like Render (falls back to `SERVER_PORT`, then 8080) |
| `VITE_API_BASE_URL` | frontend | API base (default `/api`) |
| `VITE_DEV_API_PROXY` | frontend (dev) | Where Vite forwards `/api` |

Secrets live only in `.env` files, which are gitignored. Templates: [`.env.example`](.env.example), [`backend/.env.example`](backend/.env.example), [`frontend/.env.example`](frontend/.env.example).

---

## Known limitations

These are documented honestly in [docs/INTERVIEW_GUIDE.md](docs/INTERVIEW_GUIDE.md#limitations-and-trade-offs). The short version:
- Skill tagging and resume parsing are keyword matching against a skill catalogue, not NLP.
- The auth rate limiter is in memory, per instance.
- Sessions can't be revoked one at a time before they expire (deactivating a user does cut them off immediately).
- There are no email notifications. Candidates see status changes in the app.
- The Docker setup is written but wasn't run on the machine this was last verified on (no Docker available). The backend, frontend and database were verified running natively.

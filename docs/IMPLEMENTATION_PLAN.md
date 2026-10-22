# JobPulse: implementation record

## What the audit found (before)

| Piece | State |
| --- | --- |
| `backend/` | Spring Boot 3.3. `ddl-auto: validate` would fail because 9 tables lacked the audit columns `BaseEntity` maps. All `@ManyToOne` were EAGER. The job list had an N+1. Profile skills were never saved. Application statuses mixed bookmarks with stages. Redis was unused. Tests ran on H2 with Flyway disabled. |
| `frontend/` | Every page used mock data, with no auth or API client. Filter and save buttons did nothing. Half the pages were stubs. The dashboard showed invented numbers. |
| `landing/` | A static HTML page with a duplicated design system and hardcoded fake stats. |
| `ai-service/` | A FastAPI service that nothing called, duplicating the Java logic. |

## Decisions
1. One Java backend and one React app. The unused FastAPI service was removed (it's in git history at `1906f0b`).
2. The landing page was folded into the React app, keeping the original identity: warm near-black and amber, Plus Jakarta Sans with IBM Plex Mono, the bar-mark logo, and the oversized footer wordmark.
3. Two job origins: INGESTED (apply on the company site, the candidate tracks it) and EMPLOYER (apply in-app, the employer drives the pipeline).
4. Bookmarks (`saved_jobs`) are separate from applications. Applications follow a state machine and keep an audit trail.
5. Auth is a JWT in an HttpOnly cookie, with CSRF protection. Dev and Docker serve the app from one origin through a proxy.
6. Roles are CANDIDATE, EMPLOYER and ADMIN. Admins come from bootstrap only.
7. Tests run on embedded PostgreSQL. Spring Boot went from 3.3 to 3.5, and Redis was removed.

## Status (verified)
- Backend: 72 tests pass (unit + integration on embedded Postgres). Running against Postgres 18, Flyway V1–V4 applied and the schema validated.
- Frontend: strict type-check, 13 Vitest tests and the production build all pass.
- End to end in a browser against the real API: register → profile → search/filter/sort/back → job detail and match → save/unsave → apply → tracker → another user's application gives 404 → employer reviews and schedules an interview → the candidate's dashboard shows it → role-restricted APIs give 403.
- Real ingestion: 231 GitLab Greenhouse listings imported through the admin endpoint.
- Not verified: the Docker images (Docker wasn't available on the machine).

Details: [ARCHITECTURE.md](ARCHITECTURE.md) · [API.md](API.md) · [INTERVIEW_GUIDE.md](INTERVIEW_GUIDE.md)

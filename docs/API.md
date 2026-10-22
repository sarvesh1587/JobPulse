# JobPulse REST API

Base path: `/api`. Interactive docs: `/api/docs/ui` (Swagger), and the OpenAPI JSON at `/api/docs`.

**Auth:** sign in with `POST /api/auth/login`. The server sets an HttpOnly `JOBPULSE_AUTH` cookie, and the browser sends it from then on. Every non-GET request must also send `X-XSRF-TOKEN`, copied from the `XSRF-TOKEN` cookie (call `GET /api/auth/csrf` once to get it).

**Errors** always look like this:
```json
{ "timestamp": "2026-10-22T18:42:25Z", "status": 400, "code": "VALIDATION_ERROR",
  "message": "Some fields need attention.", "path": "/api/auth/register",
  "fieldErrors": { "email": "must be a well-formed email address" } }
```
Codes: `VALIDATION_ERROR` 400 · `BAD_REQUEST` 400 · `UNAUTHENTICATED` 401 · `BAD_CREDENTIALS` 401 · `FORBIDDEN` / `CSRF_INVALID` / `ACCOUNT_DISABLED` 403 · `NOT_FOUND` 404 · `CONFLICT` 409 · `BUSINESS_RULE` 422 · `TOO_MANY_REQUESTS` 429 · `INTERNAL_ERROR` 500.

**Pages** look like this: `{ "content": [...], "page": 0, "size": 20, "totalElements": 42, "totalPages": 3 }`.

Access column: **Public**, **Signed in** (any role), **C** = candidate, **E** = employer, **A** = admin.

## Auth

| Method | Path | Access | Request → Response |
| --- | --- | --- | --- |
| GET | `/auth/csrf` | Public | → `{headerName}` and sets the `XSRF-TOKEN` cookie |
| POST | `/auth/register` | Public | `{email, password, fullName, role: CANDIDATE\|EMPLOYER, companyName?}` → **201** `CurrentUser` + auth cookie |
| POST | `/auth/login` | Public, rate-limited | `{email, password}` → `CurrentUser` + auth cookie |
| POST | `/auth/logout` | Public | → **204**, clears the cookie |
| GET | `/auth/me` | Public | → `CurrentUser`, or **401** when signed out |

```json
// CurrentUser
{ "id": "57db…", "email": "candidate@example.com", "fullName": "Aarav Kulkarni", "role": "CANDIDATE", "companyId": null }
```

## Jobs (public)

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| GET | `/jobs` | Public | Search active jobs. Query: `q`, `location`, `type`*, `level`*, `mode`*, `minSalary`, `postedWithinDays` (1–365), `companyId`, `sort` = `RECENT\|SALARY`, `page`, `size` (≤ 50). *Repeat a parameter for OR, e.g. `type=INTERNSHIP&type=FULL_TIME`. |
| GET | `/jobs/featured` | Public | The 6 newest active jobs |
| GET | `/jobs/{id}` | Public | Detail. Closed jobs are still returned, with `status: CLOSED`. |
| GET | `/jobs/{id}/match` | C | Explainable match score |
| GET | `/skills?q=` | Public | Up to 15 skill-name suggestions |

For a signed-in candidate, job summaries also carry `saved` and the cached `matchScore`.

```json
// GET /api/jobs?q=java&type=FULL_TIME&sort=SALARY&size=1
{ "content": [{
    "id": "cb61…", "title": "Backend Engineer - Payments",
    "company": { "id": "c645…", "name": "Harbor & Pine" },
    "location": "Pune", "workMode": "HYBRID", "employmentType": "FULL_TIME", "experienceLevel": "MID",
    "salary": { "min": 1600000, "max": 2400000, "currency": "INR" },
    "skills": ["Docker", "Java", "Kafka", "PostgreSQL", "Spring Boot"],
    "origin": "EMPLOYER", "postedAt": "2026-10-20T18:00:00Z", "matchScore": null, "saved": false }],
  "page": 0, "size": 1, "totalElements": 5, "totalPages": 5 }

// GET /api/jobs/{id}/match
{ "jobId": "cb61…", "overallScore": 82.0, "skillScore": 60.0, "experienceScore": 100.0,
  "educationScore": 100.0, "locationScore": 100.0, "freshnessScore": 90.0,
  "matchedSkills": ["Java", "Spring Boot", "PostgreSQL"], "missingSkills": ["REST", "JUnit"],
  "concerns": [], "positiveSignals": ["Your experience fits this role's level", "Location matches your profile"] }
```

## Saved jobs (candidate)

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| GET | `/saved-jobs?page=` | C | Page of job summaries, newest saved first |
| PUT | `/saved-jobs/{jobId}` | C | Save. Idempotent. **204** |
| DELETE | `/saved-jobs/{jobId}` | C | Unsave. Idempotent. **204** |

## Applications (candidate)

| Method | Path | Access | Request → Response |
| --- | --- | --- | --- |
| GET | `/applications?status=` | C | Own applications, newest activity first |
| POST | `/applications` | C | `{jobId, coverLetter?}` → **201** `ApplicationDetail` + `Location` header. **409** if already applied, **422** if the job is closed. Re-applying after a withdrawal reopens the same record. |
| GET | `/applications/{id}` | C (owner) | Detail with history. **404** for anyone else's id. |
| PATCH | `/applications/{id}` | C (owner) | `{notes?, nextAction?, status?, interviewAt?}`. `status` and `interviewAt` are only allowed on INGESTED jobs (**422** otherwise). |
| POST | `/applications/{id}/withdraw` | C (owner) | → `ApplicationDetail` with `status: WITHDRAWN` |

```json
// ApplicationDetail (abridged)
{ "id": "91b2…", "status": "INTERVIEW", "appliedAt": "…", "interviewAt": "2026-10-27T05:30:00Z",
  "job": { "id": "cb61…", "title": "Java Backend Engineer", "companyName": "Northwind Labs", "origin": "EMPLOYER", "status": "ACTIVE" },
  "coverLetter": "…", "notes": "private to the candidate", "nextAction": null,
  "allowedNextStatuses": ["WITHDRAWN"], "candidateManagesStatus": false,
  "history": [ { "fromStatus": null, "toStatus": "APPLIED", "changedBy": "YOU", "note": null, "at": "…" },
               { "fromStatus": "APPLIED", "toStatus": "INTERVIEW", "changedBy": "EMPLOYER", "note": "Technical round", "at": "…" } ] }
```

Status machine: `APPLIED → IN_REVIEW → ASSESSMENT → INTERVIEW → OFFER`. Every stage can skip forward. `REJECTED` and `WITHDRAWN` are terminal.

## Profile, dashboard, skill gap (candidate)

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| GET | `/profile` | C | Profile plus `completeness {percent, missing[]}` |
| PUT | `/profile` | C | Full replacement: `{fullName, headline, location, educationLevel, graduationYear, experienceYears, summary, preferredRoles[], skills[], educations[], experiences[]}`. Child entries with an `id` are updated, those without are created, and missing ones are deleted. |
| POST | `/profile/resume` | C | multipart `file` (PDF/DOCX ≤ 5 MB) + `consentToStore` (default `false`). Returns the detected skills, and only stores the file if consent is `true`. |
| GET | `/dashboard` | C | Stage counts, upcoming interviews, recent applications, recent saves, recommendations, completeness |
| GET | `/skill-gap` | C | `{targetRoles, jobsAnalyzed, readySkills, priorityGaps[{skillName, priority, frequencyInTargetRoles, percentOfJobs}]}` |

## Companies (public)

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| GET | `/companies?q=&page=&size=` | Public | Ranked by open roles: `{id, name, headquarters, website, openRoles, internships, entryLevel}` |
| GET | `/companies/{id}` | Public | Profile, open jobs, role breakdowns, top skills, locations |

## Employer workspace

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| GET | `/employer/overview` | E | Open/closed job counts and applications per stage |
| GET / PUT | `/employer/company` | E | Own company profile `{name, website, headquarters, description}` |
| GET | `/employer/jobs?page=` | E | Own listings, each with an applicant count |
| POST | `/employer/jobs` | E | `{title, description (≥ 50 chars), location, workMode, employmentType, experienceLevel, salaryMin?, salaryMax?, salaryCurrency?, skills[]}` → **201** |
| GET / PUT | `/employer/jobs/{id}` | E (owner) | Read or edit. **404** if it isn't your company's job. |
| POST | `/employer/jobs/{id}/close` · `/reopen` | E (owner) | Closing removes it from search and blocks new applications |
| GET | `/employer/jobs/{id}/applications?status=&page=` | E (owner) | Applicants with a profile summary and cover note (never the candidate's private notes) |
| GET | `/employer/applications/{id}` | E (owner) | One applicant with history |
| PATCH | `/employer/applications/{id}/status` | E (owner) | `{status, interviewAt? (future), note?}`. **422** for an invalid transition or `WITHDRAWN`. |

## Admin

| Method | Path | Access | Notes |
| --- | --- | --- | --- |
| GET | `/admin/stats` | A | Candidates, employers, open and total jobs, applications |
| GET | `/admin/users?q=&role=&page=` | A | Search users |
| PATCH | `/admin/users/{id}` | A | `{active: boolean}`. Takes effect on the user's next request. You can't deactivate yourself (422). |
| GET | `/admin/jobs?q=&page=` | A | All listings, any origin |
| POST | `/admin/jobs/{id}/close` · `/reopen` | A | Moderation |
| POST | `/admin/ingestion/run` | A | Runs ATS ingestion for the configured targets → `[{source, companySlug, fetched, created, updated, skippedInvalid, error}]` |

## Operational

| Method | Path | Access |
| --- | --- | --- |
| GET | `/actuator/health`, `/actuator/health/readiness`, `/actuator/health/liveness` | Public |

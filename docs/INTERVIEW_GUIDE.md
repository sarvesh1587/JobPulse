# JobPulse interview guide

Everything here refers to real code in this repository. Open the file mentioned while you practise.

---

## Project pitches

### 30 seconds
> JobPulse is a job portal for early-career developers that I built as a Java full-stack project: Spring Boot 3 and PostgreSQL on the backend, React and TypeScript on the front. Candidates search listings, and for each job they get an explainable match score, not just a number: which skills match, which are missing, and how experience and location fit. They apply and track every application through a pipeline. Employers post jobs and move applicants through stages, and admins moderate. It's all real data, secured with JWT cookies and role-plus-ownership checks, and tested against a real Postgres.

### 2 minutes
> The backend is a layered Spring Boot monolith: controllers handle HTTP and validation, services hold business rules and `@Transactional` boundaries, and Spring Data JPA repositories talk to PostgreSQL. Flyway owns the schema, and Hibernate only validates it.
>
> There are three roles. Candidates have a profile with skills, education and experience. Jobs come from two places: employers post them on JobPulse, or an ingestion pipeline imports them from public Greenhouse, Lever and Ashby boards. That difference matters: for an employer's job you apply in-app and the employer drives the status, while for an imported job you apply on the company site and track the stages yourself.
>
> The application pipeline is a small state machine in an enum. One component applies transitions and records an audit-trail row for each change, so the candidate sees who moved their application and when.
>
> For security, I used a JWT in an HttpOnly cookie, so JavaScript can't read the token, with CSRF protection because cookies are sent automatically. Roles are checked at the URL level, and ownership in the services. If you ask for someone else's application you get a 404, not a 403, so ids don't leak.
>
> For performance, every relationship is lazy and each query fetch-joins what it needs. A page of jobs costs a fixed number of queries thanks to batched lookups. I indexed for the real access patterns, like `(status, posted_at)` for search.
>
> I tested with JUnit and Mockito for the rules, and integration tests that boot the whole app against an embedded PostgreSQL. They cover auth, CSRF, search SQL, the full apply flow, and the authorization edge cases.

### Detailed walkthrough (about 10 minutes, follow a request)
1. **Search:** `GET /api/jobs?q=java&type=FULL_TIME`. `JobController` binds the query parameters into the `JobSearchCriteria` record (validated: `size ≤ 50`). `JobService.search` builds `JobSpecifications.searchActive`, a Criteria API predicate list (status ACTIVE, escaped `LIKE`, `IN` lists, salary coalesce) with NULLS-LAST ordering. The page query fetch-joins `company`, while the count query joins it plainly. `JobSummaryAssembler` adds skills, the saved flag and match scores in 3 batched queries. The response is a `PageResponse<JobSummaryResponse>`.
2. **Sign-in:** `AuthController.login` → `AuthService.authenticate` → `AuthenticationManager` → `AppUserDetailsService` loads the user, and BCrypt checks the password. `JwtService.issueToken` signs `sub=userId`, and `AuthCookieService` writes the cookie (HttpOnly, SameSite=Lax, Secure in prod). On later requests, `JwtAuthFilter` verifies the token, reloads the user (so deactivation is immediate) and sets the `SecurityContext`.
3. **Apply:** `POST /api/applications` (CSRF header required). `ApplicationService.apply` loads the job with its company, rejects closed jobs (422), and either re-opens a withdrawn record or creates a new one plus its first history row, all in one transaction. The `(user_id, job_id)` unique constraint backs up the duplicate check.
4. **Employer review:** `PATCH /api/employer/applications/{id}/status`. `EmployerService.ownedApplication` filters by company ownership (404 otherwise), then `ApplicationWorkflow.transition` validates the move against `ApplicationStatus.allowedNext()`, records the change and sets or clears `interviewAt`.
5. **Candidate dashboard:** `DashboardService` uses grouped counts, an upcoming-interviews query on a partial index, and recommendations ranked by skill overlap in a single grouped JPQL query.
6. **Errors:** anything thrown becomes `ApiError` through `GlobalExceptionHandler`. Security errors raised in filters go through `JsonSecurityErrorHandler`, which uses the same format.
7. **Frontend:** `lib/http.ts` is the only `fetch` caller. It adds the CSRF header, maps errors to `ApiError` and retries once on a stale token. TanStack Query caches server state, and mutations invalidate exactly the affected queries. `RequireRole` handles UX routing only, since the backend enforces everything.

---

## Interview questions and answers

**1. Why a monolith and not microservices?**
One team, one database, features that share transactions (an application and its history row; a profile change and invalidating its match scores). Microservices would add network failure modes, distributed transactions and deployment work for no benefit. The packages are separated by layer and feature, so a module could be split out later if a real scaling need appeared.

**2. Walk me through the layers. Why do controllers never return entities?**
Controller → service → repository. Returning entities would expose internal fields (like `passwordHash`), couple the API contract to the database schema, and trigger lazy loading during JSON serialization (`open-in-view` is off on purpose). DTO records are the API contract. Look at `JobMapper` and the `dto` packages.

**3. How does authentication work, and why cookies instead of `localStorage`?**
The login checks BCrypt and issues an HMAC-SHA256 JWT holding only the user id, in an HttpOnly, SameSite=Lax cookie (`AuthCookieService`). A token in `localStorage` can be read by any XSS payload; an HttpOnly cookie can't. The trade-off is CSRF exposure, which I handle with a double-submit token.

**4. Explain the CSRF protection.**
Spring's `CookieCsrfTokenRepository` sets a readable `XSRF-TOKEN` cookie, and every write must send it back in `X-XSRF-TOKEN`. A malicious site can make the browser send our cookies but can't read them, so it can't build the header. SameSite=Lax adds a second layer. There's a test: `AuthIntegrationTest.writeRequestWithoutCsrfTokenIsRejected`.

**5. Why is the JWT stateless, yet you load the user on every request?**
The token proves identity. The role and active flag come from the database, so deactivating a user or changing their role takes effect on the next request, without a token blacklist. The cost is one primary-key lookup per request. `AdminIntegrationTest.deactivatedUserLosesAccessImmediately` proves it.

**6. How do you stop a candidate reading another candidate's application?**
The URL rule only says "candidates may call `/api/applications/**`". The service then filters by owner (`findOwned`), and a foreign id gives `ResourceNotFoundException`, a 404. I return 404 rather than 403 so the API doesn't confirm the id exists. The same pattern guards employer jobs and applicants.

**7. What's the difference between `@PreAuthorize` and your URL rules, and which did you use?**
Both check authorities. I used URL rules in `SecurityConfig` because the roles map cleanly to path prefixes (`/api/employer/**`), so all the rules are in one place. `@EnableMethodSecurity` is on, so method-level rules could be added where a path rule isn't precise enough. Ownership still has to be checked in services either way.

**8. Explain LAZY vs EAGER and why you changed it.**
JPA defaults `@ManyToOne` to EAGER, so loading 20 jobs also loads 20 companies, sources and users, whether needed or not, often as extra selects. I made every association LAZY and fetch what each query needs: `@EntityGraph(attributePaths = "company")`, `join fetch`, or `root.fetch` in Specifications.

**9. What is the N+1 problem? Show me where you avoided it.**
Loading N parent rows, then running one query per row for a child. A job list needs skills, the saved flag and match score per job. `JobSummaryAssembler` fetches each of those for all job ids with one `IN (...)` query, so a page is 1 + 3 queries instead of 1 + 3N. The same applies to employer applicant cards.

**10. Why a Specification for search instead of one JPQL query?**
Search has eight optional filters. With one static query you either write `(:x is null or ...)` everywhere, which has type problems on Postgres and stops the planner using indexes, or you concatenate strings, which risks SQL injection. Specifications build only the predicates that apply, with bound parameters. See `JobSpecifications`.

**11. How do you prevent SQL injection?**
Every value is a bound parameter: derived queries, JPQL `:params`, the Criteria API. No string-built SQL. I also escape `%` and `_` in search input so users can't create wildcard patterns (`likeWildcardsInTheQueryAreTreatedLiterally`).

**12. How does `@Transactional` work, and what's the self-invocation trap?**
Spring wraps the bean in a proxy that opens and commits the transaction around the method. If a method calls another method on `this`, the call skips the proxy and the annotation does nothing. That's why `JobUpsertService` is a separate bean from `IngestionService`: each job's upsert gets its own transaction, so one bad record doesn't roll back the board.

**13. Why `readOnly = true` on queries?**
Hibernate skips dirty checking and flushing for those transactions, and the JDBC driver can optimize. It also documents intent.

**14. How do you guarantee no duplicate applications?**
Two layers. The service checks `findByUserIdAndJobId` and throws a friendly 409. The `UNIQUE (user_id, job_id)` constraint catches the race when two requests both pass the check, and the handler maps `DataIntegrityViolationException` to 409 too. The application-level check alone isn't safe under concurrency.

**15. Describe the application state machine.**
`ApplicationStatus.allowedNext()` defines the legal moves: forward-only, with REJECTED and WITHDRAWN terminal. `ApplicationWorkflow` is the only code that applies a move. It validates, appends an `ApplicationStatusChange` audit row, and keeps `interviewAt` consistent. Services decide *who* may move: employers can't withdraw; candidates manage stages only for external applications.

**16. How does validation work, end to end?**
Bean Validation annotations on request records (`@NotBlank`, `@Size`, `@Future`, `@Pattern`), plus cross-field rules as `@AssertTrue` methods (salary min ≤ max). `@Valid` triggers them, and failures become a 400 with `fieldErrors` keyed by field. The frontend runs the same checks first for instant feedback, and also maps server field errors back onto the inputs. The database has CHECK constraints as a last line of defence.

**17. How is exception handling centralized?**
`@RestControllerAdvice GlobalExceptionHandler` maps domain exceptions to statuses: 404, 409, 422 for business rules, 400 for validation. Unknown exceptions are logged with their stack trace and answered with a generic 500, so nothing internal leaks. Errors raised in security filters bypass controller advice, so `JsonSecurityErrorHandler` writes the same JSON for 401/403.

**18. Why Flyway, and why `ddl-auto: validate`?**
Schema changes are versioned SQL, reviewed like code and applied the same way everywhere. `validate` makes Hibernate fail at startup if the entities and the schema drift apart. That caught the original project's missing audit columns, which V3 fixes. V4 shows a data migration: old bookmark rows moved before a constraint was added.

**19. Which indexes did you add, and why?**
`(status, posted_at DESC)` matches the main search filter and sort. `(job_id, status)` serves employer applicant tabs. A partial index on `interview_at IS NOT NULL` serves upcoming interviews. I also indexed the foreign keys used in joins, since Postgres doesn't do that automatically. I chose them from the actual queries, not by indexing every column.

**20. How does pagination work, and why not return Spring's `Page`?**
`PageRequest` produces LIMIT/OFFSET plus a count query. I return my own `PageResponse` record because Spring's `Page` JSON isn't a stable contract. Sorting ends with `id` as a tie-breaker so rows don't repeat or vanish between pages. For very deep paging, keyset (seek) pagination would be the next step.

**21. How are passwords stored?**
`BCryptPasswordEncoder`: salted, slow on purpose, with a work factor. Plain text is never stored or logged. Passwords are capped at 72 characters because BCrypt ignores the rest.

**22. What happens when the profile changes but match scores are cached?**
`ProfileService.update` deletes that user's cached `match_scores` in the same transaction, so the next view recomputes from the new profile. Scores also expire after 6 hours, because freshness is one of the scored factors.

**23. How did you test? Why embedded Postgres instead of H2?**
Unit tests for the rules (state machine, workflow, completeness), Mockito for service decisions, and `@SpringBootTest` + MockMvc integration tests with real HTTP semantics, cookies and CSRF. H2 doesn't behave like Postgres for arrays, partial indexes and some SQL, so passing on H2 proves little. Embedded Postgres runs the real Flyway migrations without needing Docker.

**24. How would you scale this?**
The API is stateless, so it scales horizontally behind a load balancer. Then: move the rate limiter to Redis or the gateway, resumes to object storage, and add a read replica for search. Full-text search would come from Postgres `tsvector` or `pg_trgm` before adding Elasticsearch, and only if the data showed a need.

**25. Why are some IDs UUIDs?**
They can't be guessed or enumerated, so you can't walk `/applications/1, /2, …`. They can also be generated without a database round-trip. The cost is larger indexes than `bigint`. Ownership checks are still the real protection; the UUIDs are only defence in depth.

**26. What did you do about resume privacy and uploads?**
Nothing is stored unless `consentToStore=true`. Without consent, the file is parsed in memory and only the results are returned. The upload is limited to PDF/DOCX at 5 MB, the filename is sanitized, and the resolved path must stay inside the user's own directory, which prevents path traversal.

---

## Limitations and trade-offs

| Limitation | Why it's acceptable now | Next step |
| --- | --- | --- |
| Skill tagging and resume parsing are keyword matching | Transparent and testable, with no external dependency | Synonym table, then NLP extraction |
| No per-session revocation (stateless JWT) | Deactivation is still immediate. The token TTL is 12 hours | A `token_version` column on users, or short tokens plus refresh tokens |
| In-memory login rate limiter | A single instance today | Redis / API gateway |
| `LIKE` keyword search | Fast enough at thousands of rows | `pg_trgm` GIN index or `tsvector` full text |
| Education score only checks that the profile is filled in | Jobs have no structured education requirement | Extract requirements from descriptions |
| No email notifications | Status changes are visible in the app | Outbox table plus a mail sender |
| Tier-2 ingestion dedup (same company + title + location) can merge two genuinely different postings | It stops boards that rotate ids from creating duplicates | Add a description-similarity check before merging |
| Docker images not run in the verification environment (no Docker there) | The backend, frontend and database were verified running natively | Run `docker compose up --build` in CI |

## Future improvements worth doing (no hype)
1. CI pipeline (GitHub Actions): `./mvnw test`, `npm test`, `npm run build`, and a Docker build.
2. Email on status changes, using the outbox pattern for reliable delivery.
3. Keyset pagination for deep result pages.
4. A Content-Security-Policy header, and a token-version column for logout-everywhere.
5. Saved searches with alerts.

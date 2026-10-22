# JobPulse backend

Spring Boot 3.5 / Java 21 REST API. Setup, environment variables and test commands are in the [root README](../README.md). Design decisions are in [docs/ARCHITECTURE.md](../docs/ARCHITECTURE.md), and every endpoint is listed in [docs/API.md](../docs/API.md).

```
com.jobpulse
├── controller   REST endpoints: HTTP in and out, validation, status codes
├── service      business rules and transaction boundaries
│   └── resume   PDF/DOCX text extraction and keyword parsing
├── repository   Spring Data JPA interfaces, JPQL, Specifications, projections
├── entity       JPA entities and enums (ApplicationStatus holds the state machine)
├── dto          request/response records, one file per feature
├── mapper       explicit entity → DTO mapping
├── security     JWT cookie auth, CSRF handler, rate limiter, JSON 401/403
├── exception    ApiError and @RestControllerAdvice
├── config       Spring configuration, admin bootstrap, dev data seeder
├── matching     the explainable match-score engine
└── ingestion    Greenhouse / Lever / Ashby adapters and the upsert pipeline
```

```bash
./mvnw spring-boot:run   # run (dev profile, reads backend/.env)
./mvnw test              # unit + integration tests (embedded PostgreSQL)
./mvnw package           # build target/jobpulse-backend-1.0.0.jar
```

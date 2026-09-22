-- V1__init_schema.sql
-- Core JobPulse schema. UUID primary keys, created_at/updated_at on every table.

CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================
-- USERS & CANDIDATE PROFILE
-- ============================================================

CREATE TABLE users (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    email           VARCHAR(255) NOT NULL UNIQUE,
    password_hash   VARCHAR(255) NOT NULL,
    full_name       VARCHAR(255) NOT NULL,
    role            VARCHAR(30)  NOT NULL DEFAULT 'CANDIDATE',
    is_active       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE candidate_profiles (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL UNIQUE REFERENCES users(id) ON DELETE CASCADE,
    education_level     VARCHAR(50),
    graduation_year     INT,
    location            VARCHAR(120),
    preferred_roles     TEXT[],
    experience_years    NUMERIC(4,1) NOT NULL DEFAULT 0,
    summary             TEXT,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE resumes (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    file_name           VARCHAR(255) NOT NULL,
    storage_path        VARCHAR(500) NOT NULL,
    extracted_text      TEXT,
    parsed_at           TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now()
);

-- ============================================================
-- SKILLS
-- ============================================================

CREATE TABLE skills (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(120) NOT NULL UNIQUE,
    category    VARCHAR(80),
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE candidate_skills (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    skill_id             UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    proficiency          VARCHAR(20) DEFAULT 'INTERMEDIATE',
    source               VARCHAR(20) NOT NULL DEFAULT 'MANUAL', -- MANUAL | RESUME
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (candidate_profile_id, skill_id)
);

-- ============================================================
-- COMPANIES & JOBS
-- ============================================================

CREATE TABLE companies (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(255) NOT NULL,
    careers_url     VARCHAR(500),
    normalized_name VARCHAR(255) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (normalized_name)
);

CREATE TABLE job_sources (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name            VARCHAR(50) NOT NULL UNIQUE, -- GREENHOUSE | LEVER | ASHBY
    is_active       BOOLEAN NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE TABLE jobs (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    external_id         VARCHAR(255) NOT NULL,
    job_source_id       UUID NOT NULL REFERENCES job_sources(id),
    company_id          UUID NOT NULL REFERENCES companies(id),
    title               VARCHAR(300) NOT NULL,
    normalized_title    VARCHAR(300) NOT NULL,
    description         TEXT,
    location            VARCHAR(200),
    normalized_location VARCHAR(200),
    employment_type     VARCHAR(30),   -- INTERNSHIP | FULL_TIME | CONTRACT
    experience_level    VARCHAR(30),   -- INTERN | ENTRY | MID | SENIOR
    salary_min          NUMERIC(12,2),
    salary_max          NUMERIC(12,2),
    application_url     VARCHAR(500) NOT NULL,
    source_url          VARCHAR(500) NOT NULL,
    status              VARCHAR(20) NOT NULL DEFAULT 'ACTIVE', -- ACTIVE | STALE | EXPIRED | UNKNOWN
    posted_at           TIMESTAMPTZ,
    last_verified_at    TIMESTAMPTZ,
    created_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at          TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (job_source_id, external_id)
);

CREATE INDEX idx_jobs_company ON jobs(company_id);
CREATE INDEX idx_jobs_status ON jobs(status);
CREATE INDEX idx_jobs_normalized_title ON jobs(normalized_title);
CREATE INDEX idx_jobs_normalized_location ON jobs(normalized_location);
CREATE INDEX idx_jobs_posted_at ON jobs(posted_at DESC);

CREATE TABLE job_skills (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id      UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    skill_id    UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    is_required BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (job_id, skill_id)
);

CREATE TABLE job_verifications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    job_id          UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    checked_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    result_status   VARCHAR(20) NOT NULL, -- ACTIVE | STALE | EXPIRED | UNKNOWN
    http_status     INT,
    notes           TEXT
);

CREATE INDEX idx_job_verifications_job ON job_verifications(job_id, checked_at DESC);

-- ============================================================
-- CANDIDATE ACTIVITY: SAVED JOBS, APPLICATIONS, MATCH SCORES
-- ============================================================

CREATE TABLE saved_jobs (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id     UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id      UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, job_id)
);

CREATE TABLE applications (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id         UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id          UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    status          VARCHAR(20) NOT NULL DEFAULT 'SAVED',
    -- SAVED | PLANNING | APPLIED | ASSESSMENT | INTERVIEW | OFFER | REJECTED
    applied_at      TIMESTAMPTZ,
    next_action     VARCHAR(500),
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, job_id)
);

CREATE INDEX idx_applications_user_status ON applications(user_id, status);

CREATE TABLE match_scores (
    id                  UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id             UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    job_id              UUID NOT NULL REFERENCES jobs(id) ON DELETE CASCADE,
    overall_score       NUMERIC(5,2) NOT NULL,
    skill_score         NUMERIC(5,2) NOT NULL,
    experience_score    NUMERIC(5,2) NOT NULL,
    education_score     NUMERIC(5,2) NOT NULL,
    location_score      NUMERIC(5,2) NOT NULL,
    freshness_score     NUMERIC(5,2) NOT NULL,
    matched_skills      TEXT[],
    missing_skills      TEXT[],
    concerns            TEXT[],
    positive_signals    TEXT[],
    computed_at         TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, job_id)
);

CREATE INDEX idx_match_scores_user ON match_scores(user_id, overall_score DESC);

CREATE TABLE skill_gaps (
    id                      UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    user_id                 UUID NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    skill_id                UUID NOT NULL REFERENCES skills(id) ON DELETE CASCADE,
    priority                INT NOT NULL,
    frequency_in_target_roles INT NOT NULL DEFAULT 0,
    computed_at             TIMESTAMPTZ NOT NULL DEFAULT now(),
    UNIQUE (user_id, skill_id)
);

CREATE INDEX idx_skill_gaps_user ON skill_gaps(user_id, priority);

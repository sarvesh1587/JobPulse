-- V4__employers_profiles_and_application_pipeline.sql
-- 1. Roles: CANDIDATE | EMPLOYER | ADMIN, enforced in the database too.
-- 2. Employer-owned companies and employer-posted jobs alongside ingested ones.
-- 3. Bookmarks and applications are separated: SAVED/PLANNING rows move
--    into saved_jobs; applications only hold real application stages.
-- 4. Structured education and work-experience entries on the profile.
-- 5. Indexes for the queries the job search and dashboards actually run.

-- ---------------------------------------------------------------- users
ALTER TABLE users
    ADD CONSTRAINT chk_users_role CHECK (role IN ('CANDIDATE', 'EMPLOYER', 'ADMIN'));

-- ------------------------------------------------------------ companies
ALTER TABLE companies
    ADD COLUMN owner_id     UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN website      VARCHAR(500),
    ADD COLUMN headquarters VARCHAR(200),
    ADD COLUMN description  TEXT;

-- One company per employer account keeps ownership checks simple.
CREATE UNIQUE INDEX uq_companies_owner ON companies(owner_id) WHERE owner_id IS NOT NULL;

-- ----------------------------------------------------------------- jobs
ALTER TABLE jobs
    ADD COLUMN origin          VARCHAR(20) NOT NULL DEFAULT 'INGESTED',
    ADD COLUMN posted_by_id    UUID REFERENCES users(id) ON DELETE SET NULL,
    ADD COLUMN work_mode       VARCHAR(20),
    ADD COLUMN salary_currency VARCHAR(3);

-- Employer-posted jobs have no ATS source, external id, or external URLs.
ALTER TABLE jobs ALTER COLUMN job_source_id   DROP NOT NULL;
ALTER TABLE jobs ALTER COLUMN external_id     DROP NOT NULL;
ALTER TABLE jobs ALTER COLUMN application_url DROP NOT NULL;
ALTER TABLE jobs ALTER COLUMN source_url      DROP NOT NULL;

ALTER TABLE jobs
    ADD CONSTRAINT chk_jobs_origin    CHECK (origin IN ('INGESTED', 'EMPLOYER')),
    ADD CONSTRAINT chk_jobs_status    CHECK (status IN ('ACTIVE', 'STALE', 'EXPIRED', 'UNKNOWN', 'CLOSED')),
    ADD CONSTRAINT chk_jobs_work_mode CHECK (work_mode IS NULL OR work_mode IN ('REMOTE', 'HYBRID', 'ONSITE')),
    ADD CONSTRAINT chk_jobs_salary    CHECK (salary_min IS NULL OR salary_max IS NULL OR salary_min <= salary_max),
    ADD CONSTRAINT chk_jobs_ingested_has_source
        CHECK (origin <> 'INGESTED' OR (job_source_id IS NOT NULL AND external_id IS NOT NULL AND application_url IS NOT NULL));

-- Search always filters status = 'ACTIVE' and sorts by posted_at, so a
-- composite index serves both; the old single-column status index is redundant.
DROP INDEX IF EXISTS idx_jobs_status;
CREATE INDEX idx_jobs_status_posted ON jobs(status, posted_at DESC);
CREATE INDEX idx_jobs_posted_by     ON jobs(posted_by_id) WHERE posted_by_id IS NOT NULL;

-- ------------------------------------------------- saved jobs (bookmarks)
INSERT INTO saved_jobs (user_id, job_id, created_at, updated_at)
SELECT user_id, job_id, created_at, now()
FROM applications
WHERE status IN ('SAVED', 'PLANNING')
ON CONFLICT (user_id, job_id) DO NOTHING;

CREATE INDEX idx_saved_jobs_user_created ON saved_jobs(user_id, created_at DESC);

-- --------------------------------------------------------- applications
DELETE FROM applications WHERE status IN ('SAVED', 'PLANNING');

UPDATE applications SET applied_at = created_at WHERE applied_at IS NULL;

ALTER TABLE applications
    ALTER COLUMN status SET DEFAULT 'APPLIED',
    ALTER COLUMN applied_at SET NOT NULL,
    ADD COLUMN cover_letter TEXT,
    ADD COLUMN interview_at TIMESTAMPTZ,
    ADD CONSTRAINT chk_applications_status CHECK (status IN
        ('APPLIED', 'IN_REVIEW', 'ASSESSMENT', 'INTERVIEW', 'OFFER', 'REJECTED', 'WITHDRAWN'));

CREATE INDEX idx_applications_job ON applications(job_id, status);
CREATE INDEX idx_applications_interview ON applications(user_id, interview_at) WHERE interview_at IS NOT NULL;

-- Audit trail: every status change, who made it, and when.
CREATE TABLE application_status_changes (
    id             UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    application_id UUID NOT NULL REFERENCES applications(id) ON DELETE CASCADE,
    from_status    VARCHAR(20),
    to_status      VARCHAR(20) NOT NULL,
    changed_by_id  UUID REFERENCES users(id) ON DELETE SET NULL,
    note           VARCHAR(500),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_status_changes_application ON application_status_changes(application_id, created_at);

-- Existing applications get a starting history entry so the timeline isn't empty.
INSERT INTO application_status_changes (application_id, from_status, to_status, changed_by_id, created_at)
SELECT id, NULL, status, user_id, applied_at FROM applications;

-- ------------------------------------------------------------- profiles
ALTER TABLE candidate_profiles ADD COLUMN headline VARCHAR(160);

CREATE TABLE educations (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    institution          VARCHAR(200) NOT NULL,
    degree               VARCHAR(150) NOT NULL,
    field_of_study       VARCHAR(150),
    start_year           INT,
    end_year             INT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_education_years CHECK (start_year IS NULL OR end_year IS NULL OR start_year <= end_year)
);

CREATE INDEX idx_educations_profile ON educations(candidate_profile_id);

CREATE TABLE work_experiences (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    candidate_profile_id UUID NOT NULL REFERENCES candidate_profiles(id) ON DELETE CASCADE,
    company_name         VARCHAR(200) NOT NULL,
    title                VARCHAR(150) NOT NULL,
    start_date           DATE NOT NULL,
    end_date             DATE,
    description          TEXT,
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT chk_experience_dates CHECK (end_date IS NULL OR start_date <= end_date)
);

CREATE INDEX idx_work_experiences_profile ON work_experiences(candidate_profile_id);

-- Foreign-key columns Postgres doesn't index automatically, used by joins.
CREATE INDEX idx_candidate_skills_profile ON candidate_skills(candidate_profile_id);
CREATE INDEX idx_job_skills_skill ON job_skills(skill_id);
CREATE INDEX idx_resumes_profile ON resumes(candidate_profile_id, created_at DESC);

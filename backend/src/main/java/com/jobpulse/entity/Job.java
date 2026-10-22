package com.jobpulse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "jobs")
public class Job extends BaseEntity {

    /** INGESTED listings are applied to on the company's site; EMPLOYER listings in-app. */
    public enum Origin { INGESTED, EMPLOYER }

    public enum EmploymentType { INTERNSHIP, FULL_TIME, CONTRACT }

    public enum ExperienceLevel { INTERN, ENTRY, MID, SENIOR }

    public enum WorkMode { REMOTE, HYBRID, ONSITE }

    /** Only ACTIVE jobs are searchable. CLOSED is set by an employer or an admin. */
    public enum JobStatus { ACTIVE, STALE, EXPIRED, UNKNOWN, CLOSED }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Origin origin = Origin.INGESTED;

    @Column(name = "external_id")
    private String externalId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_source_id")
    private JobSourceEntity jobSource;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "posted_by_id")
    private User postedBy;

    @Column(nullable = false)
    private String title;

    @Column(name = "normalized_title", nullable = false)
    private String normalizedTitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    private String location;

    @Column(name = "normalized_location")
    private String normalizedLocation;

    @Enumerated(EnumType.STRING)
    @Column(name = "work_mode")
    private WorkMode workMode;

    @Enumerated(EnumType.STRING)
    @Column(name = "employment_type")
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level")
    private ExperienceLevel experienceLevel;

    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;

    @Column(name = "salary_currency")
    private String salaryCurrency;

    @Column(name = "application_url")
    private String applicationUrl;

    @Column(name = "source_url")
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    public boolean acceptsInAppApplications() {
        return origin == Origin.EMPLOYER;
    }

    public boolean isOpen() {
        return status == JobStatus.ACTIVE;
    }
}

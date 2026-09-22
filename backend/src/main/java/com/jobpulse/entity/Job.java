package com.jobpulse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "jobs")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job extends BaseEntity {

    @Column(name = "external_id", nullable = false)
    private String externalId;

    @ManyToOne
    @JoinColumn(name = "job_source_id", nullable = false)
    private JobSourceEntity jobSource;

    @ManyToOne
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

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
    @Column(name = "employment_type")
    private EmploymentType employmentType;

    @Enumerated(EnumType.STRING)
    @Column(name = "experience_level")
    private ExperienceLevel experienceLevel;

    @Column(name = "salary_min")
    private BigDecimal salaryMin;

    @Column(name = "salary_max")
    private BigDecimal salaryMax;

    @Column(name = "application_url", nullable = false)
    private String applicationUrl;

    @Column(name = "source_url", nullable = false)
    private String sourceUrl;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private JobStatus status = JobStatus.ACTIVE;

    @Column(name = "posted_at")
    private Instant postedAt;

    @Column(name = "last_verified_at")
    private Instant lastVerifiedAt;

    public enum EmploymentType { INTERNSHIP, FULL_TIME, CONTRACT }

    public enum ExperienceLevel { INTERN, ENTRY, MID, SENIOR }

    public enum JobStatus { ACTIVE, STALE, EXPIRED, UNKNOWN }
}

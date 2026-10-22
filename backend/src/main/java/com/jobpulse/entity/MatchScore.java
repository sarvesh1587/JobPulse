package com.jobpulse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** Cached result of MatchingService for one (candidate, job) pair. */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "match_scores", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_id"}))
public class MatchScore extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "overall_score", nullable = false)
    private BigDecimal overallScore;

    @Column(name = "skill_score", nullable = false)
    private BigDecimal skillScore;

    @Column(name = "experience_score", nullable = false)
    private BigDecimal experienceScore;

    @Column(name = "education_score", nullable = false)
    private BigDecimal educationScore;

    @Column(name = "location_score", nullable = false)
    private BigDecimal locationScore;

    @Column(name = "freshness_score", nullable = false)
    private BigDecimal freshnessScore;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "matched_skills", columnDefinition = "text[]")
    private List<String> matchedSkills;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "missing_skills", columnDefinition = "text[]")
    private List<String> missingSkills;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(columnDefinition = "text[]")
    private List<String> concerns;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "positive_signals", columnDefinition = "text[]")
    private List<String> positiveSignals;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}

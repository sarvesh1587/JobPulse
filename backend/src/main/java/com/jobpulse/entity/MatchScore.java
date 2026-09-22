package com.jobpulse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "match_scores", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_id"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MatchScore extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
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

    @Column(name = "matched_skills")
    private List<String> matchedSkills;

    @Column(name = "missing_skills")
    private List<String> missingSkills;

    private List<String> concerns;

    @Column(name = "positive_signals")
    private List<String> positiveSignals;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}

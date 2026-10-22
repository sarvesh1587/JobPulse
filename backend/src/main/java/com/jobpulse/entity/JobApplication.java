package com.jobpulse.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Maps to the "applications" table. There is one row per (candidate, job).
 * The unique constraint is the last line of defence against duplicate
 * applications if two requests race past the service-level check.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "applications", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "job_id"}))
public class JobApplication extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ApplicationStatus status = ApplicationStatus.APPLIED;

    @Column(name = "applied_at", nullable = false)
    private Instant appliedAt;

    /** Shared with the employer for in-app applications. */
    @Column(name = "cover_letter", columnDefinition = "TEXT")
    private String coverLetter;

    /** Private to the candidate. Never included in employer responses. */
    @Column(columnDefinition = "TEXT")
    private String notes;

    @Column(name = "next_action")
    private String nextAction;

    @Column(name = "interview_at")
    private Instant interviewAt;

    @OneToMany(mappedBy = "application", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<ApplicationStatusChange> history = new ArrayList<>();

    public JobApplication(User user, Job job, Instant appliedAt) {
        this.user = user;
        this.job = job;
        this.appliedAt = appliedAt;
    }

    public boolean belongsTo(UUID userId) {
        return user.getId().equals(userId);
    }

    /** Moves to a new status and records who did it. Callers validate the transition first. */
    public void changeStatus(ApplicationStatus next, User changedBy, String note) {
        history.add(new ApplicationStatusChange(this, status, next, changedBy, note));
        this.status = next;
    }

    public void recordInitialStatus(User changedBy) {
        history.add(new ApplicationStatusChange(this, null, status, changedBy, null));
    }
}

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

import java.time.Instant;

/** One "is this listing still live?" check made during ingestion. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "job_verifications")
public class JobVerification extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @Column(name = "checked_at", nullable = false)
    private Instant checkedAt;

    @Enumerated(EnumType.STRING)
    @Column(name = "result_status", nullable = false)
    private Job.JobStatus resultStatus;

    @Column(name = "http_status")
    private Integer httpStatus;

    private String notes;

    public JobVerification(Job job, Instant checkedAt, Job.JobStatus resultStatus, Integer httpStatus, String notes) {
        this.job = job;
        this.checkedAt = checkedAt;
        this.resultStatus = resultStatus;
        this.httpStatus = httpStatus;
        this.notes = notes;
    }
}

package com.jobpulse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** A supported ingestion source: GREENHOUSE, LEVER or ASHBY. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "job_sources")
public class JobSourceEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "is_active", nullable = false)
    private boolean active = true;

    public JobSourceEntity(String name) {
        this.name = name;
    }
}

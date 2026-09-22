package com.jobpulse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Represents a supported ingestion source (Greenhouse, Lever, Ashby, ...).
 * Kept as a table (not a Java enum) so new sources can be added without a
 * code deploy — the JobSourceAdapter implementations reference this by name.
 */
@Getter
@Setter
@Entity
@Table(name = "job_sources")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class JobSourceEntity extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private boolean active = true;
}

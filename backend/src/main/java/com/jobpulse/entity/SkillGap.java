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

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "skill_gaps", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skill_id"}))
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SkillGap extends BaseEntity {

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private int priority;

    @Column(name = "frequency_in_target_roles", nullable = false)
    @Builder.Default
    private int frequencyInTargetRoles = 0;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;
}

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

import java.time.Instant;

@Getter
@NoArgsConstructor
@Entity
@Table(name = "skill_gaps", uniqueConstraints = @UniqueConstraint(columnNames = {"user_id", "skill_id"}))
public class SkillGap extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private int priority;

    @Column(name = "frequency_in_target_roles", nullable = false)
    private int frequencyInTargetRoles;

    @Column(name = "computed_at", nullable = false)
    private Instant computedAt;

    public SkillGap(User user, Skill skill, int priority, int frequencyInTargetRoles, Instant computedAt) {
        this.user = user;
        this.skill = skill;
        this.priority = priority;
        this.frequencyInTargetRoles = frequencyInTargetRoles;
        this.computedAt = computedAt;
    }
}

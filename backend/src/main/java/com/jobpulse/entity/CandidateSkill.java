package com.jobpulse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;

/** Join entity between a profile and a skill, recording where the skill came from. */
@Getter
@NoArgsConstructor
@Entity
@Table(name = "candidate_skills")
public class CandidateSkill extends BaseEntity {

    public static final String SOURCE_MANUAL = "MANUAL";
    public static final String SOURCE_RESUME = "RESUME";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "candidate_profile_id", nullable = false)
    private CandidateProfile candidateProfile;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(nullable = false)
    private String proficiency = "INTERMEDIATE";

    @Column(nullable = false)
    private String source = SOURCE_MANUAL;

    public CandidateSkill(CandidateProfile candidateProfile, Skill skill, String source) {
        this.candidateProfile = candidateProfile;
        this.skill = skill;
        this.source = source;
    }
}

package com.jobpulse.service;

import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.CandidateSkill;
import com.jobpulse.entity.Role;
import com.jobpulse.entity.Skill;
import com.jobpulse.entity.User;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ProfileCompletenessTest {

    private CandidateProfile emptyProfile() {
        return new CandidateProfile(new User("c@x.com", "hash", "Candidate", Role.CANDIDATE));
    }

    @Test
    void emptyProfileScoresZeroAndListsEverything() {
        var result = ProfileService.completeness(emptyProfile(), false);
        assertThat(result.percent()).isZero();
        assertThat(result.missing()).hasSize(8);
    }

    @Test
    void twoSkillsAreNotEnoughButThreeAre() {
        CandidateProfile profile = emptyProfile();
        profile.getSkills().add(new CandidateSkill(profile, new Skill("Java"), CandidateSkill.SOURCE_MANUAL));
        profile.getSkills().add(new CandidateSkill(profile, new Skill("SQL"), CandidateSkill.SOURCE_MANUAL));
        assertThat(ProfileService.completeness(profile, false).missing()).contains("Add at least 3 skills");

        profile.getSkills().add(new CandidateSkill(profile, new Skill("Git"), CandidateSkill.SOURCE_MANUAL));
        assertThat(ProfileService.completeness(profile, false).percent()).isEqualTo(25);
    }

    @Test
    void weightsAddUpToOneHundred() {
        CandidateProfile p = emptyProfile();
        p.setHeadline("h");
        p.setLocation("l");
        p.setSummary("s");
        p.setEducationLevel("B.E.");
        p.setPreferredRoles(List.of("Backend"));
        p.getExperiences().add(new com.jobpulse.entity.WorkExperience());
        for (String s : List.of("A", "B", "C")) {
            p.getSkills().add(new CandidateSkill(p, new Skill(s), CandidateSkill.SOURCE_MANUAL));
        }
        var result = ProfileService.completeness(p, true);
        assertThat(result.percent()).isEqualTo(100);
        assertThat(result.missing()).isEmpty();
    }
}

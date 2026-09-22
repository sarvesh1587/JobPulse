package com.jobpulse.service;

import com.jobpulse.dto.ProfileRequest;
import com.jobpulse.dto.ProfileResponse;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.Skill;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final SkillRepository skillRepository;

    @Transactional(readOnly = true)
    public ProfileResponse getByUserId(UUID userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No profile found for this account"));
        return toResponse(profile);
    }

    @Transactional
    public ProfileResponse update(UUID userId, ProfileRequest request) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No profile found for this account"));

        profile.setEducationLevel(request.educationLevel());
        profile.setGraduationYear(request.graduationYear());
        profile.setLocation(request.location());
        profile.setPreferredRoles(request.preferredRoles());
        if (request.experienceYears() != null) {
            profile.setExperienceYears(request.experienceYears());
        }
        profile.setSummary(request.summary());

        candidateProfileRepository.save(profile);

        // Skill list replacement is intentionally simple here — a dedicated
        // endpoint handles resume-driven skill extraction separately.
        if (request.skills() != null) {
            for (String skillName : request.skills()) {
                skillRepository.findByNameIgnoreCase(skillName)
                        .orElseGet(() -> skillRepository.save(
                                Skill.builder().name(skillName).build()));
            }
        }

        return toResponse(profile);
    }

    private ProfileResponse toResponse(CandidateProfile profile) {
        List<String> skills = profile.getId() == null
                ? List.of()
                : List.of(); // populated once CandidateSkillRepository is wired in a follow-up pass

        return new ProfileResponse(
                profile.getUser().getId(),
                profile.getUser().getFullName(),
                profile.getUser().getEmail(),
                profile.getEducationLevel(),
                profile.getGraduationYear(),
                profile.getLocation(),
                profile.getPreferredRoles(),
                profile.getExperienceYears(),
                profile.getSummary(),
                skills
        );
    }
}

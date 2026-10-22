package com.jobpulse.service;

import com.jobpulse.dto.profile.ProfileDtos.Completeness;
import com.jobpulse.dto.profile.ProfileDtos.EducationDto;
import com.jobpulse.dto.profile.ProfileDtos.ExperienceDto;
import com.jobpulse.dto.profile.ProfileDtos.ProfileRequest;
import com.jobpulse.dto.profile.ProfileDtos.ProfileResponse;
import com.jobpulse.dto.profile.ProfileDtos.ResumeInfo;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.CandidateSkill;
import com.jobpulse.entity.Education;
import com.jobpulse.entity.Skill;
import com.jobpulse.entity.WorkExperience;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.MatchScoreRepository;
import com.jobpulse.repository.ResumeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private static final int MIN_SKILLS_FOR_COMPLETE = 3;

    private final CandidateProfileRepository profileRepository;
    private final ResumeRepository resumeRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final SkillCatalogService skillCatalog;

    @Transactional(readOnly = true)
    public ProfileResponse get(UUID userId) {
        return toResponse(load(userId));
    }

    /**
     * Saves the whole profile in one transaction. Child lists are reconciled
     * by id instead of "delete all, insert all", for two reasons:
     *  - Unchanged rows keep their ids and timestamps.
     *  - Hibernate runs INSERTs before DELETEs when it flushes. Clearing the
     *    skills and re-adding the same ones would insert a duplicate
     *    (profile, skill) pair before deleting the old one and break the
     *    unique constraint.
     */
    @Transactional
    public ProfileResponse update(UUID userId, ProfileRequest request) {
        CandidateProfile profile = load(userId);

        profile.getUser().setFullName(request.fullName().trim());
        profile.setHeadline(TextNormalizer.clean(request.headline()));
        profile.setLocation(TextNormalizer.clean(request.location()));
        profile.setEducationLevel(TextNormalizer.clean(request.educationLevel()));
        profile.setGraduationYear(request.graduationYear());
        profile.setExperienceYears(request.experienceYears());
        profile.setSummary(TextNormalizer.clean(request.summary()));
        profile.setPreferredRoles(request.preferredRoles().stream()
                .map(TextNormalizer::clean).filter(Objects::nonNull).distinct().collect(Collectors.toCollection(ArrayList::new)));

        reconcileSkills(profile, skillCatalog.resolve(request.skills()));
        reconcileEducations(profile, request.educations());
        reconcileExperiences(profile, request.experiences());

        // Cached match scores were computed from the old profile.
        matchScoreRepository.deleteByUserId(userId);

        return toResponse(profileRepository.saveAndFlush(profile));
    }

    /** Used by the dashboard as well as the profile page. */
    @Transactional(readOnly = true)
    public Completeness completeness(UUID userId) {
        CandidateProfile profile = load(userId);
        boolean hasResume = resumeRepository.findFirstByCandidateProfileIdOrderByCreatedAtDesc(profile.getId()).isPresent();
        return completeness(profile, hasResume);
    }

    private CandidateProfile load(UUID userId) {
        return profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No candidate profile found for this account."));
    }

    private void reconcileSkills(CandidateProfile profile, List<Skill> desired) {
        Set<UUID> desiredIds = desired.stream().map(Skill::getId).collect(Collectors.toSet());
        profile.getSkills().removeIf(cs -> !desiredIds.contains(cs.getSkill().getId()));
        Set<UUID> present = profile.getSkills().stream().map(cs -> cs.getSkill().getId()).collect(Collectors.toSet());
        desired.stream()
                .filter(skill -> !present.contains(skill.getId()))
                .forEach(skill -> profile.getSkills().add(new CandidateSkill(profile, skill, CandidateSkill.SOURCE_MANUAL)));
    }

    private void reconcileEducations(CandidateProfile profile, List<EducationDto> incoming) {
        Map<UUID, Education> existing = profile.getEducations().stream()
                .collect(Collectors.toMap(Education::getId, Function.identity()));
        Set<UUID> keep = incoming.stream().map(EducationDto::id).filter(existing::containsKey).collect(Collectors.toSet());
        profile.getEducations().removeIf(e -> !keep.contains(e.getId()));

        for (EducationDto dto : incoming) {
            Education education = dto.id() != null && existing.containsKey(dto.id()) ? existing.get(dto.id()) : null;
            if (education == null) {
                education = new Education();
                education.setCandidateProfile(profile);
                profile.getEducations().add(education);
            }
            education.setInstitution(dto.institution().trim());
            education.setDegree(dto.degree().trim());
            education.setFieldOfStudy(TextNormalizer.clean(dto.fieldOfStudy()));
            education.setStartYear(dto.startYear());
            education.setEndYear(dto.endYear());
        }
    }

    private void reconcileExperiences(CandidateProfile profile, List<ExperienceDto> incoming) {
        Map<UUID, WorkExperience> existing = profile.getExperiences().stream()
                .collect(Collectors.toMap(WorkExperience::getId, Function.identity()));
        Set<UUID> keep = incoming.stream().map(ExperienceDto::id).filter(existing::containsKey).collect(Collectors.toSet());
        profile.getExperiences().removeIf(e -> !keep.contains(e.getId()));

        for (ExperienceDto dto : incoming) {
            WorkExperience experience = dto.id() != null && existing.containsKey(dto.id()) ? existing.get(dto.id()) : null;
            if (experience == null) {
                experience = new WorkExperience();
                experience.setCandidateProfile(profile);
                profile.getExperiences().add(experience);
            }
            experience.setCompanyName(dto.companyName().trim());
            experience.setTitle(dto.title().trim());
            experience.setStartDate(dto.startDate());
            experience.setEndDate(dto.endDate());
            experience.setDescription(TextNormalizer.clean(dto.description()));
        }
    }

    private ProfileResponse toResponse(CandidateProfile p) {
        var resume = resumeRepository.findFirstByCandidateProfileIdOrderByCreatedAtDesc(p.getId());
        List<String> skills = p.getSkills().stream().map(cs -> cs.getSkill().getName()).sorted(String.CASE_INSENSITIVE_ORDER).toList();
        List<EducationDto> educations = p.getEducations().stream()
                .sorted(Comparator.comparing(Education::getStartYear, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(e -> new EducationDto(e.getId(), e.getInstitution(), e.getDegree(), e.getFieldOfStudy(), e.getStartYear(), e.getEndYear()))
                .toList();
        List<ExperienceDto> experiences = p.getExperiences().stream()
                .sorted(Comparator.comparing(WorkExperience::getStartDate).reversed())
                .map(x -> new ExperienceDto(x.getId(), x.getCompanyName(), x.getTitle(), x.getStartDate(), x.getEndDate(), x.getDescription()))
                .toList();

        return new ProfileResponse(
                p.getUser().getId(), p.getUser().getFullName(), p.getUser().getEmail(),
                p.getHeadline(), p.getLocation(), p.getEducationLevel(), p.getGraduationYear(),
                p.getExperienceYears(), p.getSummary(),
                p.getPreferredRoles() == null ? List.of() : List.copyOf(p.getPreferredRoles()),
                skills, educations, experiences,
                resume.map(r -> new ResumeInfo(r.getId(), r.getFileName(), r.getCreatedAt())).orElse(null),
                completeness(p, resume.isPresent()));
    }

    /**
     * A transparent checklist, not a vague score: each item has a fixed
     * weight, and the response lists exactly what's missing. Work experience
     * is worth less than skills because most users are students or freshers.
     */
    static Completeness completeness(CandidateProfile p, boolean hasResume) {
        List<String> missing = new ArrayList<>();
        int score = 0;
        score += check(p.getHeadline() != null, 10, "Add a headline", missing);
        score += check(p.getLocation() != null, 10, "Add your location", missing);
        score += check(p.getSummary() != null, 10, "Write a short summary", missing);
        score += check(p.getSkills().size() >= MIN_SKILLS_FOR_COMPLETE, 25, "Add at least 3 skills", missing);
        score += check(!p.getEducations().isEmpty() || p.getEducationLevel() != null, 15, "Add your education", missing);
        score += check(p.getPreferredRoles() != null && !p.getPreferredRoles().isEmpty(), 15, "Choose the roles you're targeting", missing);
        score += check(!p.getExperiences().isEmpty(), 10, "Add experience, an internship, or a project", missing);
        score += check(hasResume, 5, "Upload your resume", missing);
        return new Completeness(score, missing);
    }

    private static int check(boolean done, int weight, String hint, List<String> missing) {
        if (done) return weight;
        missing.add(hint);
        return 0;
    }
}

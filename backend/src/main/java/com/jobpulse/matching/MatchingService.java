package com.jobpulse.matching;

import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.CandidateSkill;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobSkill;
import com.jobpulse.entity.MatchScore;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.CandidateSkillRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.JobSkillRepository;
import com.jobpulse.repository.MatchScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Deterministic match scoring — the brief is explicit that this stays
 * rule-based and explainable rather than hidden behind an opaque model
 * call (see "Explainable AI" / section 25). Every number this produces has
 * a reason attached.
 *
 * Honest limitations, on purpose rather than by accident:
 *  - Education matching is a placeholder. The Job entity has no structured
 *    "required education" field — that would need NLP extraction from the
 *    free-text job description, which is the planned job of the Python AI
 *    service, not this engine. Until then, education scoring only checks
 *    whether the candidate's own profile is filled in, not whether it
 *    actually satisfies the role.
 *  - The configured "job-type" weight (jobpulse.matching.weights.job-type)
 *    is intentionally unused — there's no reliable candidate signal for
 *    employment-type preference yet. The overall score renormalizes across
 *    only the five dimensions actually computed, rather than silently
 *    treating the unused weight as zero contribution with a misleading
 *    100%-weight total.
 */
@Service
@RequiredArgsConstructor
public class MatchingService {

    private final CandidateProfileRepository candidateProfileRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final MatchWeights weights;

    /** Recomputes and persists (upsert) the match for one user/job pair. */
    @Transactional
    public MatchBreakdown computeAndPersist(UUID userId, UUID jobId) {        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complete your profile before requesting a match score"));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("No job found with id " + jobId));

        MatchBreakdown breakdown = compute(profile, job);

        MatchScore record = matchScoreRepository.findByUserIdAndJobId(userId, jobId)
                .orElseGet(MatchScore::new);

        record.setUser(profile.getUser());
        record.setJob(job);
        record.setOverallScore(breakdown.overallScore());
        record.setSkillScore(breakdown.skillScore());
        record.setExperienceScore(breakdown.experienceScore());
        record.setEducationScore(breakdown.educationScore());
        record.setLocationScore(breakdown.locationScore());
        record.setFreshnessScore(breakdown.freshnessScore());
        record.setMatchedSkills(breakdown.matchedSkills());
        record.setMissingSkills(breakdown.missingSkills());
        record.setConcerns(breakdown.concerns());
        record.setPositiveSignals(breakdown.positiveSignals());
        record.setComputedAt(Instant.now());

        matchScoreRepository.save(record);

        return breakdown;
    }

    /**
     * Returns a cached score if it's fresh enough, otherwise recomputes.
     * Keeps GET /api/jobs/{id}/match cheap for repeated calls without
     * serving a stale-forever number — freshness itself is one of the
     * scored dimensions, so scores do need to age out.
     */
    @Transactional
    public MatchBreakdown getOrCompute(UUID userId, UUID jobId, Duration maxAge) {
        var existing = matchScoreRepository.findByUserIdAndJobId(userId, jobId);
        if (existing.isPresent()) {
            MatchScore ms = existing.get();
            boolean fresh = ms.getComputedAt() != null
                    && Duration.between(ms.getComputedAt(), Instant.now()).compareTo(maxAge) < 0;
            if (fresh) {
                return new MatchBreakdown(
                        ms.getOverallScore(), ms.getSkillScore(), ms.getExperienceScore(),
                        ms.getEducationScore(), ms.getLocationScore(), ms.getFreshnessScore(),
                        ms.getMatchedSkills(), ms.getMissingSkills(), ms.getConcerns(), ms.getPositiveSignals()
                );
            }
        }
        return computeAndPersist(userId, jobId);
    }

    private MatchBreakdown compute(CandidateProfile profile, Job job) {
        List<String> concerns = new ArrayList<>();
        List<String> positiveSignals = new ArrayList<>();

        // ---- Skills ----
        List<JobSkill> jobSkills = jobSkillRepository.findByJobId(job.getId());
        List<String> requiredSkillNames = jobSkills.stream()
                .filter(JobSkill::isRequired)
                .map(js -> js.getSkill().getName())
                .toList();

        Set<String> candidateSkillNamesLower = candidateSkillRepository
                .findByCandidateProfileId(profile.getId()).stream()
                .map(CandidateSkill::getSkill)
                .map(s -> s.getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<String> matchedSkills = requiredSkillNames.stream()
                .filter(name -> candidateSkillNamesLower.contains(name.toLowerCase(Locale.ROOT)))
                .toList();
        List<String> missingSkills = requiredSkillNames.stream()
                .filter(name -> !candidateSkillNamesLower.contains(name.toLowerCase(Locale.ROOT)))
                .toList();

        BigDecimal skillScore = requiredSkillNames.isEmpty()
                ? pct(100)
                : pct(100.0 * matchedSkills.size() / requiredSkillNames.size());

        if (requiredSkillNames.isEmpty()) {
            concerns.add("This listing doesn't specify required skills yet — score may be less reliable");
        } else if (missingSkills.isEmpty()) {
            positiveSignals.add("All listed required skills matched");
        } else if (skillScore.doubleValue() < 60) {
            concerns.add("Missing several required skills: " + String.join(", ", missingSkills));
        }

        // ---- Experience ----
        double years = profile.getExperienceYears() == null ? 0.0 : profile.getExperienceYears().doubleValue();
        BigDecimal experienceScore = scoreExperience(years, job.getExperienceLevel());
        if (job.getExperienceLevel() == null) {
            concerns.add("Experience level not specified for this role");
        } else if (experienceScore.doubleValue() >= 90) {
            positiveSignals.add("Your experience fits this role's level");
        } else if (experienceScore.doubleValue() < 50) {
            concerns.add("Your experience level may not align with what this role expects");
        }

        // ---- Education ----
        // Placeholder — see class comment. Only checks profile completeness,
        // not fit against an actual stated requirement.
        BigDecimal educationScore;
        if (profile.getEducationLevel() != null && profile.getGraduationYear() != null) {
            educationScore = pct(100);
        } else if (profile.getEducationLevel() != null || profile.getGraduationYear() != null) {
            educationScore = pct(70);
        } else {
            educationScore = pct(50);
            concerns.add("Add your education details to improve match accuracy");
        }

        // ---- Location ----
        BigDecimal locationScore;
        String jobLoc = job.getNormalizedLocation();
        String candidateLoc = profile.getLocation() == null
                ? null
                : profile.getLocation().trim().toLowerCase(Locale.ROOT);

        if (jobLoc == null || jobLoc.isBlank()) {
            locationScore = pct(60);
        } else if (jobLoc.contains("remote")) {
            locationScore = pct(100);
            positiveSignals.add("Remote — location isn't a constraint");
        } else if (candidateLoc != null && (jobLoc.contains(candidateLoc) || candidateLoc.contains(jobLoc))) {
            locationScore = pct(100);
            positiveSignals.add("Location matches your profile");
        } else {
            locationScore = pct(40);
            concerns.add("Location may not match your preference");
        }

        // ---- Freshness ----
        BigDecimal freshnessScore;
        if (job.getPostedAt() == null) {
            freshnessScore = pct(50);
        } else {
            long hours = Duration.between(job.getPostedAt(), Instant.now()).toHours();
            if (hours < 24) { freshnessScore = pct(100); positiveSignals.add("Posted within the last day"); }
            else if (hours < 72) { freshnessScore = pct(90); }
            else if (hours < 24 * 7) { freshnessScore = pct(75); }
            else if (hours < 24 * 14) { freshnessScore = pct(55); }
            else if (hours < 24 * 30) { freshnessScore = pct(35); concerns.add("This listing is a few weeks old"); }
            else { freshnessScore = pct(15); concerns.add("This listing may be stale — verify it's still active"); }
        }

        // ---- Overall: weighted average over the five computed dimensions ----
        double usedWeightSum = weights.skills() + weights.experience() + weights.education()
                + weights.location() + weights.freshness();
        double weightedSum =
                skillScore.doubleValue() * weights.skills()
                + experienceScore.doubleValue() * weights.experience()
                + educationScore.doubleValue() * weights.education()
                + locationScore.doubleValue() * weights.location()
                + freshnessScore.doubleValue() * weights.freshness();

        BigDecimal overallScore = usedWeightSum <= 0
                ? pct(0)
                : pct(weightedSum / usedWeightSum);

        return new MatchBreakdown(
                overallScore, skillScore, experienceScore, educationScore, locationScore, freshnessScore,
                matchedSkills, missingSkills, concerns, positiveSignals
        );
    }

    private BigDecimal scoreExperience(double years, Job.ExperienceLevel level) {
        if (level == null) return pct(70); // neutral when the role doesn't state a level

        double raw = switch (level) {
            case INTERN -> years <= 1 ? 100 : Math.max(40, 100 - (years - 1) * 20);
            case ENTRY -> years <= 2 ? 100 : Math.max(50, 100 - (years - 2) * 15);
            case MID -> (years >= 2 && years <= 5)
                    ? 100
                    : (years < 2 ? Math.max(30, 100 - (2 - years) * 30) : Math.max(60, 100 - (years - 5) * 10));
            case SENIOR -> years >= 5 ? 100 : Math.max(20, 100 - (5 - years) * 20);
        };
        return pct(raw);
    }

    private BigDecimal pct(double value) {
        double clamped = Math.max(0, Math.min(100, value));
        return BigDecimal.valueOf(clamped).setScale(2, RoundingMode.HALF_UP);
    }
}

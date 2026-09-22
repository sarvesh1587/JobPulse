package com.jobpulse.skillgap;

import com.jobpulse.dto.SkillGapItemResponse;
import com.jobpulse.dto.SkillGapResponse;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.SkillGap;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.CandidateSkillRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.JobSkillRepository;
import com.jobpulse.repository.SkillGapRepository;
import com.jobpulse.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Computes which skills show up most often in the candidate's target roles
 * that they don't have yet.
 *
 * Deliberate simplification: "target roles" means active job postings whose
 * title contains one of the candidate's preferredRoles (case-insensitive
 * substring), scanned over the most recent 300 active postings rather than
 * the whole table — this is a portfolio-scale project, not a system built
 * for millions of rows, and a full-table scan with no cap would be a bad
 * habit to normalize even here. If preferredRoles is empty, this falls back
 * to the same recent-300 window with no title filter, which is a much
 * blunter signal — the response says so via jobsAnalyzed and an empty
 * targetRoles list so the caller can tell the difference.
 */
@Service
@RequiredArgsConstructor
public class SkillGapService {

    private static final int JOB_SCAN_LIMIT = 300;
    private static final int MAX_PRIORITY_GAPS = 10;

    private final CandidateProfileRepository candidateProfileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SkillRepository skillRepository;
    private final SkillGapRepository skillGapRepository;

    @Transactional
    public SkillGapResponse computeAndPersist(UUID userId) {
        CandidateProfile profile = candidateProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "Complete your profile before computing a skill gap"));

        List<String> targetRoles = profile.getPreferredRoles() == null ? List.of() : profile.getPreferredRoles();

        Pageable window = PageRequest.of(0, JOB_SCAN_LIMIT, Sort.by(Sort.Direction.DESC, "postedAt"));
        List<Job> candidateJobs = jobRepository.search(null, null, window).getContent();

        List<Job> relevantJobs = targetRoles.isEmpty()
                ? candidateJobs
                : candidateJobs.stream()
                    .filter(job -> matchesAnyRole(job.getNormalizedTitle(), targetRoles))
                    .toList();

        // Frequency of each required skill across the relevant jobs.
        Map<String, Integer> frequency = new LinkedHashMap<>();
        for (Job job : relevantJobs) {
            jobSkillRepository.findByJobId(job.getId()).stream()
                    .filter(js -> js.isRequired())
                    .map(js -> js.getSkill().getName())
                    .forEach(name -> frequency.merge(name, 1, Integer::sum));
        }

        Set<String> candidateSkillNamesLower = candidateSkillRepository.findByCandidateProfileId(profile.getId())
                .stream()
                .map(cs -> cs.getSkill().getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<String> readySkills = frequency.keySet().stream()
                .filter(name -> candidateSkillNamesLower.contains(name.toLowerCase(Locale.ROOT)))
                .sorted(Comparator.comparingInt((String name) -> frequency.get(name)).reversed())
                .toList();

        List<Map.Entry<String, Integer>> missing = frequency.entrySet().stream()
                .filter(e -> !candidateSkillNamesLower.contains(e.getKey().toLowerCase(Locale.ROOT)))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(MAX_PRIORITY_GAPS)
                .toList();

        // Persist as the current snapshot — replace whatever was there before.
        skillGapRepository.deleteByUserId(userId);
        Instant now = Instant.now();
        int priority = 1;
        for (var entry : missing) {
            var skill = skillRepository.findByNameIgnoreCase(entry.getKey()).orElse(null);
            if (skill == null) continue;
            skillGapRepository.save(SkillGap.builder()
                    .user(profile.getUser())
                    .skill(skill)
                    .priority(priority++)
                    .frequencyInTargetRoles(entry.getValue())
                    .computedAt(now)
                    .build());
        }

        List<SkillGapItemResponse> priorityGaps = rankItems(missing);

        return new SkillGapResponse(targetRoles, relevantJobs.size(), readySkills, priorityGaps, now);
    }

    private List<SkillGapItemResponse> rankItems(List<Map.Entry<String, Integer>> missing) {
        List<SkillGapItemResponse> result = new java.util.ArrayList<>();
        int rank = 1;
        for (var entry : missing) {
            result.add(new SkillGapItemResponse(entry.getKey(), rank++, entry.getValue()));
        }
        return result;
    }

    private boolean matchesAnyRole(String normalizedTitle, List<String> preferredRoles) {
        if (normalizedTitle == null) return false;
        return preferredRoles.stream()
                .map(r -> r.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedTitle::contains);
    }
}

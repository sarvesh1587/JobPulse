package com.jobpulse.service;

import com.jobpulse.dto.job.JobSearchCriteria;
import com.jobpulse.dto.skillgap.SkillGapResponse;
import com.jobpulse.dto.skillgap.SkillGapResponse.GapItem;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.Skill;
import com.jobpulse.entity.SkillGap;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.JobSkillRepository;
import com.jobpulse.repository.JobSpecifications;
import com.jobpulse.repository.SkillGapRepository;
import com.jobpulse.repository.SkillRepository;
import com.jobpulse.repository.projection.Projections.JobSkillName;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Which skills come up most often in the candidate's target roles that they
 * don't have yet.
 *
 * "Target roles" means active jobs whose title contains one of the
 * candidate's preferred roles, taken from the newest 300 active jobs. The cap
 * keeps the cost fixed. With no preferred roles, all of those jobs are used,
 * which is a blunter signal, and the empty targetRoles list in the response
 * says so.
 */
@Service
@RequiredArgsConstructor
public class SkillGapService {

    private static final int JOB_SCAN_LIMIT = 300;
    private static final int MAX_GAPS = 10;

    private final CandidateProfileRepository profileRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SkillRepository skillRepository;
    private final SkillGapRepository skillGapRepository;
    private final Clock clock;

    @Transactional
    public SkillGapResponse compute(UUID userId) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No candidate profile found for this account."));
        List<String> targetRoles = profile.getPreferredRoles() == null ? List.of() : List.copyOf(profile.getPreferredRoles());

        JobSearchCriteria recentActive = new JobSearchCriteria(null, null, null, null, null, null, null, null,
                JobSearchCriteria.Sort.RECENT, 0, JOB_SCAN_LIMIT);
        List<Job> relevantJobs = jobRepository.findAll(JobSpecifications.searchActive(recentActive),
                        PageRequest.of(0, JOB_SCAN_LIMIT)).getContent().stream()
                .filter(job -> targetRoles.isEmpty() || matchesAnyRole(job.getNormalizedTitle(), targetRoles))
                .toList();

        Map<String, Integer> frequency = new HashMap<>();
        if (!relevantJobs.isEmpty()) {
            List<UUID> ids = relevantJobs.stream().map(Job::getId).toList();
            for (JobSkillName row : jobSkillRepository.findSkillNamesByJobIds(ids)) {
                frequency.merge(row.skillName(), 1, Integer::sum);
            }
        }

        Set<String> owned = profile.getSkills().stream()
                .map(cs -> cs.getSkill().getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<String> ready = frequency.keySet().stream()
                .filter(name -> owned.contains(name.toLowerCase(Locale.ROOT)))
                .sorted((a, b) -> frequency.get(b) - frequency.get(a))
                .toList();
        List<Map.Entry<String, Integer>> missing = frequency.entrySet().stream()
                .filter(e -> !owned.contains(e.getKey().toLowerCase(Locale.ROOT)))
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(MAX_GAPS)
                .toList();

        Instant now = Instant.now(clock);
        List<GapItem> gaps = new ArrayList<>();
        for (int i = 0; i < missing.size(); i++) {
            var entry = missing.get(i);
            gaps.add(new GapItem(entry.getKey(), i + 1, entry.getValue(),
                    Math.round(100f * entry.getValue() / relevantJobs.size())));
        }
        persistSnapshot(profile, gaps, now);

        return new SkillGapResponse(targetRoles, relevantJobs.size(), ready, gaps, now);
    }

    /** Replaces the stored snapshot, so the latest analysis is queryable later. */
    private void persistSnapshot(CandidateProfile profile, List<GapItem> gaps, Instant now) {
        skillGapRepository.deleteByUserId(profile.getUser().getId());
        if (gaps.isEmpty()) return;
        Map<String, Skill> skills = skillRepository.findByLowerNames(
                        gaps.stream().map(g -> g.skillName().toLowerCase(Locale.ROOT)).toList()).stream()
                .collect(Collectors.toMap(s -> s.getName().toLowerCase(Locale.ROOT), Function.identity()));
        for (GapItem gap : gaps) {
            Skill skill = skills.get(gap.skillName().toLowerCase(Locale.ROOT));
            if (skill != null) {
                skillGapRepository.save(new SkillGap(profile.getUser(), skill, gap.priority(),
                        gap.frequencyInTargetRoles(), now));
            }
        }
    }

    private boolean matchesAnyRole(String normalizedTitle, List<String> roles) {
        return normalizedTitle != null && roles.stream()
                .map(r -> r.toLowerCase(Locale.ROOT))
                .anyMatch(normalizedTitle::contains);
    }
}

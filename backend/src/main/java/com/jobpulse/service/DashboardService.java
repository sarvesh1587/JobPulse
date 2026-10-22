package com.jobpulse.service;

import com.jobpulse.dto.application.ApplicationDtos.ApplicationResponse;
import com.jobpulse.dto.dashboard.DashboardResponse;
import com.jobpulse.dto.dashboard.DashboardResponse.RecommendationBasis;
import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.SavedJob;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.mapper.ApplicationMapper;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.JobApplicationRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.SavedJobRepository;
import com.jobpulse.repository.projection.Projections.StatusCount;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class DashboardService {

    private static final int RECOMMENDATION_COUNT = 5;
    private static final int RECENT_COUNT = 5;

    private final CandidateProfileRepository profileRepository;
    private final JobApplicationRepository applicationRepository;
    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final JobSummaryAssembler assembler;
    private final ProfileService profileService;
    private final Clock clock;

    public DashboardResponse build(UUID userId) {
        CandidateProfile profile = profileRepository.findByUserId(userId)
                .orElseThrow(() -> new ResourceNotFoundException("No candidate profile found for this account."));

        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) byStatus.put(s, 0L);
        for (StatusCount row : applicationRepository.countByStatusForCandidate(userId)) byStatus.put(row.status(), row.count());
        long active = ApplicationStatus.inProgress().stream().mapToLong(byStatus::get).sum();

        List<ApplicationResponse> interviews = applicationRepository.findUpcomingInterviews(userId, Instant.now(clock)).stream()
                .limit(RECENT_COUNT).map(ApplicationMapper::toResponse).toList();
        List<ApplicationResponse> recentApplications = applicationRepository.findForCandidate(userId, null).stream()
                .limit(RECENT_COUNT).map(ApplicationMapper::toResponse).toList();

        List<Job> recentlySavedJobs = savedJobRepository.findRecentWithJob(userId, PageRequest.of(0, RECENT_COUNT))
                .stream().map(SavedJob::getJob).toList();

        Set<UUID> skillIds = profile.getSkills().stream().map(cs -> cs.getSkill().getId()).collect(Collectors.toSet());
        RecommendationBasis basis = skillIds.isEmpty() ? RecommendationBasis.RECENT : RecommendationBasis.SKILLS;
        List<Job> recommendedJobs = basis == RecommendationBasis.SKILLS
                ? recommendBySkills(skillIds)
                : jobRepository.findTop6ByStatusOrderByPostedAtDesc(Job.JobStatus.ACTIVE).stream().limit(RECOMMENDATION_COUNT).toList();

        String firstName = profile.getUser().getFullName().split("\\s+")[0];
        return new DashboardResponse(
                firstName, byStatus, active, savedJobRepository.countByUserId(userId),
                interviews, recentApplications,
                assembler.assemble(recentlySavedJobs, userId),
                basis,
                assembler.assemble(recommendedJobs, userId),
                profileService.completeness(userId));
    }

    /**
     * Skill-overlap recommendations. One grouped query ranks job ids by how
     * many of the candidate's skills each job asks for, then one more query
     * loads those jobs. The second query doesn't keep the ranking order, so
     * the list is re-sorted by the ranked ids.
     */
    private List<Job> recommendBySkills(Set<UUID> skillIds) {
        List<UUID> rankedIds = jobRepository.findIdsRankedBySkillOverlap(
                skillIds, Job.JobStatus.ACTIVE, PageRequest.of(0, RECOMMENDATION_COUNT));
        if (rankedIds.isEmpty()) return List.of();
        Map<UUID, Integer> rank = rankedIds.stream().collect(Collectors.toMap(Function.identity(), rankedIds::indexOf));
        return jobRepository.findByIdIn(rankedIds).stream()
                .sorted(Comparator.comparingInt(job -> rank.get(job.getId())))
                .toList();
    }
}

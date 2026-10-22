package com.jobpulse.service;

import com.jobpulse.dto.job.JobDtos.JobSummaryResponse;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.MatchScore;
import com.jobpulse.mapper.JobMapper;
import com.jobpulse.repository.JobSkillRepository;
import com.jobpulse.repository.MatchScoreRepository;
import com.jobpulse.repository.SavedJobRepository;
import com.jobpulse.repository.projection.Projections.JobSkillName;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Builds job cards for any list of jobs with a fixed number of queries,
 * however long the list is:
 *   1 query for every job's skills,
 *   1 query for which of them the user has saved,
 *   1 query for the user's cached match scores.
 * Doing those lookups per job would be the classic N+1 problem: 20 jobs
 * would cost 60 extra queries.
 *
 * The jobs passed in must already have their company loaded (fetch join or
 * entity graph), and this must run inside a transaction.
 */
@Component
@RequiredArgsConstructor
public class JobSummaryAssembler {

    private final JobSkillRepository jobSkillRepository;
    private final SavedJobRepository savedJobRepository;
    private final MatchScoreRepository matchScoreRepository;

    public List<JobSummaryResponse> assemble(List<Job> jobs, UUID candidateId) {
        if (jobs.isEmpty()) return List.of();
        List<UUID> ids = jobs.stream().map(Job::getId).toList();

        Map<UUID, List<String>> skillsByJob = jobSkillRepository.findSkillNamesByJobIds(ids).stream()
                .collect(Collectors.groupingBy(JobSkillName::jobId,
                        Collectors.mapping(JobSkillName::skillName, Collectors.toList())));

        Set<UUID> savedIds = candidateId == null ? Collections.emptySet()
                : savedJobRepository.findSavedJobIds(candidateId, ids);

        Map<UUID, BigDecimal> scores = candidateId == null ? Map.of()
                : matchScoreRepository.findForJobs(candidateId, ids).stream()
                    .collect(Collectors.toMap(ms -> ms.getJob().getId(), MatchScore::getOverallScore));

        return jobs.stream()
                .map(job -> JobMapper.toSummary(job,
                        skillsByJob.getOrDefault(job.getId(), List.of()),
                        scores.get(job.getId()),
                        savedIds.contains(job.getId())))
                .toList();
    }
}

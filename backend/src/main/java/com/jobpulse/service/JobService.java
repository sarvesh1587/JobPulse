package com.jobpulse.service;

import com.jobpulse.dto.common.PageResponse;
import com.jobpulse.dto.job.JobDtos.JobDetailResponse;
import com.jobpulse.dto.job.JobDtos.JobSummaryResponse;
import com.jobpulse.dto.job.JobDtos.MatchScoreResponse;
import com.jobpulse.dto.job.JobSearchCriteria;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobApplication;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.mapper.JobMapper;
import com.jobpulse.matching.MatchingService;
import com.jobpulse.repository.JobApplicationRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.JobSkillRepository;
import com.jobpulse.repository.JobSpecifications;
import com.jobpulse.repository.SavedJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/** Public job discovery. candidateId is null for anonymous visitors and non-candidates. */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    private static final Duration MATCH_SCORE_MAX_AGE = Duration.ofHours(6);

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final SavedJobRepository savedJobRepository;
    private final JobApplicationRepository applicationRepository;
    private final JobSummaryAssembler assembler;
    private final MatchingService matchingService;

    public PageResponse<JobSummaryResponse> search(JobSearchCriteria criteria, UUID candidateId) {
        // Sorting is part of the Specification (it needs NULLS LAST), so the PageRequest is unsorted.
        PageRequest pageRequest = PageRequest.of(criteria.pageOrDefault(), criteria.sizeOrDefault());
        Page<Job> page = jobRepository.findAll(JobSpecifications.searchActive(criteria), pageRequest);
        return PageResponse.of(page, assembler.assemble(page.getContent(), candidateId));
    }

    /** The newest active listings, for the landing page. */
    public List<JobSummaryResponse> featured(UUID candidateId) {
        return assembler.assemble(jobRepository.findTop6ByStatusOrderByPostedAtDesc(Job.JobStatus.ACTIVE), candidateId);
    }

    /**
     * Closed listings stay viewable, so a candidate can still open a job they
     * applied to. The page shows the status, and the apply action is refused.
     */
    public JobDetailResponse getDetail(UUID jobId, UUID candidateId) {
        Job job = jobRepository.findWithCompanyById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("This job doesn't exist or has been removed."));
        List<String> skills = jobSkillRepository.findWithSkillByJobId(jobId).stream()
                .map(js -> js.getSkill().getName())
                .sorted()
                .toList();

        boolean saved = false;
        JobApplication myApplication = null;
        if (candidateId != null) {
            saved = savedJobRepository.existsByUserIdAndJobId(candidateId, jobId);
            myApplication = applicationRepository.findByUserIdAndJobId(candidateId, jobId).orElse(null);
        }
        return JobMapper.toDetail(job, skills, saved, myApplication);
    }

    /** Writes a cached score, so it isn't a read-only transaction. */
    @Transactional
    public MatchScoreResponse matchScore(UUID candidateId, UUID jobId) {
        return JobMapper.toMatchScore(jobId, matchingService.getOrCompute(candidateId, jobId, MATCH_SCORE_MAX_AGE));
    }
}

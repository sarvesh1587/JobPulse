package com.jobpulse.service;

import com.jobpulse.dto.common.PageResponse;
import com.jobpulse.dto.job.JobDtos.JobSummaryResponse;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.SavedJob;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.SavedJobRepository;
import com.jobpulse.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

/**
 * Bookmarks. Save and unsave are idempotent: saving twice or removing
 * something that isn't saved both succeed with no change. That makes the
 * heart button safe to double-click and safe to retry.
 */
@Service
@RequiredArgsConstructor
public class SavedJobService {

    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final JobSummaryAssembler assembler;

    @Transactional(readOnly = true)
    public PageResponse<JobSummaryResponse> list(UUID userId, int page, int size) {
        Page<SavedJob> saved = savedJobRepository.findPageWithJob(userId, PageRequest.of(page, size));
        return PageResponse.of(saved, assembler.assemble(saved.map(SavedJob::getJob).getContent(), userId));
    }

    @Transactional
    public void save(UUID userId, UUID jobId) {
        if (savedJobRepository.existsByUserIdAndJobId(userId, jobId)) return;
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("This job doesn't exist or has been removed."));
        savedJobRepository.save(new SavedJob(userRepository.getReferenceById(userId), job));
    }

    @Transactional
    public void unsave(UUID userId, UUID jobId) {
        savedJobRepository.findByUserIdAndJobId(userId, jobId).ifPresent(savedJobRepository::delete);
    }
}

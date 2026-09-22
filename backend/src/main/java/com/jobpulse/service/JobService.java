package com.jobpulse.service;

import com.jobpulse.dto.JobDetailResponse;
import com.jobpulse.dto.JobSummaryResponse;
import com.jobpulse.entity.Job;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.mapper.JobMapper;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.JobSkillRepository;
import com.jobpulse.repository.MatchScoreRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class JobService {

    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final MatchScoreRepository matchScoreRepository;
    private final JobMapper jobMapper;

    public Page<JobSummaryResponse> search(String title, String location, Pageable pageable, UUID currentUserId) {
        Page<Job> jobs = jobRepository.search(
                blankToNull(title), blankToNull(location), pageable);

        return jobs.map(job -> {
            List<String> skills = skillNames(job.getId());
            var score = currentUserId == null ? Optional.<com.jobpulse.entity.MatchScore>empty()
                    : matchScoreRepository.findByUserIdAndJobId(currentUserId, job.getId());
            return jobMapper.toSummary(job, skills, score.map(com.jobpulse.entity.MatchScore::getOverallScore).orElse(null));
        });
    }

    public JobDetailResponse getById(UUID jobId, UUID currentUserId) {
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("No job found with id " + jobId));

        List<String> skills = skillNames(jobId);
        var score = currentUserId == null ? Optional.<com.jobpulse.entity.MatchScore>empty()
                : matchScoreRepository.findByUserIdAndJobId(currentUserId, jobId);

        return jobMapper.toDetail(job, skills, score.map(com.jobpulse.entity.MatchScore::getOverallScore).orElse(null));
    }

    private List<String> skillNames(UUID jobId) {
        return jobSkillRepository.findByJobId(jobId).stream()
                .map(js -> js.getSkill().getName())
                .toList();
    }

    private String blankToNull(String value) {
        return (value == null || value.isBlank()) ? null : value.trim();
    }
}

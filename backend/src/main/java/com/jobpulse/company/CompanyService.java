package com.jobpulse.company;

import com.jobpulse.dto.CompanyDetailResponse;
import com.jobpulse.dto.CompanySummaryResponse;
import com.jobpulse.dto.JobSummaryResponse;
import com.jobpulse.entity.Company;
import com.jobpulse.entity.Job;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.mapper.JobMapper;
import com.jobpulse.repository.CompanyRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.JobSkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Every number here is derived strictly from currently ACTIVE listings this
 * instance has ingested. No historical hiring trend is computed or
 * fabricated — the brief is explicit that "Not enough data yet" is the
 * correct answer until there's a real time series to show, and a handful of
 * ingestion runs never adds up to one.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private static final int TOP_SKILLS_LIMIT = 8;
    private static final String NO_TREND_DATA = "Not enough data yet";

    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final JobMapper jobMapper;

    public Page<CompanySummaryResponse> list(Pageable pageable) {
        return companyRepository.findAll(pageable)
                .map(this::toSummary);
    }

    public CompanyDetailResponse getById(UUID companyId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("No company found with id " + companyId));

        List<Job> activeJobs = jobRepository.findByCompanyIdAndStatus(companyId, Job.JobStatus.ACTIVE);

        Map<String, Long> byEmploymentType = activeJobs.stream()
                .filter(j -> j.getEmploymentType() != null)
                .collect(Collectors.groupingBy(j -> j.getEmploymentType().name(), Collectors.counting()));

        Map<String, Long> byExperienceLevel = activeJobs.stream()
                .filter(j -> j.getExperienceLevel() != null)
                .collect(Collectors.groupingBy(j -> j.getExperienceLevel().name(), Collectors.counting()));

        List<String> locations = activeJobs.stream()
                .map(Job::getLocation)
                .filter(loc -> loc != null && !loc.isBlank())
                .distinct()
                .sorted()
                .toList();

        List<String> topSkills = topSkillsFor(activeJobs);

        Instant lastVerifiedAt = activeJobs.stream()
                .map(Job::getLastVerifiedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        List<JobSummaryResponse> openJobs = activeJobs.stream()
                .map(job -> jobMapper.toSummary(job, skillNames(job.getId()), null))
                .toList();

        return new CompanyDetailResponse(
                company.getId(),
                company.getName(),
                company.getCareersUrl(),
                activeJobs.size(),
                (int) byEmploymentType.getOrDefault("INTERNSHIP", 0L).longValue(),
                (int) byExperienceLevel.getOrDefault("ENTRY", 0L).longValue(),
                (int) (byExperienceLevel.getOrDefault("MID", 0L) + byExperienceLevel.getOrDefault("SENIOR", 0L)),
                locations,
                topSkills,
                byEmploymentType,
                byExperienceLevel,
                lastVerifiedAt,
                NO_TREND_DATA,
                openJobs
        );
    }

    private CompanySummaryResponse toSummary(Company company) {
        List<Job> activeJobs = jobRepository.findByCompanyIdAndStatus(company.getId(), Job.JobStatus.ACTIVE);

        int internships = (int) activeJobs.stream()
                .filter(j -> j.getEmploymentType() == Job.EmploymentType.INTERNSHIP)
                .count();
        int entryLevel = (int) activeJobs.stream()
                .filter(j -> j.getExperienceLevel() == Job.ExperienceLevel.ENTRY)
                .count();

        List<String> locations = activeJobs.stream()
                .map(Job::getLocation)
                .filter(loc -> loc != null && !loc.isBlank())
                .distinct()
                .limit(5)
                .toList();

        Instant lastVerifiedAt = activeJobs.stream()
                .map(Job::getLastVerifiedAt)
                .filter(java.util.Objects::nonNull)
                .max(Comparator.naturalOrder())
                .orElse(null);

        return new CompanySummaryResponse(
                company.getId(),
                company.getName(),
                company.getCareersUrl(),
                activeJobs.size(),
                internships,
                entryLevel,
                locations,
                topSkillsFor(activeJobs),
                lastVerifiedAt
        );
    }

    private List<String> topSkillsFor(List<Job> jobs) {
        Map<String, Integer> frequency = new LinkedHashMap<>();
        for (Job job : jobs) {
            skillNames(job.getId()).forEach(name -> frequency.merge(name, 1, Integer::sum));
        }
        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed())
                .limit(TOP_SKILLS_LIMIT)
                .map(Map.Entry::getKey)
                .toList();
    }

    private List<String> skillNames(UUID jobId) {
        return jobSkillRepository.findByJobId(jobId).stream()
                .map(js -> js.getSkill().getName())
                .toList();
    }
}

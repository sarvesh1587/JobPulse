package com.jobpulse.service;

import com.jobpulse.dto.common.PageResponse;
import com.jobpulse.dto.company.CompanyDtos.CompanyDetailResponse;
import com.jobpulse.dto.company.CompanyDtos.CompanySummaryResponse;
import com.jobpulse.dto.job.JobDtos.JobSummaryResponse;
import com.jobpulse.entity.Company;
import com.jobpulse.entity.Job;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.repository.CompanyRepository;
import com.jobpulse.repository.JobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Company pages. Every number comes from the company's currently ACTIVE
 * listings. There's no historical hiring trend, because the data needed
 * to compute an honest one doesn't exist.
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CompanyService {

    private static final int TOP_SKILLS = 8;

    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobSummaryAssembler assembler;

    public PageResponse<CompanySummaryResponse> list(String query, int page, int size) {
        String pattern = query == null || query.isBlank() ? "%" : "%" + query.trim().toLowerCase() + "%";
        var rows = companyRepository.findListRows(pattern, PageRequest.of(page, size));
        return PageResponse.from(rows, r -> new CompanySummaryResponse(
                r.id(), r.name(), r.headquarters(), r.website(), r.openRoles(), r.internships(), r.entryLevel()));
    }

    public CompanyDetailResponse get(UUID companyId, UUID candidateId) {
        Company company = companyRepository.findById(companyId)
                .orElseThrow(() -> new ResourceNotFoundException("Company not found."));
        List<Job> activeJobs = jobRepository.findByCompanyIdAndStatusOrderByPostedAtDesc(companyId, Job.JobStatus.ACTIVE);
        List<JobSummaryResponse> openJobs = assembler.assemble(activeJobs, candidateId);

        return new CompanyDetailResponse(
                company.getId(), company.getName(), company.getDescription(), company.getWebsite(),
                company.getHeadquarters(), company.getCareersUrl(),
                activeJobs.size(),
                activeJobs.stream().map(Job::getLocation).filter(Objects::nonNull).distinct().sorted().toList(),
                topSkills(openJobs),
                countBy(activeJobs, j -> j.getEmploymentType() == null ? null : j.getEmploymentType().name()),
                countBy(activeJobs, j -> j.getExperienceLevel() == null ? null : j.getExperienceLevel().name()),
                activeJobs.stream().map(Job::getLastVerifiedAt).filter(Objects::nonNull).max(Comparator.naturalOrder()).orElse(null),
                openJobs);
    }

    private List<String> topSkills(List<JobSummaryResponse> jobs) {
        Map<String, Long> frequency = jobs.stream().flatMap(j -> j.skills().stream())
                .collect(Collectors.groupingBy(s -> s, Collectors.counting()));
        return frequency.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed().thenComparing(Map.Entry.comparingByKey()))
                .limit(TOP_SKILLS)
                .map(Map.Entry::getKey)
                .toList();
    }

    private Map<String, Long> countBy(List<Job> jobs, Function<Job, String> key) {
        return jobs.stream().map(key).filter(Objects::nonNull)
                .collect(Collectors.groupingBy(k -> k, LinkedHashMap::new, Collectors.counting()));
    }
}

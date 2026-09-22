package com.jobpulse.ingestion;

import com.jobpulse.entity.Company;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobSkill;
import com.jobpulse.entity.JobSourceEntity;
import com.jobpulse.entity.JobVerification;
import com.jobpulse.entity.Skill;
import com.jobpulse.repository.CompanyRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.JobSkillRepository;
import com.jobpulse.repository.JobSourceRepository;
import com.jobpulse.repository.JobVerificationRepository;
import com.jobpulse.repository.SkillRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Pulled out of IngestionService as its own Spring bean so @Transactional
 * actually applies — Spring's proxy-based AOP doesn't intercept a method
 * calling another method on itself (this.upsert(...) bypasses the proxy
 * entirely), so this has to live behind its own injected boundary.
 */
@Service
@RequiredArgsConstructor
public class JobUpsertService {

    private final CompanyRepository companyRepository;
    private final JobSourceRepository jobSourceRepository;
    private final JobRepository jobRepository;
    private final JobVerificationRepository jobVerificationRepository;
    private final SkillRepository skillRepository;
    private final JobSkillRepository jobSkillRepository;

    @Transactional
    public boolean upsert(NormalizedJob nj) {
        JobSourceEntity source = jobSourceRepository.findByName(nj.sourceName())
                .orElseGet(() -> jobSourceRepository.save(
                        JobSourceEntity.builder().name(nj.sourceName()).active(true).build()));

        String normalizedCompany = normalize(nj.companyName());
        Company company = companyRepository.findByNormalizedName(normalizedCompany)
                .orElseGet(() -> companyRepository.save(
                        Company.builder()
                                .name(nj.companyName())
                                .normalizedName(normalizedCompany)
                                .build()));

        // Dedup tier 1: external id within this source.
        var existing = jobRepository.findByJobSourceIdAndExternalId(source.getId(), nj.externalId());

        String normalizedTitle = normalize(nj.title());
        String normalizedLocation = nj.location() == null ? null : normalize(nj.location());

        Job job = existing.orElse(null);
        boolean isNew = job == null;

        if (job == null) {
            // Dedup tier 2: same company + same normalized title + same
            // normalized location, in case the source rotated the external id.
            job = findLikelyDuplicate(company.getId(), normalizedTitle, normalizedLocation).orElse(null);
            isNew = job == null;
        }

        if (job == null) {
            job = Job.builder()
                    .externalId(nj.externalId())
                    .jobSource(source)
                    .company(company)
                    .status(Job.JobStatus.ACTIVE)
                    .build();
        } else {
            job.setExternalId(nj.externalId());
            job.setJobSource(source);
        }

        job.setTitle(nj.title());
        job.setNormalizedTitle(normalizedTitle);
        job.setDescription(nj.description());
        job.setLocation(nj.location());
        job.setNormalizedLocation(normalizedLocation);
        job.setEmploymentType(toEntityEmploymentType(nj.employmentType()));
        job.setExperienceLevel(toEntityExperienceLevel(nj.experienceLevel()));
        job.setSalaryMin(nj.salaryMin());
        job.setSalaryMax(nj.salaryMax());
        job.setApplicationUrl(nj.applicationUrl());
        job.setSourceUrl(nj.sourceUrl());
        job.setPostedAt(nj.postedAt());
        job.setLastVerifiedAt(Instant.now());
        job.setStatus(Job.JobStatus.ACTIVE);

        job = jobRepository.save(job);

        tagSkillsNaively(job);

        jobVerificationRepository.save(JobVerification.builder()
                .job(job)
                .checkedAt(Instant.now())
                .resultStatus(Job.JobStatus.ACTIVE)
                .httpStatus(200)
                .notes(isNew ? "Created on ingestion" : "Refreshed on ingestion")
                .build());

        return isNew;
    }

    private java.util.Optional<Job> findLikelyDuplicate(
            java.util.UUID companyId, String normalizedTitle, String normalizedLocation) {
        return jobRepository.findFirstByCompanyIdAndNormalizedTitleAndNormalizedLocation(
                companyId, normalizedTitle, normalizedLocation);
    }

    /**
     * Naive keyword tagging: whole-word, case-insensitive match of each
     * known Skill name against the job's title + description. This is
     * deliberately simple — real extraction (synonyms, context, implied
     * skills) is the Python AI service's job. This only exists so the
     * matching engine has *something* to score against before that
     * service exists. Only adds skills, never removes ones a previous
     * run tagged, so re-ingesting a job won't silently drop data an
     * admin may have manually curated later.
     */
    private void tagSkillsNaively(Job job) {
        String haystack = ((job.getTitle() == null ? "" : job.getTitle())
                + " " + (job.getDescription() == null ? "" : job.getDescription()));

        Set<String> alreadyTagged = jobSkillRepository.findByJobId(job.getId()).stream()
                .map(js -> js.getSkill().getName().toLowerCase(Locale.ROOT))
                .collect(Collectors.toSet());

        List<Skill> allSkills = skillRepository.findAll();

        for (Skill skill : allSkills) {
            String lowerName = skill.getName().toLowerCase(Locale.ROOT);
            if (alreadyTagged.contains(lowerName)) continue;

            Pattern wordBoundary = Pattern.compile(
                    "(?i)\\b" + Pattern.quote(skill.getName()) + "\\b");
            if (wordBoundary.matcher(haystack).find()) {
                jobSkillRepository.save(JobSkill.builder()
                        .job(job)
                        .skill(skill)
                        .required(true)
                        .build());
            }
        }
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
    }

    private Job.EmploymentType toEntityEmploymentType(NormalizedJob.EmploymentType type) {
        return switch (type) {
            case INTERNSHIP -> Job.EmploymentType.INTERNSHIP;
            case CONTRACT -> Job.EmploymentType.CONTRACT;
            case FULL_TIME -> Job.EmploymentType.FULL_TIME;
            case UNKNOWN -> null;
        };
    }

    private Job.ExperienceLevel toEntityExperienceLevel(NormalizedJob.ExperienceLevel level) {
        return switch (level) {
            case INTERN -> Job.ExperienceLevel.INTERN;
            case ENTRY -> Job.ExperienceLevel.ENTRY;
            case MID -> Job.ExperienceLevel.MID;
            case SENIOR -> Job.ExperienceLevel.SENIOR;
            case UNKNOWN -> null;
        };
    }
}

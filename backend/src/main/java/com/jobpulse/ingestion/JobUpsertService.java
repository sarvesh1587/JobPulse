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
import com.jobpulse.service.TextNormalizer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Lives in its own bean, separate from IngestionService, so @Transactional
 * applies. Spring's transactions work through a proxy, and a method calling
 * another method on the same object (this.upsert(...)) bypasses the proxy.
 * Each job is upserted in its own transaction, so one bad record can't roll
 * back the rest of the board.
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

    /** @return true if a new job row was created, false if an existing one was refreshed */
    @Transactional
    public boolean upsert(NormalizedJob nj) {
        JobSourceEntity source = jobSourceRepository.findByName(nj.sourceName())
                .orElseGet(() -> jobSourceRepository.save(new JobSourceEntity(nj.sourceName())));

        Company company = findOrCreateCompany(nj.companyName());
        String normalizedTitle = TextNormalizer.normalize(nj.title());
        String normalizedLocation = TextNormalizer.normalize(nj.location());

        // Dedup tier 1: the source's own id. Tier 2: same company, title and
        // location, in case the source rotated its ids.
        Job job = jobRepository.findByJobSourceIdAndExternalId(source.getId(), nj.externalId())
                .or(() -> jobRepository.findFirstByCompanyIdAndNormalizedTitleAndNormalizedLocation(
                        company.getId(), normalizedTitle, normalizedLocation))
                .orElse(null);
        boolean isNew = job == null;
        if (isNew) {
            job = new Job();
            job.setOrigin(Job.Origin.INGESTED);
            job.setCompany(company);
        }

        job.setExternalId(nj.externalId());
        job.setJobSource(source);
        job.setTitle(nj.title());
        job.setNormalizedTitle(normalizedTitle);
        job.setDescription(nj.description());
        job.setLocation(nj.location());
        job.setNormalizedLocation(normalizedLocation);
        job.setWorkMode(inferWorkMode(nj.location()));
        job.setEmploymentType(toEntityEmploymentType(nj.employmentType()));
        job.setExperienceLevel(toEntityExperienceLevel(nj.experienceLevel()));
        job.setSalaryMin(nj.salaryMin());
        job.setSalaryMax(nj.salaryMax());
        job.setApplicationUrl(nj.applicationUrl());
        job.setSourceUrl(nj.sourceUrl());
        job.setPostedAt(nj.postedAt() != null ? nj.postedAt() : Instant.now());
        job.setLastVerifiedAt(Instant.now());
        // A listing an admin closed stays closed even if the board still shows it.
        if (job.getStatus() != Job.JobStatus.CLOSED) {
            job.setStatus(Job.JobStatus.ACTIVE);
        }
        job = jobRepository.save(job);

        tagSkillsByKeyword(job);
        jobVerificationRepository.save(new JobVerification(job, Instant.now(), Job.JobStatus.ACTIVE, 200,
                isNew ? "Created on ingestion" : "Refreshed on ingestion"));
        return isNew;
    }

    private Company findOrCreateCompany(String slugOrName) {
        String normalized = TextNormalizer.normalize(slugOrName);
        return companyRepository.findByNormalizedName(normalized)
                .orElseGet(() -> companyRepository.save(new Company(displayName(slugOrName), normalized)));
    }

    /** ATS boards are keyed by slug ("acme-labs"), so turn it into "Acme Labs". */
    private String displayName(String slug) {
        return java.util.Arrays.stream(slug.trim().split("[-_\\s]+"))
                .filter(part -> !part.isEmpty())
                .map(part -> Character.toUpperCase(part.charAt(0)) + part.substring(1))
                .collect(Collectors.joining(" "));
    }

    private Job.WorkMode inferWorkMode(String location) {
        if (location == null) return null;
        String l = location.toLowerCase(Locale.ROOT);
        if (l.contains("remote")) return Job.WorkMode.REMOTE;
        if (l.contains("hybrid")) return Job.WorkMode.HYBRID;
        return Job.WorkMode.ONSITE;
    }

    /**
     * Keyword tagging: a whole-word, case-insensitive match of each catalogued
     * skill name against the title and description. It is deliberately
     * simple, and it only adds tags, so a re-ingest never removes a tag that
     * was set by hand.
     */
    private void tagSkillsByKeyword(Job job) {
        String haystack = job.getTitle() + " " + (job.getDescription() == null ? "" : job.getDescription());
        Set<UUID> alreadyTagged = jobSkillRepository.findWithSkillByJobId(job.getId()).stream()
                .map(js -> js.getSkill().getId())
                .collect(Collectors.toSet());

        List<Skill> allSkills = skillRepository.findAll();
        for (Skill skill : allSkills) {
            if (alreadyTagged.contains(skill.getId())) continue;
            if (containsWord(haystack, skill.getName())) {
                jobSkillRepository.save(new JobSkill(job, skill));
            }
        }
    }

    static boolean containsWord(String haystack, String word) {
        // \b doesn't work next to symbols like "+" or "#", so use look-arounds
        // on "not a letter or digit" instead. This matches "C++" and "Node.js" correctly.
        Pattern pattern = Pattern.compile("(?i)(?<![\\p{Alnum}])" + Pattern.quote(word) + "(?![\\p{Alnum}])");
        return pattern.matcher(haystack).find();
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

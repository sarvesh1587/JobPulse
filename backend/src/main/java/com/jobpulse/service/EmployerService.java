package com.jobpulse.service;

import com.jobpulse.dto.common.PageResponse;
import com.jobpulse.dto.employer.EmployerDtos.ApplicantSummary;
import com.jobpulse.dto.employer.EmployerDtos.CompanyUpdateRequest;
import com.jobpulse.dto.employer.EmployerDtos.EmployerApplicationResponse;
import com.jobpulse.dto.employer.EmployerDtos.EmployerCompanyResponse;
import com.jobpulse.dto.employer.EmployerDtos.EmployerJobResponse;
import com.jobpulse.dto.employer.EmployerDtos.EmployerOverviewResponse;
import com.jobpulse.dto.employer.EmployerDtos.JobUpsertRequest;
import com.jobpulse.dto.employer.EmployerDtos.StatusUpdateRequest;
import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.CandidateProfile;
import com.jobpulse.entity.Company;
import com.jobpulse.entity.Job;
import com.jobpulse.entity.JobApplication;
import com.jobpulse.entity.JobSkill;
import com.jobpulse.entity.Skill;
import com.jobpulse.exception.BusinessRuleException;
import com.jobpulse.exception.DuplicateResourceException;
import com.jobpulse.exception.ResourceNotFoundException;
import com.jobpulse.mapper.ApplicationMapper;
import com.jobpulse.repository.CandidateProfileRepository;
import com.jobpulse.repository.CandidateSkillRepository;
import com.jobpulse.repository.CompanyRepository;
import com.jobpulse.repository.JobApplicationRepository;
import com.jobpulse.repository.JobRepository;
import com.jobpulse.repository.JobSkillRepository;
import com.jobpulse.repository.UserRepository;
import com.jobpulse.repository.projection.Projections.JobApplicationCount;
import com.jobpulse.repository.projection.Projections.JobSkillName;
import com.jobpulse.repository.projection.Projections.ProfileSkillName;
import com.jobpulse.repository.projection.Projections.StatusCount;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Instant;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * The employer workspace. The URL rules already ensure the caller has the
 * EMPLOYER role. Every method here also checks ownership: the job or
 * application must belong to the caller's own company. Anything else is
 * reported as not found, so employers can't discover other companies' ids.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmployerService {

    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final JobSkillRepository jobSkillRepository;
    private final JobApplicationRepository applicationRepository;
    private final CandidateProfileRepository profileRepository;
    private final CandidateSkillRepository candidateSkillRepository;
    private final UserRepository userRepository;
    private final SkillCatalogService skillCatalog;
    private final ApplicationWorkflow workflow;
    private final Clock clock;

    // ------------------------------------------------------------- company

    @Transactional(readOnly = true)
    public EmployerCompanyResponse getCompany(UUID employerId) {
        return toCompanyResponse(ownedCompany(employerId));
    }

    @Transactional
    public EmployerCompanyResponse updateCompany(UUID employerId, CompanyUpdateRequest request) {
        Company company = ownedCompany(employerId);
        String normalized = TextNormalizer.normalize(request.name());
        if (!normalized.equals(company.getNormalizedName()) && companyRepository.existsByNormalizedName(normalized)) {
            throw new DuplicateResourceException("Another company already uses this name.");
        }
        company.setName(request.name().trim());
        company.setNormalizedName(normalized);
        company.setWebsite(TextNormalizer.clean(request.website()));
        company.setHeadquarters(TextNormalizer.clean(request.headquarters()));
        company.setDescription(TextNormalizer.clean(request.description()));
        return toCompanyResponse(company);
    }

    @Transactional(readOnly = true)
    public EmployerOverviewResponse overview(UUID employerId) {
        Company company = ownedCompany(employerId);
        List<Job> jobs = jobRepository.findByCompanyId(company.getId(), PageRequest.of(0, 1000)).getContent();
        long active = jobs.stream().filter(Job::isOpen).count();
        Map<ApplicationStatus, Long> byStatus = new EnumMap<>(ApplicationStatus.class);
        for (ApplicationStatus s : ApplicationStatus.values()) byStatus.put(s, 0L);
        for (StatusCount row : applicationRepository.countByStatusForCompany(company.getId())) {
            byStatus.put(row.status(), row.count());
        }
        return new EmployerOverviewResponse(active, jobs.size() - active, byStatus);
    }

    // ---------------------------------------------------------------- jobs

    @Transactional(readOnly = true)
    public PageResponse<EmployerJobResponse> listJobs(UUID employerId, int page, int size) {
        Company company = ownedCompany(employerId);
        Page<Job> jobs = jobRepository.findByCompanyId(company.getId(),
                PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt")));
        List<UUID> ids = jobs.getContent().stream().map(Job::getId).toList();
        Map<UUID, Long> counts = ids.isEmpty() ? Map.of() : applicationRepository.countActiveByJobIds(ids).stream()
                .collect(Collectors.toMap(JobApplicationCount::jobId, JobApplicationCount::count));
        Map<UUID, List<String>> skills = ids.isEmpty() ? Map.of() : jobSkillRepository.findSkillNamesByJobIds(ids).stream()
                .collect(Collectors.groupingBy(JobSkillName::jobId,
                        Collectors.mapping(JobSkillName::skillName, Collectors.toList())));
        return PageResponse.of(jobs, jobs.getContent().stream()
                .map(job -> toJobResponse(job, skills.getOrDefault(job.getId(), List.of()), counts.getOrDefault(job.getId(), 0L)))
                .toList());
    }

    @Transactional(readOnly = true)
    public EmployerJobResponse getJob(UUID employerId, UUID jobId) {
        Job job = ownedJob(employerId, jobId);
        return toJobResponse(job, skillNames(job), countApplications(job));
    }

    @Transactional
    public EmployerJobResponse createJob(UUID employerId, JobUpsertRequest request) {
        Company company = ownedCompany(employerId);
        Job job = new Job();
        job.setOrigin(Job.Origin.EMPLOYER);
        job.setCompany(company);
        job.setPostedBy(userRepository.getReferenceById(employerId));
        job.setStatus(Job.JobStatus.ACTIVE);
        job.setPostedAt(Instant.now(clock));
        job.setLastVerifiedAt(Instant.now(clock));
        applyFields(job, request);
        job = jobRepository.save(job);
        replaceSkills(job, skillCatalog.resolve(request.skills()));
        log.info("Employer {} posted job {}", employerId, job.getId());
        return toJobResponse(job, skillNames(job), 0);
    }

    @Transactional
    public EmployerJobResponse updateJob(UUID employerId, UUID jobId, JobUpsertRequest request) {
        Job job = ownedJob(employerId, jobId);
        applyFields(job, request);
        replaceSkills(job, skillCatalog.resolve(request.skills()));
        return toJobResponse(job, skillNames(job), countApplications(job));
    }

    @Transactional
    public EmployerJobResponse setJobOpen(UUID employerId, UUID jobId, boolean open) {
        Job job = ownedJob(employerId, jobId);
        job.setStatus(open ? Job.JobStatus.ACTIVE : Job.JobStatus.CLOSED);
        if (open) job.setLastVerifiedAt(Instant.now(clock));
        log.info("Employer {} {} job {}", employerId, open ? "reopened" : "closed", jobId);
        return toJobResponse(job, skillNames(job), countApplications(job));
    }

    // -------------------------------------------------------- applications

    @Transactional(readOnly = true)
    public PageResponse<EmployerApplicationResponse> listApplications(UUID employerId, UUID jobId,
                                                                      ApplicationStatus status, int page, int size) {
        Job job = ownedJob(employerId, jobId);
        Page<JobApplication> applications = applicationRepository.findForJob(job.getId(), status, PageRequest.of(page, size));
        Map<UUID, ApplicantSummary> applicants = applicantSummaries(
                applications.getContent().stream().map(a -> a.getUser().getId()).collect(Collectors.toSet()));
        return PageResponse.of(applications, applications.getContent().stream()
                .map(a -> toEmployerResponse(a, job, applicants.get(a.getUser().getId()), false))
                .toList());
    }

    @Transactional(readOnly = true)
    public EmployerApplicationResponse getApplication(UUID employerId, UUID applicationId) {
        JobApplication application = ownedApplication(employerId, applicationId);
        ApplicantSummary applicant = applicantSummaries(Set.of(application.getUser().getId())).get(application.getUser().getId());
        return toEmployerResponse(application, application.getJob(), applicant, true);
    }

    @Transactional
    public EmployerApplicationResponse updateApplicationStatus(UUID employerId, UUID applicationId, StatusUpdateRequest request) {
        JobApplication application = ownedApplication(employerId, applicationId);
        if (request.status() == ApplicationStatus.WITHDRAWN) {
            throw new BusinessRuleException("Only the candidate can withdraw an application.");
        }
        workflow.transition(application, request.status(), request.interviewAt(),
                userRepository.getReferenceById(employerId), TextNormalizer.clean(request.note()));
        applicationRepository.saveAndFlush(application);
        log.info("Employer {} moved application {} to {}", employerId, applicationId, application.getStatus());

        ApplicantSummary applicant = applicantSummaries(Set.of(application.getUser().getId())).get(application.getUser().getId());
        return toEmployerResponse(application, application.getJob(), applicant, true);
    }

    // ------------------------------------------------------------- helpers

    private Company ownedCompany(UUID employerId) {
        return companyRepository.findByOwnerId(employerId)
                .orElseThrow(() -> new ResourceNotFoundException("No company is linked to this employer account."));
    }

    private Job ownedJob(UUID employerId, UUID jobId) {
        return jobRepository.findWithCompanyById(jobId)
                .filter(job -> job.getCompany().isOwnedBy(employerId))
                .orElseThrow(() -> new ResourceNotFoundException("Job not found."));
    }

    private JobApplication ownedApplication(UUID employerId, UUID applicationId) {
        return applicationRepository.findWithDetailsById(applicationId)
                .filter(a -> a.getJob().getCompany().isOwnedBy(employerId))
                .orElseThrow(() -> new ResourceNotFoundException("Application not found."));
    }

    private void applyFields(Job job, JobUpsertRequest r) {
        job.setTitle(r.title().trim());
        job.setNormalizedTitle(TextNormalizer.normalize(r.title()));
        job.setDescription(r.description().trim());
        job.setLocation(r.location().trim());
        job.setNormalizedLocation(TextNormalizer.normalize(r.location()));
        job.setWorkMode(r.workMode());
        job.setEmploymentType(r.employmentType());
        job.setExperienceLevel(r.experienceLevel());
        job.setSalaryMin(r.salaryMin());
        job.setSalaryMax(r.salaryMax());
        job.setSalaryCurrency(r.salaryMin() == null && r.salaryMax() == null ? null : r.salaryCurrency());
    }

    /** Same reconcile-don't-recreate approach as profile skills (see ProfileService). */
    private void replaceSkills(Job job, List<Skill> desired) {
        Set<UUID> desiredIds = desired.stream().map(Skill::getId).collect(Collectors.toSet());
        List<JobSkill> current = jobSkillRepository.findWithSkillByJobId(job.getId());
        current.stream().filter(js -> !desiredIds.contains(js.getSkill().getId())).forEach(jobSkillRepository::delete);
        Set<UUID> present = current.stream().map(js -> js.getSkill().getId()).collect(Collectors.toSet());
        desired.stream().filter(s -> !present.contains(s.getId()))
                .forEach(skill -> jobSkillRepository.save(new JobSkill(job, skill)));
    }

    private List<String> skillNames(Job job) {
        return jobSkillRepository.findSkillNamesByJobIds(List.of(job.getId())).stream()
                .map(JobSkillName::skillName).toList();
    }

    private long countApplications(Job job) {
        return applicationRepository.countActiveByJobIds(List.of(job.getId())).stream()
                .mapToLong(JobApplicationCount::count).sum();
    }

    /** Applicant cards for a page: one query for profiles and one for skills, however many applicants. */
    private Map<UUID, ApplicantSummary> applicantSummaries(Set<UUID> userIds) {
        if (userIds.isEmpty()) return Map.of();
        List<CandidateProfile> profiles = profileRepository.findAllByUserIds(userIds);
        Map<UUID, List<String>> skillsByProfile = candidateSkillRepository
                .findSkillNamesByProfileIds(profiles.stream().map(CandidateProfile::getId).toList()).stream()
                .collect(Collectors.groupingBy(ProfileSkillName::profileId,
                        Collectors.mapping(ProfileSkillName::skillName, Collectors.toList())));
        return profiles.stream().collect(Collectors.toMap(p -> p.getUser().getId(), p -> new ApplicantSummary(
                p.getUser().getId(), p.getUser().getFullName(), p.getUser().getEmail(), p.getHeadline(),
                p.getLocation(), p.getExperienceYears(), p.getEducationLevel(), p.getGraduationYear(),
                skillsByProfile.getOrDefault(p.getId(), List.of())), (a, b) -> a, HashMap::new));
    }

    private EmployerApplicationResponse toEmployerResponse(JobApplication a, Job job, ApplicantSummary applicant,
                                                           boolean withHistory) {
        List<ApplicationStatus> allowed = a.getStatus().allowedNext().stream()
                .filter(s -> s != ApplicationStatus.WITHDRAWN).toList();
        return new EmployerApplicationResponse(a.getId(), job.getId(), job.getTitle(), applicant, a.getStatus(),
                a.getAppliedAt(), a.getInterviewAt(), a.getCoverLetter(), allowed,
                withHistory ? ApplicationMapper.history(a, a.getUser().getId()) : List.of());
    }

    private EmployerJobResponse toJobResponse(Job job, List<String> skills, long applicationCount) {
        return new EmployerJobResponse(job.getId(), job.getTitle(), job.getLocation(), job.getWorkMode(),
                job.getEmploymentType(), job.getExperienceLevel(), job.getSalaryMin(), job.getSalaryMax(),
                job.getSalaryCurrency(), job.getDescription(), skills, job.getStatus(), job.getPostedAt(),
                applicationCount);
    }

    private EmployerCompanyResponse toCompanyResponse(Company c) {
        return new EmployerCompanyResponse(c.getId(), c.getName(), c.getWebsite(), c.getHeadquarters(), c.getDescription());
    }
}

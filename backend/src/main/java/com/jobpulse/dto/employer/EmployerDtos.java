package com.jobpulse.dto.employer;

import com.jobpulse.dto.application.ApplicationDtos.StatusChangeResponse;
import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.Job;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/** Employer workspace bodies (/api/employer). */
public final class EmployerDtos {

    private EmployerDtos() {}

    public record CompanyUpdateRequest(
            @NotBlank @Size(max = 200) String name,
            @Size(max = 500) @Pattern(regexp = "^$|^https?://.+", message = "must start with http:// or https://") String website,
            @Size(max = 200) String headquarters,
            @Size(max = 4000) String description
    ) {}

    public record EmployerCompanyResponse(
            UUID id,
            String name,
            String website,
            String headquarters,
            String description
    ) {}

    public record JobUpsertRequest(
            @NotBlank @Size(max = 300) String title,
            @NotBlank @Size(min = 50, max = 20000, message = "must be 50 to 20,000 characters") String description,
            @NotBlank @Size(max = 200) String location,
            @NotNull Job.WorkMode workMode,
            @NotNull Job.EmploymentType employmentType,
            @NotNull Job.ExperienceLevel experienceLevel,
            @DecimalMin("0") BigDecimal salaryMin,
            @DecimalMin("0") BigDecimal salaryMax,
            @Pattern(regexp = "^[A-Z]{3}$", message = "must be a 3-letter currency code like INR") String salaryCurrency,
            @NotNull @Size(max = 20) List<@NotBlank @Size(max = 60) String> skills
    ) {
        @AssertTrue(message = "minimum salary can't exceed maximum")
        public boolean isSalaryRangeValid() {
            return salaryMin == null || salaryMax == null || salaryMin.compareTo(salaryMax) <= 0;
        }

        @AssertTrue(message = "currency is required when a salary is given")
        public boolean isCurrencyPresentWithSalary() {
            return (salaryMin == null && salaryMax == null) || salaryCurrency != null;
        }
    }

    public record EmployerJobResponse(
            UUID id,
            String title,
            String location,
            Job.WorkMode workMode,
            Job.EmploymentType employmentType,
            Job.ExperienceLevel experienceLevel,
            BigDecimal salaryMin,
            BigDecimal salaryMax,
            String salaryCurrency,
            String description,
            List<String> skills,
            Job.JobStatus status,
            Instant postedAt,
            long applicationCount
    ) {}

    public record EmployerOverviewResponse(
            long activeJobs,
            long closedJobs,
            Map<ApplicationStatus, Long> applicationsByStatus
    ) {}

    /** What an employer sees about an applicant. The candidate's private notes are never included. */
    public record ApplicantSummary(
            UUID userId,
            String fullName,
            String email,
            String headline,
            String location,
            BigDecimal experienceYears,
            String educationLevel,
            Integer graduationYear,
            List<String> skills
    ) {}

    public record EmployerApplicationResponse(
            UUID id,
            UUID jobId,
            String jobTitle,
            ApplicantSummary applicant,
            ApplicationStatus status,
            Instant appliedAt,
            Instant interviewAt,
            String coverLetter,
            List<ApplicationStatus> allowedNextStatuses,
            List<StatusChangeResponse> history
    ) {}

    public record StatusUpdateRequest(
            @NotNull ApplicationStatus status,
            @Future Instant interviewAt,
            @Size(max = 500) String note
    ) {}
}

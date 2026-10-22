package com.jobpulse.dto.profile;

import jakarta.validation.Valid;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

/** Candidate profile bodies (/api/profile). */
public final class ProfileDtos {

    private ProfileDtos() {}

    /** id is null for a new entry and set for an existing one being edited. */
    public record EducationDto(
            UUID id,
            @NotBlank @Size(max = 200) String institution,
            @NotBlank @Size(max = 150) String degree,
            @Size(max = 150) String fieldOfStudy,
            @Min(1950) @Max(2100) Integer startYear,
            @Min(1950) @Max(2100) Integer endYear
    ) {
        @AssertTrue(message = "end year can't be before start year")
        public boolean isYearRangeValid() {
            return startYear == null || endYear == null || startYear <= endYear;
        }
    }

    public record ExperienceDto(
            UUID id,
            @NotBlank @Size(max = 200) String companyName,
            @NotBlank @Size(max = 150) String title,
            @NotNull @PastOrPresent LocalDate startDate,
            LocalDate endDate,
            @Size(max = 3000) String description
    ) {
        @AssertTrue(message = "end date can't be before start date")
        public boolean isDateRangeValid() {
            return startDate == null || endDate == null || !endDate.isBefore(startDate);
        }
    }

    /**
     * Full replacement of the editable profile (PUT semantics). Lists are
     * reconciled by id: new entries are inserted, changed ones updated, and
     * missing ones deleted.
     */
    public record ProfileRequest(
            @NotBlank @Size(max = 120) String fullName,
            @Size(max = 160) String headline,
            @Size(max = 120) String location,
            @Size(max = 50) String educationLevel,
            @Min(1950) @Max(2100) Integer graduationYear,
            @NotNull @DecimalMin("0") @DecimalMax("60") BigDecimal experienceYears,
            @Size(max = 2000) String summary,
            @NotNull @Size(max = 10) List<@NotBlank @Size(max = 80) String> preferredRoles,
            @NotNull @Size(max = 50) List<@NotBlank @Size(max = 60) String> skills,
            @NotNull @Size(max = 10) List<@Valid EducationDto> educations,
            @NotNull @Size(max = 20) List<@Valid ExperienceDto> experiences
    ) {}

    public record ResumeInfo(UUID id, String fileName, Instant uploadedAt) {}

    public record Completeness(int percent, List<String> missing) {}

    public record ProfileResponse(
            UUID userId,
            String fullName,
            String email,
            String headline,
            String location,
            String educationLevel,
            Integer graduationYear,
            BigDecimal experienceYears,
            String summary,
            List<String> preferredRoles,
            List<String> skills,
            List<EducationDto> educations,
            List<ExperienceDto> experiences,
            ResumeInfo resume,
            Completeness completeness
    ) {}
}

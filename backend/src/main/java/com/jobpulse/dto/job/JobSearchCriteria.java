package com.jobpulse.dto.job;

import com.jobpulse.entity.Job;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Query parameters for GET /api/jobs, bound by Spring MVC straight into this
 * record. Repeated parameters become lists, e.g. ?type=INTERNSHIP&type=FULL_TIME.
 */
public record JobSearchCriteria(
        @Size(max = 100) String q,
        @Size(max = 100) String location,
        List<Job.EmploymentType> type,
        List<Job.ExperienceLevel> level,
        List<Job.WorkMode> mode,
        @PositiveOrZero BigDecimal minSalary,
        @Min(1) @Max(365) Integer postedWithinDays,
        UUID companyId,
        Sort sort,
        @Min(0) Integer page,
        @Min(1) @Max(50) Integer size
) {
    public enum Sort { RECENT, SALARY }

    public int pageOrDefault() {
        return page == null ? 0 : page;
    }

    public int sizeOrDefault() {
        return size == null ? 20 : size;
    }

    public Sort sortOrDefault() {
        return sort == null ? Sort.RECENT : sort;
    }
}

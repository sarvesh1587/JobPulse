package com.jobpulse.repository;

import com.jobpulse.dto.job.JobSearchCriteria;
import com.jobpulse.entity.Company;
import com.jobpulse.entity.Job;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Builds the job-search WHERE clause from whichever filters are present.
 * Every user value goes through the Criteria API as a bound parameter, never
 * concatenated into SQL, so search input can't inject SQL. LIKE wildcards
 * typed by the user (% and _) are escaped so they match literally.
 */
public final class JobSpecifications {

    private static final char LIKE_ESCAPE = '\\';

    private JobSpecifications() {}

    public static Specification<Job> searchActive(JobSearchCriteria c) {
        return (root, query, cb) -> {
            boolean isCountQuery = Long.class == query.getResultType() || long.class == query.getResultType();

            // Fetch the company with the jobs so mapping a page doesn't lazy-load
            // it row by row. A count query can't contain a fetch, so it gets a plain join.
            // Hibernate's fetch object is also a Join, which is what lets us filter on it.
            Join<Job, Company> company = isCountQuery
                    ? root.join("company", JoinType.INNER)
                    : (Join<Job, Company>) root.<Job, Company>fetch("company", JoinType.INNER);

            List<Predicate> predicates = new ArrayList<>();
            predicates.add(cb.equal(root.get("status"), Job.JobStatus.ACTIVE));

            if (hasText(c.q())) {
                String pattern = likePattern(c.q());
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("title")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(company.get("name")), pattern, LIKE_ESCAPE),
                        cb.like(cb.lower(root.get("description")), pattern, LIKE_ESCAPE)));
            }
            if (hasText(c.location())) {
                predicates.add(cb.like(root.get("normalizedLocation"), likePattern(c.location()), LIKE_ESCAPE));
            }
            if (c.type() != null && !c.type().isEmpty()) {
                predicates.add(root.get("employmentType").in(c.type()));
            }
            if (c.level() != null && !c.level().isEmpty()) {
                predicates.add(root.get("experienceLevel").in(c.level()));
            }
            if (c.mode() != null && !c.mode().isEmpty()) {
                predicates.add(root.get("workMode").in(c.mode()));
            }
            if (c.minSalary() != null) {
                // A role qualifies if the top of its range reaches the minimum.
                predicates.add(cb.greaterThanOrEqualTo(
                        cb.coalesce(root.get("salaryMax"), root.get("salaryMin")), c.minSalary()));
            }
            if (c.postedWithinDays() != null) {
                Instant since = Instant.now().minus(c.postedWithinDays(), ChronoUnit.DAYS);
                predicates.add(cb.greaterThanOrEqualTo(root.get("postedAt"), since));
            }
            if (c.companyId() != null) {
                predicates.add(cb.equal(company.get("id"), c.companyId()));
            }

            if (!isCountQuery) {
                applySort(c.sortOrDefault(), root, cb, query);
            }
            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    /**
     * Postgres sorts NULLs first in DESC order, which would put jobs with no
     * salary or posting date at the top. The CASE expression pushes them last.
     * The id is a final tie-breaker so pages are stable across requests.
     */
    private static void applySort(JobSearchCriteria.Sort sort, Root<Job> root, CriteriaBuilder cb,
                                  CriteriaQuery<?> query) {
        String field = sort == JobSearchCriteria.Sort.SALARY ? "salaryMax" : "postedAt";
        Expression<Integer> nullsLast = cb.<Integer>selectCase()
                .when(cb.isNull(root.get(field)), 1)
                .otherwise(0);
        query.orderBy(
                cb.asc(nullsLast),
                cb.desc(root.get(field)),
                cb.desc(root.get("createdAt")),
                cb.asc(root.get("id")));
    }

    static String likePattern(String raw) {
        String escaped = raw.trim().toLowerCase(Locale.ROOT)
                .replace("\\", "\\\\")
                .replace("%", "\\%")
                .replace("_", "\\_");
        return "%" + escaped + "%";
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

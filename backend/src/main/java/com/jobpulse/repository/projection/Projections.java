package com.jobpulse.repository.projection;

import com.jobpulse.entity.ApplicationStatus;

import java.util.UUID;

/**
 * Small read-only rows returned by JPQL constructor expressions
 * ("select new ...(...)"). They let one query fetch exactly the columns a
 * page needs for many parent rows at once, which is how the services avoid
 * N+1 queries without loading whole entity graphs.
 */
public final class Projections {

    private Projections() {}

    public record JobSkillName(UUID jobId, String skillName) {}

    public record ProfileSkillName(UUID profileId, String skillName) {}

    public record StatusCount(ApplicationStatus status, long count) {}

    public record JobApplicationCount(UUID jobId, long count) {}

    public record CompanyListRow(UUID id, String name, String headquarters, String website,
                                 long openRoles, long internships, long entryLevel) {}
}

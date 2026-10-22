package com.jobpulse.repository;

import com.jobpulse.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * JpaSpecificationExecutor gives findAll(Specification, Pageable), which the
 * job search uses to build its WHERE clause from whichever filters are set
 * (see JobSpecifications).
 */
public interface JobRepository extends JpaRepository<Job, UUID>, JpaSpecificationExecutor<Job> {

    Optional<Job> findByJobSourceIdAndExternalId(UUID jobSourceId, String externalId);

    Optional<Job> findFirstByCompanyIdAndNormalizedTitleAndNormalizedLocation(
            UUID companyId, String normalizedTitle, String normalizedLocation);

    @EntityGraph(attributePaths = "company")
    @Query("select j from Job j where j.id = :id")
    Optional<Job> findWithCompanyById(@Param("id") UUID id);

    @EntityGraph(attributePaths = "company")
    List<Job> findByIdIn(Collection<UUID> ids);

    @EntityGraph(attributePaths = "company")
    List<Job> findByCompanyIdAndStatusOrderByPostedAtDesc(UUID companyId, Job.JobStatus status);

    @EntityGraph(attributePaths = "company")
    List<Job> findTop6ByStatusOrderByPostedAtDesc(Job.JobStatus status);

    @EntityGraph(attributePaths = "company")
    Page<Job> findByCompanyId(UUID companyId, Pageable pageable);

    long countByStatus(Job.JobStatus status);

    /**
     * Ranks active jobs by how many of the given skills they ask for, then by
     * recency. Only ids come back, so the caller loads the page of jobs in a
     * second query with their companies fetched.
     */
    @Query("""
            select j.id from JobSkill js join js.job j
            where js.skill.id in :skillIds and j.status = :status
            group by j.id, j.postedAt
            order by count(js.id) desc, j.postedAt desc
            """)
    List<UUID> findIdsRankedBySkillOverlap(@Param("skillIds") Collection<UUID> skillIds,
                                           @Param("status") Job.JobStatus status,
                                           Pageable pageable);

    /** Admin listing search. pattern is never null. */
    @EntityGraph(attributePaths = "company")
    @Query(value = """
            select j from Job j
            where lower(j.title) like :pattern or lower(j.company.name) like :pattern
            """,
            countQuery = """
            select count(j) from Job j
            where lower(j.title) like :pattern or lower(j.company.name) like :pattern
            """)
    Page<Job> searchForAdmin(@Param("pattern") String pattern, Pageable pageable);
}

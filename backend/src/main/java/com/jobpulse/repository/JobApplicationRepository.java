package com.jobpulse.repository;

import com.jobpulse.entity.ApplicationStatus;
import com.jobpulse.entity.JobApplication;
import com.jobpulse.repository.projection.Projections.JobApplicationCount;
import com.jobpulse.repository.projection.Projections.StatusCount;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {

    Optional<JobApplication> findByUserIdAndJobId(UUID userId, UUID jobId);

    @Query("""
            select a from JobApplication a
            join fetch a.job j join fetch j.company join fetch a.user
            where a.id = :id
            """)
    Optional<JobApplication> findWithDetailsById(@Param("id") UUID id);

    /** A candidate's tracker. status may be null to mean "all". */
    @Query("""
            select a from JobApplication a join fetch a.job j join fetch j.company
            where a.user.id = :userId and (:status is null or a.status = :status)
            order by a.updatedAt desc
            """)
    List<JobApplication> findForCandidate(@Param("userId") UUID userId,
                                          @Param("status") ApplicationStatus status);

    @Query("""
            select new com.jobpulse.repository.projection.Projections$StatusCount(a.status, count(a))
            from JobApplication a where a.user.id = :userId group by a.status
            """)
    List<StatusCount> countByStatusForCandidate(@Param("userId") UUID userId);

    @Query("""
            select a from JobApplication a join fetch a.job j join fetch j.company
            where a.user.id = :userId and a.status = com.jobpulse.entity.ApplicationStatus.INTERVIEW
              and a.interviewAt >= :from
            order by a.interviewAt asc
            """)
    List<JobApplication> findUpcomingInterviews(@Param("userId") UUID userId, @Param("from") Instant from);

    /** Applicants for one job, with the candidate user fetched for display. */
    @Query(value = """
            select a from JobApplication a join fetch a.user
            where a.job.id = :jobId and (:status is null or a.status = :status)
            order by a.appliedAt desc
            """,
            countQuery = """
            select count(a) from JobApplication a
            where a.job.id = :jobId and (:status is null or a.status = :status)
            """)
    Page<JobApplication> findForJob(@Param("jobId") UUID jobId,
                                    @Param("status") ApplicationStatus status,
                                    Pageable pageable);

    @Query("""
            select new com.jobpulse.repository.projection.Projections$JobApplicationCount(a.job.id, count(a))
            from JobApplication a
            where a.job.id in :jobIds and a.status <> com.jobpulse.entity.ApplicationStatus.WITHDRAWN
            group by a.job.id
            """)
    List<JobApplicationCount> countActiveByJobIds(@Param("jobIds") Collection<UUID> jobIds);

    @Query("""
            select new com.jobpulse.repository.projection.Projections$StatusCount(a.status, count(a))
            from JobApplication a where a.job.company.id = :companyId group by a.status
            """)
    List<StatusCount> countByStatusForCompany(@Param("companyId") UUID companyId);
}

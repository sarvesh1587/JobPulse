package com.jobpulse.repository;

import com.jobpulse.entity.Job;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobRepository extends JpaRepository<Job, UUID> {

    Optional<Job> findByJobSourceIdAndExternalId(UUID jobSourceId, String externalId);

    Optional<Job> findFirstByCompanyIdAndNormalizedTitleAndNormalizedLocation(
            UUID companyId, String normalizedTitle, String normalizedLocation);

    List<Job> findByCompanyIdAndStatus(UUID companyId, Job.JobStatus status);

    long countByCompanyIdAndStatus(UUID companyId, Job.JobStatus status);

    @Query("""
        SELECT j FROM Job j
        WHERE j.status = 'ACTIVE'
          AND (:title IS NULL OR LOWER(j.normalizedTitle) LIKE LOWER(CONCAT('%', :title, '%')))
          AND (:location IS NULL OR LOWER(j.normalizedLocation) LIKE LOWER(CONCAT('%', :location, '%')))
        ORDER BY j.postedAt DESC
        """)
    Page<Job> search(@Param("title") String title, @Param("location") String location, Pageable pageable);
}

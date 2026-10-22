package com.jobpulse.repository;

import com.jobpulse.entity.SavedJob;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface SavedJobRepository extends JpaRepository<SavedJob, UUID> {

    Optional<SavedJob> findByUserIdAndJobId(UUID userId, UUID jobId);

    boolean existsByUserIdAndJobId(UUID userId, UUID jobId);

    long countByUserId(UUID userId);

    /** Which of these jobs has the user saved? One query for a whole result page. */
    @Query("select s.job.id from SavedJob s where s.user.id = :userId and s.job.id in :jobIds")
    Set<UUID> findSavedJobIds(@Param("userId") UUID userId, @Param("jobIds") Collection<UUID> jobIds);

    @Query(value = """
            select s from SavedJob s join fetch s.job j join fetch j.company
            where s.user.id = :userId
            order by s.createdAt desc
            """,
            countQuery = "select count(s) from SavedJob s where s.user.id = :userId")
    Page<SavedJob> findPageWithJob(@Param("userId") UUID userId, Pageable pageable);

    @Query("""
            select s from SavedJob s join fetch s.job j join fetch j.company
            where s.user.id = :userId
            order by s.createdAt desc
            """)
    List<SavedJob> findRecentWithJob(@Param("userId") UUID userId, Pageable pageable);
}

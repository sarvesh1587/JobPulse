package com.jobpulse.repository;

import com.jobpulse.entity.MatchScore;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface MatchScoreRepository extends JpaRepository<MatchScore, UUID> {

    Optional<MatchScore> findByUserIdAndJobId(UUID userId, UUID jobId);

    @Query("select m from MatchScore m where m.user.id = :userId and m.job.id in :jobIds")
    List<MatchScore> findForJobs(@Param("userId") UUID userId, @Param("jobIds") Collection<UUID> jobIds);

    /** Cached scores depend on the profile, so they're dropped whenever the profile changes. */
    @Modifying
    @Query("delete from MatchScore m where m.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}

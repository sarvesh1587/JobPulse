package com.jobpulse.repository;

import com.jobpulse.entity.MatchScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface MatchScoreRepository extends JpaRepository<MatchScore, UUID> {
    Optional<MatchScore> findByUserIdAndJobId(UUID userId, UUID jobId);
}

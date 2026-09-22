package com.jobpulse.repository;

import com.jobpulse.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SavedJobRepository extends JpaRepository<SavedJob, UUID> {
    List<SavedJob> findByUserId(UUID userId);
    Optional<SavedJob> findByUserIdAndJobId(UUID userId, UUID jobId);
    boolean existsByUserIdAndJobId(UUID userId, UUID jobId);
}

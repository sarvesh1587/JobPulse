package com.jobpulse.repository;

import com.jobpulse.entity.JobApplication;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface JobApplicationRepository extends JpaRepository<JobApplication, UUID> {
    List<JobApplication> findByUserIdOrderByUpdatedAtDesc(UUID userId);
    Optional<JobApplication> findByUserIdAndJobId(UUID userId, UUID jobId);
}

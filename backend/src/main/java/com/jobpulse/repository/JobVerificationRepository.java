package com.jobpulse.repository;

import com.jobpulse.entity.JobVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface JobVerificationRepository extends JpaRepository<JobVerification, UUID> {
}

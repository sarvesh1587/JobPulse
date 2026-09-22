package com.jobpulse.repository;

import com.jobpulse.entity.JobSourceEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface JobSourceRepository extends JpaRepository<JobSourceEntity, UUID> {
    Optional<JobSourceEntity> findByName(String name);
}

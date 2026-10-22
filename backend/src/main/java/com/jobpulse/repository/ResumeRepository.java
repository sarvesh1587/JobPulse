package com.jobpulse.repository;

import com.jobpulse.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface ResumeRepository extends JpaRepository<Resume, UUID> {

    Optional<Resume> findFirstByCandidateProfileIdOrderByCreatedAtDesc(UUID candidateProfileId);
}

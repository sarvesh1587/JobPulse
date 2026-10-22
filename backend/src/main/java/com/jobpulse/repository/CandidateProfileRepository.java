package com.jobpulse.repository;

import com.jobpulse.entity.CandidateProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface CandidateProfileRepository extends JpaRepository<CandidateProfile, UUID> {

    Optional<CandidateProfile> findByUserId(UUID userId);

    @Query("select p from CandidateProfile p join fetch p.user where p.user.id in :userIds")
    List<CandidateProfile> findAllByUserIds(@Param("userIds") Collection<UUID> userIds);
}

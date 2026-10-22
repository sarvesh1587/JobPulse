package com.jobpulse.repository;

import com.jobpulse.entity.CandidateSkill;
import com.jobpulse.repository.projection.Projections.ProfileSkillName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface CandidateSkillRepository extends JpaRepository<CandidateSkill, UUID> {

    @Query("select cs from CandidateSkill cs join fetch cs.skill where cs.candidateProfile.id = :profileId")
    List<CandidateSkill> findWithSkillByProfileId(@Param("profileId") UUID profileId);

    @Query("""
            select new com.jobpulse.repository.projection.Projections$ProfileSkillName(cs.candidateProfile.id, s.name)
            from CandidateSkill cs join cs.skill s
            where cs.candidateProfile.id in :profileIds
            order by s.name
            """)
    List<ProfileSkillName> findSkillNamesByProfileIds(@Param("profileIds") Collection<UUID> profileIds);
}

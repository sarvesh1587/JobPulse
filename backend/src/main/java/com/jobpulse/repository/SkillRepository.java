package com.jobpulse.repository;

import com.jobpulse.entity.Skill;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SkillRepository extends JpaRepository<Skill, UUID> {

    Optional<Skill> findByNameIgnoreCase(String name);

    @Query("select s from Skill s where lower(s.name) in :lowerNames")
    List<Skill> findByLowerNames(@Param("lowerNames") Collection<String> lowerNames);

    List<Skill> findTop15ByNameContainingIgnoreCaseOrderByNameAsc(String fragment);
}

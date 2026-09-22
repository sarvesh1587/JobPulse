package com.jobpulse.repository;

import com.jobpulse.entity.SkillGap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface SkillGapRepository extends JpaRepository<SkillGap, UUID> {

    List<SkillGap> findByUserIdOrderByPriorityAsc(UUID userId);

    @Modifying
    @Query("DELETE FROM SkillGap sg WHERE sg.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}

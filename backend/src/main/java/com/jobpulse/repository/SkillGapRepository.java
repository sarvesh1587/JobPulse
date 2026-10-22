package com.jobpulse.repository;

import com.jobpulse.entity.SkillGap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.UUID;

public interface SkillGapRepository extends JpaRepository<SkillGap, UUID> {

    @Modifying
    @Query("delete from SkillGap sg where sg.user.id = :userId")
    void deleteByUserId(@Param("userId") UUID userId);
}

package com.jobpulse.repository;

import com.jobpulse.entity.JobSkill;
import com.jobpulse.repository.projection.Projections.JobSkillName;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

public interface JobSkillRepository extends JpaRepository<JobSkill, UUID> {

    @Query("select js from JobSkill js join fetch js.skill where js.job.id = :jobId")
    List<JobSkill> findWithSkillByJobId(@Param("jobId") UUID jobId);

    /** Skill names for a whole page of jobs in one query. */
    @Query("""
            select new com.jobpulse.repository.projection.Projections$JobSkillName(js.job.id, s.name)
            from JobSkill js join js.skill s
            where js.job.id in :jobIds
            order by s.name
            """)
    List<JobSkillName> findSkillNamesByJobIds(@Param("jobIds") Collection<UUID> jobIds);
}

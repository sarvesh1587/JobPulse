package com.jobpulse.repository;

import com.jobpulse.entity.Company;
import com.jobpulse.repository.projection.Projections.CompanyListRow;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface CompanyRepository extends JpaRepository<Company, UUID> {

    Optional<Company> findByNormalizedName(String normalizedName);

    Optional<Company> findByOwnerId(UUID ownerId);

    boolean existsByNormalizedName(String normalizedName);

    /**
     * One aggregate query for the whole page instead of one count query per
     * company. The LEFT JOIN keeps companies with zero open roles in the list.
     */
    @Query(value = """
            select new com.jobpulse.repository.projection.Projections$CompanyListRow(
                c.id, c.name, c.headquarters, c.website,
                count(j.id),
                coalesce(sum(case when j.employmentType = com.jobpulse.entity.Job$EmploymentType.INTERNSHIP then 1 else 0 end), 0),
                coalesce(sum(case when j.experienceLevel = com.jobpulse.entity.Job$ExperienceLevel.ENTRY then 1 else 0 end), 0))
            from Company c
            left join Job j on j.company = c and j.status = com.jobpulse.entity.Job$JobStatus.ACTIVE
            where lower(c.name) like :pattern
            group by c.id, c.name, c.headquarters, c.website
            order by count(j.id) desc, c.name asc
            """,
            countQuery = "select count(c) from Company c where lower(c.name) like :pattern")
    Page<CompanyListRow> findListRows(@Param("pattern") String pattern, Pageable pageable);
}

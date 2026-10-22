package com.jobpulse.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToMany;
import jakarta.persistence.OneToOne;
import jakarta.persistence.OrderBy;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * One profile per candidate (one-to-one with User). Education, experience
 * and skills are owned children: cascade ALL + orphanRemoval means removing
 * an item from a list deletes its row when the transaction commits.
 */
@Getter
@Setter
@NoArgsConstructor
@Entity
@Table(name = "candidate_profiles")
public class CandidateProfile extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    private String headline;

    @Column(name = "education_level")
    private String educationLevel;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    private String location;

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "preferred_roles", columnDefinition = "text[]")
    private List<String> preferredRoles = new ArrayList<>();

    @Column(name = "experience_years", nullable = false)
    private BigDecimal experienceYears = BigDecimal.ZERO;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startYear DESC")
    private List<Education> educations = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("startDate DESC")
    private List<WorkExperience> experiences = new ArrayList<>();

    @OneToMany(mappedBy = "candidateProfile", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CandidateSkill> skills = new ArrayList<>();

    public CandidateProfile(User user) {
        this.user = user;
    }
}

package com.jobpulse.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@Entity
@Table(name = "candidate_profiles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CandidateProfile extends BaseEntity {

    @OneToOne
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "education_level")
    private String educationLevel;

    @Column(name = "graduation_year")
    private Integer graduationYear;

    private String location;

    @Column(name = "preferred_roles")
    private List<String> preferredRoles;

    @Column(name = "experience_years", nullable = false)
    @Builder.Default
    private BigDecimal experienceYears = BigDecimal.ZERO;

    private String summary;
}

package com.jobpulse.repository;

import com.jobpulse.entity.Role;
import com.jobpulse.entity.User;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    boolean existsByRole(Role role);

    long countByRole(Role role);

    /** pattern is always a non-null LIKE pattern ("%" means everything). */
    @Query("""
            select u from User u
            where (:role is null or u.role = :role)
              and (lower(u.email) like :pattern or lower(u.fullName) like :pattern)
            """)
    Page<User> search(@Param("role") Role role, @Param("pattern") String pattern, Pageable pageable);
}

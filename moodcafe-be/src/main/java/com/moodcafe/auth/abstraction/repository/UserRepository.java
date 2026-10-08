package com.moodcafe.auth.abstraction.repository;

import com.moodcafe.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);

    @EntityGraph(attributePaths = {"role"})
    List<User> findAllByRoleNameAndActiveTrue(String roleName);

    @org.springframework.data.jpa.repository.Query("SELECT u.role.name, COUNT(u) FROM User u GROUP BY u.role.name")
    List<Object[]> countGroupedByRoleName();
}
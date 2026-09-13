package com.moodcafe.auth.abstraction.repository;

import com.moodcafe.auth.entity.User;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findByEmail(String email);

    @EntityGraph(attributePaths = {"role"})
    Optional<User> findById(UUID id);

    boolean existsByEmail(String email);
}
package com.moodcafe.auth.abstraction.repository;

import com.moodcafe.auth.entity.RefreshToken;
import com.moodcafe.auth.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser_UserId(UUID userId);
    List<RefreshToken> findByUserAndRevokedFalse(User user);
}
package com.moodcafe.auth.abstraction.service;

import io.jsonwebtoken.Claims;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Date;
import java.util.List;
import java.util.function.Function;

public interface IJwtService {
    String generateToken(UserDetails userDetails);

    String extractUsername(String token);

    String extractJwtId(String token);

    Date extractExpiration(String token);

    List<String> extractRoles(String token);

    <T> T extractClaim(
            String token,
            Function<Claims, T> resolver
    );

    boolean isTokenValid(
            String token,
            UserDetails userDetails
    );
}
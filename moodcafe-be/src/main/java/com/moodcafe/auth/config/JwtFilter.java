package com.moodcafe.auth.config;


import com.moodcafe.auth.abstraction.cache.RedisTokenService;
import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.JwtService;
import com.moodcafe.auth.dto.user.CustomUserDetails;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.antlr.v4.runtime.misc.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.web.authentication.WebAuthenticationDetailsSource;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.JwtException;
import java.io.IOException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

@Slf4j
@RequiredArgsConstructor
public class JwtFilter extends OncePerRequestFilter {

    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final UserRepository userRepository;

    @Override
    protected void doFilterInternal(HttpServletRequest request,
                                    @NotNull HttpServletResponse response,
                                    @NotNull FilterChain filterChain)
            throws ServletException, IOException {

        String token = null;
        final String header = request.getHeader("Authorization");

        if (header != null && header.regionMatches(true, 0, "Bearer ", 0, 7)) {
            token = header.substring(7).trim();
        } else {
            // 2. Dự phòng: Kiểm tra trong Query Parameter (dành riêng cho kết nối SSE Stream)
            token = request.getParameter("token");
        }

        if (token == null || request.getServletPath().startsWith("/api/auth/login")
                || request.getServletPath().startsWith("/api/auth/refresh")
                || request.getServletPath().startsWith("/api/auth/logout")) {
            filterChain.doFilter(request, response);
            return;
        }

        String path = request.getServletPath();

        try {
            java.util.Date now = new java.util.Date();
            java.util.Date exp = jwtService.extractExpiration(token);
            long remainingSec = exp != null ? (exp.getTime() - now.getTime()) / 1000 : 0;
            log.debug("JWT Filter checking URI [{} {}]: now={}, exp={}, remaining={}s",
                    request.getMethod(), path, now, exp, remainingSec);

            String jti = jwtService.extractJwtId(token);
            if (redisTokenService.isBlacklisted(jti)) {
                log.warn("JWT Filter rejected revoked token for URI [{}]: jti={}", path, jti);
                sendUnauthorizedResponse(request, response, "Token has been revoked", "TOKEN_REVOKED");
                return;
            }
            String email = jwtService.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found with email: " + email));

            UserDetails userDetails = new CustomUserDetails(user);
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (email != null && authentication == null) {
                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken usernamePasswordAuthenticationToken = new UsernamePasswordAuthenticationToken(
                            userDetails, null, userDetails.getAuthorities());
                    usernamePasswordAuthenticationToken
                            .setDetails(new WebAuthenticationDetailsSource().buildDetails(request));
                    SecurityContextHolder.getContext().setAuthentication(usernamePasswordAuthenticationToken);

                    // Force Password Change Check
                    if (user.isRequirePasswordChange()) {
                        if (!path.equals("/api/auth/change-password") && !path.equals("/api/auth/logout")) {
                            response.setContentType("application/json;charset=UTF-8");
                            response.setStatus(HttpServletResponse.SC_FORBIDDEN);

                            ApiResponse<Void> apiResponse = ApiResponse.failed(
                                    HttpServletResponse.SC_FORBIDDEN,
                                    "Bạn bắt buộc phải đổi mật khẩu trước khi sử dụng hệ thống.",
                                    "PASSWORD_CHANGE_REQUIRED",
                                    request.getRequestURI());

                            ObjectMapper objectMapper = new ObjectMapper();
                            objectMapper.registerModule(new JavaTimeModule());
                            response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
                            return;
                        }
                    }
                } else {
                    log.warn("JWT Filter token invalid for user {} on URI [{}]: now={}, exp={}", email, path, now, exp);
                    sendUnauthorizedResponse(request, response, "Token is invalid or expired", "TOKEN_INVALID");
                    return;
                }
            }
        } catch (ExpiredJwtException e) {
            java.util.Date now = new java.util.Date();
            java.util.Date exp = e.getClaims() != null ? e.getClaims().getExpiration() : null;
            log.warn("JWT Filter token expired for URI [{}]: now={}, exp={}, error={}", request.getRequestURI(), now, exp, e.getMessage());
            sendUnauthorizedResponse(request, response, "Token has expired", "TOKEN_EXPIRED");
            return;
        } catch (JwtException e) {
            log.warn("JWT Filter invalid JWT for URI [{}]: {}", request.getRequestURI(), e.getMessage());
            sendUnauthorizedResponse(request, response, "Invalid authentication token", "TOKEN_INVALID");
            return;
        } catch (Exception e) {
            log.error("JWT Filter validation failed for URI [{}]: {}", request.getRequestURI(), e.getMessage(), e);
            sendUnauthorizedResponse(request, response, "Authentication failed: " + e.getMessage(), "UNAUTHORIZED");
            return;
        }

        filterChain.doFilter(request, response);
    }

    private void sendUnauthorizedResponse(HttpServletRequest request,
                                          HttpServletResponse response,
                                          String message,
                                          String errorCode) throws IOException {
        response.setContentType("application/json;charset=UTF-8");
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);

        ApiResponse<Void> apiResponse = ApiResponse.failed(
                HttpServletResponse.SC_UNAUTHORIZED,
                message,
                errorCode,
                request.getRequestURI());

        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}

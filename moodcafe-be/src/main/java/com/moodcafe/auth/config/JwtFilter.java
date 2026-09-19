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

        if (header != null && header.startsWith("Bearer ")) {
            token = header.substring(7);
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

            String jti = jwtService.extractJwtId(token);
            if (redisTokenService.isBlacklisted(jti)) {
                sendUnauthorizedResponse(request, response, "Token has been revoked", "TOKEN_REVOKED");
                return;
            }
            String email = jwtService.extractUsername(token);
            User user = userRepository.findByEmail(email)
                    .orElseThrow(() -> new RuntimeException("User not found"));

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
                    sendUnauthorizedResponse(request, response, "Token is invalid or expired", "TOKEN_INVALID");
                    return;
                }
            }
        } catch (ExpiredJwtException e) {
            log.warn("JWT token expired for URI {}: {}", request.getRequestURI(), e.getMessage());
            sendUnauthorizedResponse(request, response, "Token has expired", "TOKEN_EXPIRED");
            return;
        } catch (JwtException e) {
            log.warn("Invalid JWT token for URI {}: {}", request.getRequestURI(), e.getMessage());
            sendUnauthorizedResponse(request, response, "Invalid authentication token", "TOKEN_INVALID");
            return;
        } catch (Exception e) {
            log.error("JWT Filter validation failed for URI {}: {}", request.getRequestURI(), e.getMessage(), e);
            sendUnauthorizedResponse(request, response, "Authentication failed", "UNAUTHORIZED");
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

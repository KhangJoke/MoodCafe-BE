package com.moodcafe.notification.config;

import com.moodcafe.auth.abstraction.cache.RedisTokenService;
import com.moodcafe.auth.abstraction.service.JwtService;
import com.moodcafe.auth.abstraction.service.UserService;
import com.moodcafe.auth.dto.user.CustomUserDetails;
import com.moodcafe.auth.entity.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;

import java.security.Principal;

@Component
@RequiredArgsConstructor
@Slf4j
public class StompAuthChannelInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final RedisTokenService redisTokenService;
    private final UserService userService;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor = MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        StompCommand command = accessor.getCommand();

        if (StompCommand.CONNECT.equals(command)) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(command)) {
            handleSubscribe(accessor);
        } else if (StompCommand.DISCONNECT.equals(command)) {
            handleDisconnect(accessor);
        }

        return message;
    }

    private String extractToken(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader == null) {
            authHeader = accessor.getFirstNativeHeader("authorization");
        }
        if (authHeader == null && accessor.toNativeHeaderMap() != null) {
            for (var entry : accessor.toNativeHeaderMap().entrySet()) {
                if ("authorization".equalsIgnoreCase(entry.getKey()) && !entry.getValue().isEmpty()) {
                    authHeader = entry.getValue().get(0);
                    break;
                }
            }
        }
        if (authHeader == null) {
            authHeader = accessor.getFirstNativeHeader("passcode");
        }
        if (authHeader == null) {
            authHeader = accessor.getFirstNativeHeader("token");
        }

        if (authHeader != null && !authHeader.isBlank()) {
            authHeader = authHeader.trim();
            if (authHeader.regionMatches(true, 0, "Bearer ", 0, 7)) {
                return authHeader.substring(7).trim();
            }
            return authHeader;
        }

        return null;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String token = extractToken(accessor);
        if (token == null || token.isBlank()) {
            log.warn("STOMP CONNECT rejected: Missing or invalid Authorization/passcode header. Native headers present: {}",
                    accessor.toNativeHeaderMap() != null ? accessor.toNativeHeaderMap().keySet() : "null");
            throw new IllegalArgumentException("Missing or invalid Authorization header in STOMP CONNECT");
        }

        try {
            java.util.Date now = new java.util.Date();
            java.util.Date exp = jwtService.extractExpiration(token);
            long remainingSec = exp != null ? (exp.getTime() - now.getTime()) / 1000 : 0;
            log.info("STOMP CONNECT validating token: now={}, exp={}, remaining={}s", now, exp, remainingSec);

            String jti = jwtService.extractJwtId(token);
            if (redisTokenService.isBlacklisted(jti)) {
                log.warn("STOMP CONNECT rejected: Token is blacklisted (jti={})", jti);
                throw new IllegalArgumentException("Token is blacklisted");
            }

            String username = jwtService.extractUsername(token);
            if (username == null) {
                log.warn("STOMP CONNECT rejected: Username could not be extracted from token");
                throw new IllegalArgumentException("Invalid token: username missing");
            }

            User user = userService.getUserEntityByEmail(username);

            UserDetails userDetails = new CustomUserDetails(user);
            if (!jwtService.isTokenValid(token, userDetails)) {
                log.warn("STOMP CONNECT rejected: Token validation failed for user {} (now={}, exp={})", username, now, exp);
                throw new IllegalArgumentException("Invalid or expired token");
            }

            UsernamePasswordAuthenticationToken authentication =
                    new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());

            accessor.setUser(authentication);
            log.info("STOMP client connected successfully as user: {} (userId: {})", username, user.getUserId());
        } catch (Exception e) {
            log.error("STOMP CONNECT authentication error: {}", e.getMessage());
            throw new IllegalArgumentException("Authentication failed: " + e.getMessage(), e);
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();
        Principal principal = accessor.getUser();

        if (principal == null) {
            log.warn("STOMP SUBSCRIBE rejected: Connection is not authenticated for destination: {}", destination);
            throw new IllegalArgumentException("Authentication required to subscribe");
        }

        if (destination != null && destination.startsWith("/topic/notifications/")) {
            String requestedUserId = destination.substring("/topic/notifications/".length()).replaceAll("/+$", "").trim();
            String authenticatedUserId = null;

            if (principal instanceof Authentication auth && auth.getPrincipal() instanceof CustomUserDetails userDetails) {
                authenticatedUserId = userDetails.user().getUserId().toString();
            }

            if (authenticatedUserId == null || !authenticatedUserId.equalsIgnoreCase(requestedUserId)) {
                log.warn("STOMP SUBSCRIBE forbidden: Authenticated user {} attempted to subscribe to unauthorized destination {}",
                        authenticatedUserId, destination);
                throw new IllegalArgumentException("Forbidden: You cannot subscribe to another user's notifications");
            }

            log.info("User {} successfully subscribed to {}", authenticatedUserId, destination);
        }
    }

    private void handleDisconnect(StompHeaderAccessor accessor) {
        Principal principal = accessor.getUser();
        if (principal != null) {
            log.info("STOMP client disconnected: {}", principal.getName());
        }
    }
}

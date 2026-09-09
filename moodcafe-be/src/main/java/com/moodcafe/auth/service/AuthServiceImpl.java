package com.moodcafe.auth.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodcafe.auth.abstraction.repository.RoleRepository;
import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.IAuthService;
import com.moodcafe.auth.abstraction.service.IJwtService;
import com.moodcafe.auth.abstraction.service.IRefreshTokenService;
import com.moodcafe.auth.dto.auth.PendingUser;
import com.moodcafe.auth.dto.auth.request.ConfirmOtpRequest;
import com.moodcafe.auth.dto.auth.request.LoginRequest;
import com.moodcafe.auth.dto.auth.request.RefreshTokenRequest;
import com.moodcafe.auth.dto.auth.request.RegisterRequest;
import com.moodcafe.auth.dto.auth.request.ResetPasswordRequest;
import com.moodcafe.auth.dto.auth.request.SendOtpRequest;
import com.moodcafe.auth.dto.auth.response.AuthResponse;
import com.moodcafe.auth.dto.auth.response.ConfirmOtpResponse;
import com.moodcafe.auth.dto.auth.response.EmailActionResponse;
import com.moodcafe.auth.dto.user.response.UserResponse;
import com.moodcafe.auth.entity.Role;
import com.moodcafe.auth.entity.User;
import com.moodcafe.auth.mapper.UserMapper;
import com.moodcafe.auth.service.cache.OtpAttemptTracker;
import com.moodcafe.notification.abstraction.cache.IRedisOtpService;
import com.moodcafe.notification.abstraction.service.INotificationService;
import com.moodcafe.notification.dto.request.OtpNotificationRequest;
import com.moodcafe.notification.dto.request.OtpRequest;
import com.moodcafe.shared.enums.OtpType;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final String PENDING_REGISTER_PREFIX = "auth:register:";
    private static final long REGISTRATION_TTL_MINUTES = 5;

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;

    private final PasswordEncoder passwordEncoder;
    private final IJwtService jwtService;
    private final IRefreshTokenService refreshTokenService;

    private final UserMapper userMapper;

    private final INotificationService notificationService;
    private final IRedisOtpService redisOtpService;
    private final OtpAttemptTracker otpAttemptTracker;
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper;

    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public EmailActionResponse register(RegisterRequest request) {

        // 1. Check whether email already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_REGISTERED);
        }

        // 2. Generate a 6-digit OTP using SecureRandom
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));

        // 3. Create PendingUser (encode password before Redis caching for security)
        PendingUser pendingUser = new PendingUser(
                request.getEmail(),
                request.getUserName(),
                passwordEncoder.encode(request.getPassword())
        );

        // 4. Save PendingUser to Redis with 5-minute TTL
        savePendingUser(pendingUser, REGISTRATION_TTL_MINUTES);

        // 5. Send OTP via existing INotificationService flow
        OtpNotificationRequest notificationRequest = new OtpNotificationRequest(
                new OtpRequest(request.getEmail(), otp),
                UUID.randomUUID().toString(),
                OtpType.REGISTER
        );
        notificationService.otpNotificationHandler(notificationRequest);

        // 6. Return EmailActionResponse
        return EmailActionResponse.builder()
                .email(request.getEmail())
                .expiresInSeconds(REGISTRATION_TTL_MINUTES * 60)
                .message("Registration OTP has been sent to your email. Please verify within 5 minutes.")
                .build();
    }

    @Override
    @Transactional
    public ConfirmOtpResponse confirmOtp(ConfirmOtpRequest request) {
        String email = request.getEmail();
        String otp = request.getOtp();
        OtpType type = request.getType();

        // 1. Check lockout (brute-force prevention)
        if (otpAttemptTracker.isLockedOut(email)) {
            long remaining = otpAttemptTracker.getLockoutRemainingTime(email);
            throw new AppException(
                    ErrorCode.TOO_MANY_FAILED_ATTEMPTS,
                    "Too many failed attempts. Please try again after " + remaining + " seconds"
            );
        }

        // 2. Validate OTP from Redis
        boolean isValid = redisOtpService.validateOtp(type.name(), email, otp);
        if (!isValid) {
            int remaining = otpAttemptTracker.recordFailedAttempt(email);
            String msg = remaining > 0
                    ? "Wrong OTP code. " + remaining + " attempt(s) remaining."
                    : "Wrong OTP code. Account locked out for 15 minutes.";
            throw new AppException(ErrorCode.WRONG_OTP_CODE, msg);
        }

        // 3. Reset failed attempts on success
        otpAttemptTracker.resetAttempts(email);

        // 4. Handle REGISTER flow
        if (type == OtpType.REGISTER) {
            PendingUser pendingUser = getPendingUser(email);
            if (pendingUser == null) {
                throw new AppException(ErrorCode.OTP_EXPIRED, "Registration session expired. Please register again.");
            }

            if (userRepository.existsByEmail(email)) {
                deletePendingUser(email);
                throw new AppException(ErrorCode.EMAIL_ALREADY_REGISTERED);
            }

            Role role = roleRepository.findByName("CUSTOMER")
                    .orElseThrow(() -> new AppException(ErrorCode.ROLE_NOT_FOUND, "CUSTOMER role not found"));

            User user = User.builder()
                    .email(pendingUser.email())
                    .userName(pendingUser.userName())
                    .password(pendingUser.password()) // already encoded
                    .role(role)
                    .active(true)
                    .emailVerified(true)
                    .requirePasswordChange(false)
                    .build();

            User savedUser = userRepository.save(user);
            deletePendingUser(email);

            return ConfirmOtpResponse.builder()
                    .verified(true)
                    .message("User registration completed successfully")
                    .user(userMapper.toResponse(savedUser))
                    .build();

        // 5. Handle FORGET_PASSWORD flow
        } else if (type == OtpType.FORGET_PASSWORD) {
            String resetToken = UUID.randomUUID().toString();
            String resetKey = "password_reset:" + email;
            redisTemplate.opsForValue().set(resetKey, resetToken, 10, TimeUnit.MINUTES);

            return ConfirmOtpResponse.builder()
                    .verified(true)
                    .message("OTP verified successfully. Use the reset token to update your password.")
                    .resetToken(resetToken)
                    .build();
        }

        throw new AppException(ErrorCode.BAD_REQUEST, "Unsupported OTP type: " + type);
    }

    @Override
    public AuthResponse login(LoginRequest request) {

        // 1. Find user
        User user = userRepository
                .findByEmail(request.getEmail())
                .orElseThrow(() ->
                        new AppException(
                                ErrorCode.EMAIL_NOT_FOUND,
                                "Invalid email or password"
                        ));

        // 2. Check account
        if (!user.isActive()) {
            throw new AppException(ErrorCode.FORBIDDEN, "User is inactive");
        }

        // 3. Check password
        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new AppException(
                    ErrorCode.WRONG_PASSWORD,
                    "Invalid email or password"
            );
        }

        // 4. Generate access token
        UserDetails userDetails =
                createUserDetails(user);

        String accessToken =
                jwtService.generateToken(userDetails);

        // 5. Generate refresh token
        String refreshToken =
                refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(
                user,
                accessToken,
                refreshToken
        );
    }

    @Override
    public AuthResponse refresh(RefreshTokenRequest request) {

        // 1. Validate old refresh token
        var oldRefreshToken =
                refreshTokenService.validateRefreshToken(
                        request.getRefreshToken()
                );

        // 2. Revoke old refresh token
        refreshTokenService.revokeToken(
                request.getRefreshToken()
        );

        // 3. Get user
        User user = oldRefreshToken.getUser();

        if (!user.isActive()) {
            throw new AppException(ErrorCode.FORBIDDEN, "User is inactive");
        }

        // 4. Generate new access token
        UserDetails userDetails =
                createUserDetails(user);

        String newAccessToken =
                jwtService.generateToken(userDetails);

        // 5. Generate new refresh token
        String newRefreshToken =
                refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(
                user,
                newAccessToken,
                newRefreshToken
        );
    }

    @Override
    public void logout(String refreshToken) {

        try {
            refreshTokenService.revokeToken(refreshToken);
        } catch (Exception ignored) {
            // Logout should be idempotent
        }
    }

    @Override
    public void sendOtp(SendOtpRequest request) {
        String email = request.getEmail();
        OtpType type = request.getType();

        if (type == OtpType.FORGET_PASSWORD) {
            if (!userRepository.existsByEmail(email)) {
                throw new AppException(ErrorCode.EMAIL_NOT_FOUND, "No account found with this email");
            }
        } else if (type == OtpType.REGISTER) {
            User user = userRepository.findByEmail(email).orElse(null);
            if (user != null && user.isEmailVerified()) {
                throw new AppException(ErrorCode.BAD_REQUEST, "Email is already verified");
            }
        }

        sendOtpInternal(email, type);
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String email = request.getEmail();
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (request.getResetToken() != null && !request.getResetToken().isBlank()) {
            String resetKey = "password_reset:" + email;
            String storedToken = redisTemplate.opsForValue().get(resetKey);
            if (storedToken == null || !storedToken.equals(request.getResetToken())) {
                throw new AppException(ErrorCode.TOKEN_INVALID, "Invalid or expired password reset token");
            }
            redisTemplate.delete(resetKey);
        } else if (request.getOtp() != null && !request.getOtp().isBlank()) {
            if (otpAttemptTracker.isLockedOut(email)) {
                long remaining = otpAttemptTracker.getLockoutRemainingTime(email);
                throw new AppException(
                        ErrorCode.TOO_MANY_FAILED_ATTEMPTS,
                        "Too many failed attempts. Please try again after " + remaining + " seconds"
                );
            }
            boolean isValid = redisOtpService.validateOtp(OtpType.FORGET_PASSWORD.name(), email, request.getOtp());
            if (!isValid) {
                otpAttemptTracker.recordFailedAttempt(email);
                throw new AppException(ErrorCode.WRONG_OTP_CODE, "Invalid or expired OTP code");
            }
            otpAttemptTracker.resetAttempts(email);
        } else {
            throw new AppException(ErrorCode.BAD_REQUEST, "Either otp or resetToken must be provided");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setRequirePasswordChange(false);
        userRepository.save(user);

        try {
            refreshTokenService.revokeAllUserTokens(user);
        } catch (Exception ignored) {
        }
    }

    private void savePendingUser(PendingUser pendingUser, long ttlMinutes) {
        try {
            String json = objectMapper.writeValueAsString(pendingUser);
            String key = PENDING_REGISTER_PREFIX + pendingUser.email();
            redisTemplate.opsForValue().set(key, json, ttlMinutes, TimeUnit.MINUTES);
        } catch (JsonProcessingException e) {
            log.error("Failed to serialize PendingUser for email {}", pendingUser.email(), e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Failed to cache pending registration data");
        }
    }

    private PendingUser getPendingUser(String email) {
        try {
            String key = PENDING_REGISTER_PREFIX + email;
            String json = redisTemplate.opsForValue().get(key);
            if (json == null) {
                return null;
            }
            return objectMapper.readValue(json, PendingUser.class);
        } catch (JsonProcessingException e) {
            log.error("Failed to deserialize PendingUser for email {}", email, e);
            throw new AppException(ErrorCode.INTERNAL_ERROR, "Failed to read pending registration data");
        }
    }

    private void deletePendingUser(String email) {
        String key = PENDING_REGISTER_PREFIX + email;
        redisTemplate.delete(key);
    }

    private void sendOtpInternal(String email, OtpType otpType) {
        String otp = String.format("%06d", secureRandom.nextInt(1_000_000));
        OtpNotificationRequest notificationRequest = new OtpNotificationRequest(
                new OtpRequest(email, otp),
                UUID.randomUUID().toString(),
                otpType
        );
        notificationService.otpNotificationHandler(notificationRequest);
    }

    private UserDetails createUserDetails(User user) {

        return new org.springframework.security.core.userdetails.User(
                user.getEmail(),
                user.getPassword(),
                user.isActive(),
                true,
                true,
                true,
                List.of(
                        new SimpleGrantedAuthority(
                                "ROLE_" + user.getRole().getName()
                        )
                )
        );
    }

    private AuthResponse buildAuthResponse(
            User user,
            String accessToken,
            String refreshToken
    ) {

        return AuthResponse.builder()
                .user(userMapper.toResponse(user))
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(15 * 60)
                .build();
    }
}
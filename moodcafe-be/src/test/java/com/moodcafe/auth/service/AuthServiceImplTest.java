package com.moodcafe.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodcafe.auth.abstraction.repository.RoleRepository;
import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.JwtService;
import com.moodcafe.auth.abstraction.service.RefreshTokenService;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.abstraction.service.SocialAuthService;
import com.moodcafe.auth.dto.auth.SocialUserInfo;
import com.moodcafe.auth.dto.auth.request.SetupPasswordRequest;
import com.moodcafe.auth.dto.user.request.ChangePasswordRequest;
import com.moodcafe.auth.dto.auth.request.SocialLoginRequest;
import com.moodcafe.auth.dto.auth.response.AuthResponse;
import com.moodcafe.auth.dto.user.response.UserResponse;
import com.moodcafe.auth.entity.Role;
import com.moodcafe.auth.entity.User;
import com.moodcafe.auth.mapper.UserMapper;
import com.moodcafe.auth.service.cache.OtpAttemptTracker;
import com.moodcafe.notification.abstraction.cache.RedisOtpService;
import com.moodcafe.notification.abstraction.service.NotificationService;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private RoleRepository roleRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @Mock
    private JwtService jwtService;

    @Mock
    private RefreshTokenService refreshTokenService;

    @Mock
    private UserMapper userMapper;

    @Mock
    private RedisOtpService redisOtpService;

    @Mock
    private NotificationService notificationService;

    @Mock
    private RedisTemplate<String, String> redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ObjectMapper objectMapper;

    @Mock
    private OtpAttemptTracker otpAttemptTracker;

    @Mock
    private SocialAuthService googleAuthService;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private AuthServiceImpl authService;


    private User sampleUser;
    private Role customerRole;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "socialAuthServices", List.of(googleAuthService));

        customerRole = Role.builder()
                .roleId(UUID.randomUUID())
                .name("CUSTOMER")
                .build();


        sampleUser = User.builder()
                .userId(UUID.randomUUID())
                .email("social@moodcafe.vn")
                .fullName("Social User")
                .role(customerRole)
                .password("old_encoded_hash")
                .requirePasswordChange(true)
                .firstLogin(true)
                .active(true)
                .emailVerified(true)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("socialLogin - new user is created with requirePasswordChange=true")
    void socialLogin_NewUser_SetsRequirePasswordChangeTrue() {
        SocialLoginRequest request = SocialLoginRequest.builder()
                .provider("GOOGLE")
                .token("valid_google_token")
                .build();

        when(googleAuthService.supports("GOOGLE")).thenReturn(true);
        when(googleAuthService.verifyToken("valid_google_token")).thenReturn(
                SocialUserInfo.builder()
                        .provider("GOOGLE")
                        .providerId("google-12345")
                        .email("newgoogle@moodcafe.vn")
                        .fullName("New Google User")
                        .avatarUrl("https://lh3.googleusercontent.com/avatar.jpg")
                        .build()
        );

        when(userRepository.findByEmail("newgoogle@moodcafe.vn")).thenReturn(Optional.empty());
        when(roleRepository.findByName("CUSTOMER")).thenReturn(Optional.of(customerRole));
        when(passwordEncoder.encode(anyString())).thenReturn("random_uuid_encoded");

        User savedUser = User.builder()
                .userId(UUID.randomUUID())
                .email("newgoogle@moodcafe.vn")
                .fullName("New Google User")
                .avatarUrl("https://lh3.googleusercontent.com/avatar.jpg")
                .role(customerRole)
                .requirePasswordChange(true)
                .firstLogin(true)
                .active(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        UserResponse mappedResponse = UserResponse.builder()
                .userId(savedUser.getUserId())
                .email(savedUser.getEmail())
                .requirePasswordChange(true)
                .firstLogin(true)
                .build();
        when(userMapper.toResponse(savedUser)).thenReturn(mappedResponse);

        AuthResponse response = authService.socialLogin(request);

        assertThat(response).isNotNull();
        assertThat(response.getUser()).isNotNull();
        assertThat(response.isNeedsPasswordSetup()).isTrue();
        assertThat(response.getSetupToken()).isNotNull();
        assertThat(response.getAccessToken()).isNull();
    }

    @Test
    @DisplayName("setupPassword - valid setupToken sets password and returns AuthResponse")
    void setupPassword_ValidToken_ReturnsAuthResponse() {
        String setupToken = UUID.randomUUID().toString();
        UUID userId = sampleUser.getUserId();
        SetupPasswordRequest request = SetupPasswordRequest.builder()
                .setupToken(setupToken)
                .newPassword("myNewPassword123")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("setup_pwd:" + setupToken)).thenReturn(userId.toString());
        when(userRepository.findById(userId)).thenReturn(Optional.of(sampleUser));
        when(passwordEncoder.encode("myNewPassword123")).thenReturn("encoded_new_password");
        when(userRepository.save(sampleUser)).thenReturn(sampleUser);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("new_access_token");
        when(refreshTokenService.createRefreshToken(sampleUser)).thenReturn("new_refresh_token");

        UserResponse mappedUser = UserResponse.builder()
                .userId(userId)
                .email(sampleUser.getEmail())
                .requirePasswordChange(false)
                .build();
        when(userMapper.toResponse(sampleUser)).thenReturn(mappedUser);

        AuthResponse response = authService.setupPassword(request);

        assertThat(response).isNotNull();
        assertThat(response.getAccessToken()).isEqualTo("new_access_token");
        assertThat(response.getRefreshToken()).isEqualTo("new_refresh_token");
        assertThat(sampleUser.isRequirePasswordChange()).isFalse();
        assertThat(sampleUser.getPassword()).isEqualTo("encoded_new_password");
        verify(redisTemplate).delete("setup_pwd:" + setupToken);
    }

    @Test
    @DisplayName("setupPassword - expired or invalid setupToken throws TOKEN_INVALID")
    void setupPassword_InvalidToken_ThrowsTokenInvalid() {
        SetupPasswordRequest request = SetupPasswordRequest.builder()
                .setupToken("expired_token")
                .newPassword("myNewPassword123")
                .build();

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("setup_pwd:expired_token")).thenReturn(null);

        assertThatThrownBy(() -> authService.setupPassword(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.TOKEN_INVALID);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("socialLogin - unsupported provider throws BAD_REQUEST")
    void socialLogin_UnsupportedProvider_ThrowsBadRequest() {
        SocialLoginRequest request = SocialLoginRequest.builder()
                .provider("TIKTOK")
                .token("valid_token")
                .build();

        when(googleAuthService.supports("TIKTOK")).thenReturn(false);

        assertThatThrownBy(() -> authService.socialLogin(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);
    }

    @Test
    @DisplayName("changePassword - success updates password and revokes tokens")
    void changePassword_Success() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass123", "newPass123", "newPass123");

        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);
        sampleUser.setPassword("encoded_oldPass");
        when(passwordEncoder.matches("oldPass123", "encoded_oldPass")).thenReturn(true);
        when(passwordEncoder.encode("newPass123")).thenReturn("encoded_newPass");

        authService.changePassword(request);

        assertThat(sampleUser.getPassword()).isEqualTo("encoded_newPass");
        assertThat(sampleUser.isRequirePasswordChange()).isFalse();
        verify(userRepository).save(sampleUser);
        verify(refreshTokenService).revokeAllUserTokens(sampleUser);
    }

    @Test
    @DisplayName("changePassword - mismatch confirmation throws INVALID_INPUT")
    void changePassword_MismatchConfirm_ThrowsInvalidInput() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass123", "newPass123", "differentPass");

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_INPUT);

        verify(currentUserService, never()).getCurrentUser();
        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("changePassword - wrong current password throws WRONG_PASSWORD")
    void changePassword_WrongCurrentPassword_ThrowsWrongPassword() {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPass123", "newPass123");

        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);
        sampleUser.setPassword("encoded_realPass");
        when(passwordEncoder.matches("wrongPass", "encoded_realPass")).thenReturn(false);

        assertThatThrownBy(() -> authService.changePassword(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.WRONG_PASSWORD);

        verify(userRepository, never()).save(any());
    }
}


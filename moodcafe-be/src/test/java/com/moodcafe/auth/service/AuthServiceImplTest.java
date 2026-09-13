package com.moodcafe.auth.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.moodcafe.auth.abstraction.repository.RoleRepository;
import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.JwtService;
import com.moodcafe.auth.abstraction.service.RefreshTokenService;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.abstraction.service.SocialAuthService;
import com.moodcafe.auth.dto.auth.SocialUserInfo;
import com.moodcafe.auth.dto.auth.request.SetPasswordRequest;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

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
    private RedisTemplate<String, Object> redisTemplate;

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
                .userName("Social User")
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
    @DisplayName("setPassword - successfully encodes new password and clears requirePasswordChange")
    void setPassword_Success() {
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);
        when(passwordEncoder.encode("mySecurePassword123")).thenReturn("new_encoded_hash");
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);

        UserResponse userResponse = UserResponse.builder()
                .userId(sampleUser.getUserId())
                .email(sampleUser.getEmail())
                .requirePasswordChange(false)
                .build();
        when(userMapper.toResponse(sampleUser)).thenReturn(userResponse);

        SetPasswordRequest request = SetPasswordRequest.builder()
                .newPassword("mySecurePassword123")
                .build();

        UserResponse response = authService.setPassword(request);

        assertThat(response).isNotNull();
        assertThat(response.getRequirePasswordChange()).isFalse();
        assertThat(sampleUser.getPassword()).isEqualTo("new_encoded_hash");
        assertThat(sampleUser.isRequirePasswordChange()).isFalse();
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("setPassword - throws USER_NOT_FOUND when user does not exist")
    void setPassword_UserNotFound() {
        when(currentUserService.getCurrentUser()).thenThrow(new AppException(ErrorCode.USER_NOT_FOUND));

        SetPasswordRequest request = SetPasswordRequest.builder()
                .newPassword("mySecurePassword123")
                .build();

        assertThatThrownBy(() -> authService.setPassword(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("setPassword - throws BAD_REQUEST when requirePasswordChange is false")
    void setPassword_NotRequired_ThrowsBadRequest() {
        sampleUser.setRequirePasswordChange(false);
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);

        SetPasswordRequest request = SetPasswordRequest.builder()
                .newPassword("mySecurePassword123")
                .build();

        assertThatThrownBy(() -> authService.setPassword(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.BAD_REQUEST);

        verify(userRepository, never()).save(any());
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
                .userName("New Google User")
                .avatarUrl("https://lh3.googleusercontent.com/avatar.jpg")
                .role(customerRole)
                .requirePasswordChange(true)
                .firstLogin(true)
                .active(true)
                .build();

        when(userRepository.save(any(User.class))).thenReturn(savedUser);
        when(jwtService.generateToken(any(UserDetails.class))).thenReturn("mock_access_token");
        when(refreshTokenService.createRefreshToken(any(User.class))).thenReturn("mock_refresh_token");

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
        assertThat(response.getUser().getRequirePasswordChange()).isTrue();
        assertThat(response.getAccessToken()).isEqualTo("mock_access_token");
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
}


package com.moodcafe.auth.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.dto.user.request.OnboardingRequest;
import com.moodcafe.auth.dto.user.response.UserResponse;
import com.moodcafe.auth.entity.User;
import com.moodcafe.auth.mapper.UserMapper;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private UserMapper userMapper;

    @Mock
    private CurrentUserService currentUserService;

    @InjectMocks
    private UserServiceImpl userService;


    private User sampleUser;
    private UserResponse sampleResponse;

    @BeforeEach
    void setUp() {
        sampleUser = User.builder()
                .userId(UUID.randomUUID())
                .email("test@moodcafe.vn")
                .userName("Minh Khang")
                .firstLogin(true)
                .noiseTolerance(null)
                .active(true)
                .build();

        sampleResponse = UserResponse.builder()
                .userId(sampleUser.getUserId())
                .email(sampleUser.getEmail())
                .userName(sampleUser.getUserName())
                .firstLogin(false)
                .noiseTolerance("LOW")
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("completeOnboarding - successfully updates noise tolerance and sets firstLogin to false")
    void completeOnboarding_Success() {
        when(currentUserService.getCurrentUser()).thenReturn(sampleUser);
        when(userRepository.save(any(User.class))).thenReturn(sampleUser);
        when(userMapper.toResponse(sampleUser)).thenReturn(sampleResponse);

        OnboardingRequest request = new OnboardingRequest();
        request.setNoiseTolerance("low");

        UserResponse response = userService.completeOnboarding(request);

        assertThat(response).isNotNull();
        assertThat(response.getFirstLogin()).isFalse();
        assertThat(sampleUser.getNoiseTolerance()).isEqualTo("LOW");
        assertThat(sampleUser.isFirstLogin()).isFalse();
        verify(userRepository, times(1)).save(sampleUser);
    }

    @Test
    @DisplayName("completeOnboarding - throws USER_NOT_FOUND if user not found")
    void completeOnboarding_UserNotFound() {
        when(currentUserService.getCurrentUser()).thenThrow(new AppException(ErrorCode.USER_NOT_FOUND));

        OnboardingRequest request = new OnboardingRequest();
        request.setNoiseTolerance("HIGH");

        assertThatThrownBy(() -> userService.completeOnboarding(request))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.USER_NOT_FOUND);

        verify(userRepository, never()).save(any());
    }
}


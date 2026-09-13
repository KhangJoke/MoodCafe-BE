package com.moodcafe.auth.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.dto.user.CustomUserDetails;
import com.moodcafe.auth.entity.Role;
import com.moodcafe.auth.entity.User;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CurrentUserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private CurrentUserServiceImpl currentUserService;

    private User sampleUser;

    @BeforeEach
    void setUp() {
        Role role = Role.builder().name("CUSTOMER").build();
        sampleUser = User.builder()
                .userId(UUID.randomUUID())
                .email("user@moodcafe.vn")
                .fullName("Mood User")
                .role(role)
                .build();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("getCurrentUser - returns user from CustomUserDetails principal")
    void getCurrentUser_CustomUserDetails_ReturnsUser() {
        CustomUserDetails userDetails = new CustomUserDetails(sampleUser);
        Authentication auth = new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        User result = currentUserService.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("user@moodcafe.vn");
        assertThat(currentUserService.getCurrentUserEmail()).isEqualTo("user@moodcafe.vn");
        assertThat(currentUserService.getCurrentUserId()).isEqualTo(sampleUser.getUserId());
    }

    @Test
    @DisplayName("getCurrentUser - fetches user from repo when principal is email string")
    void getCurrentUser_StringPrincipal_FetchesFromRepo() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user@moodcafe.vn", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(userRepository.findByEmail("user@moodcafe.vn")).thenReturn(Optional.of(sampleUser));

        User result = currentUserService.getCurrentUser();

        assertThat(result).isNotNull();
        assertThat(result.getEmail()).isEqualTo("user@moodcafe.vn");
    }

    @Test
    @DisplayName("getCurrentUser - throws UNAUTHORIZED when no authentication exists")
    void getCurrentUser_NoAuth_ThrowsUnauthorized() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(() -> currentUserService.getCurrentUser())
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.UNAUTHORIZED);
    }

    @Test
    @DisplayName("isSystemAdmin - returns true when authority contains ROLE_ADMIN")
    void isSystemAdmin_AdminRole_ReturnsTrue() {
        Authentication auth = new UsernamePasswordAuthenticationToken("admin@moodcafe.vn", null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThat(currentUserService.isSystemAdmin()).isTrue();
    }

    @Test
    @DisplayName("requireSystemAdmin - throws FORBIDDEN when user is not admin")
    void requireSystemAdmin_NotAdmin_ThrowsForbidden() {
        Authentication auth = new UsernamePasswordAuthenticationToken("user@moodcafe.vn", null, List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        assertThatThrownBy(() -> currentUserService.requireSystemAdmin())
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FORBIDDEN);
    }
}

package com.moodcafe.auth.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.abstraction.service.UserService;
import com.moodcafe.auth.dto.user.request.OnboardingRequest;
import com.moodcafe.auth.dto.user.request.UserCommonRequest;
import com.moodcafe.auth.dto.user.response.UserResponse;
import com.moodcafe.auth.entity.User;
import com.moodcafe.auth.mapper.UserMapper;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.gamification.abstraction.service.CustomerTitleService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;
    private final PasswordEncoder passwordEncoder;

    @org.springframework.beans.factory.annotation.Autowired(required = false)
    private CustomerTitleService customerTitleService;

    private UserResponse toEnrichedUserResponse(User user) {
        if (user == null) return null;
        UserResponse response = userMapper.toResponse(user);
        if (response != null && customerTitleService != null) {
            try {
                response.setEquippedTitle(customerTitleService.getEquippedTitle(user.getUserId()));
            } catch (Exception ex) {
                log.warn("Failed to get equipped title for user {}: {}", user.getUserId(), ex.getMessage());
            }
        }
        return response;
    }

    @Override
    public UserResponse getCurrentUser() {
        User user = currentUserService.getCurrentUser();
        return toEnrichedUserResponse(user);
    }


    @Override
    public UserResponse getById(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return toEnrichedUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse update(
            UUID userId,
            UserCommonRequest request
    ) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        userMapper.updateUser(request, user);

        userRepository.save(user);

        return toEnrichedUserResponse(user);
    }

    @Override
    @Transactional
    public UserResponse completeOnboarding(OnboardingRequest request) {
        User user = currentUserService.getCurrentUser();
        user.setFirstLogin(false);
        user = userRepository.save(user);

        return toEnrichedUserResponse(user);
    }

    @Override
    public void deactivate(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() ->
                        new RuntimeException("User not found"));

        user.setActive(false);

        userRepository.save(user);
    }
    @Override
    public boolean existsById(UUID userId) {
        return userRepository.existsById(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean existsByEmail(String email) {
        return userRepository.existsByEmail(email);
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityById(UUID userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional(readOnly = true)
    public User getUserEntityByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));
    }

    @Override
    @Transactional
    public User createUserEntity(User user) {
        return userRepository.save(user);
    }

    @Override
    @Transactional
    public void updateUserPassword(UUID userId, String rawNewPassword) {
        User user = getUserEntityById(userId);
        user.setPassword(passwordEncoder.encode(rawNewPassword.trim()));
        user.setRequirePasswordChange(false);
        userRepository.save(user);
    }

    @Override
    @Transactional
    public void setFirstLoginFalse(UUID userId) {
        User user = getUserEntityById(userId);
        user.setFirstLogin(false);
        userRepository.save(user);
    }
}
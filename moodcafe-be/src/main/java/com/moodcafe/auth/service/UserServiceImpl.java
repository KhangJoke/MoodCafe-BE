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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserMapper userMapper;
    private final CurrentUserService currentUserService;

    @Override
    public UserResponse getCurrentUser() {
        User user = currentUserService.getCurrentUser();
        return userMapper.toResponse(user);
    }


    @Override
    public UserResponse getById(UUID userId) {

        User user = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        return userMapper.toResponse(user);
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

        return userMapper.toResponse(user);
    }

    @Override
    @Transactional
    public UserResponse completeOnboarding(OnboardingRequest request) {
        User user = currentUserService.getCurrentUser();

        if (request.getNoiseTolerance() != null && !request.getNoiseTolerance().isBlank()) {

            user.setNoiseTolerance(request.getNoiseTolerance().trim().toUpperCase());
        }

        user.setFirstLogin(false);
        user = userRepository.save(user);

        return userMapper.toResponse(user);
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
}
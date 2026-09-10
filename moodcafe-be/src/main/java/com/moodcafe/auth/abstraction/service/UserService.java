package com.moodcafe.auth.abstraction.service;

import com.moodcafe.auth.dto.user.request.UserCommonRequest;
import com.moodcafe.auth.dto.user.response.UserResponse;

import java.util.UUID;

public interface UserService {
    UserResponse getCurrentUser();

    UserResponse getById(UUID userId);

    UserResponse update(UUID userId, UserCommonRequest request);

    void deactivate(UUID userId);
    boolean existsById(UUID userId);

}

package com.moodcafe.auth.abstraction.service;

import com.moodcafe.auth.entity.User;

import java.util.UUID;

public interface CurrentUserService {

    User getCurrentUser();

    String getCurrentUserEmail();

    UUID getCurrentUserId();

    boolean isAuthenticated();

    boolean isSystemAdmin();

    void requireSystemAdmin();
}

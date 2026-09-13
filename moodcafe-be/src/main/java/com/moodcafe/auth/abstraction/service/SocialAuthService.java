package com.moodcafe.auth.abstraction.service;

import com.moodcafe.auth.dto.auth.SocialUserInfo;

public interface SocialAuthService {

    boolean supports(String provider);

    SocialUserInfo verifyToken(String token);
}

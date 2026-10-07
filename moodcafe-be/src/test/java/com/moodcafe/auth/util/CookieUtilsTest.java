package com.moodcafe.auth.util;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletResponse;

import static org.assertj.core.api.Assertions.assertThat;

class CookieUtilsTest {
    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
            .withBean(CookieUtils.class);

    @Test
    void setsSecureHttpOnlyCookieForCrossSiteVercelLogin() {
        contextRunner.withPropertyValues(
                "app.security.cookie-secure=true",
                "app.security.cookie-same-site=None").run(context -> {
            MockHttpServletResponse response = new MockHttpServletResponse();
            context.getBean(CookieUtils.class).setRefreshTokenCookie(response, "test-refresh-token");

            assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                    .contains("refreshToken=test-refresh-token", "Path=/", "HttpOnly", "Secure",
                            "SameSite=None");
        });
    }

    @Test
    void clearsCookieUsingTheSameCrossSiteAttributes() {
        contextRunner.withPropertyValues(
                "app.security.cookie-secure=true",
                "app.security.cookie-same-site=None").run(context -> {
            MockHttpServletResponse response = new MockHttpServletResponse();
            context.getBean(CookieUtils.class).clearRefreshTokenCookie(response);

            assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                    .contains("Max-Age=0", "Path=/", "HttpOnly", "Secure", "SameSite=None");
        });
    }

    @Test
    void defaultsToLaxForExistingLocalClients() {
        contextRunner.withPropertyValues("app.security.cookie-secure=false").run(context -> {
            MockHttpServletResponse response = new MockHttpServletResponse();
            context.getBean(CookieUtils.class).setRefreshTokenCookie(response, "test-token");

            assertThat(response.getHeader(HttpHeaders.SET_COOKIE))
                    .contains("SameSite=Lax", "HttpOnly")
                    .doesNotContain("Secure");
        });
    }

    @Test
    void rejectsNoneWithoutSecureAtStartup() {
        contextRunner.withPropertyValues(
                "app.security.cookie-secure=false",
                "app.security.cookie-same-site=None").run(context ->
                assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class));
    }

    @Test
    void rejectsUnknownSameSiteAtStartup() {
        contextRunner.withPropertyValues("app.security.cookie-same-site=Anything").run(context ->
                assertThat(context.getStartupFailure()).hasRootCauseInstanceOf(IllegalArgumentException.class));
    }
}

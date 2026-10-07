package com.moodcafe.auth.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.AutowiredAnnotationBeanPostProcessor;
import org.springframework.context.annotation.ContextAnnotationAutowireCandidateResolver;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.MapPropertySource;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.DefaultCorsProcessor;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SecurityConfigCorsTest {

    @Test
    void permitsCredentialedVercelPreflightFromConfiguredOrigin() throws Exception {
        CorsConfiguration configuration = configurationFor(
                "http://localhost:3000, https://mood-cafe-fe.vercel.app");
        MockHttpServletResponse response = preflight(configuration,
                "https://mood-cafe-fe.vercel.app");

        assertThat(response.getStatus()).isEqualTo(200);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("https://mood-cafe-fe.vercel.app");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_CREDENTIALS))
                .isEqualTo("true");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_HEADERS))
                .containsIgnoringCase("authorization")
                .containsIgnoringCase("content-type")
                .containsIgnoringCase("x-cookies-enabled");
    }

    @Test
    void rejectsUntrustedOriginEvenWhenCredentialsAreRequested() throws Exception {
        MockHttpServletResponse response = preflight(
                configurationFor("https://mood-cafe-fe.vercel.app"),
                "https://attacker.example");

        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    void rejectsCrossSiteCookiePostFromUntrustedOrigin() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("POST", "/api/auth/refresh");
        request.addHeader(HttpHeaders.ORIGIN, "https://attacker.example");
        request.addHeader(HttpHeaders.COOKIE, "refreshToken=test-refresh-token");
        request.setContentType("application/x-www-form-urlencoded");
        MockHttpServletResponse response = new MockHttpServletResponse();

        boolean allowed = new DefaultCorsProcessor().processRequest(
                configurationFor("https://mood-cafe-fe.vercel.app"), request, response);

        assertThat(allowed).isFalse();
        assertThat(response.getStatus()).isEqualTo(403);
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN)).isNull();
    }

    @Test
    void keepsLocalDevelopmentOriginAvailable() throws Exception {
        MockHttpServletResponse response = preflight(
                configurationFor("http://localhost:3000,https://mood-cafe-fe.vercel.app"),
                "http://localhost:3000");
        assertThat(response.getHeader(HttpHeaders.ACCESS_CONTROL_ALLOW_ORIGIN))
                .isEqualTo("http://localhost:3000");
    }

    @Test
    void rejectsWildcardForCredentialedCors() {
        assertThatThrownBy(() -> configurationFor("*"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsEmptyOriginConfiguration() {
        assertThatThrownBy(() -> configurationFor(" , "))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private CorsConfiguration configurationFor(String origins) {
        SecurityConfig securityConfig = new SecurityConfig(null, null, null, null);
        // Only inject configuration values: no DB, Redis, security chain or network.
        try (GenericApplicationContext context = new GenericApplicationContext()) {
            context.getDefaultListableBeanFactory().setAutowireCandidateResolver(
                    new ContextAnnotationAutowireCandidateResolver());
            context.getEnvironment().getPropertySources().addFirst(new MapPropertySource(
                    "test-origins", Map.of("app.security.cors-allowed-origins", origins)));
            context.getBeanFactory().addEmbeddedValueResolver(
                    context.getEnvironment()::resolveRequiredPlaceholders);
            AutowiredAnnotationBeanPostProcessor injector =
                    new AutowiredAnnotationBeanPostProcessor();
            injector.setBeanFactory(context.getBeanFactory());
            injector.processInjection(securityConfig);
            return securityConfig.corsConfigurationSource()
                    .getCorsConfiguration(new MockHttpServletRequest("OPTIONS", "/api/auth/login"));
        }
    }

    private MockHttpServletResponse preflight(CorsConfiguration configuration, String origin)
            throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest("OPTIONS", "/api/auth/login");
        request.addHeader(HttpHeaders.ORIGIN, origin);
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_METHOD, "POST");
        request.addHeader(HttpHeaders.ACCESS_CONTROL_REQUEST_HEADERS,
                "authorization,content-type,x-cookies-enabled");
        MockHttpServletResponse response = new MockHttpServletResponse();
        new DefaultCorsProcessor().processRequest(configuration, request, response);
        return response;
    }
}

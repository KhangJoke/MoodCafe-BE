package com.moodcafe.auth.controller;

import com.moodcafe.auth.abstraction.service.UserService;
import com.moodcafe.auth.dto.role.response.RoleResponse;
import com.moodcafe.auth.dto.user.request.UserCommonRequest;
import com.moodcafe.auth.dto.user.response.UserResponse;
import com.moodcafe.shared.exceptions.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.http.converter.json.JacksonJsonHttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.Instant;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    private MockMvc mockMvc;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController)
                .setMessageConverters(new JacksonJsonHttpMessageConverter())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("PUT /api/users/{userId} - Deserializes UserCommonRequest and returns 200")
    void updateUser_Success() throws Exception {
        UUID userId = UUID.fromString("56ca79ee-a86c-4037-893a-2ddb89b7884f");
        String avatarUrl = "https://res.cloudinary.com/dy45rrkhf/image/upload/v1/moodcafe/avatars/test.jpg";

        UserResponse response = UserResponse.builder()
                .userId(userId)
                .email("khangngoc3082005@gmail.com")
                .fullName("Bùi Ngọc Duy Khang")
                .avatarUrl(avatarUrl)
                .role(RoleResponse.builder().roleId(UUID.randomUUID()).name("CUSTOMER").build())
                .active(true)
                .emailVerified(true)
                .requirePasswordChange(false)
                .firstLogin(false)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(userService.update(eq(userId), any(UserCommonRequest.class))).thenReturn(response);

        mockMvc.perform(put("/api/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "Bùi Ngọc Duy Khang",
                                  "email": "khangngoc3082005@gmail.com",
                                  "avatarUrl": "https://res.cloudinary.com/dy45rrkhf/image/upload/v1/moodcafe/avatars/test.jpg"
                                }
                                """))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.avatarUrl").value(avatarUrl));
    }

    @Test
    @DisplayName("PUT /api/users/{userId} - Returns 400 when validation fails")
    void updateUser_ValidationError_Returns400() throws Exception {
        UUID userId = UUID.fromString("56ca79ee-a86c-4037-893a-2ddb89b7884f");

        mockMvc.perform(put("/api/users/{userId}", userId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "fullName": "",
                                  "email": "not-an-email"
                                }
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }
}

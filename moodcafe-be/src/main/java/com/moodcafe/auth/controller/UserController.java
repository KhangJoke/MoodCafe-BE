package com.moodcafe.auth.controller;

import com.moodcafe.auth.abstraction.service.IUserService;
import com.moodcafe.auth.dto.user.request.UserCommonRequest;
import com.moodcafe.auth.dto.user.response.UserResponse;
import com.moodcafe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserResponse>> getCurrentUser() {

        UserResponse user = userService.getCurrentUser();

        return ResponseEntity.ok(
                ApiResponse.success(
                        user,
                        "Get current user successfully"
                )
        );
    }

    @GetMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> getById(
            @PathVariable UUID userId
    ) {

        UserResponse user = userService.getById(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        user,
                        "Get user successfully"
                )
        );
    }

    @PutMapping("/{userId}")
    public ResponseEntity<ApiResponse<UserResponse>> update(
            @PathVariable UUID userId,
            @Valid @RequestBody UserCommonRequest request
    ) {

        UserResponse user = userService.update(
                userId,
                request
        );

        return ResponseEntity.ok(
                ApiResponse.success(
                        user,
                        "Update user successfully"
                )
        );
    }

    @DeleteMapping("/{userId}")
    public ResponseEntity<ApiResponse<Void>> deactivate(
            @PathVariable UUID userId
    ) {

        userService.deactivate(userId);

        return ResponseEntity.ok(
                ApiResponse.success(
                        null,
                        "Deactivate user successfully"
                )
        );
    }
}
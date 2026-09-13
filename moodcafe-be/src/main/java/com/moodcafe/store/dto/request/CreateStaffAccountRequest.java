package com.moodcafe.store.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStaffAccountRequest {

    @NotBlank(message = "Staff name is required")
    @Size(max = 150, message = "Staff name cannot exceed 150 characters")
    private String name;

    @NotBlank(message = "Email is required")
    @Email(message = "Invalid email format")
    @Size(max = 255, message = "Email cannot exceed 255 characters")
    private String email;

    @NotBlank(message = "Password is required")
    @Size(min = 6, max = 100, message = "Password must be at least 6 characters")
    private String password;

    @NotBlank(message = "Store role is required (CASHIER or MANAGER)")
    @Pattern(regexp = "^(?i)(CASHIER|STAFF|MANAGER)$", message = "Store role must be CASHIER, STAFF, or MANAGER")
    private String storeRole;
}

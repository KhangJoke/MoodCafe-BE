package com.moodcafe.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AddStoreStaffRequest {

    @NotNull(message = "User ID is required")
    private UUID userId;

    @NotBlank(message = "Store role is required")
    @Pattern(regexp = "^(OWNER|MANAGER|STAFF)$", message = "Store role must be OWNER, MANAGER, or STAFF")
    private String storeRole;
}

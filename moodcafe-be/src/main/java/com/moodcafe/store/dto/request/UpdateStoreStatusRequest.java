package com.moodcafe.store.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStoreStatusRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(PENDING|ACTIVE|INACTIVE)$", message = "Status must be PENDING, ACTIVE, or INACTIVE")
    private String status;
}

package com.moodcafe.tag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewStoreTagRequest {

    @NotBlank(message = "Status is required")
    @Pattern(regexp = "^(APPROVED|REJECTED|REVOKED)$", message = "Status must be APPROVED, REJECTED, or REVOKED")
    private String status;

    private String rejectReason;
}

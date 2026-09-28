package com.moodcafe.store.dto.request;

import jakarta.validation.constraints.NotBlank;
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
public class ResolveReviewReportRequest {

    @NotBlank(message = "Hành động xử lý không được để trống (APPROVE_AND_DELETE_REVIEW hoặc DISMISS)")
    private String action;

    private String adminNote;
}

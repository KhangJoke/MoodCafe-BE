package com.moodcafe.tag.dto.request;

import com.moodcafe.tag.entity.enums.ApprovalMode;
import com.moodcafe.tag.entity.enums.ControlType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class CreateTagCategoryRequest {

    @NotBlank(message = "Category name is required")
    @Size(max = 100, message = "Category name cannot exceed 100 characters")
    private String name;

    @NotBlank(message = "Category code is required")
    @Size(max = 50, message = "Category code cannot exceed 50 characters")
    private String code;

    @NotNull(message = "Approval mode is required")
    private ApprovalMode approvalMode;

    @NotNull(message = "Control type is required")
    private ControlType controlType;

    private Integer displayOrder;

    private Boolean active;
}

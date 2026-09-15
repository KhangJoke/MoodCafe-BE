package com.moodcafe.tag.dto.request;

import com.moodcafe.tag.entity.ApprovalMode;
import com.moodcafe.tag.entity.ControlType;
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
public class UpdateTagCategoryRequest {

    @Size(max = 100, message = "Category name cannot exceed 100 characters")
    private String name;

    @Size(max = 50, message = "Category code cannot exceed 50 characters")
    private String code;

    private ApprovalMode approvalMode;

    private ControlType controlType;

    private Integer displayOrder;

    private Boolean active;
}

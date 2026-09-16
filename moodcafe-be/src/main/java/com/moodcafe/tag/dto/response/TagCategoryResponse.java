package com.moodcafe.tag.dto.response;

import com.moodcafe.tag.entity.enums.ApprovalMode;
import com.moodcafe.tag.entity.enums.ControlType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TagCategoryResponse {

    private UUID tagCategoryId;
    private String name;
    private String code;
    private ApprovalMode approvalMode;
    private ControlType controlType;
    private Integer displayOrder;
    private Boolean active;
    private Instant createdAt;
}

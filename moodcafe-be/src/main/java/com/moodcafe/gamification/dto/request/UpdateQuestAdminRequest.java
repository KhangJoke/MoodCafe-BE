package com.moodcafe.gamification.dto.request;

import com.moodcafe.gamification.entity.enums.QuestGroup;
import com.moodcafe.gamification.entity.enums.QuestType;
import com.moodcafe.gamification.entity.enums.RuleType;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateQuestAdminRequest {

    @NotBlank(message = "Tên nhiệm vụ không được để trống")
    @Size(max = 150, message = "Tên nhiệm vụ tối đa 150 ký tự")
    private String name;

    private String description;

    private QuestGroup questGroup;
    private QuestType questType;
    private RuleType ruleType;

    @NotNull(message = "Chỉ tiêu không được để trống")
    @Min(value = 1, message = "Chỉ tiêu phải lớn hơn hoặc bằng 1")
    private Long targetValue;

    @NotNull(message = "Điểm thưởng không được để trống")
    @Min(value = 0, message = "Điểm thưởng phải lớn hơn hoặc bằng 0")
    private Long rewardPoints;

    private UUID rewardVoucherTemplateId;
    private UUID rewardTitleId;

    private Integer displayOrder;
    private Boolean isActive;
}

package com.moodcafe.booking.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CategoryScheduleDTO {

    private UUID categoryId;

    private String categoryName;

    private Integer maxCapacity;

    private Long minSpendAmount;

    private Integer totalQuota;

    private String imageUrl;

    private String description;

    private List<TimeSlotDTO> timeSlots;
}

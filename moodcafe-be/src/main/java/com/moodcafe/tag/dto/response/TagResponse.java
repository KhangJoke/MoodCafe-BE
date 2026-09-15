package com.moodcafe.tag.dto.response;

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
public class TagResponse {

    private UUID tagId;
    private UUID tagCategoryId;
    private String categoryCode;
    private String categoryName;
    private String name;
    private String description;
    private Integer scaleValue;
    private Boolean active;
    private Instant createdAt;
}

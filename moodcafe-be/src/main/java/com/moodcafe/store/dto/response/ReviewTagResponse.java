package com.moodcafe.store.dto.response;

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
public class ReviewTagResponse {

    private UUID tagRatingId;
    private UUID tagId;
    private String tagName;
    private String categoryName;
    private String categoryCode;
    private Integer score;
}

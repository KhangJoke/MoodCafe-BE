package com.moodcafe.store.dto.response;

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
public class FeaturedMoodStoreResponse {

    private String categoryCode;
    private String categoryName;
    private String moodBadgeText;
    private Long preferenceCount;
    private StoreSearchItemResponse store;
}

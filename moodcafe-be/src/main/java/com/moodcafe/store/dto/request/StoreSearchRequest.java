package com.moodcafe.store.dto.request;

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
public class StoreSearchRequest {

    private String keyword;
    private String district;
    private List<String> districts;
    private List<UUID> tagIds;
    private Integer noiseLevel;
    private Boolean openNow;
    private Boolean highRatingOnly;
    private Boolean matchPersonalGuOnly;
    private String priceRange;
    private Long priceFrom;
    private Long priceTo;

    @Builder.Default
    private String sortBy = "RECOMMENDED";

    @Builder.Default
    private Integer page = 0;

    @Builder.Default
    private Integer size = 15;
}

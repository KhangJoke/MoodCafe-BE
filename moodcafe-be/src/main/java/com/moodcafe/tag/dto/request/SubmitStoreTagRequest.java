package com.moodcafe.tag.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SubmitStoreTagRequest {

    private UUID tagId;

    private String customTagName;

    private String categoryCode;

    @Builder.Default
    private List<String> proofImageUrls = new ArrayList<>();

    public void setProofImageUrl(String proofImageUrl) {
        if (proofImageUrl != null && !proofImageUrl.isBlank() && (this.proofImageUrls == null || this.proofImageUrls.isEmpty())) {
            this.proofImageUrls = new ArrayList<>(List.of(proofImageUrl.trim()));
        }
    }
}

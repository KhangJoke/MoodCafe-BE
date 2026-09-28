package com.moodcafe.tag.dto.request;

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
public class SubmitStoreTagRequest {

    private UUID tagId;

    private String customTagName;

    private String categoryCode;

    private String proofImageUrl;
}

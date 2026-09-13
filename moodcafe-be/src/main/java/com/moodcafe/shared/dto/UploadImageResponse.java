package com.moodcafe.shared.dto;

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
public class UploadImageResponse {
    private String imageUrl;
    private String publicId;
    private String format;
    private Long bytes;
}

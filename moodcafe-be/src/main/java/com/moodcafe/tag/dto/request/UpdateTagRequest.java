package com.moodcafe.tag.dto.request;

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
public class UpdateTagRequest {

    @Size(max = 100, message = "Tag name cannot exceed 100 characters")
    private String name;

    private String description;

    private UUID tagCategoryId;

    private Integer scaleValue;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;

    private Boolean active;
}

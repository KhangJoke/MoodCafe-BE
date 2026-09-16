package com.moodcafe.tag.dto.request;

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
public class CreateTagRequest {

    @NotBlank(message = "Tag name is required")
    @Size(max = 100, message = "Tag name cannot exceed 100 characters")
    private String name;

    private String description;

    @NotNull(message = "Tag category ID is required")
    private UUID tagCategoryId;

    private Integer scaleValue;

    @Size(max = 500, message = "Image URL cannot exceed 500 characters")
    private String imageUrl;
}

package com.moodcafe.tag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateTagRequest {

    @NotBlank(message = "Tag name is required")
    private String name;

    private String description;

    private String tagType;

    @Pattern(regexp = "VIBE|PURPOSE", message = "Category must be VIBE or PURPOSE")
    private String category;

    private Boolean active;
}

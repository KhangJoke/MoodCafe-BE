package com.moodcafe.tag.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateTagRequest {

    @NotBlank(message = "Tag name is required")
    private String name;

    private String description;

    @Builder.Default
    private String tagType = "PRIMARY";

    @NotBlank(message = "Category is required (VIBE or PURPOSE)")
    @Pattern(regexp = "VIBE|PURPOSE", message = "Category must be VIBE or PURPOSE")
    private String category;
}

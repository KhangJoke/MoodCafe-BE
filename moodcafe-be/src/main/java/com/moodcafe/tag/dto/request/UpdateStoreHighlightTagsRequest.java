package com.moodcafe.tag.dto.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
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
public class UpdateStoreHighlightTagsRequest {

    @NotNull(message = "tagIds must not be null")
    @Size(max = 4, message = "Maximum 4 highlight tags allowed")
    private List<UUID> tagIds;
}

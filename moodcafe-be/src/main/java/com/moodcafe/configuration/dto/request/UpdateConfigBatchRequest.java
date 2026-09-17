package com.moodcafe.configuration.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateConfigBatchRequest {

    @NotEmpty(message = "Items list cannot be empty")
    @Valid
    private List<UpdateConfigItemRequest> items;
}

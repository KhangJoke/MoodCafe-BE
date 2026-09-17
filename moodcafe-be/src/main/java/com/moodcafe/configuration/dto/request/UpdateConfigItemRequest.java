package com.moodcafe.configuration.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
public class UpdateConfigItemRequest {

    @NotBlank(message = "Config key cannot be blank")
    private String configKey;

    @NotNull(message = "Config value cannot be null")
    private String configValue;
}

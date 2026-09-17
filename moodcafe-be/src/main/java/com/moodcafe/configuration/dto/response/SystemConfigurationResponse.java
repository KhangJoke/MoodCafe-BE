package com.moodcafe.configuration.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SystemConfigurationResponse {

    private UUID configId;
    private String configGroup;
    private String configKey;
    private String configValue;
    private String dataType;
    private String displayName;
    private String description;
    private boolean isPublic;
    private Instant updatedAt;
}

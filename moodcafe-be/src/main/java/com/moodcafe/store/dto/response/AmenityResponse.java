package com.moodcafe.store.dto.response;

import lombok.*;

import java.time.LocalDateTime;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AmenityResponse {

    private UUID amenityId;
    private String name;
    private String description;
    private LocalDateTime createdAt;
}

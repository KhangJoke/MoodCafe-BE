package com.moodcafe.loyalty.dto;

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
public class RefundPointCommand {
    private UUID userId;
    private Long points;
    private String refundId;
    private String businessKey;
    private String description;
}


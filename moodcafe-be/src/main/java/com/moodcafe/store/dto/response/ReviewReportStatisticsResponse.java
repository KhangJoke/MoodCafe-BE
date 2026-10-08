package com.moodcafe.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReviewReportStatisticsResponse {

    private long total;
    private long pending;
    private long resolved;
    private long dismissed;
    private Map<String, Long> byReason;
}

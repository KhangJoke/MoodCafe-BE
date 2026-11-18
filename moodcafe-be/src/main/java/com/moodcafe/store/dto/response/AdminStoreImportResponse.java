package com.moodcafe.store.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStoreImportResponse {

    private int totalSubmitted;
    private int createdCount;
    private int skippedCount;
    private int failedCount;

    @Builder.Default
    private List<AdminStoreImportRowResult> results = new ArrayList<>();

    public enum ImportOutcome {
        CREATED,
        SKIPPED,
        FAILED
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class AdminStoreImportRowResult {
        private int rowNumber;
        private String storeName;
        private ImportOutcome outcome;
        private UUID storeId;
        private String message;
    }
}

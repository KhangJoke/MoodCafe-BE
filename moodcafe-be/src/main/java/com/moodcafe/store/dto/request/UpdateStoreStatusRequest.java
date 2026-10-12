package com.moodcafe.store.dto.request;

import com.moodcafe.store.entity.enums.StoreStatus;
import jakarta.validation.constraints.NotNull;
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
public class UpdateStoreStatusRequest {

    @NotNull(message = "Status is required")
    private StoreStatus status;

    private String rejectReason;

    @Builder.Default
    private Boolean allowResubmit = true;

    private List<UUID> approvedTagIds;

    private List<UUID> rejectedTagIds;

    private String tagRejectReason;
}

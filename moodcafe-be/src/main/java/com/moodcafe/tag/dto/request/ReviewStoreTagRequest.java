package com.moodcafe.tag.dto.request;

import com.moodcafe.tag.entity.enums.StoreTagStatus;
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
public class ReviewStoreTagRequest {

    @NotNull(message = "Status is required")
    private StoreTagStatus status;

    private String rejectReason;
}

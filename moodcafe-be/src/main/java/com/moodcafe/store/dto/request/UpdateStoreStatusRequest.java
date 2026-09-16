package com.moodcafe.store.dto.request;

import com.moodcafe.store.entity.enums.StoreStatus;
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
public class UpdateStoreStatusRequest {

    @NotNull(message = "Status is required")
    private StoreStatus status;
}

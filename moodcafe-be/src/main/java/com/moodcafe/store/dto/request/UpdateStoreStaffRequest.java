package com.moodcafe.store.dto.request;

import com.moodcafe.store.entity.enums.StoreStaffStatus;
import jakarta.validation.constraints.Pattern;
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
public class UpdateStoreStaffRequest {

    @Pattern(regexp = "^(OWNER|MANAGER|STAFF)$", message = "Store role must be OWNER, MANAGER, or STAFF")
    private String storeRole;

    private StoreStaffStatus status;
}

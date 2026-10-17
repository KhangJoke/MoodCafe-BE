package com.moodcafe.store.dto.request;

import com.moodcafe.store.entity.enums.StoreStaffStatus;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
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

    @Size(max = 150, message = "Full name cannot exceed 150 characters")
    private String fullName;

    @Pattern(regexp = "^(?i)(OWNER|MANAGER|CASHIER)$", message = "Store role must be OWNER, MANAGER, or CASHIER")
    private String storeRole;

    private StoreStaffStatus status;
}

package com.moodcafe.store.dto.request;

import jakarta.validation.constraints.Pattern;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateStoreStaffRequest {

    @Pattern(regexp = "^(OWNER|MANAGER|STAFF)$", message = "Store role must be OWNER, MANAGER, or STAFF")
    private String storeRole;

    @Pattern(regexp = "^(ACTIVE|INACTIVE)$", message = "Status must be ACTIVE or INACTIVE")
    private String status;
}

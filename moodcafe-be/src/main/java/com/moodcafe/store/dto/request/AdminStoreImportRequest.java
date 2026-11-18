package com.moodcafe.store.dto.request;

import com.moodcafe.store.entity.enums.StoreStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminStoreImportRequest {

    @Builder.Default
    private StoreStatus targetStatus = StoreStatus.ACTIVE;

    @Builder.Default
    private Boolean skipDuplicates = Boolean.TRUE;

    @Email(message = "Định dạng email chủ quán mặc định không hợp lệ")
    @Size(max = 255, message = "Email chủ quán mặc định tối đa 255 ký tự")
    private String defaultOwnerEmail;

    @NotEmpty(message = "Danh sách quán import không được để trống")
    @Builder.Default
    private List<@Valid AdminStoreImportItemRequest> items = new ArrayList<>();
}

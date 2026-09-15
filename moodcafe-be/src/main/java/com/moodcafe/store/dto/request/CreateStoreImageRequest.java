package com.moodcafe.store.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.web.multipart.MultipartFile;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CreateStoreImageRequest {

    @NotNull(message = "Image file is required")
    private MultipartFile file;

    @Builder.Default
    private Boolean isPrimary = false;
}

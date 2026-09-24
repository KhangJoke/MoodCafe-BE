package com.moodcafe.shared.controller;

import com.moodcafe.shared.abstraction.service.FileStorageService;
import com.moodcafe.shared.dto.UploadImageResponse;
import com.moodcafe.shared.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/upload")
@RequiredArgsConstructor
public class FileUploadController {

    private final FileStorageService fileStorageService;

    @PostMapping(value = "/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<UploadImageResponse>> uploadImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "storeId", required = false) String storeId
    ) {
        String folderPath = (storeId != null && !storeId.isBlank()) ? "stores/" + storeId.trim() : "stores/onboarding";
        UploadImageResponse response = fileStorageService.uploadImage(file, folderPath);
        return ResponseEntity.ok(ApiResponse.success(response, "Upload image successfully"));
    }

    @PostMapping(value = "/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<List<UploadImageResponse>>> uploadImages(
            @RequestParam("files") List<MultipartFile> files,
            @RequestParam(value = "storeId", required = false) String storeId
    ) {
        String folderPath = (storeId != null && !storeId.isBlank()) ? "stores/" + storeId.trim() : "stores/onboarding";
        List<UploadImageResponse> response = fileStorageService.uploadImages(files, folderPath);
        return ResponseEntity.ok(ApiResponse.success(response, "Upload images successfully"));
    }
}


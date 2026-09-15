package com.moodcafe.shared.abstraction.service;

import com.moodcafe.shared.dto.UploadImageResponse;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {

    UploadImageResponse uploadImage(MultipartFile file, String folder);

    void deleteImage(String publicId);

    void deleteImageByUrl(String imageUrl);

    String extractPublicId(String imageUrl);
}

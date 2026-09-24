package com.moodcafe.shared.abstraction.service;

import com.moodcafe.shared.dto.UploadImageResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    UploadImageResponse uploadImage(MultipartFile file, String folder);

    List<UploadImageResponse> uploadImages(List<MultipartFile> files, String folder);

    void deleteImage(String publicId);

    void deleteImageByUrl(String imageUrl);

    String extractPublicId(String imageUrl);
}


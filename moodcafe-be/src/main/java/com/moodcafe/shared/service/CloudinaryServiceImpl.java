package com.moodcafe.shared.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import com.moodcafe.shared.abstraction.service.FileStorageService;
import com.moodcafe.shared.dto.UploadImageResponse;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CloudinaryServiceImpl implements FileStorageService {

    private static final long MAX_FILE_SIZE = 5 * 1024 * 1024; // 5 MB
    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    private final Cloudinary cloudinary;

    @Override
    public UploadImageResponse uploadImage(MultipartFile file, String folder) {
        if (file == null || file.isEmpty()) {
            throw new AppException(ErrorCode.FILE_EMPTY);
        }

        if (file.getSize() > MAX_FILE_SIZE) {
            throw new AppException(ErrorCode.FILE_TOO_LARGE);
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new AppException(ErrorCode.FILE_TYPE_INVALID);
        }

        try {
            String targetFolder = (folder != null && !folder.isBlank()) ? "moodcafe/" + folder : "moodcafe/general";

            Map<?, ?> uploadResult = cloudinary.uploader().upload(
                    file.getBytes(),
                    ObjectUtils.asMap(
                            "folder", targetFolder,
                            "resource_type", "image"
                    )
            );

            String secureUrl = (String) uploadResult.get("secure_url");
            String publicId = (String) uploadResult.get("public_id");
            String format = (String) uploadResult.get("format");
            Long bytes = uploadResult.get("bytes") != null ? Long.valueOf(uploadResult.get("bytes").toString()) : file.getSize();

            return UploadImageResponse.builder()
                    .imageUrl(secureUrl)
                    .publicId(publicId)
                    .format(format)
                    .bytes(bytes)
                    .build();

        } catch (Exception e) {
            log.error("Failed to upload image to Cloudinary: {}", e.getMessage(), e);
            throw new AppException(ErrorCode.FILE_UPLOAD_FAILED);
        }
    }

    @Override
    public void deleteImage(String publicId) {
        if (publicId == null || publicId.isBlank()) {
            return;
        }
        try {
            cloudinary.uploader().destroy(publicId, ObjectUtils.emptyMap());
        } catch (IOException e) {
            log.warn("Failed to delete image from Cloudinary with publicId: {}", publicId, e);
        }
    }

    @Override
    public void deleteImageByUrl(String imageUrl) {
        String publicId = extractPublicId(imageUrl);
        if (publicId != null && !publicId.isBlank()) {
            deleteImage(publicId);
        }
    }

    @Override
    public String extractPublicId(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return null;
        }

        // If it is already a public_id (not a full URL)
        if (!imageUrl.startsWith("http://") && !imageUrl.startsWith("https://")) {
            return imageUrl;
        }

        try {
            int uploadIndex = imageUrl.indexOf("/upload/");
            if (uploadIndex == -1) {
                return null;
            }

            String pathAfterUpload = imageUrl.substring(uploadIndex + "/upload/".length());

            // Remove query params or fragments if any
            int queryIndex = pathAfterUpload.indexOf('?');
            if (queryIndex != -1) {
                pathAfterUpload = pathAfterUpload.substring(0, queryIndex);
            }

            // Remove file extension
            int lastDotIndex = pathAfterUpload.lastIndexOf('.');
            if (lastDotIndex != -1) {
                pathAfterUpload = pathAfterUpload.substring(0, lastDotIndex);
            }

            // Split into segments to strip transformations and version
            String[] segments = pathAfterUpload.split("/");
            int startIndex = 0;

            while (startIndex < segments.length) {
                String segment = segments[startIndex];
                // Cloudinary version segment matches v + digits (e.g., v1, v1712345678)
                if (segment.matches("^v\\d+$")) {
                    startIndex++;
                    break;
                }
                // If it hits our base folder 'moodcafe', stop stripping
                if ("moodcafe".equalsIgnoreCase(segment)) {
                    break;
                }
                // Transformation parameters (e.g. w_500,c_fill, q_auto, f_auto)
                if (segment.matches("^[a-z]{1,2}_.*") || segment.contains(",")) {
                    startIndex++;
                } else {
                    break;
                }
            }

            if (startIndex < segments.length) {
                return String.join("/", Arrays.copyOfRange(segments, startIndex, segments.length));
            }

            return pathAfterUpload;
        } catch (Exception e) {
            log.warn("Failed to extract publicId from URL: {}", imageUrl, e);
            return null;
        }
    }
}

package com.moodcafe.shared.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.moodcafe.shared.dto.UploadImageResponse;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceImplTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryServiceImpl cloudinaryService;

    @BeforeEach
    void setUp() {
        // leniency for mock
    }

    @Test
    @DisplayName("uploadImage - throws FILE_EMPTY when file is null or empty")
    void uploadImage_EmptyFile_ThrowsException() {
        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);

        assertThatThrownBy(() -> cloudinaryService.uploadImage(emptyFile, "general"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_EMPTY);

        assertThatThrownBy(() -> cloudinaryService.uploadImage(null, "general"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_EMPTY);
    }

    @Test
    @DisplayName("uploadImage - throws FILE_TOO_LARGE when file exceeds 5MB")
    void uploadImage_TooLarge_ThrowsException() {
        byte[] largeBytes = new byte[6 * 1024 * 1024]; // 6MB
        MockMultipartFile largeFile = new MockMultipartFile("file", "large.jpg", "image/jpeg", largeBytes);

        assertThatThrownBy(() -> cloudinaryService.uploadImage(largeFile, "general"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_TOO_LARGE);
    }

    @Test
    @DisplayName("uploadImage - throws FILE_TYPE_INVALID for non-image format")
    void uploadImage_InvalidType_ThrowsException() {
        MockMultipartFile pdfFile = new MockMultipartFile("file", "doc.pdf", "application/pdf", new byte[]{1, 2, 3});

        assertThatThrownBy(() -> cloudinaryService.uploadImage(pdfFile, "general"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_TYPE_INVALID);
    }

    @Test
    @DisplayName("uploadImage - success when file is valid jpg/png")
    void uploadImage_Success() throws IOException {
        MockMultipartFile validFile = new MockMultipartFile("file", "cafe.jpg", "image/jpeg", "image content".getBytes());

        when(cloudinary.uploader()).thenReturn(uploader);
        when(uploader.upload(any(byte[].class), anyMap())).thenReturn(Map.of(
                "secure_url", "https://res.cloudinary.com/demo/image/upload/v1/moodcafe/cafe.jpg",
                "public_id", "moodcafe/cafe",
                "format", "jpg",
                "bytes", 1024L
        ));

        UploadImageResponse response = cloudinaryService.uploadImage(validFile, "stores");

        assertThat(response).isNotNull();
        assertThat(response.getImageUrl()).isEqualTo("https://res.cloudinary.com/demo/image/upload/v1/moodcafe/cafe.jpg");
        assertThat(response.getPublicId()).isEqualTo("moodcafe/cafe");
        assertThat(response.getFormat()).isEqualTo("jpg");
    }

    @Test
    @DisplayName("extractPublicId - returns expected public ID for various URL formats")
    void extractPublicId_Scenarios() {
        assertThat(cloudinaryService.extractPublicId(null)).isNull();
        assertThat(cloudinaryService.extractPublicId("")).isNull();

        // Already a publicId
        assertThat(cloudinaryService.extractPublicId("moodcafe/stores/sample"))
                .isEqualTo("moodcafe/stores/sample");

        // Standard Cloudinary URL with version and extension
        assertThat(cloudinaryService.extractPublicId("https://res.cloudinary.com/demo/image/upload/v1/moodcafe/cafe.jpg"))
                .isEqualTo("moodcafe/cafe");

        // Store image URL with timestamp version and nested folders
        assertThat(cloudinaryService.extractPublicId("https://res.cloudinary.com/demo/image/upload/v1712345678/moodcafe/stores/store-123/img-abc.png"))
                .isEqualTo("moodcafe/stores/store-123/img-abc");

        // URL with transformations and version
        assertThat(cloudinaryService.extractPublicId("https://res.cloudinary.com/demo/image/upload/c_fill,w_300/v12345/moodcafe/stores/store-123/img-abc.webp"))
                .isEqualTo("moodcafe/stores/store-123/img-abc");
    }

    @Test
    @DisplayName("deleteImage - calls destroy when publicId is valid")
    void deleteImage_Success() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);

        cloudinaryService.deleteImage("moodcafe/stores/sample");

        verify(uploader, times(1)).destroy(eq("moodcafe/stores/sample"), anyMap());
    }

    @Test
    @DisplayName("deleteImage - does not call destroy when publicId is null or blank")
    void deleteImage_NullOrBlank_DoesNothing() {
        cloudinaryService.deleteImage(null);
        cloudinaryService.deleteImage("  ");

        verifyNoInteractions(cloudinary);
    }

    @Test
    @DisplayName("deleteImageByUrl - extracts publicId and destroys image")
    void deleteImageByUrl_Success() throws IOException {
        when(cloudinary.uploader()).thenReturn(uploader);

        String url = "https://res.cloudinary.com/demo/image/upload/v1712345678/moodcafe/stores/store-123/img-abc.png";
        cloudinaryService.deleteImageByUrl(url);

        verify(uploader, times(1)).destroy(eq("moodcafe/stores/store-123/img-abc"), anyMap());
    }
}

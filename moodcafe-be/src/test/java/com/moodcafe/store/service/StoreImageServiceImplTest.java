package com.moodcafe.store.service;

import com.moodcafe.shared.abstraction.service.FileStorageService;
import com.moodcafe.shared.dto.UploadImageResponse;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.request.CreateStoreImageRequest;
import com.moodcafe.store.dto.response.StoreImageResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.mapper.StoreImageMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreImageServiceImplTest {

    @Mock
    private StoreRepository storeRepository;

    @Mock
    private StoreImageRepository storeImageRepository;

    @Mock
    private StoreImageMapper storeImageMapper;

    @Mock
    private StoreStaffService storeStaffService;

    @Mock
    private FileStorageService fileStorageService;

    @InjectMocks
    private StoreImageServiceImpl storeImageService;

    private UUID storeId;
    private UUID imageId;
    private Store store;

    @BeforeEach
    void setUp() {
        storeId = UUID.randomUUID();
        imageId = UUID.randomUUID();
        store = Store.builder()
                .storeId(storeId)
                .name("Mood Cafe Test")
                .build();
    }

    @Test
    @DisplayName("addImage - throws FILE_EMPTY when file is null or empty")
    void addImage_EmptyFile_ThrowsException() {
        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));

        CreateStoreImageRequest nullFileRequest = CreateStoreImageRequest.builder()
                .file(null)
                .isPrimary(false)
                .build();

        assertThatThrownBy(() -> storeImageService.addImage(storeId, nullFileRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_EMPTY);

        MockMultipartFile emptyFile = new MockMultipartFile("file", "empty.jpg", "image/jpeg", new byte[0]);
        CreateStoreImageRequest emptyFileRequest = CreateStoreImageRequest.builder()
                .file(emptyFile)
                .isPrimary(false)
                .build();

        assertThatThrownBy(() -> storeImageService.addImage(storeId, emptyFileRequest))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.FILE_EMPTY);
    }

    @Test
    @DisplayName("addImage - successfully uploads to Cloudinary and saves entity")
    void addImage_Success() {
        MockMultipartFile file = new MockMultipartFile("file", "store.jpg", "image/jpeg", "image content".getBytes());
        CreateStoreImageRequest request = CreateStoreImageRequest.builder()
                .file(file)
                .isPrimary(false)
                .build();

        UploadImageResponse uploadResponse = UploadImageResponse.builder()
                .imageUrl("https://res.cloudinary.com/demo/image/upload/v1/moodcafe/stores/" + storeId + "/store.jpg")
                .publicId("moodcafe/stores/" + storeId + "/store")
                .format("jpg")
                .bytes(1024L)
                .build();

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(fileStorageService.uploadImage(eq(file), eq("stores/" + storeId))).thenReturn(uploadResponse);

        StoreImage savedImage = StoreImage.builder()
                .storeImageId(imageId)
                .store(store)
                .imageUrl(uploadResponse.getImageUrl())
                .primary(false)
                .build();

        when(storeImageRepository.save(any(StoreImage.class))).thenReturn(savedImage);

        StoreImageResponse expectedResponse = StoreImageResponse.builder()
                .storeImageId(imageId)
                .storeId(storeId)
                .imageUrl(uploadResponse.getImageUrl())
                .isPrimary(false)
                .build();

        when(storeImageMapper.toResponse(savedImage)).thenReturn(expectedResponse);

        StoreImageResponse result = storeImageService.addImage(storeId, request);

        assertThat(result).isNotNull();
        assertThat(result.getImageUrl()).isEqualTo(uploadResponse.getImageUrl());
        assertThat(result.isPrimary()).isFalse();

        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
        verify(fileStorageService).uploadImage(eq(file), eq("stores/" + storeId));
        verify(storeImageRepository).save(any(StoreImage.class));
    }

    @Test
    @DisplayName("addImage - demotes existing primary image when new primary is added")
    void addImage_AsPrimary_DemotesExistingPrimary() {
        MockMultipartFile file = new MockMultipartFile("file", "primary.jpg", "image/jpeg", "content".getBytes());
        CreateStoreImageRequest request = CreateStoreImageRequest.builder()
                .file(file)
                .isPrimary(true)
                .build();

        StoreImage existingPrimary = StoreImage.builder()
                .storeImageId(UUID.randomUUID())
                .store(store)
                .imageUrl("https://old-primary.jpg")
                .primary(true)
                .build();

        when(storeRepository.findById(storeId)).thenReturn(Optional.of(store));
        when(storeImageRepository.findByStoreStoreIdAndPrimaryTrue(storeId)).thenReturn(Optional.of(existingPrimary));

        UploadImageResponse uploadResponse = UploadImageResponse.builder()
                .imageUrl("https://new-primary.jpg")
                .publicId("moodcafe/stores/" + storeId + "/new-primary")
                .build();
        when(fileStorageService.uploadImage(eq(file), eq("stores/" + storeId))).thenReturn(uploadResponse);

        StoreImage savedImage = StoreImage.builder()
                .storeImageId(imageId)
                .store(store)
                .imageUrl(uploadResponse.getImageUrl())
                .primary(true)
                .build();
        when(storeImageRepository.save(any(StoreImage.class))).thenReturn(savedImage);

        storeImageService.addImage(storeId, request);

        assertThat(existingPrimary.isPrimary()).isFalse();
        verify(storeImageRepository).save(existingPrimary);
        verify(storeImageRepository).save(any(StoreImage.class));
    }

    @Test
    @DisplayName("removeImage - deletes from Cloudinary and deletes database entity")
    void removeImage_Success() {
        String imageUrl = "https://res.cloudinary.com/demo/image/upload/v1/moodcafe/stores/" + storeId + "/store.jpg";
        StoreImage image = StoreImage.builder()
                .storeImageId(imageId)
                .store(store)
                .imageUrl(imageUrl)
                .build();

        when(storeImageRepository.findByStoreImageIdAndStoreStoreId(imageId, storeId))
                .thenReturn(Optional.of(image));

        storeImageService.removeImage(storeId, imageId);

        verify(storeStaffService).requireStoreAccess(storeId, "OWNER", "MANAGER");
        verify(fileStorageService).deleteImageByUrl(imageUrl);
        verify(storeImageRepository).delete(image);
    }

    @Test
    @DisplayName("removeImage - throws STORE_IMAGE_NOT_FOUND when image does not exist")
    void removeImage_NotFound_ThrowsException() {
        when(storeImageRepository.findByStoreImageIdAndStoreStoreId(imageId, storeId))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> storeImageService.removeImage(storeId, imageId))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.STORE_IMAGE_NOT_FOUND);

        verifyNoInteractions(fileStorageService);
    }
}

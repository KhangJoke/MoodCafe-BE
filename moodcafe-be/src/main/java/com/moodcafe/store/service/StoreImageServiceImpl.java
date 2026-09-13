package com.moodcafe.store.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreImageService;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.request.CreateStoreImageRequest;
import com.moodcafe.store.dto.response.StoreImageResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreImage;
import com.moodcafe.store.mapper.StoreImageMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreImageServiceImpl implements StoreImageService {

    private final StoreRepository storeRepository;
    private final StoreImageRepository storeImageRepository;
    private final StoreImageMapper storeImageMapper;
    private final StoreStaffService storeStaffService;

    @Override
    @Transactional
    public StoreImageResponse addImage(UUID storeId, CreateStoreImageRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        boolean isPrimary = Boolean.TRUE.equals(request.getIsPrimary());

        // If setting as primary, demote any existing primary image for this store
        if (isPrimary) {
            storeImageRepository.findByStoreStoreIdAndPrimaryTrue(storeId)
                    .ifPresent(existingPrimary -> {
                        existingPrimary.setPrimary(false);
                        storeImageRepository.save(existingPrimary);
                    });
        }

        StoreImage image = StoreImage.builder()
                .store(store)
                .imageUrl(request.getImageUrl().trim())
                .primary(isPrimary)
                .build();

        image = storeImageRepository.save(image);
        return storeImageMapper.toResponse(image);
    }

    @Override
    @Transactional
    public void removeImage(UUID storeId, UUID imageId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        StoreImage image = storeImageRepository.findByStoreImageIdAndStoreStoreId(imageId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_IMAGE_NOT_FOUND));

        storeImageRepository.delete(image);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreImageResponse> getStoreImages(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        return storeImageRepository.findAllByStoreStoreId(storeId)
                .stream()
                .map(storeImageMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public StoreImageResponse setPrimaryImage(UUID storeId, UUID imageId) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        StoreImage targetImage = storeImageRepository.findByStoreImageIdAndStoreStoreId(imageId, storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_IMAGE_NOT_FOUND));

        // Demote existing primary image if different
        storeImageRepository.findByStoreStoreIdAndPrimaryTrue(storeId)
                .filter(img -> !img.getStoreImageId().equals(imageId))
                .ifPresent(existingPrimary -> {
                    existingPrimary.setPrimary(false);
                    storeImageRepository.save(existingPrimary);
                });

        targetImage.setPrimary(true);
        targetImage = storeImageRepository.save(targetImage);

        return storeImageMapper.toResponse(targetImage);
    }
}

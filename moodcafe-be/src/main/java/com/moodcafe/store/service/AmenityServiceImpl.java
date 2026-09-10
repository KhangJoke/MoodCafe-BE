package com.moodcafe.store.service;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.AmenityRepository;
import com.moodcafe.store.abstraction.repository.StoreAmenityRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.IAmenityService;
import com.moodcafe.store.dto.request.CreateAmenityRequest;
import com.moodcafe.store.dto.request.UpdateAmenityRequest;
import com.moodcafe.store.dto.response.AmenityResponse;
import com.moodcafe.store.entity.Amenity;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreAmenity;
import com.moodcafe.store.mapper.AmenityMapper;
import com.moodcafe.store.security.StoreSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AmenityServiceImpl implements IAmenityService {

    private final AmenityRepository amenityRepository;
    private final StoreRepository storeRepository;
    private final StoreAmenityRepository storeAmenityRepository;
    private final AmenityMapper amenityMapper;
    private final StoreSecurityService storeSecurityService;

    @Override
    @Transactional
    public AmenityResponse createAmenity(CreateAmenityRequest request) {
        storeSecurityService.requireSystemAdmin();

        String trimmedName = request.getName().trim();
        if (amenityRepository.existsByName(trimmedName)) {
            throw new AppException(ErrorCode.AMENITY_ALREADY_EXISTS);
        }

        Amenity amenity = amenityMapper.toEntity(request);
        amenity.setName(trimmedName);
        amenity = amenityRepository.save(amenity);

        return amenityMapper.toResponse(amenity);
    }

    @Override
    @Transactional
    public AmenityResponse updateAmenity(UUID amenityId, UpdateAmenityRequest request) {
        storeSecurityService.requireSystemAdmin();

        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> new AppException(ErrorCode.AMENITY_NOT_FOUND));

        if (request.getName() != null && !request.getName().isBlank()) {
            String trimmedName = request.getName().trim();
            if (!trimmedName.equalsIgnoreCase(amenity.getName()) && amenityRepository.existsByName(trimmedName)) {
                throw new AppException(ErrorCode.AMENITY_ALREADY_EXISTS);
            }
            amenity.setName(trimmedName);
        }

        amenityMapper.updateEntity(request, amenity);
        amenity = amenityRepository.save(amenity);

        return amenityMapper.toResponse(amenity);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponse> getAllAmenities() {
        return amenityRepository.findAll()
                .stream()
                .map(amenityMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional
    public void addAmenityToStore(UUID storeId, UUID amenityId) {
        storeSecurityService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        Amenity amenity = amenityRepository.findById(amenityId)
                .orElseThrow(() -> new AppException(ErrorCode.AMENITY_NOT_FOUND));

        if (storeAmenityRepository.existsByStoreStoreIdAndAmenityAmenityId(storeId, amenityId)) {
            throw new AppException(ErrorCode.STORE_AMENITY_ALREADY_EXISTS);
        }

        StoreAmenity storeAmenity = StoreAmenity.builder()
                .store(store)
                .amenity(amenity)
                .build();

        storeAmenityRepository.save(storeAmenity);
    }

    @Override
    @Transactional
    public void removeAmenityFromStore(UUID storeId, UUID amenityId) {
        storeSecurityService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        if (!storeAmenityRepository.existsByStoreStoreIdAndAmenityAmenityId(storeId, amenityId)) {
            throw new AppException(ErrorCode.STORE_AMENITY_NOT_FOUND);
        }

        storeAmenityRepository.deleteByStoreStoreIdAndAmenityAmenityId(storeId, amenityId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AmenityResponse> getStoreAmenities(UUID storeId) {
        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        return storeAmenityRepository.findAllByStoreStoreId(storeId)
                .stream()
                .map(sa -> amenityMapper.toResponse(sa.getAmenity()))
                .toList();
    }
}

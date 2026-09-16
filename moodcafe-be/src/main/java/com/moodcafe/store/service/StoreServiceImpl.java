package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreAmenityRepository;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreRoleRepository;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.abstraction.service.StoreService;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.request.CreateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreRequest;
import com.moodcafe.store.dto.request.UpdateStoreStatusRequest;
import com.moodcafe.store.dto.response.AmenityResponse;
import com.moodcafe.store.dto.response.StoreImageResponse;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreRole;
import com.moodcafe.store.entity.StoreStaff;
import com.moodcafe.store.entity.enums.StoreStaffStatus;
import com.moodcafe.store.entity.enums.StoreStatus;
import com.moodcafe.store.mapper.AmenityMapper;
import com.moodcafe.store.mapper.StoreImageMapper;
import com.moodcafe.store.mapper.StoreMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreServiceImpl implements StoreService {

    private final StoreRepository storeRepository;
    private final StoreRoleRepository storeRoleRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final StoreImageRepository storeImageRepository;
    private final StoreAmenityRepository storeAmenityRepository;
    private final StoreMapper storeMapper;
    private final StoreImageMapper storeImageMapper;
    private final AmenityMapper amenityMapper;
    private final StoreStaffService storeStaffService;
    private final CurrentUserService currentUserService;

    @Override
    @Transactional
    public StoreResponse createStore(CreateStoreRequest request) {
        User currentUser = currentUserService.getCurrentUser();

        Store store = storeMapper.toEntity(request);
        store.setStatus(StoreStatus.PENDING);
        store = storeRepository.save(store);

        // Register the creator as the OWNER in store_staffs
        StoreRole ownerRole = storeRoleRepository.findByName("OWNER")
                .orElseThrow(() -> new AppException(ErrorCode.STORE_ROLE_NOT_FOUND, "OWNER role not found"));

        StoreStaff staff = StoreStaff.builder()
                .store(store)
                .user(currentUser)
                .storeRole(ownerRole)
                .status(StoreStaffStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        storeStaffRepository.save(staff);

        return toStoreResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public StoreResponse getStoreById(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));
        return toStoreResponse(store);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreResponse> getAllStores(String status) {
        List<Store> stores;
        if (status != null && !status.isBlank()) {
            try {
                StoreStatus storeStatus = StoreStatus.valueOf(status.trim().toUpperCase());
                stores = storeRepository.findAllByStatus(storeStatus);
            } catch (IllegalArgumentException e) {
                stores = List.of();
            }
        } else {
            stores = storeRepository.findAll();
        }
        return stores.stream()
                .map(this::toStoreResponse)
                .toList();
    }

    @Override
    @Transactional
    public StoreResponse updateStore(UUID storeId, UpdateStoreRequest request) {
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        storeMapper.updateEntity(request, store);
        store = storeRepository.save(store);

        return toStoreResponse(store);
    }

    @Override
    @Transactional
    public StoreResponse changeStoreStatus(UUID storeId, UpdateStoreStatusRequest request) {
        currentUserService.requireSystemAdmin();

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        store.setStatus(request.getStatus());
        store = storeRepository.save(store);

        return toStoreResponse(store);
    }

    private StoreResponse toStoreResponse(Store store) {
        StoreResponse response = storeMapper.toResponse(store);

        List<StoreImageResponse> images = storeImageRepository.findAllByStoreStoreId(store.getStoreId())
                .stream()
                .map(storeImageMapper::toResponse)
                .toList();
        response.setImages(images);

        List<AmenityResponse> amenities = storeAmenityRepository.findAllByStoreStoreId(store.getStoreId())
                .stream()
                .map(sa -> amenityMapper.toResponse(sa.getAmenity()))
                .toList();
        response.setAmenities(amenities);

        return response;
    }
}

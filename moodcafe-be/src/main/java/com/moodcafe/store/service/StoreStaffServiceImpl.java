package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.repository.UserRepository;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.repository.StoreRoleRepository;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.abstraction.service.IStoreStaffService;
import com.moodcafe.store.dto.request.AddStoreStaffRequest;
import com.moodcafe.store.dto.request.UpdateStoreStaffRequest;
import com.moodcafe.store.dto.response.StoreStaffResponse;
import com.moodcafe.store.dto.response.UserStoreResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreRole;
import com.moodcafe.store.entity.StoreStaff;
import com.moodcafe.store.mapper.StoreStaffMapper;
import com.moodcafe.store.security.StoreSecurityService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreStaffServiceImpl implements IStoreStaffService {

    private final StoreRepository storeRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final StoreRoleRepository storeRoleRepository;
    private final UserRepository userRepository;
    private final StoreStaffMapper storeStaffMapper;
    private final StoreSecurityService storeSecurityService;

    @Override
    @Transactional
    public StoreStaffResponse addStaff(UUID storeId, AddStoreStaffRequest request) {
        storeSecurityService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        User targetUser = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.USER_NOT_FOUND));

        if (storeStaffRepository.existsByStoreStoreIdAndUserUserId(storeId, targetUser.getUserId())) {
            throw new AppException(ErrorCode.STORE_STAFF_ALREADY_EXISTS);
        }

        StoreRole role = storeRoleRepository.findByName(request.getStoreRole().trim().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.STORE_ROLE_NOT_FOUND));

        StoreStaff staff = StoreStaff.builder()
                .store(store)
                .user(targetUser)
                .storeRole(role)
                .status("ACTIVE")
                .joinedAt(LocalDateTime.now())
                .build();

        staff = storeStaffRepository.save(staff);
        return storeStaffMapper.toResponse(staff);
    }

    @Override
    @Transactional
    public StoreStaffResponse updateStaffRole(UUID storeId, UUID userId, UpdateStoreStaffRequest request) {
        storeSecurityService.requireStoreAccess(storeId, "OWNER");

        StoreStaff staff = storeStaffRepository.findByStoreStoreIdAndUserUserId(storeId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_STAFF_NOT_FOUND));

        if (request.getStoreRole() != null && !request.getStoreRole().isBlank()) {
            StoreRole role = storeRoleRepository.findByName(request.getStoreRole().trim().toUpperCase())
                    .orElseThrow(() -> new AppException(ErrorCode.STORE_ROLE_NOT_FOUND));
            staff.setStoreRole(role);
        }

        if (request.getStatus() != null && !request.getStatus().isBlank()) {
            staff.setStatus(request.getStatus().trim().toUpperCase());
        }

        staff = storeStaffRepository.save(staff);
        return storeStaffMapper.toResponse(staff);
    }

    @Override
    @Transactional
    public void removeStaff(UUID storeId, UUID userId) {
        storeSecurityService.requireStoreAccess(storeId, "OWNER");

        if (!storeStaffRepository.existsByStoreStoreIdAndUserUserId(storeId, userId)) {
            throw new AppException(ErrorCode.STORE_STAFF_NOT_FOUND);
        }

        storeStaffRepository.deleteByStoreStoreIdAndUserUserId(storeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreStaffResponse> getStoreStaff(UUID storeId) {
        storeSecurityService.requireStoreAccess(storeId, "OWNER", "MANAGER", "STAFF");

        if (!storeRepository.existsById(storeId)) {
            throw new AppException(ErrorCode.STORE_NOT_FOUND);
        }

        return storeStaffRepository.findAllByStoreStoreId(storeId)
                .stream()
                .map(storeStaffMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserStoreResponse> getUserStores() {
        User currentUser = storeSecurityService.getCurrentAuthenticatedUser();

        return storeStaffRepository.findAllByUserUserId(currentUser.getUserId())
                .stream()
                .map(storeStaffMapper::toUserStoreResponse)
                .toList();
    }
}

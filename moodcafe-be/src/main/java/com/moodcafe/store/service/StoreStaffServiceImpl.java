package com.moodcafe.store.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.abstraction.service.RoleService;
import com.moodcafe.auth.abstraction.service.UserService;
import com.moodcafe.auth.entity.Role;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreImageRepository;
import com.moodcafe.store.abstraction.repository.StoreRepository;

import com.moodcafe.store.abstraction.repository.StoreRoleRepository;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.request.AddStoreStaffRequest;
import com.moodcafe.store.dto.request.CreateStaffAccountRequest;
import com.moodcafe.store.dto.request.UpdateStaffPasswordRequest;
import com.moodcafe.store.dto.request.UpdateStoreStaffRequest;
import com.moodcafe.store.dto.response.StoreStaffResponse;
import com.moodcafe.store.dto.response.UserStoreResponse;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreRole;
import com.moodcafe.store.entity.StoreStaff;
import com.moodcafe.store.entity.enums.StoreStaffStatus;
import com.moodcafe.store.mapper.StoreStaffMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.Arrays;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreStaffServiceImpl implements StoreStaffService {

    private final StoreRepository storeRepository;
    private final StoreStaffRepository storeStaffRepository;
    private final StoreRoleRepository storeRoleRepository;
    private final UserService userService;
    private final RoleService roleService;
    private final StoreStaffMapper storeStaffMapper;
    private final StoreImageRepository storeImageRepository;
    private final PasswordEncoder passwordEncoder;
    private final CurrentUserService currentUserService;


    @Override
    @Transactional
    public StoreStaffResponse createStaffAccount(UUID storeId, CreateStaffAccountRequest request) {
        requireStoreAccess(storeId, "OWNER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        String email = request.getEmail().trim().toLowerCase();
        if (userService.existsByEmail(email)) {
            throw new AppException(ErrorCode.EMAIL_ALREADY_REGISTERED, "Email đã tồn tại trong hệ thống");
        }

        Role merchantStaffRole;
        try {
            merchantStaffRole = roleService.getRoleByName("MERCHANT_STAFF");
        } catch (AppException e) {
            merchantStaffRole = roleService.getRoleByName("CUSTOMER");
        }

        User newStaffUser = User.builder()
                .fullName(request.getName().trim())
                .email(email)
                .password(passwordEncoder.encode(request.getPassword()))
                .role(merchantStaffRole)
                .active(true)
                .emailVerified(true)
                .requirePasswordChange(false)
                .firstLogin(false)
                .build();

        newStaffUser = userService.createUserEntity(newStaffUser);

        StoreRole role = storeRoleRepository.findByName(request.getStoreRole().trim().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.STORE_ROLE_NOT_FOUND));

        StoreStaff staff = StoreStaff.builder()
                .store(store)
                .user(newStaffUser)
                .storeRole(role)
                .status(StoreStaffStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        staff = storeStaffRepository.save(staff);
        return storeStaffMapper.toResponse(staff);
    }

    @Override
    @Transactional
    public StoreStaffResponse addStaff(UUID storeId, AddStoreStaffRequest request) {
        requireStoreAccess(storeId, "OWNER", "MANAGER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        User targetUser = userService.getUserEntityById(request.getUserId());

        if (storeStaffRepository.existsByStoreStoreIdAndUserUserId(storeId, targetUser.getUserId())) {
            throw new AppException(ErrorCode.STORE_STAFF_ALREADY_EXISTS);
        }

        StoreRole role = storeRoleRepository.findByName(request.getStoreRole().trim().toUpperCase())
                .orElseThrow(() -> new AppException(ErrorCode.STORE_ROLE_NOT_FOUND));

        StoreStaff staff = StoreStaff.builder()
                .store(store)
                .user(targetUser)
                .storeRole(role)
                .status(StoreStaffStatus.ACTIVE)
                .joinedAt(Instant.now())
                .build();

        staff = storeStaffRepository.save(staff);
        return storeStaffMapper.toResponse(staff);
    }

    @Override
    @Transactional
    public StoreStaffResponse updateStaffRole(UUID storeId, UUID userId, UpdateStoreStaffRequest request) {
        requireStoreAccess(storeId, "OWNER");

        StoreStaff staff = storeStaffRepository.findByStoreStoreIdAndUserUserId(storeId, userId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_STAFF_NOT_FOUND));

        if (request.getStoreRole() != null && !request.getStoreRole().isBlank()) {
            StoreRole role = storeRoleRepository.findByName(request.getStoreRole().trim().toUpperCase())
                    .orElseThrow(() -> new AppException(ErrorCode.STORE_ROLE_NOT_FOUND));
            staff.setStoreRole(role);
        }

        if (request.getStatus() != null) {
            staff.setStatus(request.getStatus());
        }

        staff = storeStaffRepository.save(staff);
        return storeStaffMapper.toResponse(staff);
    }

    @Override
    @Transactional
    public void updateStaffPassword(UUID storeId, UUID userId, UpdateStaffPasswordRequest request) {
        requireStoreAccess(storeId, "OWNER");

        if (!storeStaffRepository.existsByStoreStoreIdAndUserUserId(storeId, userId)) {
            throw new AppException(ErrorCode.STORE_STAFF_NOT_FOUND);
        }

        userService.updateUserPassword(userId, request.getNewPassword().trim());
    }

    @Override
    @Transactional
    public void removeStaff(UUID storeId, UUID userId) {
        requireStoreAccess(storeId, "OWNER");

        if (!storeStaffRepository.existsByStoreStoreIdAndUserUserId(storeId, userId)) {
            throw new AppException(ErrorCode.STORE_STAFF_NOT_FOUND);
        }

        storeStaffRepository.deleteByStoreStoreIdAndUserUserId(storeId, userId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<StoreStaffResponse> getStoreStaff(UUID storeId) {
        requireStoreAccess(storeId, "OWNER", "MANAGER", "STAFF");

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
        User currentUser = currentUserService.getCurrentUser();

        return storeStaffRepository.findAllByUserUserId(currentUser.getUserId())
                .stream()
                .map(staff -> {
                    UserStoreResponse res = storeStaffMapper.toUserStoreResponse(staff);
                    if (staff.getStore() != null) {
                        UUID storeId = staff.getStore().getStoreId();
                        storeImageRepository.findByStoreStoreIdAndPrimaryTrue(storeId)
                                .or(() -> storeImageRepository.findAllByStoreStoreId(storeId).stream().findFirst())
                                .ifPresent(img -> res.setPrimaryImageUrl(img.getImageUrl()));
                    }
                    return res;
                })
                .toList();
    }

    @Override
    public StoreStaff requireStoreAccess(UUID storeId, String... allowedRoles) {
        if (currentUserService.isSystemAdmin()) {
            return null;
        }

        User currentUser = currentUserService.getCurrentUser();
        StoreStaff staff = storeStaffRepository.findByStoreStoreIdAndUserUserId(storeId, currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS));


        if (StoreStaffStatus.ACTIVE != staff.getStatus()) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Store staff status is not active");
        }

        if (allowedRoles != null && allowedRoles.length > 0) {
            String staffRole = staff.getStoreRole().getName();
            boolean hasRole = Arrays.stream(allowedRoles)
                    .anyMatch(role -> role.equalsIgnoreCase(staffRole));
            if (!hasRole) {
                throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Insufficient store role permissions");
            }
        }

        return staff;
    }
}

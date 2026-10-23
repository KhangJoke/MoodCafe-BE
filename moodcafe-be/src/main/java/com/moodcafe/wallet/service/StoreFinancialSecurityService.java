package com.moodcafe.wallet.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.entity.StoreStaff;
import com.moodcafe.store.entity.enums.StoreStaffStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Enforces strict financial security boundary for store owners:
 * - Only an active user with role OWNER of the requested store is allowed.
 * - Non-owners (Customer, Manager, Cashier, or Owner of another store) receive 403 FORBIDDEN.
 * - System admins managing payouts use separate /api/admin endpoints.
 */
@Service
@RequiredArgsConstructor
public class StoreFinancialSecurityService {

    private final CurrentUserService currentUserService;
    private final StoreStaffRepository storeStaffRepository;

    public User requireActiveStoreOwner(UUID storeId) {
        User user = currentUserService.getCurrentUser();
        if (user == null) {
            throw new AppException(ErrorCode.UNAUTHORIZED, "Vui lòng đăng nhập");
        }

        StoreStaff staff = storeStaffRepository.findByStoreStoreIdAndUserUserId(storeId, user.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Bạn không thuộc cơ sở này"));

        if (staff.getStatus() != StoreStaffStatus.ACTIVE) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Tài khoản nhân sự của bạn chưa được kích hoạt hoặc đã bị khóa");
        }

        if (staff.getStoreRole() == null || !"OWNER".equalsIgnoreCase(staff.getStoreRole().getName())) {
            throw new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS, "Chỉ OWNER của cơ sở mới có quyền xem ví và thao tác tài chính");
        }

        return user;
    }
}

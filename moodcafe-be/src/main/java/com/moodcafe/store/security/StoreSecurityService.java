package com.moodcafe.store.security;

import com.moodcafe.auth.dto.user.CustomUserDetails;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.entity.StoreStaff;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class StoreSecurityService {

    private final StoreStaffRepository storeStaffRepository;

    public User getCurrentAuthenticatedUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            throw new AppException(ErrorCode.UNAUTHORIZED);
        }

        if (authentication.getPrincipal() instanceof CustomUserDetails customUserDetails) {
            return customUserDetails.user();
        }

        throw new AppException(ErrorCode.UNAUTHORIZED);
    }

    public boolean isSystemAdmin() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null) {
            return false;
        }
        return authentication.getAuthorities().stream()
                .anyMatch(grantedAuthority -> "ROLE_ADMIN".equals(grantedAuthority.getAuthority()));
    }

    public void requireSystemAdmin() {
        if (!isSystemAdmin()) {
            throw new AppException(ErrorCode.FORBIDDEN_ACTION, "Admin privileges required");
        }
    }

    /**
     * Checks if the current authenticated user is an ADMIN or an ACTIVE staff member with one of the allowed roles.
     */
    public StoreStaff requireStoreAccess(UUID storeId, String... allowedRoles) {
        if (isSystemAdmin()) {
            return null; // Admin has universal access
        }

        User currentUser = getCurrentAuthenticatedUser();
        StoreStaff staff = storeStaffRepository.findByStoreStoreIdAndUserUserId(storeId, currentUser.getUserId())
                .orElseThrow(() -> new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS));

        if (!"ACTIVE".equalsIgnoreCase(staff.getStatus())) {
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

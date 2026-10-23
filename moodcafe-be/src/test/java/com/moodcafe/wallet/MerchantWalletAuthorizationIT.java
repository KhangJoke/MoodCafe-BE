package com.moodcafe.wallet;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.Role;
import com.moodcafe.auth.entity.User;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreStaffRepository;
import com.moodcafe.store.entity.Store;
import com.moodcafe.store.entity.StoreRole;
import com.moodcafe.store.entity.StoreStaff;
import com.moodcafe.store.entity.enums.StoreStaffStatus;
import com.moodcafe.wallet.abstraction.repository.StoreBankAccountRepository;
import com.moodcafe.wallet.abstraction.repository.WithdrawalRequestRepository;
import com.moodcafe.wallet.controller.MerchantWalletController;
import com.moodcafe.wallet.controller.MerchantWithdrawalController;
import com.moodcafe.wallet.entity.MerchantWallet;
import com.moodcafe.wallet.entity.StoreBankAccount;
import com.moodcafe.wallet.service.MerchantWalletService;
import com.moodcafe.wallet.service.StoreFinancialSecurityService;
import com.moodcafe.wallet.service.WithdrawalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class MerchantWalletAuthorizationIT {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private StoreStaffRepository storeStaffRepository;

    @Mock
    private MerchantWalletService merchantWalletService;

    @Mock
    private WithdrawalRequestRepository withdrawalRequestRepository;

    @Mock
    private StoreBankAccountRepository bankAccountRepository;

    private StoreFinancialSecurityService financialSecurityService;
    private WithdrawalService withdrawalService;
    private MerchantWalletController walletController;
    private MerchantWithdrawalController withdrawalController;

    private UUID storeAId;
    private UUID storeBId;
    private User customerUser;
    private User managerUser;
    private User cashierUser;
    private User ownerAUser;
    private User ownerBUser;

    @BeforeEach
    void setUp() {
        financialSecurityService = new StoreFinancialSecurityService(currentUserService, storeStaffRepository);
        withdrawalService = new WithdrawalService(withdrawalRequestRepository, bankAccountRepository, merchantWalletService, financialSecurityService);
        walletController = new MerchantWalletController(merchantWalletService, null, financialSecurityService);
        withdrawalController = new MerchantWithdrawalController(withdrawalService, withdrawalRequestRepository, bankAccountRepository, financialSecurityService);

        storeAId = UUID.randomUUID();
        storeBId = UUID.randomUUID();

        customerUser = User.builder().userId(UUID.randomUUID()).email("customer@test.com").build();
        managerUser = User.builder().userId(UUID.randomUUID()).email("manager@test.com").build();
        cashierUser = User.builder().userId(UUID.randomUUID()).email("cashier@test.com").build();
        ownerAUser = User.builder().userId(UUID.randomUUID()).email("ownerA@test.com").build();
        ownerBUser = User.builder().userId(UUID.randomUUID()).email("ownerB@test.com").build();
    }

    @Test
    @DisplayName("1. Customer (no store staff record) accessing store wallet must receive 403 FORBIDDEN")
    void customerAccessStoreWallet_ThrowsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(customerUser);
        when(storeStaffRepository.findByStoreStoreIdAndUserUserId(storeAId, customerUser.getUserId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> walletController.getStoreWallet(storeAId));
        assertEquals(ErrorCode.FORBIDDEN_STORE_ACCESS, ex.getErrorCode());
    }

    @Test
    @DisplayName("2. Store MANAGER accessing store wallet must receive 403 FORBIDDEN (Only OWNER allowed)")
    void managerAccessStoreWallet_ThrowsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(managerUser);
        StoreRole managerRole = StoreRole.builder().name("MANAGER").build();
        StoreStaff staff = StoreStaff.builder()
                .status(StoreStaffStatus.ACTIVE)
                .storeRole(managerRole)
                .build();
        when(storeStaffRepository.findByStoreStoreIdAndUserUserId(storeAId, managerUser.getUserId()))
                .thenReturn(Optional.of(staff));

        AppException ex = assertThrows(AppException.class, () -> walletController.getStoreWallet(storeAId));
        assertEquals(ErrorCode.FORBIDDEN_STORE_ACCESS, ex.getErrorCode());
    }

    @Test
    @DisplayName("3. Store CASHIER creating withdrawal request must receive 403 FORBIDDEN")
    void cashierRequestWithdrawal_ThrowsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(cashierUser);
        StoreRole cashierRole = StoreRole.builder().name("CASHIER").build();
        StoreStaff staff = StoreStaff.builder()
                .status(StoreStaffStatus.ACTIVE)
                .storeRole(cashierRole)
                .build();
        when(storeStaffRepository.findByStoreStoreIdAndUserUserId(storeAId, cashierUser.getUserId()))
                .thenReturn(Optional.of(staff));

        MerchantWithdrawalController.RequestWithdrawalPayload payload = new MerchantWithdrawalController.RequestWithdrawalPayload();
        payload.setAmount(new BigDecimal("150000"));

        AppException ex = assertThrows(AppException.class, () -> withdrawalController.requestWithdrawal(storeAId, payload));
        assertEquals(ErrorCode.FORBIDDEN_STORE_ACCESS, ex.getErrorCode());
    }

    @Test
    @DisplayName("4. Owner of Store B attempting to view or withdraw from Store A must receive 403 FORBIDDEN")
    void crossStoreOwnerAccess_ThrowsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(ownerBUser);
        // Owner B is not staff of Store A
        when(storeStaffRepository.findByStoreStoreIdAndUserUserId(storeAId, ownerBUser.getUserId()))
                .thenReturn(Optional.empty());

        AppException ex = assertThrows(AppException.class, () -> walletController.getStoreWallet(storeAId));
        assertEquals(ErrorCode.FORBIDDEN_STORE_ACCESS, ex.getErrorCode());
    }

    @Test
    @DisplayName("5. Bank account belonging to Store A used on Store B must receive 403 FORBIDDEN")
    void bankAccountBelongingToDifferentStore_ThrowsForbidden() {
        when(currentUserService.getCurrentUser()).thenReturn(ownerAUser);
        StoreRole ownerRole = StoreRole.builder().name("OWNER").build();
        StoreStaff staff = StoreStaff.builder()
                .status(StoreStaffStatus.ACTIVE)
                .storeRole(ownerRole)
                .build();
        when(storeStaffRepository.findByStoreStoreIdAndUserUserId(storeBId, ownerAUser.getUserId()))
                .thenReturn(Optional.of(staff));

        UUID bankId = UUID.randomUUID();
        // Bank account belongs to storeAId, but request is for storeBId!
        StoreBankAccount foreignBank = StoreBankAccount.builder()
                .bankAccountId(bankId)
                .storeId(storeAId)
                .bankName("Vietcombank")
                .accountNumber("123456789")
                .accountHolderName("CAFE A")
                .isDeleted(false)
                .build();
        when(bankAccountRepository.findById(bankId)).thenReturn(Optional.of(foreignBank));

        AppException ex = assertThrows(AppException.class, () ->
                withdrawalService.requestWithdrawal(ownerAUser.getUserId(), storeBId, new BigDecimal("200000"), bankId, "IDEMP_1")
        );
        assertEquals(ErrorCode.FORBIDDEN_STORE_ACCESS, ex.getErrorCode());
    }

    @Test
    @DisplayName("6. Active OWNER of Store A successfully views wallet")
    void validOwnerAccessStoreWallet_Success() {
        when(currentUserService.getCurrentUser()).thenReturn(ownerAUser);
        StoreRole ownerRole = StoreRole.builder().name("OWNER").build();
        StoreStaff staff = StoreStaff.builder()
                .status(StoreStaffStatus.ACTIVE)
                .storeRole(ownerRole)
                .build();
        when(storeStaffRepository.findByStoreStoreIdAndUserUserId(storeAId, ownerAUser.getUserId()))
                .thenReturn(Optional.of(staff));

        MerchantWallet wallet = MerchantWallet.builder()
                .walletId(UUID.randomUUID())
                .storeId(storeAId)
                .pendingBalance(new BigDecimal("500000"))
                .availableBalance(new BigDecimal("1000000"))
                .lockedBalance(BigDecimal.ZERO)
                .build();
        when(merchantWalletService.getWallet(storeAId)).thenReturn(wallet);

        ResponseEntity<?> response = walletController.getStoreWallet(storeAId);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
    }
}

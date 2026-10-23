package com.moodcafe.voucher;

import com.moodcafe.voucher.abstraction.repository.CustomerVoucherRepository;
import com.moodcafe.voucher.entity.CustomerVoucher;
import com.moodcafe.voucher.service.VoucherLifecycleService;
import com.moodcafe.wallet.service.CustomerWalletService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VoucherTransitionTest {

    @Mock
    private CustomerVoucherRepository customerVoucherRepository;
    @Mock
    private CustomerWalletService customerWalletService;

    @InjectMocks
    private VoucherLifecycleService voucherLifecycleService;

    private UUID reservationId;
    private CustomerVoucher voucher;

    @BeforeEach
    void setUp() {
        reservationId = UUID.randomUUID();
        voucher = new CustomerVoucher();
        voucher.setVoucherId(UUID.randomUUID());
        voucher.setLockedReservationId(reservationId);
        voucher.setExpiresAt(Instant.now().plus(30, ChronoUnit.DAYS));
    }

    @Test
    @DisplayName("L2 Regression: Voucher USED không bao giờ bị chuyển lùi về AVAILABLE khi release")
    void testReleaseVoucher_DoesNotRevertUsedToAvailable() {
        voucher.setStatus("USED");
        when(customerVoucherRepository.findByLockedReservationIdWithLock(reservationId))
                .thenReturn(Optional.of(voucher));

        voucherLifecycleService.releaseVoucher(reservationId);

        assertThat(voucher.getStatus()).isEqualTo("USED");
        verify(customerVoucherRepository, never()).save(any());
    }

    @Test
    @DisplayName("L2: Voucher LOCKED hợp lệ được release về AVAILABLE nếu còn hạn")
    void testReleaseVoucher_ReleasesLockedToAvailable() {
        voucher.setStatus("LOCKED");
        when(customerVoucherRepository.findByLockedReservationIdWithLock(reservationId))
                .thenReturn(Optional.of(voucher));

        voucherLifecycleService.releaseVoucher(reservationId);

        assertThat(voucher.getStatus()).isEqualTo("AVAILABLE");
        assertThat(voucher.getLockedReservationId()).isNull();
        verify(customerVoucherRepository).save(voucher);
    }

    @Test
    @DisplayName("L2: Capture Voucher chuyển từ LOCKED sang USED an toàn")
    void testCaptureVoucher_CapturesLockedToUsed() {
        voucher.setStatus("LOCKED");
        when(customerVoucherRepository.findByLockedReservationIdWithLock(reservationId))
                .thenReturn(Optional.of(voucher));

        voucherLifecycleService.captureVoucher(reservationId);

        assertThat(voucher.getStatus()).isEqualTo("USED");
        assertThat(voucher.getUsedAt()).isNotNull();
        verify(customerVoucherRepository).save(voucher);
    }
}

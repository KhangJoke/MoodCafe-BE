package com.moodcafe.booking.service;

import com.moodcafe.booking.abstraction.repository.StoreBookingConfigRepository;
import com.moodcafe.booking.abstraction.service.BookingConfigService;
import com.moodcafe.booking.dto.request.UpdateStoreBookingConfigRequest;
import com.moodcafe.booking.dto.response.StoreBookingConfigResponse;
import com.moodcafe.booking.entity.StoreBookingConfig;
import com.moodcafe.booking.mapper.BookingMapper;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.store.abstraction.repository.StoreRepository;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.entity.Store;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class BookingConfigServiceImpl implements BookingConfigService {

    private final StoreBookingConfigRepository configRepository;
    private final StoreRepository storeRepository;
    private final StoreStaffService storeStaffService;
    private final BookingMapper bookingMapper;

    @Override
    @Transactional
    public StoreBookingConfigResponse getStoreBookingConfig(UUID storeId) {
        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        StoreBookingConfig config = configRepository.findByStoreStoreId(storeId)
                .orElseGet(() -> configRepository.save(
                        StoreBookingConfig.builder()
                                .store(store)
                                .isBookingEnabled(false)
                                .isTimeLimitEnabled(true)
                                .sittingDurationMins(120)
                                .cleanupBufferMins(15)
                                .gracePeriodMins(30)
                                .minAdvanceMins(30)
                                .freeCancelHours(2)
                                .isEarlyCheckInEnabled(true)
                                .maxEarlyCheckInMins(60)
                                .build()
                ));

        return bookingMapper.toConfigResponse(config);
    }

    @Override
    @Transactional
    public StoreBookingConfigResponse updateStoreBookingConfig(UUID storeId, UpdateStoreBookingConfigRequest request) {
        // Chỉ OWNER hoặc MANAGER mới có quyền sửa đổi cấu hình đặt chỗ
        storeStaffService.requireStoreAccess(storeId, "OWNER", "MANAGER");

        Store store = storeRepository.findById(storeId)
                .orElseThrow(() -> new AppException(ErrorCode.STORE_NOT_FOUND));

        StoreBookingConfig config = configRepository.findByStoreStoreId(storeId)
                .orElseGet(() -> StoreBookingConfig.builder().store(store).build());

        config.setIsBookingEnabled(request.getIsBookingEnabled());
        config.setIsTimeLimitEnabled(request.getIsTimeLimitEnabled());
        config.setSittingDurationMins(request.getSittingDurationMins());
        config.setCleanupBufferMins(request.getCleanupBufferMins());
        config.setGracePeriodMins(request.getGracePeriodMins());
        config.setMinAdvanceMins(request.getMinAdvanceMins());
        config.setFreeCancelHours(request.getFreeCancelHours());
        if (request.getIsEarlyCheckInEnabled() != null) {
            config.setIsEarlyCheckInEnabled(request.getIsEarlyCheckInEnabled());
        }
        if (request.getMaxEarlyCheckInMins() != null) {
            config.setMaxEarlyCheckInMins(request.getMaxEarlyCheckInMins());
        }

        config = configRepository.save(config);
        return bookingMapper.toConfigResponse(config);
    }
}

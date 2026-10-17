package com.moodcafe.booking.abstraction.service;

import com.moodcafe.booking.dto.request.UpdateStoreBookingConfigRequest;
import com.moodcafe.booking.dto.response.StoreBookingConfigResponse;

import java.util.UUID;

public interface BookingConfigService {

    StoreBookingConfigResponse getStoreBookingConfig(UUID storeId);

    StoreBookingConfigResponse updateStoreBookingConfig(UUID storeId, UpdateStoreBookingConfigRequest request);
}

package com.moodcafe.store.controller;

import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import com.moodcafe.shared.exceptions.GlobalExceptionHandler;
import com.moodcafe.store.abstraction.service.StoreService;
import com.moodcafe.store.abstraction.service.StoreStaffService;
import com.moodcafe.store.dto.response.MerchantDashboardResponse;
import com.moodcafe.store.dto.response.MerchantDashboardResponse.MerchantMetrics;
import com.moodcafe.store.dto.response.MerchantDashboardResponse.MerchantRecentReview;
import com.moodcafe.store.dto.response.MerchantDashboardResponse.MerchantRecentSnap;
import com.moodcafe.store.dto.response.MerchantDashboardResponse.MerchantStoreSummary;
import com.moodcafe.store.entity.enums.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class StoreControllerDashboardTest {

    private MockMvc mockMvc;

    @Mock
    private StoreService storeService;

    @Mock
    private StoreStaffService storeStaffService;

    @InjectMocks
    private StoreController storeController;

    private UUID storeId;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(storeController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
        storeId = UUID.randomUUID();
    }

    @Test
    @DisplayName("GET /api/stores/{storeId}/merchant/dashboard - 200 OK with MerchantDashboardResponse")
    void getMerchantDashboard_Success_Returns200AndData() throws Exception {
        MerchantDashboardResponse response = MerchantDashboardResponse.builder()
                .store(MerchantStoreSummary.builder()
                        .storeId(storeId)
                        .name("Test Cafe")
                        .address("456 Nguyen Thi Minh Khai")
                        .status(StoreStatus.ACTIVE)
                        .coverImageUrl("https://example.com/cover.jpg")
                        .build())
                .metrics(MerchantMetrics.builder()
                        .totalSnaps(5)
                        .totalReviews(10)
                        .averageRating(4.8)
                        .activeTagsCount(3)
                        .build())
                .recentSnaps(List.of(
                        MerchantRecentSnap.builder()
                                .visitVerificationId(UUID.randomUUID())
                                .imageUrl("https://example.com/snap1.jpg")
                                .userFullName("Customer One")
                                .userAvatarUrl("https://example.com/avatar1.jpg")
                                .capturedAt(Instant.now())
                                .distanceFromStoreMeters(new BigDecimal("10.0"))
                                .build()
                ))
                .recentReviews(List.of(
                        MerchantRecentReview.builder()
                                .reviewId(UUID.randomUUID())
                                .userFullName("Customer One")
                                .userAvatarUrl("https://example.com/avatar1.jpg")
                                .overallRating(new BigDecimal("5.0"))
                                .content("Great coffee!")
                                .createdAt(Instant.now())
                                .build()
                ))
                .build();

        when(storeService.getMerchantDashboard(storeId)).thenReturn(response);

        mockMvc.perform(get("/api/stores/{storeId}/merchant/dashboard", storeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.store.storeId").value(storeId.toString()))
                .andExpect(jsonPath("$.data.store.name").value("Test Cafe"))
                .andExpect(jsonPath("$.data.metrics.totalSnaps").value(5))
                .andExpect(jsonPath("$.data.metrics.totalReviews").value(10))
                .andExpect(jsonPath("$.data.metrics.averageRating").value(4.8))
                .andExpect(jsonPath("$.data.recentSnaps").isArray())
                .andExpect(jsonPath("$.data.recentSnaps[0].userFullName").value("Customer One"))
                .andExpect(jsonPath("$.data.recentReviews").isArray())
                .andExpect(jsonPath("$.data.recentReviews[0].content").value("Great coffee!"));
    }

    @Test
    @DisplayName("GET /api/stores/{storeId}/merchant/dashboard - 403 Forbidden when unauthorized access")
    void getMerchantDashboard_Forbidden_Returns403() throws Exception {
        when(storeService.getMerchantDashboard(storeId))
                .thenThrow(new AppException(ErrorCode.FORBIDDEN_STORE_ACCESS));

        mockMvc.perform(get("/api/stores/{storeId}/merchant/dashboard", storeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.code").value(403))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.FORBIDDEN_STORE_ACCESS.name()));
    }

    @Test
    @DisplayName("GET /api/stores/{storeId}/merchant/dashboard - 404 Not Found when store does not exist")
    void getMerchantDashboard_NotFound_Returns404() throws Exception {
        when(storeService.getMerchantDashboard(storeId))
                .thenThrow(new AppException(ErrorCode.STORE_NOT_FOUND));

        mockMvc.perform(get("/api/stores/{storeId}/merchant/dashboard", storeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.errorCode").value(ErrorCode.STORE_NOT_FOUND.name()));
    }
}

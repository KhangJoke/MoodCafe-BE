package com.moodcafe.store.controller;

import com.moodcafe.shared.exceptions.GlobalExceptionHandler;
import com.moodcafe.store.abstraction.service.StoreReviewService;
import com.moodcafe.store.dto.response.ReviewReportStatisticsResponse;
import com.moodcafe.store.dto.response.TopReportedStoreResponse;
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

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminReviewReportStatisticsTest {

    private MockMvc mockMvc;

    @Mock
    private StoreReviewService storeReviewService;

    @InjectMocks
    private AdminReviewController adminReviewController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminReviewController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/reviews/reports/statistics - Returns 200 with review report counts")
    void getReviewReportStatistics_Success() throws Exception {
        ReviewReportStatisticsResponse response = ReviewReportStatisticsResponse.builder()
                .total(45)
                .pending(8)
                .resolved(32)
                .dismissed(5)
                .byReason(Map.of(
                        "SPAM", 15L,
                        "HARASSMENT", 10L,
                        "INAPPROPRIATE_CONTENT", 12L,
                        "OTHER", 8L
                ))
                .build();

        when(storeReviewService.getReviewReportStatistics()).thenReturn(response);

        mockMvc.perform(get("/api/admin/reviews/reports/statistics")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.total").value(45))
                .andExpect(jsonPath("$.data.pending").value(8))
                .andExpect(jsonPath("$.data.resolved").value(32))
                .andExpect(jsonPath("$.data.dismissed").value(5))
                .andExpect(jsonPath("$.data.byReason.SPAM").value(15));

        // Test alias endpoint
        mockMvc.perform(get("/api/admin/reviews/reports/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.total").value(45));
    }

    @Test
    @DisplayName("GET /api/admin/reviews/reports/top-reported-stores - Returns 200 with top stores")
    void getTopReportedStores_Success() throws Exception {
        UUID storeId = UUID.randomUUID();
        TopReportedStoreResponse topStore = TopReportedStoreResponse.builder()
                .storeId(storeId)
                .storeName("The Coffee House")
                .reportCount(12)
                .avgRating(3.8)
                .build();

        when(storeReviewService.getTopReportedStores(10)).thenReturn(List.of(topStore));

        mockMvc.perform(get("/api/admin/reviews/reports/top-reported-stores")
                        .param("limit", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].storeName").value("The Coffee House"))
                .andExpect(jsonPath("$.data[0].reportCount").value(12))
                .andExpect(jsonPath("$.data[0].avgRating").value(3.8));
    }
}

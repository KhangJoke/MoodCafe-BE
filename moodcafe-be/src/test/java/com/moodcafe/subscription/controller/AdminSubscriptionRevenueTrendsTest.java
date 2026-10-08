package com.moodcafe.subscription.controller;

import com.moodcafe.shared.exceptions.GlobalExceptionHandler;
import com.moodcafe.subscription.abstraction.service.SubscriptionService;
import com.moodcafe.subscription.dto.response.RevenueTrendItemResponse;
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
import java.time.LocalDate;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminSubscriptionRevenueTrendsTest {

    private MockMvc mockMvc;

    @Mock
    private SubscriptionService subscriptionService;

    @InjectMocks
    private AdminSubscriptionPlanController adminSubscriptionPlanController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminSubscriptionPlanController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/subscription-plans/reports/revenue-trends - Returns 200 with revenue trend items")
    void getRevenueTrends_Success() throws Exception {
        RevenueTrendItemResponse item1 = RevenueTrendItemResponse.builder()
                .date("2026-10-01")
                .revenue(new BigDecimal("1500000"))
                .transactionCount(5)
                .build();
        RevenueTrendItemResponse item2 = RevenueTrendItemResponse.builder()
                .date("2026-10-02")
                .revenue(new BigDecimal("2100000"))
                .transactionCount(7)
                .build();

        when(subscriptionService.getRevenueTrends(any(LocalDate.class), any(LocalDate.class), eq("DAILY")))
                .thenReturn(List.of(item1, item2));

        mockMvc.perform(get("/api/admin/subscription-plans/reports/revenue-trends")
                        .param("from", "2026-10-01")
                        .param("to", "2026-10-02")
                        .param("groupBy", "DAILY")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].date").value("2026-10-01"))
                .andExpect(jsonPath("$.data[0].revenue").value(1500000))
                .andExpect(jsonPath("$.data[0].transactionCount").value(5))
                .andExpect(jsonPath("$.data[1].date").value("2026-10-02"))
                .andExpect(jsonPath("$.data[1].revenue").value(2100000))
                .andExpect(jsonPath("$.data[1].transactionCount").value(7));
    }
}

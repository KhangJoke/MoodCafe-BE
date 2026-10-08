package com.moodcafe.dashboard.controller;

import com.moodcafe.dashboard.abstraction.service.AdminDashboardService;
import com.moodcafe.dashboard.dto.response.AdminDashboardStatsResponse;
import com.moodcafe.shared.exceptions.GlobalExceptionHandler;
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

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AdminDashboardControllerTest {

    private MockMvc mockMvc;

    @Mock
    private AdminDashboardService adminDashboardService;

    @InjectMocks
    private AdminDashboardController adminDashboardController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminDashboardController)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/dashboard/stats - Returns 200 with platform KPI overview")
    void getAdminDashboardStats_Success() throws Exception {
        AdminDashboardStatsResponse response = AdminDashboardStatsResponse.builder()
                .stores(AdminDashboardStatsResponse.StoreStats.builder()
                        .total(120)
                        .active(95)
                        .pending(15)
                        .inactive(7)
                        .rejected(3)
                        .build())
                .tags(AdminDashboardStatsResponse.TagStats.builder()
                        .pendingRequests(12)
                        .totalTags(48)
                        .build())
                .reviewReports(AdminDashboardStatsResponse.ReviewReportStats.builder()
                        .pending(8)
                        .total(45)
                        .build())
                .subscriptions(AdminDashboardStatsResponse.SubscriptionStats.builder()
                        .totalSubscribedOwners(35)
                        .totalRevenue(new BigDecimal("24500000"))
                        .monthlyRevenue(new BigDecimal("8200000"))
                        .build())
                .users(AdminDashboardStatsResponse.UserStats.builder()
                        .total(1250)
                        .merchants(120)
                        .customers(1130)
                        .build())
                .build();

        when(adminDashboardService.getAdminDashboardStats()).thenReturn(response);

        mockMvc.perform(get("/api/admin/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stores.total").value(120))
                .andExpect(jsonPath("$.data.stores.active").value(95))
                .andExpect(jsonPath("$.data.tags.pendingRequests").value(12))
                .andExpect(jsonPath("$.data.reviewReports.pending").value(8))
                .andExpect(jsonPath("$.data.subscriptions.totalSubscribedOwners").value(35))
                .andExpect(jsonPath("$.data.users.merchants").value(120))
                .andExpect(jsonPath("$.data.users.customers").value(1130));

        // Test alias endpoint
        mockMvc.perform(get("/api/admin/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.stores.total").value(120));
    }
}

package com.moodcafe.store.controller;

import com.moodcafe.shared.exceptions.GlobalExceptionHandler;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.abstraction.service.StoreService;
import com.moodcafe.store.dto.response.StoreResponse;
import com.moodcafe.store.entity.enums.StoreStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.data.web.PageableHandlerMethodArgumentResolver;

@ExtendWith(MockitoExtension.class)
class AdminStoreControllerTest {

    private MockMvc mockMvc;

    @Mock
    private StoreService storeService;

    @InjectMocks
    private AdminStoreController adminStoreController;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(adminStoreController)
                .setCustomArgumentResolvers(new PageableHandlerMethodArgumentResolver())
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    @Test
    @DisplayName("GET /api/admin/stores - Returns 200 with paged store list")
    void getAllStores_Success() throws Exception {
        UUID storeId = UUID.randomUUID();
        StoreResponse item = StoreResponse.builder()
                .storeId(storeId)
                .name("Mood Cafe District 1")
                .status(StoreStatus.ACTIVE)
                .phone("0901234567")
                .email("store@moodcafe.com")
                .build();

        PageResponse<StoreResponse> pageResponse = PageResponse.<StoreResponse>builder()
                .items(List.of(item))
                .page(0)
                .size(10)
                .totalElements(1)
                .totalPages(1)
                .last(true)
                .build();

        when(storeService.getAllStoresAdmin(eq(StoreStatus.ACTIVE), eq("Mood"), any(Pageable.class)))
                .thenReturn(pageResponse);

        mockMvc.perform(get("/api/admin/stores")
                        .param("status", "ACTIVE")
                        .param("search", "Mood")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.items[0].name").value("Mood Cafe District 1"))
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
    }
}

package com.moodcafe.store.controller;

import com.moodcafe.shared.exceptions.GlobalExceptionHandler;
import com.moodcafe.shared.response.PageResponse;
import com.moodcafe.store.abstraction.service.StoreService;
import com.moodcafe.store.dto.request.AdminStoreImportRequest;
import com.moodcafe.store.dto.response.AdminStoreImportResponse;
import com.moodcafe.store.dto.response.AdminStoreImportResponse.AdminStoreImportRowResult;
import com.moodcafe.store.dto.response.AdminStoreImportResponse.ImportOutcome;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
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

    @Test
    @DisplayName("POST /api/admin/stores/import - Returns 200 with batch import summary")
    void importStores_Success() throws Exception {
        UUID createdId = UUID.randomUUID();
        AdminStoreImportResponse response = AdminStoreImportResponse.builder()
                .totalSubmitted(1)
                .createdCount(1)
                .skippedCount(0)
                .failedCount(0)
                .results(List.of(AdminStoreImportRowResult.builder()
                        .rowNumber(2)
                        .storeName("Katinat Đồng Khởi")
                        .outcome(ImportOutcome.CREATED)
                        .storeId(createdId)
                        .message("Import quán thành công")
                        .build()))
                .build();

        when(storeService.importStoresAdmin(any(AdminStoreImportRequest.class))).thenReturn(response);

        String body = """
                {
                  "targetStatus": "ACTIVE",
                  "skipDuplicates": true,
                  "defaultOwnerEmail": "owner1@moodcafe.com",
                  "items": [
                    {
                      "rowNumber": 2,
                      "name": "Katinat Đồng Khởi",
                      "address": "123 Đồng Khởi, Quận 1",
                      "latitude": 10.7745,
                      "longitude": 106.7032,
                      "tagNames": ["Yên tĩnh", "Wifi"]
                    }
                  ]
                }
                """;

        mockMvc.perform(post("/api/admin/stores/import")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.createdCount").value(1))
                .andExpect(jsonPath("$.data.results[0].outcome").value("CREATED"));
    }
}

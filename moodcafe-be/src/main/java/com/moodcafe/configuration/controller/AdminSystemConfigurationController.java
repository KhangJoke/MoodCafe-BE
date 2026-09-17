package com.moodcafe.configuration.controller;

import com.moodcafe.configuration.abstraction.service.SystemConfigurationService;
import com.moodcafe.configuration.dto.request.UpdateConfigBatchRequest;
import com.moodcafe.configuration.dto.request.UpdateConfigItemRequest;
import com.moodcafe.configuration.dto.response.SystemConfigurationResponse;
import com.moodcafe.shared.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class AdminSystemConfigurationController {

    private final SystemConfigurationService configurationService;

    @GetMapping("/api/configs/public")
    public ResponseEntity<ApiResponse<List<SystemConfigurationResponse>>> getPublicConfigs() {
        List<SystemConfigurationResponse> configs = configurationService.getPublicConfigs();
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    @GetMapping("/api/admin/configs")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SystemConfigurationResponse>>> getAllConfigs(
            @RequestParam(required = false) String group) {
        List<SystemConfigurationResponse> configs = configurationService.getAllConfigs(group);
        return ResponseEntity.ok(ApiResponse.success(configs));
    }

    @GetMapping("/api/admin/configs/{configKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemConfigurationResponse>> getConfigByKey(
            @PathVariable String configKey) {
        SystemConfigurationResponse config = configurationService.getConfigByKey(configKey);
        return ResponseEntity.ok(ApiResponse.success(config));
    }

    @PutMapping("/api/admin/configs/{configKey}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<SystemConfigurationResponse>> updateConfig(
            @PathVariable String configKey,
            @Valid @RequestBody UpdateConfigItemRequest request) {
        SystemConfigurationResponse updated = configurationService.updateConfig(configKey, request.getConfigValue());
        return ResponseEntity.ok(ApiResponse.success(updated, "Configuration updated successfully"));
    }

    @PutMapping("/api/admin/configs/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ApiResponse<List<SystemConfigurationResponse>>> updateConfigsBatch(
            @Valid @RequestBody UpdateConfigBatchRequest request) {
        List<SystemConfigurationResponse> updated = configurationService.updateConfigsBatch(request);
        return ResponseEntity.ok(ApiResponse.success(updated, "Configurations updated successfully"));
    }
}

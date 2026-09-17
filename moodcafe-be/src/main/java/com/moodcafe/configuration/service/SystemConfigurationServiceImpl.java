package com.moodcafe.configuration.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.auth.entity.User;
import com.moodcafe.configuration.abstraction.repository.SystemConfigurationRepository;
import com.moodcafe.configuration.abstraction.service.SystemConfigurationService;
import com.moodcafe.configuration.config.CacheConfig;
import com.moodcafe.configuration.dto.request.UpdateConfigBatchRequest;
import com.moodcafe.configuration.dto.request.UpdateConfigItemRequest;
import com.moodcafe.configuration.dto.response.MatchScoreWeights;
import com.moodcafe.configuration.dto.response.SystemConfigurationResponse;
import com.moodcafe.configuration.entity.SystemConfiguration;
import com.moodcafe.configuration.mapper.SystemConfigurationMapper;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SystemConfigurationServiceImpl implements SystemConfigurationService {

    private final SystemConfigurationRepository configRepository;
    private final CurrentUserService currentUserService;
    private final SystemConfigurationMapper systemConfigurationMapper;

    @Override
    @Transactional(readOnly = true)
    public List<SystemConfigurationResponse> getAllConfigs(String group) {
        List<SystemConfiguration> configs;
        if (group != null && !group.isBlank()) {
            configs = configRepository.findAllByConfigGroupOrderByConfigKeyAsc(group.trim().toUpperCase());
        } else {
            configs = configRepository.findAll();
        }
        return configs.stream().map(systemConfigurationMapper::toResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CONFIG_CACHE, key = "'public_configs'")
    public List<SystemConfigurationResponse> getPublicConfigs() {
        return configRepository.findAllByIsPublicTrueOrderByConfigGroupAsc().stream()
                .map(systemConfigurationMapper::toResponse)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CONFIG_CACHE, key = "'config:' + #key")
    public SystemConfigurationResponse getConfigByKey(String key) {
        SystemConfiguration config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new AppException(ErrorCode.SYSTEM_CONFIG_NOT_FOUND));
        return systemConfigurationMapper.toResponse(config);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = CacheConfig.CONFIG_CACHE, key = "'match_score_weights'")
    public MatchScoreWeights getMatchScoreWeights() {
        double vibe = parseDoubleConfig("MATCH_WEIGHT_VIBE", 0.30);
        double purpose = parseDoubleConfig("MATCH_WEIGHT_PURPOSE", 0.30);
        double noise = parseDoubleConfig("MATCH_WEIGHT_NOISE", 0.15);
        double amenity = parseDoubleConfig("MATCH_WEIGHT_AMENITY", 0.15);
        double rating = parseDoubleConfig("MATCH_WEIGHT_RATING", 0.10);

        return MatchScoreWeights.builder()
                .vibeWeight(vibe)
                .purposeWeight(purpose)
                .noiseWeight(noise)
                .amenityWeight(amenity)
                .ratingWeight(rating)
                .build();
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CONFIG_CACHE, allEntries = true)
    public SystemConfigurationResponse updateConfig(String key, String value) {
        SystemConfiguration config = configRepository.findByConfigKey(key)
                .orElseThrow(() -> new AppException(ErrorCode.SYSTEM_CONFIG_NOT_FOUND));

        validateConfigValue(config.getDataType(), value);
        config.setConfigValue(value.trim());
        config.setUpdatedAt(Instant.now());
        setAuditUser(config);

        SystemConfiguration saved = configRepository.save(config);
        return systemConfigurationMapper.toResponse(saved);
    }

    @Override
    @Transactional
    @CacheEvict(value = CacheConfig.CONFIG_CACHE, allEntries = true)
    public List<SystemConfigurationResponse> updateConfigsBatch(UpdateConfigBatchRequest request) {
        if (request == null || request.getItems() == null || request.getItems().isEmpty()) {
            return List.of();
        }

        List<SystemConfigurationResponse> responses = new ArrayList<>();
        for (UpdateConfigItemRequest item : request.getItems()) {
            if (item.getConfigKey() != null && item.getConfigValue() != null) {
                responses.add(updateConfig(item.getConfigKey(), item.getConfigValue()));
            }
        }
        return responses;
    }

    private double parseDoubleConfig(String key, double defaultValue) {
        return configRepository.findByConfigKey(key)
                .map(cfg -> {
                    try {
                        return Double.parseDouble(cfg.getConfigValue());
                    } catch (NumberFormatException e) {
                        log.warn("Invalid DB number format for config key {}: {}", key, cfg.getConfigValue());
                        return defaultValue;
                    }
                })
                .orElse(defaultValue);
    }

    private void validateConfigValue(String dataType, String value) {
        if ("NUMBER".equalsIgnoreCase(dataType)) {
            try {
                Double.parseDouble(value.trim());
            } catch (NumberFormatException e) {
                throw new AppException(ErrorCode.INVALID_CONFIG_VALUE);
            }
        } else if ("BOOLEAN".equalsIgnoreCase(dataType)) {
            if (!"true".equalsIgnoreCase(value.trim()) && !"false".equalsIgnoreCase(value.trim())) {
                throw new AppException(ErrorCode.INVALID_CONFIG_VALUE);
            }
        }
    }

    private void setAuditUser(SystemConfiguration config) {
        try {
            User currentUser = currentUserService.getCurrentUser();
            if (currentUser != null) {
                config.setUpdatedBy(currentUser.getUserId());
            }
        } catch (Exception ignored) {
            // Unauthenticated or system background task
        }
    }
}

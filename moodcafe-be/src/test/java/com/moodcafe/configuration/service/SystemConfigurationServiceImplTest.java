package com.moodcafe.configuration.service;

import com.moodcafe.auth.abstraction.service.CurrentUserService;
import com.moodcafe.configuration.abstraction.repository.SystemConfigurationRepository;
import com.moodcafe.configuration.dto.request.UpdateConfigBatchRequest;
import com.moodcafe.configuration.dto.request.UpdateConfigItemRequest;
import com.moodcafe.configuration.dto.response.MatchScoreWeights;
import com.moodcafe.configuration.dto.response.SystemConfigurationResponse;
import com.moodcafe.configuration.entity.SystemConfiguration;
import com.moodcafe.configuration.mapper.SystemConfigurationMapper;
import com.moodcafe.shared.error.ErrorCode;
import com.moodcafe.shared.exceptions.AppException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemConfigurationServiceImplTest {

    @Mock
    private SystemConfigurationRepository configRepository;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private SystemConfigurationMapper systemConfigurationMapper;

    @InjectMocks
    private SystemConfigurationServiceImpl configurationService;

    private SystemConfiguration sampleConfig;

    @BeforeEach
    void setUp() {
        sampleConfig = SystemConfiguration.builder()
                .configId(UUID.randomUUID())
                .configGroup("MATCH_SCORE")
                .configKey("MATCH_WEIGHT_VIBE")
                .configValue("0.35")
                .dataType("NUMBER")
                .displayName("Trọng số Vibe")
                .description("Mô tả")
                .isPublic(true)
                .updatedAt(Instant.now())
                .build();

        lenient().when(systemConfigurationMapper.toResponse(any())).thenAnswer(invocation -> {
            SystemConfiguration entity = invocation.getArgument(0);
            if (entity == null) return null;
            return SystemConfigurationResponse.builder()
                    .configId(entity.getConfigId())
                    .configGroup(entity.getConfigGroup())
                    .configKey(entity.getConfigKey())
                    .configValue(entity.getConfigValue())
                    .dataType(entity.getDataType())
                    .displayName(entity.getDisplayName())
                    .description(entity.getDescription())
                    .isPublic(entity.isPublic())
                    .updatedAt(entity.getUpdatedAt())
                    .build();
        });
    }

    @Test
    @DisplayName("getAllConfigs - returns all configs when group is null")
    void getAllConfigs_NoGroup_ReturnsAll() {
        when(configRepository.findAll()).thenReturn(List.of(sampleConfig));

        List<SystemConfigurationResponse> result = configurationService.getAllConfigs(null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConfigKey()).isEqualTo("MATCH_WEIGHT_VIBE");
    }

    @Test
    @DisplayName("getAllConfigs - filters by group")
    void getAllConfigs_WithGroup_ReturnsFiltered() {
        when(configRepository.findAllByConfigGroupOrderByConfigKeyAsc("MATCH_SCORE"))
                .thenReturn(List.of(sampleConfig));

        List<SystemConfigurationResponse> result = configurationService.getAllConfigs("MATCH_SCORE");

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getConfigGroup()).isEqualTo("MATCH_SCORE");
    }

    @Test
    @DisplayName("getPublicConfigs - returns only public configs")
    void getPublicConfigs_Success() {
        when(configRepository.findAllByIsPublicTrueOrderByConfigGroupAsc())
                .thenReturn(List.of(sampleConfig));

        List<SystemConfigurationResponse> result = configurationService.getPublicConfigs();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).isPublic()).isTrue();
    }

    @Test
    @DisplayName("getConfigByKey - throws exception when key not found")
    void getConfigByKey_NotFound_ThrowsException() {
        when(configRepository.findByConfigKey("NON_EXISTENT")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> configurationService.getConfigByKey("NON_EXISTENT"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.SYSTEM_CONFIG_NOT_FOUND);
    }

    @Test
    @DisplayName("getMatchScoreWeights - parses weights from DB")
    void getMatchScoreWeights_ParsesValues() {
        SystemConfiguration vibeCfg = SystemConfiguration.builder().configKey("MATCH_WEIGHT_VIBE").configValue("0.30").build();
        SystemConfiguration purpCfg = SystemConfiguration.builder().configKey("MATCH_WEIGHT_PURPOSE").configValue("0.30").build();
        SystemConfiguration noiseCfg = SystemConfiguration.builder().configKey("MATCH_WEIGHT_NOISE").configValue("0.15").build();
        SystemConfiguration amenCfg = SystemConfiguration.builder().configKey("MATCH_WEIGHT_AMENITY").configValue("0.15").build();
        SystemConfiguration rateCfg = SystemConfiguration.builder().configKey("MATCH_WEIGHT_RATING").configValue("0.10").build();

        when(configRepository.findByConfigKey("MATCH_WEIGHT_VIBE")).thenReturn(Optional.of(vibeCfg));
        when(configRepository.findByConfigKey("MATCH_WEIGHT_PURPOSE")).thenReturn(Optional.of(purpCfg));
        when(configRepository.findByConfigKey("MATCH_WEIGHT_NOISE")).thenReturn(Optional.of(noiseCfg));
        when(configRepository.findByConfigKey("MATCH_WEIGHT_AMENITY")).thenReturn(Optional.of(amenCfg));
        when(configRepository.findByConfigKey("MATCH_WEIGHT_RATING")).thenReturn(Optional.of(rateCfg));

        MatchScoreWeights weights = configurationService.getMatchScoreWeights();

        assertThat(weights.getVibeWeight()).isEqualTo(0.30);
        assertThat(weights.getPurposeWeight()).isEqualTo(0.30);
        assertThat(weights.getNoiseWeight()).isEqualTo(0.15);
        assertThat(weights.getAmenityWeight()).isEqualTo(0.15);
        assertThat(weights.getRatingWeight()).isEqualTo(0.10);
    }

    @Test
    @DisplayName("updateConfig - updates and validates NUMBER format")
    void updateConfig_ValidNumber_Success() {
        when(configRepository.findByConfigKey("MATCH_WEIGHT_VIBE")).thenReturn(Optional.of(sampleConfig));
        when(configRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        SystemConfigurationResponse response = configurationService.updateConfig("MATCH_WEIGHT_VIBE", "0.40");

        assertThat(response.getConfigValue()).isEqualTo("0.40");
        verify(configRepository).save(sampleConfig);
    }

    @Test
    @DisplayName("updateConfig - throws exception on invalid NUMBER format")
    void updateConfig_InvalidNumber_ThrowsException() {
        when(configRepository.findByConfigKey("MATCH_WEIGHT_VIBE")).thenReturn(Optional.of(sampleConfig));

        assertThatThrownBy(() -> configurationService.updateConfig("MATCH_WEIGHT_VIBE", "not-a-number"))
                .isInstanceOf(AppException.class)
                .hasFieldOrPropertyWithValue("errorCode", ErrorCode.INVALID_CONFIG_VALUE);
    }

    @Test
    @DisplayName("updateConfigsBatch - updates all items in request")
    void updateConfigsBatch_Success() {
        when(configRepository.findByConfigKey("MATCH_WEIGHT_VIBE")).thenReturn(Optional.of(sampleConfig));
        when(configRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        UpdateConfigBatchRequest request = UpdateConfigBatchRequest.builder()
                .items(List.of(
                        UpdateConfigItemRequest.builder().configKey("MATCH_WEIGHT_VIBE").configValue("0.25").build()
                ))
                .build();

        List<SystemConfigurationResponse> responses = configurationService.updateConfigsBatch(request);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getConfigValue()).isEqualTo("0.25");
    }
}

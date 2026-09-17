package com.moodcafe.configuration.abstraction.service;

import com.moodcafe.configuration.dto.request.UpdateConfigBatchRequest;
import com.moodcafe.configuration.dto.response.MatchScoreWeights;
import com.moodcafe.configuration.dto.response.SystemConfigurationResponse;

import java.util.List;

public interface SystemConfigurationService {

    List<SystemConfigurationResponse> getAllConfigs(String group);

    List<SystemConfigurationResponse> getPublicConfigs();

    SystemConfigurationResponse getConfigByKey(String key);

    MatchScoreWeights getMatchScoreWeights();

    SystemConfigurationResponse updateConfig(String key, String value);

    List<SystemConfigurationResponse> updateConfigsBatch(UpdateConfigBatchRequest request);
}

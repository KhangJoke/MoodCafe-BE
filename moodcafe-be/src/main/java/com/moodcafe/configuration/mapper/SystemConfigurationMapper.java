package com.moodcafe.configuration.mapper;

import com.moodcafe.configuration.dto.response.SystemConfigurationResponse;
import com.moodcafe.configuration.entity.SystemConfiguration;
import org.mapstruct.Mapper;

import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface SystemConfigurationMapper {

    @Mapping(target = "isPublic", expression = "java(entity.isPublic())")
    SystemConfigurationResponse toResponse(SystemConfiguration entity);
}

package com.moodcafe.tag.mapper;

import com.moodcafe.tag.dto.request.CreateTagRequest;
import com.moodcafe.tag.dto.request.UpdateTagRequest;
import com.moodcafe.tag.dto.response.TagResponse;
import com.moodcafe.tag.entity.MasterTag;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface TagMapper {

    TagResponse toResponse(MasterTag tag);

    @Mapping(target = "tagId", ignore = true)
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "createdAt", ignore = true)
    MasterTag toEntity(CreateTagRequest request);

    @Mapping(target = "tagId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UpdateTagRequest request, @MappingTarget MasterTag tag);
}

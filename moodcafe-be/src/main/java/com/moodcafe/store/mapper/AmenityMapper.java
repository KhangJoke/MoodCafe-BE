package com.moodcafe.store.mapper;

import com.moodcafe.store.dto.request.CreateAmenityRequest;
import com.moodcafe.store.dto.request.UpdateAmenityRequest;
import com.moodcafe.store.dto.response.AmenityResponse;
import com.moodcafe.store.entity.Amenity;
import org.mapstruct.*;

@Mapper(componentModel = "spring")
public interface AmenityMapper {

    AmenityResponse toResponse(Amenity amenity);

    @Mapping(target = "amenityId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    Amenity toEntity(CreateAmenityRequest request);

    @BeanMapping(nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
    @Mapping(target = "amenityId", ignore = true)
    @Mapping(target = "createdAt", ignore = true)
    void updateEntity(UpdateAmenityRequest request, @MappingTarget Amenity amenity);
}

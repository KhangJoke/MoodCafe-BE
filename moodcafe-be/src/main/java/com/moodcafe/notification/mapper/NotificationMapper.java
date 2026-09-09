package com.moodcafe.notification.mapper;

import com.moodcafe.notification.dto.response.NotificationResponse;
import com.moodcafe.notification.entity.Notification;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);
}

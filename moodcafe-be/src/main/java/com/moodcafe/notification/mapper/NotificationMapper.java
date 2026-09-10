package com.moodcafe.notification.mapper;

import com.moodcafe.notification.dto.response.NotificationMessage;
import com.moodcafe.notification.dto.response.NotificationResponse;
import com.moodcafe.notification.entity.Notification;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface NotificationMapper {

    NotificationResponse toResponse(Notification notification);

    @Mapping(target = "id", source = "notificationId")
    @Mapping(target = "message", source = "content")
    NotificationMessage toMessage(Notification notification);
}

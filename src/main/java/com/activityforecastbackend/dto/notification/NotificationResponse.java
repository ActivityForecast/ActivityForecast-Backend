package com.activityforecastbackend.dto.notification;

import com.activityforecastbackend.entity.Notification;
import com.activityforecastbackend.entity.Notification.NotificationType;
import com.activityforecastbackend.entity.Notification.RelatedType;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class NotificationResponse {
    private Long id;
    private NotificationType type;
    private String title;
    private String content;
    private Long relatedId;
    private RelatedType relatedType;
    private boolean isRead;
    private LocalDateTime createdAt;

    // User 정보를 포함하지 않아 순환 참조를 방지
    public static NotificationResponse from(Notification notification) {
        return NotificationResponse.builder()
                .id(notification.getNotificationId())
                .type(notification.getNotificationType())
                .title(notification.getTitle())
                .content(notification.getContent())
                .relatedId(notification.getRelatedId())
                .relatedType(notification.getRelatedType())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
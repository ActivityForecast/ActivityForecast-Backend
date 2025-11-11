package com.activityforecastbackend.controller;

import com.activityforecastbackend.dto.notification.NotificationResponse;
import com.activityforecastbackend.service.NotificationService;
import com.activityforecastbackend.service.SseNotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import org.springframework.security.core.annotation.AuthenticationPrincipal;

import java.util.List;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {
    private final NotificationService notificationService;
    private final SseNotificationService sseNotificationService; // SSE 서비스 주입

    /**
     * 1. SSE 구독 엔드포인트 (실시간 알림 연결)
     * URL: GET /api/notifications/subscribe
     */
    @GetMapping(value = "/subscribe", produces = "text/event-stream")
    public SseEmitter subscribe(
            @AuthenticationPrincipal Long currentUserId) {

        return sseNotificationService.subscribe(currentUserId);
    }

    /**
     * 2. 알림 목록 조회 (종 모양 클릭 시)
     * URL: GET /api/notifications
     */
    @GetMapping
    public ResponseEntity<List<NotificationResponse>> getNotifications(
            @AuthenticationPrincipal Long currentUserId) {

        List<NotificationResponse> notifications = notificationService.getNotificationsByUserId(currentUserId);
        return ResponseEntity.ok(notifications);
    }

    /**
     * 3. 읽지 않은 알림 상태 확인 (빨간 점 표시)
     * URL: GET /api/notifications/unread
     */
    @GetMapping("/unread")
    public ResponseEntity<Boolean> getUnreadStatus(
            @AuthenticationPrincipal Long currentUserId) {

        boolean hasUnread = notificationService.hasUnreadNotifications(currentUserId);
        return ResponseEntity.ok(hasUnread);
    }

    /**
     * 4. 모두 읽음 처리
     * URL: PUT /api/notifications/read
     */
    @PutMapping("/read")
    public ResponseEntity<Void> markAsRead(
            @AuthenticationPrincipal Long currentUserId) {

        notificationService.markAllAsRead(currentUserId);
        return ResponseEntity.noContent().build();
    }
}

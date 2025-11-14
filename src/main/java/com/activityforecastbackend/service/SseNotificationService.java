package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.notification.NotificationResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class SseNotificationService {

    private static final Long DEFAULT_TIMEOUT = 10 * 60 * 1000L;
    private final Map<Long, SseEmitter> emitters = new ConcurrentHashMap<>();

    // 1. 클라이언트와 연결을 생성하고 SseEmitter를 맵에 등록
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(DEFAULT_TIMEOUT);
        emitters.put(userId, emitter);

        emitter.onCompletion(() -> emitters.remove(userId));
        emitter.onTimeout(() -> {
            emitter.complete();
            emitters.remove(userId);
        });
        emitter.onError((e) -> emitters.remove(userId)); // 에러 발생 시 제거

        // 더미 데이터 전송 (503 방지)
        try {
            emitter.send(SseEmitter.event().id("0").name("connect").data("Connected"));
        } catch (IOException e) {
            log.error("SSE 연결 초기화 실패", e);
        }

        return emitter;
    }

    // 2. 특정 사용자에게 알림 데이터를 실시간으로 전송
    public void sendNotification(Long userId, NotificationResponse notification) {
        SseEmitter emitter = emitters.get(userId);

        if (emitter != null) {
            try {
                emitter.send(SseEmitter.event()
                        .id(String.valueOf(notification.getId()))
                        .name("notification")
                        .data(notification));
                log.info("알림 전송 성공: User {}", userId);
            } catch (IOException e) {
                log.error("알림 전송 실패: User {}", userId, e);
                emitter.completeWithError(e);
                emitters.remove(userId);
            }
        }
    }
}

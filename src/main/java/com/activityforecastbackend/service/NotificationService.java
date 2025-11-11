package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.notification.NotificationResponse;
import com.activityforecastbackend.entity.Crew;
import com.activityforecastbackend.entity.CrewMember;
import com.activityforecastbackend.entity.Notification;
import com.activityforecastbackend.entity.Schedule;
import com.activityforecastbackend.entity.User;
import com.activityforecastbackend.repository.CrewMemberRepository;
import com.activityforecastbackend.repository.NotificationRepository;
import com.activityforecastbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

import static com.activityforecastbackend.entity.Notification.NotificationType.CREW_MEMBER_JOIN;
import static com.activityforecastbackend.entity.Notification.NotificationType.CREW_SCHEDULE;

@Service
@RequiredArgsConstructor
public class NotificationService {
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final CrewMemberRepository crewMemberRepository; // 멤버 조회를 위해 추가
    private final SseNotificationService sseNotificationService; // SSE 서비스 주입

    // --- 1. 알림 생성 및 푸시 로직 ---

    @Transactional
    private void saveAndSend(Notification notification) {
        notificationRepository.save(notification);
        NotificationResponse response = NotificationResponse.from(notification);
        sseNotificationService.sendNotification(notification.getUser().getUserId(), response);
    }

    /**
     * 크루 가입 완료 알림 (스크린샷: "활동하조 크루에 가입되었습니다.")
     */
    @Transactional
    public void notifyCrewMemberJoin(Long targetUserId, Crew crew) {
        User targetUser = userRepository.findById(targetUserId).orElse(null);
        if (targetUser == null) return;

        //핵심 수정: 임시 로직을 제거하고, 엔티티의 팩토리 메서드를 사용하여 알림 객체를 생성합니다.
        Notification notification = Notification.createCrewMemberJoinNotification(
                targetUser,
                crew.getCrewName(),
                crew.getCrewId()
        );

        // 알림 저장 및 실시간 푸시
        saveAndSend(notification);
    }

    /**
     * 일정 생성 알림 (스크린샷: "농구하조 크루에서 10월 20일 일정이 생성되었습니다.")
     */
    @Transactional
    public void notifyScheduleCreated(Long crewId, Schedule schedule, String activityName) {
        // 1. 크루의 모든 활성 멤버 조회
        Crew crew = schedule.getCrew();
        List<CrewMember> activeMemberships = crewMemberRepository.findByCrewAndIsActiveTrue(crew);

        // 2. 각 멤버에게 알림 생성 및 푸시
        for (CrewMember membership : activeMemberships) {
            User user = membership.getUser();

            // Notification.java의 createCrewScheduleNotification 팩토리 메서드 활용
            Notification notification = Notification.createCrewScheduleNotification(
                    user,
                    crew.getCrewName(),
                    activityName,
                    schedule.getScheduleId() // relatedId로 scheduleId 대신 crewScheduleId를 사용해야 할 수 있음
            );
            saveAndSend(notification);
        }
    }

    // --- 2. 알림 조회 및 읽음 처리 로직 ---

    @Transactional(readOnly = true)
    public List<NotificationResponse> getNotificationsByUserId(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        // 최신순으로 정렬하여 알림 목록 조회 (최대 10개)
        List<Notification> notifications = notificationRepository.findTop10ByUserOrderByCreatedAtDesc(user);

        return notifications.stream()
                .map(NotificationResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void markAllAsRead(Long userId) {
        notificationRepository.markAllAsReadByUserId(userId);
    }

    @Transactional(readOnly = true)
    public boolean hasUnreadNotifications(Long userId) {
        return notificationRepository.existsByUserIdAndIsReadFalse(userId);
    }
}

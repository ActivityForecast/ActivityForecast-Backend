package com.activityforecastbackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notification_user_id", columnList = "user_id"),
        @Index(name = "idx_notification_user_read", columnList = "user_id, is_read"),
        @Index(name = "idx_notification_created_at", columnList = "created_at")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Notification {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long notificationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 50)
    private NotificationType notificationType;

    @Column(name = "title", nullable = false, length = 255)
    private String title;

    @Column(name = "content", columnDefinition = "TEXT")
    private String content;

    @Column(name = "related_id")
    private Long relatedId;

    @Enumerated(EnumType.STRING)
    @Column(name = "related_type", length = 50)
    private RelatedType relatedType;

    @Column(name = "is_read", nullable = false)
    private Boolean isRead = false;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum NotificationType {
        CREW_INVITE,
        SCHEDULE_REMINDER,
        CREW_SCHEDULE,
        ACTIVITY_RECOMMENDATION,
        CREW_MEMBER_JOIN,
        CREW_SCHEDULE_UPDATE,
        RATING_REQUEST,
        CREW_DISBANDED,
        CREW_CREATED,
        CREW_SCHEDULE_DELETE  //추가
    }

    public enum RelatedType {
        CREW,
        SCHEDULE,
        CREW_SCHEDULE,
        ACTIVITY,
        USER
    }

    public static Notification createCrewInviteNotification(User user, String crewName, Long crewId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.CREW_INVITE);
        notification.setTitle("크루 초대");
        notification.setContent(crewName + " 크루에 초대되었습니다.");
        notification.setRelatedId(crewId);
        notification.setRelatedType(RelatedType.CREW);
        return notification;
    }

    //가입 완료 알람 추가
    public static Notification createCrewMemberJoinNotification(User user, String crewName, Long crewId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.CREW_MEMBER_JOIN);
        notification.setTitle("크루 가입");
        notification.setContent(crewName + " 크루에 가입되었습니다.");
        notification.setRelatedId(crewId);
        notification.setRelatedType(RelatedType.CREW);
        return notification;
    }

    // 크루 일정 수정
    public static Notification createCrewScheduleUpdateNotification(User user, String crewName, String activityName, Long crewScheduleId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.CREW_SCHEDULE_UPDATE);
        notification.setTitle("크루 일정 수정");
        notification.setContent(String.format("[%s] 크루의 %s 일정이 변경되었습니다.", crewName, activityName));
        notification.setRelatedId(crewScheduleId);
        notification.setRelatedType(RelatedType.CREW_SCHEDULE);
        return notification;
    }

    // 크루 일정 삭제
    public static Notification createCrewScheduleDeleteNotification(User user, String crewName, String activityName, Long crewId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.CREW_SCHEDULE_DELETE);
        notification.setTitle("크루 일정 삭제");
        notification.setContent(String.format("[%s] 크루의 %s 일정이 취소되었습니다.", crewName, activityName));
        notification.setRelatedId(crewId);
        notification.setRelatedType(RelatedType.CREW);
        return notification;
    }

    // [크루 이름] 크루해체, 크루해제 추가
    public static Notification createCrewDisbandedNotification(User user, String crewName) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.CREW_DISBANDED);
        notification.setTitle(String.format("[%s] 크루 해체", crewName));
        notification.setContent("크루가 해체되어 활동이 종료되었습니다.");
        notification.setRelatedId(null);
        notification.setRelatedType(RelatedType.CREW);
        return notification;
    }

    // 크루 생성 추가
    public static Notification createCrewCreatedNotification(User user, String crewName, Long crewId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.CREW_CREATED);
        notification.setTitle(String.format("[%s] 생성 완료", crewName));
        notification.setContent("새로운 크루 생성을 축하합니다! 이제 멤버를 초대하거나 일정을 만들어보세요.");
        notification.setRelatedId(crewId);
        notification.setRelatedType(RelatedType.CREW);
        return notification;
    }

    public static Notification createScheduleReminderNotification(User user, String activityName, Long scheduleId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.SCHEDULE_REMINDER);
        notification.setTitle("일정 알림");
        notification.setContent(activityName + " 활동 일정이 곧 시작됩니다.");
        notification.setRelatedId(scheduleId);
        notification.setRelatedType(RelatedType.SCHEDULE);
        return notification;
    }

    public static Notification createCrewScheduleNotification(User user, String crewName, String activityName, Long crewScheduleId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.CREW_SCHEDULE);
        notification.setTitle("크루 일정");
        notification.setContent(crewName + " 크루에서 " + activityName + " 일정이 생성되었습니다.");
        notification.setRelatedId(crewScheduleId);
        notification.setRelatedType(RelatedType.CREW_SCHEDULE);
        return notification;
    }

    public static Notification createActivityRecommendationNotification(User user, String activityName, Long activityId) {
        Notification notification = new Notification();
        notification.setUser(user);
        notification.setNotificationType(NotificationType.ACTIVITY_RECOMMENDATION);
        notification.setTitle("활동 추천");
        notification.setContent("오늘 날씨에 " + activityName + " 활동을 추천드립니다.");
        notification.setRelatedId(activityId);
        notification.setRelatedType(RelatedType.ACTIVITY);
        return notification;
    }



    public void markAsRead() {
        this.isRead = true;
    }

    public void markAsUnread() {
        this.isRead = false;
    }

    public boolean isUnread() {
        return !this.isRead;
    }
}
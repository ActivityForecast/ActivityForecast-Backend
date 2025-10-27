package com.activityforecastbackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs", indexes = {
        @Index(name = "idx_system_log_admin_id", columnList = "admin_id"),
        @Index(name = "idx_system_log_created_at", columnList = "created_at"),
        @Index(name = "idx_system_log_action_type", columnList = "action_type")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SystemLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "admin_id")
    private User admin;

    @Enumerated(EnumType.STRING)
    @Column(name = "action_type", nullable = false, length = 50)
    private ActionType actionType;

    @Enumerated(EnumType.STRING)
    @Column(name = "target_type", length = 50)
    private TargetType targetType;

    @Column(name = "target_id")
    private Long targetId;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "ip_address", length = 45)
    private String ipAddress;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public enum ActionType {
        USER_DELETE,
        USER_UPDATE,
        RATING_DELETE,
        MODEL_RETRAIN,
        ACTIVITY_CREATE,
        ACTIVITY_UPDATE,
        ACTIVITY_DELETE,
        CATEGORY_CREATE,
        CATEGORY_UPDATE,
        CATEGORY_DELETE,
        CREW_DELETE,
        SCHEDULE_DELETE,
        LOGIN_ATTEMPT,
        SYSTEM_BACKUP,
        DATA_EXPORT
    }

    public enum TargetType {
        USER,
        SCHEDULE,
        RATING,
        ACTIVITY,
        CATEGORY,
        CREW,
        AI_MODEL,
        SYSTEM
    }

    public static SystemLog createUserActionLog(User admin, ActionType actionType, TargetType targetType, 
                                              Long targetId, String description, String ipAddress) {
        SystemLog log = new SystemLog();
        log.setAdmin(admin);
        log.setActionType(actionType);
        log.setTargetType(targetType);
        log.setTargetId(targetId);
        log.setDescription(description);
        log.setIpAddress(ipAddress);
        return log;
    }

    public static SystemLog createSystemActionLog(ActionType actionType, String description) {
        SystemLog log = new SystemLog();
        log.setActionType(actionType);
        log.setTargetType(TargetType.SYSTEM);
        log.setDescription(description);
        return log;
    }

    public static SystemLog createUserDeleteLog(User admin, Long userId, String userEmail, String ipAddress) {
        return createUserActionLog(admin, ActionType.USER_DELETE, TargetType.USER, userId, 
                                 "사용자 삭제: " + userEmail, ipAddress);
    }

    public static SystemLog createRatingDeleteLog(User admin, Long ratingId, String description, String ipAddress) {
        return createUserActionLog(admin, ActionType.RATING_DELETE, TargetType.RATING, ratingId, 
                                 "평점 삭제: " + description, ipAddress);
    }

    public static SystemLog createModelRetrainLog(User admin, String modelInfo, String ipAddress) {
        return createUserActionLog(admin, ActionType.MODEL_RETRAIN, TargetType.AI_MODEL, null, 
                                 "AI 모델 재학습: " + modelInfo, ipAddress);
    }

    public boolean isUserAction() {
        return this.admin != null;
    }

    public boolean isSystemAction() {
        return this.admin == null;
    }
}
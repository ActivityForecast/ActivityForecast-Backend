package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.Notification;
import com.activityforecastbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

    List<Notification> findByUserOrderByCreatedAtDesc(User user);
    
    List<Notification> findByUserAndIsReadFalseOrderByCreatedAtDesc(User user);
    
    List<Notification> findByUserAndIsReadTrueOrderByCreatedAtDesc(User user);
    
    List<Notification> findByUserAndNotificationTypeOrderByCreatedAtDesc(User user, Notification.NotificationType notificationType);
    
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<Notification> findRecentNotificationsByUser(@Param("user") User user, @Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(n) FROM Notification n WHERE n.user = :user AND n.isRead = false")
    long countUnreadNotificationsByUser(@Param("user") User user);
    
    @Query("SELECT n FROM Notification n WHERE n.relatedType = :relatedType AND n.relatedId = :relatedId")
    List<Notification> findByRelatedEntity(@Param("relatedType") Notification.RelatedType relatedType, @Param("relatedId") Long relatedId);
    
    @Query("SELECT n FROM Notification n WHERE n.user = :user AND n.relatedType = :relatedType AND n.relatedId = :relatedId")
    List<Notification> findByUserAndRelatedEntity(@Param("user") User user, @Param("relatedType") Notification.RelatedType relatedType, @Param("relatedId") Long relatedId);
    
    @Query("DELETE FROM Notification n WHERE n.user = :user AND n.createdAt < :before")
    void deleteOldNotificationsByUser(@Param("user") User user, @Param("before") LocalDateTime before);
    
    @Query("SELECT n.notificationType, COUNT(n) FROM Notification n WHERE n.user = :user GROUP BY n.notificationType")
    List<Object[]> getNotificationStatisticsByUser(@Param("user") User user);
}
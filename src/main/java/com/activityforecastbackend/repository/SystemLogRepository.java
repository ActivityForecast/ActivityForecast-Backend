package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.SystemLog;
import com.activityforecastbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface SystemLogRepository extends JpaRepository<SystemLog, Long> {

    List<SystemLog> findByAdminOrderByCreatedAtDesc(User admin);
    
    List<SystemLog> findByActionTypeOrderByCreatedAtDesc(SystemLog.ActionType actionType);
    
    List<SystemLog> findByTargetTypeAndTargetIdOrderByCreatedAtDesc(SystemLog.TargetType targetType, Long targetId);
    
    @Query("SELECT sl FROM SystemLog sl WHERE sl.createdAt BETWEEN :startDate AND :endDate ORDER BY sl.createdAt DESC")
    List<SystemLog> findLogsBetweenDates(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT sl FROM SystemLog sl WHERE sl.admin = :admin AND sl.createdAt BETWEEN :startDate AND :endDate ORDER BY sl.createdAt DESC")
    List<SystemLog> findLogsByAdminBetweenDates(@Param("admin") User admin, @Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);
    
    @Query("SELECT sl FROM SystemLog sl WHERE sl.actionType = :actionType AND sl.createdAt >= :since ORDER BY sl.createdAt DESC")
    List<SystemLog> findRecentLogsByActionType(@Param("actionType") SystemLog.ActionType actionType, @Param("since") LocalDateTime since);
    
    @Query("SELECT sl FROM SystemLog sl WHERE sl.admin IS NULL ORDER BY sl.createdAt DESC")
    List<SystemLog> findSystemGeneratedLogs();
    
    @Query("SELECT sl FROM SystemLog sl WHERE sl.admin IS NOT NULL ORDER BY sl.createdAt DESC")
    List<SystemLog> findUserGeneratedLogs();
    
    @Query("SELECT sl.actionType, COUNT(sl) FROM SystemLog sl WHERE sl.createdAt >= :since GROUP BY sl.actionType ORDER BY COUNT(sl) DESC")
    List<Object[]> getActionStatisticsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT sl.admin, COUNT(sl) FROM SystemLog sl WHERE sl.admin IS NOT NULL AND sl.createdAt >= :since GROUP BY sl.admin ORDER BY COUNT(sl) DESC")
    List<Object[]> getAdminActivityStatisticsSince(@Param("since") LocalDateTime since);
    
    @Query("SELECT COUNT(sl) FROM SystemLog sl WHERE sl.actionType = :actionType AND sl.createdAt >= :since")
    long countActionsSince(@Param("actionType") SystemLog.ActionType actionType, @Param("since") LocalDateTime since);
    
    @Query("SELECT sl FROM SystemLog sl WHERE sl.ipAddress = :ipAddress ORDER BY sl.createdAt DESC")
    List<SystemLog> findLogsByIpAddress(@Param("ipAddress") String ipAddress);
}
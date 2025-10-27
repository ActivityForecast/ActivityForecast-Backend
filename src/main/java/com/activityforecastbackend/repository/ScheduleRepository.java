package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.Activity;
import com.activityforecastbackend.entity.Crew;
import com.activityforecastbackend.entity.Schedule;
import com.activityforecastbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface ScheduleRepository extends JpaRepository<Schedule, Long> {

    List<Schedule> findByIsDeletedFalse();
    
    Optional<Schedule> findByScheduleIdAndIsDeletedFalse(Long scheduleId);
    
    List<Schedule> findByUserAndIsDeletedFalse(User user);
    
    List<Schedule> findByUserAndScheduleDateAndIsDeletedFalse(User user, LocalDate scheduleDate);
    
    List<Schedule> findByUserAndScheduleDateBetweenAndIsDeletedFalse(User user, LocalDate startDate, LocalDate endDate);
    
    List<Schedule> findByCrewAndIsDeletedFalse(Crew crew);
    
    List<Schedule> findByCrewAndScheduleDateAndIsDeletedFalse(Crew crew, LocalDate scheduleDate);
    
    List<Schedule> findByActivityAndIsDeletedFalse(Activity activity);
    
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isDeleted = false AND s.crew IS NULL")
    List<Schedule> findPersonalSchedulesByUser(@Param("user") User user);
    
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isDeleted = false AND s.crew IS NOT NULL")
    List<Schedule> findCrewSchedulesByUser(@Param("user") User user);
    
    @Query("SELECT s FROM Schedule s WHERE s.isDeleted = false AND s.scheduleDate = :date")
    List<Schedule> findByScheduleDate(@Param("date") LocalDate date);
    
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isDeleted = false AND s.isParticipated = true")
    List<Schedule> findParticipatedSchedulesByUser(@Param("user") User user);
    
    @Query("SELECT s FROM Schedule s WHERE s.user = :user AND s.isDeleted = false AND s.rating IS NOT NULL")
    List<Schedule> findRatedSchedulesByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(s) FROM Schedule s WHERE s.user = :user AND s.isDeleted = false AND s.isParticipated = true AND s.activity = :activity")
    long countParticipatedSchedulesByUserAndActivity(@Param("user") User user, @Param("activity") Activity activity);
    
    @Query("SELECT s.activity, COUNT(s) FROM Schedule s WHERE s.user = :user AND s.isDeleted = false AND s.isParticipated = true GROUP BY s.activity ORDER BY COUNT(s) DESC")
    List<Object[]> findActivityStatisticsByUser(@Param("user") User user);
}
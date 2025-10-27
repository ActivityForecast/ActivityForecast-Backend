package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.Activity;
import com.activityforecastbackend.entity.ActivityCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityRepository extends JpaRepository<Activity, Long> {

    List<Activity> findByIsDeletedFalse();
    
    Optional<Activity> findByActivityIdAndIsDeletedFalse(Long activityId);
    
    List<Activity> findByCategoryAndIsDeletedFalse(ActivityCategory category);
    
    List<Activity> findByActivityTypeAndIsDeletedFalse(String activityType);
    
    List<Activity> findByLocationTypeAndIsDeletedFalse(Activity.LocationType locationType);
    
    List<Activity> findByDifficultyLevelAndIsDeletedFalse(Integer difficultyLevel);
    
    @Query("SELECT a FROM Activity a WHERE a.isDeleted = false AND a.activityName LIKE %:keyword%")
    List<Activity> findByActivityNameContainingAndIsDeletedFalse(@Param("keyword") String keyword);
    
    @Query("SELECT a FROM Activity a WHERE a.isDeleted = false AND a.difficultyLevel BETWEEN :minLevel AND :maxLevel")
    List<Activity> findByDifficultyLevelBetweenAndIsDeletedFalse(@Param("minLevel") Integer minLevel, @Param("maxLevel") Integer maxLevel);
    
    @Query("SELECT a FROM Activity a JOIN UserPreference up ON a = up.activity WHERE up.user.userId = :userId AND a.isDeleted = false")
    List<Activity> findPreferredActivitiesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT COUNT(a) FROM Activity a WHERE a.category = :category AND a.isDeleted = false")
    long countByCategoryAndIsDeletedFalse(@Param("category") ActivityCategory category);
}
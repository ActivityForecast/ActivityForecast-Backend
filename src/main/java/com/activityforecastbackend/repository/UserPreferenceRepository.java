package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.Activity;
import com.activityforecastbackend.entity.User;
import com.activityforecastbackend.entity.UserPreference;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserPreferenceRepository extends JpaRepository<UserPreference, Long> {

    List<UserPreference> findByUser(User user);
    
    List<UserPreference> findByActivity(Activity activity);
    
    Optional<UserPreference> findByUserAndActivity(User user, Activity activity);
    
    boolean existsByUserAndActivity(User user, Activity activity);
    
    void deleteByUser(User user);
    
    void deleteByUserAndActivity(User user, Activity activity);
    
    @Query("SELECT up.activity FROM UserPreference up WHERE up.user = :user")
    List<Activity> findPreferredActivitiesByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(up) FROM UserPreference up WHERE up.activity = :activity")
    long countUsersByActivity(@Param("activity") Activity activity);
    
    @Query("SELECT AVG(up.weight) FROM UserPreference up WHERE up.user = :user")
    Double findAverageWeightByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(up) FROM UserPreference up WHERE up.user = :user")
    long countPreferencesByUser(@Param("user") User user);
    
    @Query("SELECT up FROM UserPreference up JOIN FETCH up.activity a JOIN FETCH a.category WHERE up.user = :user ORDER BY up.weight DESC")
    List<UserPreference> findByUserWithActivityAndCategory(@Param("user") User user);
}
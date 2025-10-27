package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.Activity;
import com.activityforecastbackend.entity.ActivityLocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityLocationRepository extends JpaRepository<ActivityLocation, Long> {

    List<ActivityLocation> findByIsDeletedFalse();
    
    Optional<ActivityLocation> findByLocationIdAndIsDeletedFalse(Long locationId);
    
    List<ActivityLocation> findByActivityAndIsDeletedFalse(Activity activity);
    
    @Query("SELECT al FROM ActivityLocation al WHERE al.isDeleted = false AND al.locationName LIKE %:keyword%")
    List<ActivityLocation> findByLocationNameContainingAndIsDeletedFalse(@Param("keyword") String keyword);
    
    @Query("SELECT al FROM ActivityLocation al WHERE al.isDeleted = false AND al.address LIKE %:address%")
    List<ActivityLocation> findByAddressContainingAndIsDeletedFalse(@Param("address") String address);
    
    @Query(value = "SELECT * FROM activity_locations al " +
           "WHERE al.is_deleted = false " +
           "AND (6371 * acos(cos(radians(:latitude)) * cos(radians(al.latitude)) * " +
           "cos(radians(al.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(al.latitude)))) <= :radiusKm " +
           "ORDER BY (6371 * acos(cos(radians(:latitude)) * cos(radians(al.latitude)) * " +
           "cos(radians(al.longitude) - radians(:longitude)) + sin(radians(:latitude)) * " +
           "sin(radians(al.latitude))))", 
           nativeQuery = true)
    List<ActivityLocation> findLocationsByDistanceAndIsDeletedFalse(
            @Param("latitude") BigDecimal latitude, 
            @Param("longitude") BigDecimal longitude, 
            @Param("radiusKm") Double radiusKm);
    
    @Query("SELECT COUNT(al) FROM ActivityLocation al WHERE al.activity = :activity AND al.isDeleted = false")
    long countByActivityAndIsDeletedFalse(@Param("activity") Activity activity);
}
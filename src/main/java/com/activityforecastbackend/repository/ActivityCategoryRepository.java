package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.ActivityCategory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ActivityCategoryRepository extends JpaRepository<ActivityCategory, Long> {

    Optional<ActivityCategory> findByCategoryName(String categoryName);
    
    boolean existsByCategoryName(String categoryName);
    
    List<ActivityCategory> findAllByOrderByCategoryNameAsc();
    
    @Query("SELECT ac FROM ActivityCategory ac JOIN ac.activities a WHERE a.isDeleted = false GROUP BY ac ORDER BY COUNT(a) DESC")
    List<ActivityCategory> findCategoriesOrderByActivityCount();
}
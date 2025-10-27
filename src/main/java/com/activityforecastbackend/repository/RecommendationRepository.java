package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.Activity;
import com.activityforecastbackend.entity.Recommendation;
import com.activityforecastbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    List<Recommendation> findByUser(User user);
    
    List<Recommendation> findByActivity(Activity activity);
    
    List<Recommendation> findByUserOrderByRecommendedAtDesc(User user);
    
    List<Recommendation> findByUserAndRecommendedAtBetween(User user, LocalDateTime startDate, LocalDateTime endDate);
    
    @Query("SELECT r FROM Recommendation r WHERE r.user = :user AND r.recommendationScore >= :minScore ORDER BY r.recommendationScore DESC")
    List<Recommendation> findHighQualityRecommendationsByUser(@Param("user") User user, @Param("minScore") BigDecimal minScore);
    
    @Query("SELECT r.activity, COUNT(r) FROM Recommendation r WHERE r.user = :user GROUP BY r.activity ORDER BY COUNT(r) DESC")
    List<Object[]> findMostRecommendedActivitiesByUser(@Param("user") User user);
    
    @Query("SELECT COUNT(r) FROM Recommendation r WHERE r.user = :user AND r.recommendedAt >= :since")
    long countRecommendationsByUserSince(@Param("user") User user, @Param("since") LocalDateTime since);
    
    @Query("SELECT AVG(r.recommendationScore) FROM Recommendation r WHERE r.user = :user")
    BigDecimal getAverageRecommendationScoreByUser(@Param("user") User user);
    
    @Query("SELECT r FROM Recommendation r WHERE r.user = :user AND r.comfortScore >= :minComfortScore ORDER BY r.comfortScore DESC")
    List<Recommendation> findComfortableRecommendationsByUser(@Param("user") User user, @Param("minComfortScore") BigDecimal minComfortScore);
}
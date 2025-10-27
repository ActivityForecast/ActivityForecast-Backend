package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.AiTrainingData;
import com.activityforecastbackend.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AiTrainingDataRepository extends JpaRepository<AiTrainingData, Long> {

    List<AiTrainingData> findByIsUsedForTrainingTrue();
    
    List<AiTrainingData> findByIsUsedForTrainingFalse();
    
    Optional<AiTrainingData> findBySchedule(Schedule schedule);
    
    List<AiTrainingData> findByActivityType(String activityType);
    
    List<AiTrainingData> findByIsParticipatedTrue();
    
    List<AiTrainingData> findByIsParticipatedFalse();
    
    @Query("SELECT atd FROM AiTrainingData atd WHERE atd.weatherTemp BETWEEN :minTemp AND :maxTemp")
    List<AiTrainingData> findByTemperatureRange(@Param("minTemp") Double minTemp, @Param("maxTemp") Double maxTemp);
    
    @Query("SELECT atd FROM AiTrainingData atd WHERE atd.weatherCondition = :condition")
    List<AiTrainingData> findByWeatherCondition(@Param("condition") String condition);
    
    @Query("SELECT atd FROM AiTrainingData atd WHERE atd.airQualityIndex BETWEEN :minIndex AND :maxIndex")
    List<AiTrainingData> findByAirQualityRange(@Param("minIndex") Integer minIndex, @Param("maxIndex") Integer maxIndex);
    
    @Query("SELECT atd FROM AiTrainingData atd WHERE atd.rating >= :minRating AND atd.isParticipated = true")
    List<AiTrainingData> findPositiveOutcomes(@Param("minRating") Double minRating);
    
    @Query("SELECT atd FROM AiTrainingData atd WHERE atd.isUsedForTraining = false AND atd.weatherTemp IS NOT NULL AND atd.weatherCondition IS NOT NULL AND atd.activityType IS NOT NULL AND atd.rating IS NOT NULL")
    List<AiTrainingData> findValidUnusedTrainingData();
    
    @Query("SELECT COUNT(atd) FROM AiTrainingData atd WHERE atd.isUsedForTraining = true")
    long countUsedTrainingData();
    
    @Query("SELECT COUNT(atd) FROM AiTrainingData atd WHERE atd.isUsedForTraining = false")
    long countUnusedTrainingData();
    
    @Query("SELECT atd FROM AiTrainingData atd WHERE atd.createdAt >= :since ORDER BY atd.createdAt DESC")
    List<AiTrainingData> findRecentTrainingData(@Param("since") LocalDateTime since);
    
    @Query("SELECT atd.activityType, COUNT(atd) FROM AiTrainingData atd WHERE atd.isUsedForTraining = true GROUP BY atd.activityType")
    List<Object[]> getTrainingDataStatisticsByActivityType();
}
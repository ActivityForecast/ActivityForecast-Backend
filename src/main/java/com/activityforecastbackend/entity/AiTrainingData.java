package com.activityforecastbackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "ai_training_data", indexes = {
        @Index(name = "idx_ai_training_used", columnList = "is_used_for_training"),
        @Index(name = "idx_ai_training_created_at", columnList = "created_at"),
        @Index(name = "idx_ai_training_schedule_id", columnList = "schedule_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AiTrainingData {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "training_id")
    private Long trainingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id")
    private Schedule schedule;

    @Column(name = "weather_temp", precision = 5, scale = 2)
    private BigDecimal weatherTemp;

    @Column(name = "weather_condition", length = 100)
    private String weatherCondition;

    @Column(name = "air_quality_index")
    private Integer airQualityIndex;

    @Column(name = "activity_type", length = 50)
    private String activityType;

    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "is_participated", nullable = false)
    private Boolean isParticipated;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_used_for_training", nullable = false)
    private Boolean isUsedForTraining = false;

    public static AiTrainingData createFromSchedule(Schedule schedule) {
        AiTrainingData trainingData = new AiTrainingData();
        trainingData.setSchedule(schedule);
        trainingData.setWeatherTemp(schedule.getWeatherTemp());
        trainingData.setWeatherCondition(schedule.getWeatherCondition());
        trainingData.setAirQualityIndex(schedule.getAirQualityIndex());
        trainingData.setActivityType(schedule.getActivity().getActivityType());
        trainingData.setRating(schedule.getRating());
        trainingData.setIsParticipated(schedule.getIsParticipated());
        return trainingData;
    }

    public static AiTrainingData createManualData(BigDecimal weatherTemp, String weatherCondition, 
                                                 Integer airQualityIndex, String activityType, 
                                                 BigDecimal rating, Boolean isParticipated) {
        AiTrainingData trainingData = new AiTrainingData();
        trainingData.setWeatherTemp(weatherTemp);
        trainingData.setWeatherCondition(weatherCondition);
        trainingData.setAirQualityIndex(airQualityIndex);
        trainingData.setActivityType(activityType);
        trainingData.setRating(rating);
        trainingData.setIsParticipated(isParticipated);
        return trainingData;
    }

    public void markAsUsedForTraining() {
        this.isUsedForTraining = true;
    }

    public void markAsUnusedForTraining() {
        this.isUsedForTraining = false;
    }

    public boolean hasValidData() {
        return this.weatherTemp != null && 
               this.weatherCondition != null && 
               this.activityType != null && 
               this.rating != null && 
               this.isParticipated != null;
    }

    public boolean isPositiveOutcome() {
        return this.isParticipated && 
               this.rating != null && 
               this.rating.compareTo(BigDecimal.valueOf(3.0)) >= 0;
    }
}
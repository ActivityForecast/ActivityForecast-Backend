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
@Table(name = "recommendations", indexes = {
        @Index(name = "idx_recommendation_user_id", columnList = "user_id"),
        @Index(name = "idx_recommendation_user_date", columnList = "user_id, recommended_at")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Recommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recommendation_id")
    private Long recommendationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(name = "location_latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal locationLatitude;

    @Column(name = "location_longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal locationLongitude;

    @Column(name = "weather_temp", precision = 5, scale = 2)
    private BigDecimal weatherTemp;

    @Column(name = "weather_condition", length = 100)
    private String weatherCondition;

    @Column(name = "air_quality_index")
    private Integer airQualityIndex;

    @Column(name = "recommendation_score", nullable = false, precision = 5, scale = 2)
    private BigDecimal recommendationScore;

    @Column(name = "comfort_score", precision = 3, scale = 2)
    private BigDecimal comfortScore;

    @CreationTimestamp
    @Column(name = "recommended_at", nullable = false)
    private LocalDateTime recommendedAt;

    public static Recommendation createRecommendation(User user, Activity activity, 
                                                    BigDecimal latitude, BigDecimal longitude,
                                                    BigDecimal recommendationScore) {
        Recommendation recommendation = new Recommendation();
        recommendation.setUser(user);
        recommendation.setActivity(activity);
        recommendation.setLocationLatitude(latitude);
        recommendation.setLocationLongitude(longitude);
        recommendation.setRecommendationScore(recommendationScore);
        return recommendation;
    }

    public void setWeatherData(BigDecimal temperature, String condition, Integer airQualityIndex, BigDecimal comfortScore) {
        this.weatherTemp = temperature;
        this.weatherCondition = condition;
        this.airQualityIndex = airQualityIndex;
        this.comfortScore = comfortScore;
    }

    public boolean isHighQualityRecommendation() {
        return this.recommendationScore.compareTo(BigDecimal.valueOf(4.0)) >= 0;
    }

    public boolean isComfortableWeather() {
        return this.comfortScore != null && this.comfortScore.compareTo(BigDecimal.valueOf(0.7)) >= 0;
    }
}
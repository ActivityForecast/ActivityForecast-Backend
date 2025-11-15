package com.activityforecastbackend.dto.recommendation;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RecommendationSummary {
    
    // 활동 기본 정보
    private Long activityId;
    private String activityName;
    private String categoryName;
    private String description;
    
    // UI 표시용 정보
    private String imageUrl;
    private Integer difficultyLevel;
    private String locationType; // 실내/실외
    private String equipmentNeeded;
    private String precautions;
    
    // 추천 관련 정보
    private BigDecimal recommendationScore;
    private String recommendationReason;
    private BigDecimal comfortScore; // 쾌적도 지수
    
    // 날씨 정보
    private BigDecimal temperature;
    private String weatherCondition;
    private String pmGrade; // 미세먼지 등급
    
    // 위치 정보
    private String locationName;
    private BigDecimal latitude;
    private BigDecimal longitude;
    
    // 시간 정보
    private LocalDateTime targetDatetime;
    private LocalDateTime recommendedAt;
    
    // AI 예측 정보
    private String predictedCategory;
    private Boolean indoorPreferred;
    
    // 추가 메타데이터
    private String season;
    private String timeRange;
}
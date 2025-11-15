package com.activityforecastbackend.dto.recommendation;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationResponse {
    
    @JsonProperty("input")
    private InputData inputData;
    
    @JsonProperty("지오코딩")
    private GeocodingData geocoding;
    
    @JsonProperty("지오코딩_출처")
    private String geocodingSource;
    
    @JsonProperty("사용된_날씨")
    private WeatherData weatherData;
    
    @JsonProperty("XGBoost_예측_카테고리")
    private String xgboostPredictedCategory;
    
    @JsonProperty("최종_선택_카테고리")
    private List<String> finalSelectedCategories;
    
    @JsonProperty("실내_우선여부")
    private Boolean indoorPreferred;
    
    @JsonProperty("추천_운동")
    private String recommendedActivity;
    
    @JsonProperty("추천_운동_목록")
    private List<String> recommendedActivityList;
    
    @JsonProperty("추천_근거")
    private String recommendationReason;
    
    // 기존 호환성을 위한 헬퍼 메서드
    public String getFinalSelectedCategory() {
        return finalSelectedCategories != null && !finalSelectedCategories.isEmpty() 
            ? finalSelectedCategories.get(0) : null;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class InputData {
        @JsonProperty("user_id")
        private String userId;
        
        @JsonProperty("raw_location_name")
        private String rawLocationName;
        
        @JsonProperty("normalized_address")
        private String normalizedAddress;
        
        @JsonProperty("addr_level2")
        private String addrLevel2;
        
        @JsonProperty("target_datetime")
        private OffsetDateTime targetDatetime;
        
        @JsonProperty("favorites_from_request")
        private List<String> favoritesFromRequest;
        
        // 헬퍼 메서드: OffsetDateTime을 LocalDateTime으로 변환
        public LocalDateTime getTargetDatetimeAsLocal() {
            return targetDatetime != null ? targetDatetime.toLocalDateTime() : null;
        }
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class GeocodingData {
        @JsonProperty("위도")
        private BigDecimal latitude;
        
        @JsonProperty("경도")
        private BigDecimal longitude;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WeatherData {
        private BigDecimal temperature;
        private Integer humidity;
        
        @JsonProperty("wind_speed")
        private BigDecimal windSpeed;
        
        private BigDecimal precipitation;
        private BigDecimal pm25;
        private BigDecimal pm10;
        
        @JsonProperty("pm_grade")
        private String pmGrade;
        
        private String season;
        
        @JsonProperty("time_range")
        private String timeRange;
        
        @JsonProperty("precipitation_mm(강수량)")
        private BigDecimal precipitationMm;
        
        @JsonProperty("owm_dt_txt")
        private String owmDtTxt;
        
        private String source;
        
        @JsonProperty("is_rainy")
        private Boolean isRainy;
        
        @JsonProperty("is_windy")
        private Boolean isWindy;
        
        @JsonProperty("is_too_cold")
        private Boolean isTooCold;
        
        @JsonProperty("is_too_hot")
        private Boolean isTooHot;
        
        @JsonProperty("is_bad_air")
        private Boolean isBadAir;
    }
    
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DebugData {
        @JsonProperty("favorites_raw")
        private List<String> favoritesRaw;
        
        @JsonProperty("favorites_clean")
        private List<String> favoritesClean;
        
        @JsonProperty("favorite_categories")
        private List<String> favoriteCategories;
        
        @JsonProperty("favorites_matched_in_category")
        private List<String> favoritesMatchedInCategory;
    }
}
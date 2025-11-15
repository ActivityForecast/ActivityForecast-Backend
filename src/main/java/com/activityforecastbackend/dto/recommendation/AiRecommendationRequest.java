package com.activityforecastbackend.dto.recommendation;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationRequest {
    
    @JsonProperty("user_id")
    private String userId;
    
    @JsonProperty("location_name")
    private String locationName;
    
    @JsonProperty("target_datetime")
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime targetDatetime;
    
    @JsonProperty("favorites")
    private List<String> favorites;
}
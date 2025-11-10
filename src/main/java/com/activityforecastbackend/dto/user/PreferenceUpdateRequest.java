package com.activityforecastbackend.dto.user;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
public class PreferenceUpdateRequest {
    
    @NotEmpty(message = "선호 활동 목록은 필수입니다")
    private List<PreferenceItem> preferences;
    
    @Getter
    @Setter
    public static class PreferenceItem {
        
        @NotNull(message = "활동 ID는 필수입니다")
        private Long activityId;
        
        @NotNull(message = "가중치는 필수입니다")
        @DecimalMin(value = "0.0", message = "가중치는 0 이상이어야 합니다")
        @DecimalMax(value = "1.0", message = "가중치는 1 이하여야 합니다")
        private BigDecimal weight;
    }
}
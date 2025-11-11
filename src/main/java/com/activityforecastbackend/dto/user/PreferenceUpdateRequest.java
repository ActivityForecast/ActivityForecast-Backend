package com.activityforecastbackend.dto.user;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

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
    }
}
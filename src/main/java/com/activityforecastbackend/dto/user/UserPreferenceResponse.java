package com.activityforecastbackend.dto.user;

import com.activityforecastbackend.dto.activity.ActivityDto;
import com.activityforecastbackend.entity.UserPreference;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserPreferenceResponse {
    
    private Long preferenceId;
    private ActivityDto activity;
    private BigDecimal weight;
    private LocalDateTime createdAt;
    
    public static UserPreferenceResponse fromEntity(UserPreference userPreference) {
        return UserPreferenceResponse.builder()
                .preferenceId(userPreference.getPreferenceId())
                .activity(ActivityDto.from(userPreference.getActivity()))
                .weight(userPreference.getWeight())
                .createdAt(userPreference.getCreatedAt())
                .build();
    }
}
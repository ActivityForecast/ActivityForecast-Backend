package com.activityforecastbackend.dto.user;

import com.activityforecastbackend.dto.activity.ActivityDto;
import com.activityforecastbackend.entity.UserPreference;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserPreferenceResponse {
    
    private Long preferenceId;
    private ActivityDto activity;
    private LocalDateTime createdAt;
    
    public static UserPreferenceResponse fromEntity(UserPreference userPreference) {
        return UserPreferenceResponse.builder()
                .preferenceId(userPreference.getPreferenceId())
                .activity(ActivityDto.from(userPreference.getActivity()))
                .createdAt(userPreference.getCreatedAt())
                .build();
    }
}
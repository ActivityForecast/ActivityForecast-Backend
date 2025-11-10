package com.activityforecastbackend.dto.user;

import com.activityforecastbackend.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class UserProfileResponse {
    
    private Long userId;
    private String email;
    private String name;
    private String provider;
    private LocalDateTime lastLoginAt;
    private LocalDateTime createdAt;
    
    public static UserProfileResponse fromEntity(User user) {
        return UserProfileResponse.builder()
                .userId(user.getUserId())
                .email(user.getEmail())
                .name(user.getName())
                .provider(user.getProvider())
                .lastLoginAt(user.getLastLoginAt())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
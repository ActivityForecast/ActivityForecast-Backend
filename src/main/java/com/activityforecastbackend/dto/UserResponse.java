package com.activityforecastbackend.dto;

import com.activityforecastbackend.entity.User;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

// 민감 정보(이메일, 비밀번호 등)를 제외
@Getter
@Setter
@Builder
public class UserResponse {
    private Long userId;
    private String name;

    public static UserResponse from(User user) {
        return UserResponse.builder()
                .userId(user.getUserId())
                .name(user.getName())
                .build();
    }
}

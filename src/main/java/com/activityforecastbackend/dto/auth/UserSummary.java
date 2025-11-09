package com.activityforecastbackend.dto.auth;

import com.activityforecastbackend.entity.User;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class UserSummary {
    
    private Long id;
    private String email;
    private String name;
    private String role;
    
    public static UserSummary from(User user) {
        return new UserSummary(
                user.getUserId(),
                user.getEmail(),
                user.getName(),
                user.getRole().name()
        );
    }
}
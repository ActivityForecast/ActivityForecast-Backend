package com.activityforecastbackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class UserSummary {
    
    private Long id;
    private String email;
    private String name;
    private String role;
}
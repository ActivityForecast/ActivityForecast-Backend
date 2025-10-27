package com.activityforecastbackend.dto.auth;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class JwtAuthenticationResponse {
    
    private String accessToken;
    private String refreshToken;
    private String tokenType = "Bearer";
    private UserSummary user;
    
    public JwtAuthenticationResponse(String accessToken, String refreshToken, UserSummary user) {
        this.accessToken = accessToken;
        this.refreshToken = refreshToken;
        this.user = user;
    }
}
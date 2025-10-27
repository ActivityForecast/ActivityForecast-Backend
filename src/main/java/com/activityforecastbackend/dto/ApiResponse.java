package com.activityforecastbackend.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class ApiResponse {
    
    private Boolean success;
    private String message;
    
    public static ApiResponse success(String message) {
        return new ApiResponse(true, message);
    }
    
    public static ApiResponse error(String message) {
        return new ApiResponse(false, message);
    }
}
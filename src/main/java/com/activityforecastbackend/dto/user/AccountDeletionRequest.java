package com.activityforecastbackend.dto.user;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AccountDeletionRequest {
    
    @NotBlank(message = "비밀번호 확인은 필수입니다")
    private String password;
    
    private String reason;
}
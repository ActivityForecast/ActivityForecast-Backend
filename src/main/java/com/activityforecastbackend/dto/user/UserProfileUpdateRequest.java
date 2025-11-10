package com.activityforecastbackend.dto.user;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserProfileUpdateRequest {
    
    @NotBlank(message = "이름은 필수입니다")
    @Size(min = 2, max = 50, message = "이름은 2-50자 사이여야 합니다")
    private String name;
    
    @Size(min = 8, max = 100, message = "비밀번호는 8-100자 사이여야 합니다")
    private String password;
    
    @Size(min = 8, max = 100, message = "비밀번호 확인은 8-100자 사이여야 합니다")
    private String confirmPassword;
    
    public boolean isPasswordUpdateRequested() {
        return password != null && !password.trim().isEmpty();
    }
    
    public boolean isPasswordMatch() {
        if (!isPasswordUpdateRequested()) {
            return true;
        }
        return password.equals(confirmPassword);
    }
}
package com.activityforecastbackend.controller;

import com.activityforecastbackend.dto.ApiResponse;
import com.activityforecastbackend.dto.user.*;
import com.activityforecastbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "User", description = "사용자 프로필 및 선호도 관리 API")
@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Slf4j
public class UserController {
    
    private final UserService userService;
    
    @Operation(summary = "사용자 프로필 조회", description = "로그인한 사용자의 프로필 정보를 조회합니다")
    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> getUserProfile() {
        
        log.info("사용자 프로필 조회 요청");
        UserProfileResponse response = userService.getUserProfile();
        
        return ResponseEntity.ok(ApiResponse.success("프로필 조회 성공", response));
    }
    
    @Operation(summary = "사용자 프로필 수정", description = "사용자의 이름과 비밀번호를 수정합니다")
    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<UserProfileResponse>> updateUserProfile(
            @Valid @RequestBody UserProfileUpdateRequest request) {
        
        log.info("사용자 프로필 수정 요청: name={}", request.getName());
        UserProfileResponse response = userService.updateUserProfile(request);
        
        return ResponseEntity.ok(ApiResponse.success("프로필 수정 성공", response));
    }
    
    @Operation(summary = "사용자 선호 활동 조회", description = "사용자가 설정한 선호 활동 목록을 조회합니다")
    @GetMapping("/preferences")
    public ResponseEntity<ApiResponse<List<UserPreferenceResponse>>> getUserPreferences() {
        
        log.info("사용자 선호도 조회 요청");
        List<UserPreferenceResponse> response = userService.getUserPreferences();
        
        return ResponseEntity.ok(ApiResponse.success("선호도 조회 성공", response));
    }
    
    @Operation(summary = "사용자 선호 활동 설정", description = "사용자의 선호 활동과 가중치를 설정합니다")
    @PutMapping("/preferences")
    public ResponseEntity<ApiResponse<List<UserPreferenceResponse>>> updateUserPreferences(
            @Valid @RequestBody PreferenceUpdateRequest request) {
        
        log.info("사용자 선호도 설정 요청: count={}", request.getPreferences().size());
        List<UserPreferenceResponse> response = userService.updateUserPreferences(request);
        
        return ResponseEntity.ok(ApiResponse.success("선호도 설정 성공", response));
    }
    
    @Operation(summary = "선호 활동 통계 조회", description = "사용자의 선호 활동 통계 정보를 조회합니다")
    @GetMapping("/preferences/statistics")
    public ResponseEntity<ApiResponse<PreferenceStatisticsResponse>> getPreferenceStatistics() {
        
        log.info("사용자 선호도 통계 조회 요청");
        PreferenceStatisticsResponse response = userService.getPreferenceStatistics();
        
        return ResponseEntity.ok(ApiResponse.success("선호도 통계 조회 성공", response));
    }
    
    @Operation(summary = "회원 탈퇴", description = "사용자 계정을 삭제합니다 (소프트 삭제)")
    @DeleteMapping("/account")
    public ResponseEntity<ApiResponse<Void>> deleteUserAccount(
            @Valid @RequestBody AccountDeletionRequest request) {
        
        log.info("회원 탈퇴 요청");
        userService.deleteUserAccount(request);
        
        return ResponseEntity.ok(ApiResponse.success("회원 탈퇴 완료"));
    }
}
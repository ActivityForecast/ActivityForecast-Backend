package com.activityforecastbackend.controller;

import com.activityforecastbackend.dto.ApiResponse;
import com.activityforecastbackend.dto.auth.JwtAuthenticationResponse;
import com.activityforecastbackend.dto.auth.LoginRequest;
import com.activityforecastbackend.dto.auth.SignupRequest;
import com.activityforecastbackend.dto.auth.TokenRefreshRequest;
import com.activityforecastbackend.dto.auth.UserSummary;
import com.activityforecastbackend.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Auth", description = "인증 관련 API")
public class AuthController {

    private final AuthService authService;

    @Operation(summary = "일반 로그인", description = "이메일과 패스워드를 사용한 일반 로그인")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그인 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 또는 패스워드 오류)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    @PostMapping("/login")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> login(
            @Valid @RequestBody LoginRequest loginRequest) {
        
        log.info("Login request for email: {}", loginRequest.getEmail());
        
        JwtAuthenticationResponse response = authService.login(loginRequest);
        
        return ResponseEntity.ok(ApiResponse.success(
                "Login successful",
                response
        ));
    }

    @Operation(summary = "회원가입", description = "새로운 사용자 계정 생성")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "201", description = "회원가입 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "잘못된 요청 (이메일 중복, 유효성 검사 실패)")
    @PostMapping("/signup")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> signup(
            @Valid @RequestBody SignupRequest signupRequest) {
        
        log.info("Signup request for email: {}", signupRequest.getEmail());
        
        JwtAuthenticationResponse response = authService.signup(signupRequest);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                        "User registered successfully",
                        response
                ));
    }

    @Operation(summary = "토큰 갱신", description = "Refresh Token을 사용하여 새로운 Access Token 발급")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "토큰 갱신 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "400", description = "유효하지 않은 Refresh Token")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    @PostMapping("/refresh")
    public ResponseEntity<ApiResponse<JwtAuthenticationResponse>> refreshToken(
            @Valid @RequestBody TokenRefreshRequest tokenRefreshRequest) {
        
        log.info("Token refresh request");
        
        JwtAuthenticationResponse response = authService.refreshToken(tokenRefreshRequest.getRefreshToken());
        
        return ResponseEntity.ok(ApiResponse.success(
                "Token refreshed successfully",
                response
        ));
    }

    @Operation(summary = "현재 사용자 정보 조회", description = "인증된 사용자의 정보를 조회")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "사용자 정보 조회 성공")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "401", description = "인증되지 않은 사용자")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<UserSummary>> getCurrentUser() {
        log.info("Get current user request");
        
        UserSummary userSummary = authService.getCurrentUser();
        
        return ResponseEntity.ok(ApiResponse.success(
                "Current user retrieved successfully",
                userSummary
        ));
    }

    @Operation(summary = "로그아웃", description = "사용자 로그아웃 (클라이언트에서 토큰 제거 필요)")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "로그아웃 성공")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<String>> logout() {
        log.info("Logout request");
        
        authService.logout();
        
        return ResponseEntity.ok(ApiResponse.success(
                "User logged out successfully",
                "Please remove the token from client storage"
        ));
    }

    @Operation(summary = "OAuth2 로그인 성공 콜백", 
               description = "OAuth2 로그인 성공 후 프론트엔드로 리다이렉트하기 위한 엔드포인트")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "프론트엔드로 리다이렉트")
    @GetMapping("/oauth2/redirect")
    public ResponseEntity<ApiResponse<String>> oauth2Redirect(
            @Parameter(description = "JWT Access Token") @RequestParam(required = false) String token,
            @Parameter(description = "JWT Refresh Token") @RequestParam(required = false) String refreshToken,
            @Parameter(description = "성공 여부") @RequestParam(required = false) String success,
            @Parameter(description = "오류 코드") @RequestParam(required = false) String error,
            @Parameter(description = "오류 메시지") @RequestParam(required = false) String message) {
        
        if ("true".equals(success) && token != null) {
            log.info("OAuth2 login successful, token provided");
            return ResponseEntity.ok(ApiResponse.success(
                    "OAuth2 authentication successful",
                    "Token: " + token
            ));
        } else {
            log.error("OAuth2 login failed: error={}, message={}", error, message);
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body(ApiResponse.error(
                            message != null ? message : "OAuth2 authentication failed"
                    ));
        }
    }

    @Operation(summary = "카카오 로그인", description = "카카오 OAuth2 로그인 시작")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "302", description = "카카오 인증 페이지로 리다이렉트")
    @GetMapping("/oauth2/authorization/kakao")
    public ResponseEntity<ApiResponse<String>> kakaoLogin() {
        return ResponseEntity.status(HttpStatus.FOUND)
                .body(ApiResponse.success(
                        "Redirecting to Kakao OAuth2 authorization",
                        "/oauth2/authorization/kakao"
                ));
    }
}
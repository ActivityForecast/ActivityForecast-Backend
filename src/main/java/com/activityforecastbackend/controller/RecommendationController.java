package com.activityforecastbackend.controller;

import com.activityforecastbackend.dto.recommendation.RecommendationSummary;
import com.activityforecastbackend.security.UserPrincipal;
import com.activityforecastbackend.service.RecommendationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Tag(name = "추천 API", description = "AI 기반 활동 추천 관련 API")
@Slf4j
@RestController
@RequestMapping("/api/recommendation")
@RequiredArgsConstructor
public class RecommendationController {

    private final RecommendationService recommendationService;

    @Operation(summary = "메인 화면 활동 추천", description = "로그인한 사용자의 위치와 날짜를 기반으로 AI가 추천하는 3개의 활동을 조회합니다.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "추천 성공"),
            @ApiResponse(responseCode = "400", description = "잘못된 요청 (위치명, 날짜 오류)"),
            @ApiResponse(responseCode = "401", description = "인증 실패 (로그인 필요)"),
            @ApiResponse(responseCode = "404", description = "사용자를 찾을 수 없음"),
            @ApiResponse(responseCode = "500", description = "AI 서버 연결 오류")
    })
    @GetMapping("/main")
    public ResponseEntity<List<RecommendationSummary>> getMainRecommendation(
            @AuthenticationPrincipal UserPrincipal currentUser,
            
            @Parameter(description = "위치명 (예: 서울특별시 강남구)", required = true, example = "서울특별시 강남구")
            @RequestParam String locationName,
            
            @Parameter(description = "목표 날짜시간 (ISO 8601 형식)", required = true, 
                      example = "2024-01-15T14:30:00")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime targetDatetime) {
        
        Long userId = currentUser.getId();
        log.info("메인 추천 요청: userId={}, location={}, datetime={}", userId, locationName, targetDatetime);
        
        List<RecommendationSummary> recommendations = recommendationService.getRecommendationForUser(
                userId, locationName, targetDatetime
        );
        
        return ResponseEntity.ok(recommendations);
    }


    @Operation(summary = "AI 서버 상태 확인", description = "AI 추천 서버의 연결 상태를 확인합니다.")
    @GetMapping("/health")
    public ResponseEntity<Map<String, Object>> checkAiServerHealth() {
        
        boolean isHealthy = recommendationService.checkAiServerHealth();
        
        Map<String, Object> response = Map.of(
                "status", isHealthy ? "UP" : "DOWN",
                "message", isHealthy ? "AI 서버가 정상 작동 중입니다." : "AI 서버 연결에 문제가 있습니다.",
                "timestamp", LocalDateTime.now()
        );
        
        return ResponseEntity.ok(response);
    }
}
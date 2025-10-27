package com.activityforecastbackend.controller;

import com.activityforecastbackend.dto.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/test")
@Tag(name = "Test", description = "테스트 및 헬스체크 API")
public class TestController {

    @GetMapping("/health")
    @Operation(
            summary = "헬스체크",
            description = "애플리케이션 상태를 확인합니다."
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "정상 응답"
            )
    })
    public ResponseEntity<Map<String, Object>> healthCheck() {
        Map<String, Object> response = new HashMap<>();
        response.put("status", "UP");
        response.put("timestamp", LocalDateTime.now());
        response.put("service", "ActivityForecast Backend");
        response.put("version", "1.0.0");
        return ResponseEntity.ok(response);
    }

    @GetMapping("/echo")
    @Operation(
            summary = "에코 테스트",
            description = "전달받은 메시지를 그대로 반환합니다."
    )
    public ResponseEntity<ApiResponse> echo(
            @Parameter(description = "반환할 메시지", example = "Hello World")
            @RequestParam(defaultValue = "Hello World") String message
    ) {
        return ResponseEntity.ok(ApiResponse.success("Echo: " + message));
    }

    @GetMapping("/secure")
    @Operation(
            summary = "인증 테스트",
            description = "JWT 토큰이 필요한 보안 엔드포인트입니다.",
            security = @SecurityRequirement(name = "bearerAuth")
    )
    @ApiResponses(value = {
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "200",
                    description = "인증 성공"
            ),
            @io.swagger.v3.oas.annotations.responses.ApiResponse(
                    responseCode = "401",
                    description = "인증 실패"
            )
    })
    public ResponseEntity<ApiResponse> secureEndpoint() {
        return ResponseEntity.ok(ApiResponse.success("인증된 사용자만 접근 가능한 엔드포인트입니다."));
    }

    @PostMapping("/echo")
    @Operation(
            summary = "POST 에코 테스트",
            description = "POST 요청으로 전달받은 데이터를 그대로 반환합니다."
    )
    public ResponseEntity<Map<String, Object>> postEcho(
            @Parameter(description = "요청 본문 데이터")
            @RequestBody Map<String, Object> requestData
    ) {
        Map<String, Object> response = new HashMap<>();
        response.put("received", requestData);
        response.put("timestamp", LocalDateTime.now());
        response.put("method", "POST");
        return ResponseEntity.ok(response);
    }
}
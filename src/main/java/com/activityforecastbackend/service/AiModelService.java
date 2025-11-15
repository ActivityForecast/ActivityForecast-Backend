package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.recommendation.AiRecommendationRequest;
import com.activityforecastbackend.dto.recommendation.AiRecommendationResponse;
import com.activityforecastbackend.exception.AiModelException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiModelService {
    
    private final RestTemplate restTemplate;
    
    @Value("${ai.server.url:http://134.185.108.251:8000}")
    private String aiServerUrl;
    
    @Value("${ai.server.timeout.connection:5000}")
    private int connectionTimeout;
    
    @Value("${ai.server.timeout.read:10000}")
    private int readTimeout;
    
    private static final String RECOMMENDATION_ENDPOINT = "/recommend/by-location-and-user";
    
    /**
     * AI 모델 서버에서 활동 추천을 요청합니다.
     *
     * @param userId 사용자 ID
     * @param locationName 위치명 (예: "서울특별시 강남구")
     * @param targetDatetime 목표 시간
     * @param favorites 사용자 선호 활동 목록
     * @return AI 모델 추천 응답
     */
    public AiRecommendationResponse getRecommendation(
            String userId, 
            String locationName, 
            LocalDateTime targetDatetime, 
            List<String> favorites) {
        
        log.info("AI 모델 추천 요청: userId={}, location={}, datetime={}, favorites={}", 
                userId, locationName, targetDatetime, favorites);
        
        try {
            // 요청 데이터 구성
            AiRecommendationRequest request = AiRecommendationRequest.builder()
                    .userId(userId)
                    .locationName(locationName)
                    .targetDatetime(targetDatetime)
                    .favorites(favorites)
                    .build();
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.add("User-Agent", "ActivityForecast-Backend/1.0");
            
            HttpEntity<AiRecommendationRequest> entity = new HttpEntity<>(request, headers);
            String url = aiServerUrl + RECOMMENDATION_ENDPOINT;
            
            log.debug("AI 서버 요청 URL: {}", url);
            log.debug("요청 데이터: {}", request);
            
            // AI 서버 호출
            ResponseEntity<AiRecommendationResponse> response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    entity,
                    AiRecommendationResponse.class
            );
            
            AiRecommendationResponse aiResponse = response.getBody();
            
            if (aiResponse == null) {
                throw AiModelException.invalidResponseError("AI 서버로부터 빈 응답을 받았습니다.");
            }
            
            log.info("AI 모델 추천 성공: 추천 활동={}, 카테고리={}", 
                    aiResponse.getRecommendedActivity(), aiResponse.getFinalSelectedCategory());
            
            return aiResponse;
            
        } catch (HttpClientErrorException e) {
            log.error("AI 서버 클라이언트 오류: {} {}", e.getStatusCode(), e.getStatusText());
            if (e.getStatusCode() == HttpStatus.BAD_REQUEST) {
                throw AiModelException.invalidResponseError("잘못된 요청 데이터입니다: " + e.getMessage());
            } else if (e.getStatusCode() == HttpStatus.NOT_FOUND) {
                throw AiModelException.connectionError("AI 서버 엔드포인트를 찾을 수 없습니다.");
            } else {
                throw AiModelException.serverError("AI 서버 클라이언트 오류: " + e.getMessage());
            }
            
        } catch (HttpServerErrorException e) {
            log.error("AI 서버 서버 오류: {} {}", e.getStatusCode(), e.getStatusText());
            throw AiModelException.serverError("AI 서버 내부 오류: " + e.getMessage());
            
        } catch (ResourceAccessException e) {
            log.error("AI 서버 연결 실패: {}", e.getMessage());
            if (e.getMessage().contains("timeout")) {
                throw AiModelException.timeoutError();
            } else {
                throw AiModelException.connectionError("네트워크 연결 실패", e);
            }
            
        } catch (Exception e) {
            log.error("AI 모델 추천 요청 중 예외 발생", e);
            throw AiModelException.recommendationError("알 수 없는 오류가 발생했습니다.", e);
        }
    }
    
    
    /**
     * AI 서버 상태 확인
     */
    public boolean checkServerHealth() {
        try {
            // FastAPI docs 페이지로 상태 확인 (간단한 GET 요청)
            String healthUrl = aiServerUrl + "/docs";
            ResponseEntity<String> response = restTemplate.getForEntity(healthUrl, String.class);
            
            boolean isHealthy = response.getStatusCode().is2xxSuccessful();
            log.info("AI 서버 상태 확인: {}", isHealthy ? "정상" : "이상");
            
            return isHealthy;
            
        } catch (Exception e) {
            log.warn("AI 서버 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }
}
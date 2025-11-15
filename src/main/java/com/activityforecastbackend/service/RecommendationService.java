package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.recommendation.AiRecommendationResponse;
import com.activityforecastbackend.dto.recommendation.RecommendationSummary;
import com.activityforecastbackend.entity.*;
import com.activityforecastbackend.exception.AiModelException;
import com.activityforecastbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class RecommendationService {

    private final AiModelService aiModelService;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final RecommendationRepository recommendationRepository;
    private final UserPreferenceRepository userPreferenceRepository;

    /**
     * 사용자를 위한 AI 기반 활동 추천 3개를 생성합니다.
     *
     * @param userId 사용자 ID
     * @param locationName 위치명 (예: "서울특별시 강남구")
     * @param targetDatetime 목표 시간
     * @return 추천 요약 정보 리스트 (3개)
     */
    @Transactional
    public List<RecommendationSummary> getRecommendationForUser(Long userId, String locationName, LocalDateTime targetDatetime) {
        
        log.info("사용자 추천 요청: userId={}, location={}, datetime={}", userId, locationName, targetDatetime);
        
        // 1. 사용자 확인
        User user = userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
        
        // 2. 사용자 선호 활동 조회
        List<String> userFavorites = getUserFavoriteActivityNames(userId);
        
        if (userFavorites.isEmpty()) {
            throw AiModelException.insufficientDataError(userId.toString());
        }
        
        log.debug("사용자 선호 활동: {}", userFavorites);
        
        // 3. AI 모델 추천 요청
        AiRecommendationResponse aiResponse = aiModelService.getRecommendation(
                userId.toString(), 
                locationName, 
                targetDatetime, 
                userFavorites
        );
        
        // 4. AI 추천 운동 목록으로부터 3개 추천 생성
        List<RecommendationSummary> recommendations = new ArrayList<>();
        
        List<String> recommendedActivities = aiResponse.getRecommendedActivityList();
        if (recommendedActivities == null || recommendedActivities.isEmpty()) {
            // fallback: 단일 추천 운동 사용
            recommendedActivities = List.of(aiResponse.getRecommendedActivity());
        }
        
        for (int i = 0; i < Math.min(3, recommendedActivities.size()); i++) {
            String activityName = recommendedActivities.get(i);
            
            RecommendationSummary summary = convertToRecommendationSummary(
                    user, aiResponse, locationName, targetDatetime, activityName, i + 1
            );
            
            recommendations.add(summary);
            
            // 각 추천별로 기록 저장
            saveRecommendationRecord(user, aiResponse, summary);
        }
        
        // 3개가 안 되면 첫 번째 추천으로 채우기
        while (recommendations.size() < 3 && !recommendedActivities.isEmpty()) {
            String activityName = recommendedActivities.get(0);
            RecommendationSummary summary = convertToRecommendationSummary(
                    user, aiResponse, locationName, targetDatetime, activityName, recommendations.size() + 1
            );
            recommendations.add(summary);
        }
        
        log.info("추천 완료: {} 개 활동 추천", recommendations.size());
        
        return recommendations;
    }
    
    
    /**
     * 사용자의 선호 활동 목록을 조회합니다.
     */
    private List<String> getUserFavoriteActivityNames(Long userId) {
        return userPreferenceRepository.findByUserUserId(userId)
                .stream()
                .map(pref -> pref.getActivity().getActivityName())
                .collect(Collectors.toList());
    }
    
    /**
     * AI 응답을 RecommendationSummary로 변환합니다.
     */
    private RecommendationSummary convertToRecommendationSummary(
            User user, 
            AiRecommendationResponse aiResponse, 
            String locationName, 
            LocalDateTime targetDatetime) {
        
        // AI에서 추천된 활동명으로 실제 활동 정보 조회
        Optional<Activity> recommendedActivity = activityRepository
                .findByActivityNameContainingAndIsDeletedFalse(aiResponse.getRecommendedActivity())
                .stream()
                .findFirst();
        
        // 추천 점수 계산 (0.5 ~ 1.0 범위)
        BigDecimal recommendationScore = calculateRecommendationScore(aiResponse);
        
        // 쾌적도 지수 계산
        BigDecimal comfortScore = calculateComfortScore(aiResponse.getWeatherData());
        
        RecommendationSummary.RecommendationSummaryBuilder builder = RecommendationSummary.builder()
                .activityName(aiResponse.getRecommendedActivity())
                .categoryName(aiResponse.getFinalSelectedCategory())
                .recommendationScore(recommendationScore)
                .recommendationReason(aiResponse.getRecommendationReason())
                .comfortScore(comfortScore)
                .locationName(locationName)
                .targetDatetime(targetDatetime)
                .recommendedAt(LocalDateTime.now())
                .predictedCategory(aiResponse.getXgboostPredictedCategory())
                .indoorPreferred(aiResponse.getIndoorPreferred());
        
        // 실제 활동 정보가 있으면 추가
        if (recommendedActivity.isPresent()) {
            Activity activity = recommendedActivity.get();
            builder
                    .activityId(activity.getActivityId())
                    .description(activity.getDescription())
                    .difficultyLevel(activity.getDifficultyLevel())
                    .locationType(activity.getLocationType() != null ? activity.getLocationType().toString() : null)
                    .equipmentNeeded(activity.getEquipmentNeeded())
                    .precautions(activity.getPrecautions())
                    .imageUrl(activity.getImageUrl());
        }
        
        // 날씨 정보 추가
        if (aiResponse.getWeatherData() != null) {
            AiRecommendationResponse.WeatherData weather = aiResponse.getWeatherData();
            builder
                    .temperature(weather.getTemperature())
                    .weatherCondition(determineWeatherCondition(weather))
                    .pmGrade(weather.getPmGrade())
                    .season(weather.getSeason())
                    .timeRange(weather.getTimeRange());
        }
        
        // 위치 정보 추가
        if (aiResponse.getGeocoding() != null) {
            builder
                    .latitude(aiResponse.getGeocoding().getLatitude())
                    .longitude(aiResponse.getGeocoding().getLongitude());
        }
        
        return builder.build();
    }
    
    /**
     * AI 응답을 특정 활동명으로 RecommendationSummary로 변환합니다. (3개 추천용)
     */
    private RecommendationSummary convertToRecommendationSummary(
            User user, 
            AiRecommendationResponse aiResponse, 
            String locationName, 
            LocalDateTime targetDatetime,
            String activityName,
            int order) {
        
        // 특정 활동명으로 실제 활동 정보 조회
        Optional<Activity> recommendedActivity = activityRepository
                .findByActivityNameContainingAndIsDeletedFalse(activityName)
                .stream()
                .findFirst();
        
        // 추천 점수 계산 (순서에 따라 약간 감점)
        BigDecimal recommendationScore = calculateRecommendationScore(aiResponse);
        // 2번째, 3번째 추천은 약간 점수 감점
        if (order > 1) {
            recommendationScore = recommendationScore.subtract(new BigDecimal("0.05").multiply(new BigDecimal(order - 1)));
            if (recommendationScore.compareTo(new BigDecimal("0.5")) < 0) {
                recommendationScore = new BigDecimal("0.5");
            }
        }
        
        // 쾌적도 지수 계산
        BigDecimal comfortScore = calculateComfortScore(aiResponse.getWeatherData());
        
        RecommendationSummary.RecommendationSummaryBuilder builder = RecommendationSummary.builder()
                .activityName(activityName)
                .categoryName(aiResponse.getFinalSelectedCategory())
                .recommendationScore(recommendationScore)
                .recommendationReason(aiResponse.getRecommendationReason() + " (#" + order + " 추천)")
                .comfortScore(comfortScore)
                .locationName(locationName)
                .targetDatetime(targetDatetime)
                .recommendedAt(LocalDateTime.now())
                .predictedCategory(aiResponse.getXgboostPredictedCategory())
                .indoorPreferred(aiResponse.getIndoorPreferred());
        
        // 실제 활동 정보가 있으면 추가
        if (recommendedActivity.isPresent()) {
            Activity activity = recommendedActivity.get();
            builder
                    .activityId(activity.getActivityId())
                    .description(activity.getDescription())
                    .difficultyLevel(activity.getDifficultyLevel())
                    .locationType(activity.getLocationType() != null ? activity.getLocationType().toString() : null)
                    .equipmentNeeded(activity.getEquipmentNeeded())
                    .precautions(activity.getPrecautions())
                    .imageUrl(activity.getImageUrl());
        }
        
        // 날씨 정보 추가
        if (aiResponse.getWeatherData() != null) {
            AiRecommendationResponse.WeatherData weather = aiResponse.getWeatherData();
            builder
                    .temperature(weather.getTemperature())
                    .weatherCondition(determineWeatherCondition(weather))
                    .pmGrade(weather.getPmGrade())
                    .season(weather.getSeason())
                    .timeRange(weather.getTimeRange());
        }
        
        // 위치 정보 추가
        if (aiResponse.getGeocoding() != null) {
            builder
                    .latitude(aiResponse.getGeocoding().getLatitude())
                    .longitude(aiResponse.getGeocoding().getLongitude());
        }
        
        return builder.build();
    }
    
    /**
     * 추천 점수를 계산합니다.
     */
    private BigDecimal calculateRecommendationScore(AiRecommendationResponse aiResponse) {
        // 기본 점수 0.7에서 시작
        BigDecimal baseScore = new BigDecimal("0.7");
        
        // 날씨 조건에 따른 보정
        if (aiResponse.getWeatherData() != null) {
            AiRecommendationResponse.WeatherData weather = aiResponse.getWeatherData();
            
            // 비 오면 -0.1
            if (Boolean.TRUE.equals(weather.getIsRainy())) {
                baseScore = baseScore.subtract(new BigDecimal("0.1"));
            }
            
            // 바람 강하면 -0.05
            if (Boolean.TRUE.equals(weather.getIsWindy())) {
                baseScore = baseScore.subtract(new BigDecimal("0.05"));
            }
            
            // 너무 춥거나 더우면 -0.1
            if (Boolean.TRUE.equals(weather.getIsTooCold()) || Boolean.TRUE.equals(weather.getIsTooHot())) {
                baseScore = baseScore.subtract(new BigDecimal("0.1"));
            }
            
            // 공기 안좋으면 -0.05
            if (Boolean.TRUE.equals(weather.getIsBadAir())) {
                baseScore = baseScore.subtract(new BigDecimal("0.05"));
            }
        }
        
        // 실내 선호와 날씨 조건 일치시 +0.1
        if (Boolean.TRUE.equals(aiResponse.getIndoorPreferred())) {
            baseScore = baseScore.add(new BigDecimal("0.1"));
        }
        
        // 0.5 ~ 1.0 범위로 제한
        if (baseScore.compareTo(new BigDecimal("0.5")) < 0) {
            baseScore = new BigDecimal("0.5");
        } else if (baseScore.compareTo(BigDecimal.ONE) > 0) {
            baseScore = BigDecimal.ONE;
        }
        
        return baseScore.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 쾌적도 지수를 계산합니다.
     */
    private BigDecimal calculateComfortScore(AiRecommendationResponse.WeatherData weatherData) {
        if (weatherData == null) {
            return new BigDecimal("0.5"); // 기본값
        }
        
        BigDecimal comfortScore = new BigDecimal("1.0"); // 최대 1.0에서 시작
        
        // 각 날씨 조건에 따라 감점
        if (Boolean.TRUE.equals(weatherData.getIsRainy())) {
            comfortScore = comfortScore.subtract(new BigDecimal("0.3"));
        }
        
        if (Boolean.TRUE.equals(weatherData.getIsWindy())) {
            comfortScore = comfortScore.subtract(new BigDecimal("0.1"));
        }
        
        if (Boolean.TRUE.equals(weatherData.getIsTooCold()) || Boolean.TRUE.equals(weatherData.getIsTooHot())) {
            comfortScore = comfortScore.subtract(new BigDecimal("0.2"));
        }
        
        if (Boolean.TRUE.equals(weatherData.getIsBadAir())) {
            comfortScore = comfortScore.subtract(new BigDecimal("0.2"));
        }
        
        // 0.0 ~ 1.0 범위로 제한
        if (comfortScore.compareTo(BigDecimal.ZERO) < 0) {
            comfortScore = BigDecimal.ZERO;
        }
        
        return comfortScore.setScale(2, RoundingMode.HALF_UP);
    }
    
    /**
     * 날씨 상태를 결정합니다.
     */
    private String determineWeatherCondition(AiRecommendationResponse.WeatherData weatherData) {
        if (Boolean.TRUE.equals(weatherData.getIsRainy())) {
            return "비";
        } else if (Boolean.TRUE.equals(weatherData.getIsWindy())) {
            return "바람";
        } else if (Boolean.TRUE.equals(weatherData.getIsTooCold())) {
            return "추움";
        } else if (Boolean.TRUE.equals(weatherData.getIsTooHot())) {
            return "더움";
        } else {
            return "맑음";
        }
    }
    
    /**
     * AI 서버 상태를 확인합니다.
     */
    public boolean checkAiServerHealth() {
        return aiModelService.checkServerHealth();
    }
    
    /**
     * 추천 기록을 데이터베이스에 저장합니다.
     */
    private void saveRecommendationRecord(User user, AiRecommendationResponse aiResponse, RecommendationSummary summary) {
        try {
            // 추천된 활동 찾기
            Optional<Activity> activity = activityRepository
                    .findByActivityNameContainingAndIsDeletedFalse(aiResponse.getRecommendedActivity())
                    .stream()
                    .findFirst();
            
            if (activity.isPresent()) {
                Recommendation recommendation = Recommendation.createRecommendation(
                        user, 
                        activity.get(), 
                        aiResponse.getGeocoding() != null ? aiResponse.getGeocoding().getLatitude() : BigDecimal.ZERO,
                        aiResponse.getGeocoding() != null ? aiResponse.getGeocoding().getLongitude() : BigDecimal.ZERO,
                        summary.getRecommendationScore()
                );
                
                // 날씨 정보 설정
                if (aiResponse.getWeatherData() != null) {
                    AiRecommendationResponse.WeatherData weather = aiResponse.getWeatherData();
                    recommendation.setWeatherData(
                            weather.getTemperature(),
                            determineWeatherCondition(weather),
                            weather.getPm25() != null ? weather.getPm25().intValue() : null,
                            summary.getComfortScore()
                    );
                }
                
                recommendationRepository.save(recommendation);
                
                log.debug("추천 기록 저장 완료: recommendationId={}", recommendation.getRecommendationId());
            }
            
        } catch (Exception e) {
            log.error("추천 기록 저장 실패", e);
            // 추천 기록 저장 실패는 전체 프로세스를 중단시키지 않음
        }
    }
}
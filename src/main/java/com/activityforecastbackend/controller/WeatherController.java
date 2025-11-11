package com.activityforecastbackend.controller;

import com.activityforecastbackend.dto.ApiResponse;
import com.activityforecastbackend.dto.weather.AirQualityDto;
import com.activityforecastbackend.dto.weather.ForecastDto;
import com.activityforecastbackend.dto.weather.WeatherDto;
import com.activityforecastbackend.service.WeatherService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/weather")
@RequiredArgsConstructor
@Tag(name = "날씨 API", description = "날씨 정보 조회 API")
public class WeatherController {

    private final WeatherService weatherService;

    @GetMapping("/current")
    @Operation(
            summary = "현재 날씨 조회",
            description = "위도와 경도를 기반으로 현재 날씨 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<WeatherDto>> getCurrentWeather(
            @Parameter(description = "위도 (-90 ~ 90)", example = "37.5665")
            @RequestParam("lat") double latitude,
            @Parameter(description = "경도 (-180 ~ 180)", example = "126.9780")
            @RequestParam("lon") double longitude) {

        log.info("현재 날씨 조회 요청 - 위도: {}, 경도: {}", latitude, longitude);

        WeatherDto weatherData = weatherService.getCurrentWeather(latitude, longitude);
        
        return ResponseEntity.ok(ApiResponse.success("현재 날씨 조회가 완료되었습니다.", weatherData));
    }

    @GetMapping("/forecast")
    @Operation(
            summary = "5일 예보 조회",
            description = "위도와 경도를 기반으로 5일간의 날씨 예보를 3시간 간격으로 조회합니다."
    )
    public ResponseEntity<ApiResponse<ForecastDto>> getForecast(
            @Parameter(description = "위도 (-90 ~ 90)", example = "37.5665")
            @RequestParam("lat") double latitude,
            @Parameter(description = "경도 (-180 ~ 180)", example = "126.9780")
            @RequestParam("lon") double longitude) {

        log.info("5일 예보 조회 요청 - 위도: {}, 경도: {}", latitude, longitude);

        ForecastDto forecastData = weatherService.getForecast(latitude, longitude);
        
        return ResponseEntity.ok(ApiResponse.success("5일 예보 조회가 완료되었습니다.", forecastData));
    }

    @GetMapping("/air-quality")
    @Operation(
            summary = "대기질 조회",
            description = "위도와 경도를 기반으로 현재 대기질 정보를 조회합니다."
    )
    public ResponseEntity<ApiResponse<AirQualityDto>> getAirQuality(
            @Parameter(description = "위도 (-90 ~ 90)", example = "37.5665")
            @RequestParam("lat") double latitude,
            @Parameter(description = "경도 (-180 ~ 180)", example = "126.9780")
            @RequestParam("lon") double longitude) {

        log.info("대기질 조회 요청 - 위도: {}, 경도: {}", latitude, longitude);

        AirQualityDto airQualityData = weatherService.getAirQuality(latitude, longitude);
        
        return ResponseEntity.ok(ApiResponse.success("대기질 조회가 완료되었습니다.", airQualityData));
    }

    @GetMapping("/comprehensive")
    @Operation(
            summary = "종합 날씨 정보 조회",
            description = "현재 날씨, 예보, 대기질을 한 번에 조회합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getComprehensiveWeather(
            @Parameter(description = "위도 (-90 ~ 90)", example = "37.5665")
            @RequestParam("lat") double latitude,
            @Parameter(description = "경도 (-180 ~ 180)", example = "126.9780")
            @RequestParam("lon") double longitude) {

        log.info("종합 날씨 정보 조회 요청 - 위도: {}, 경도: {}", latitude, longitude);

        WeatherDto currentWeather = weatherService.getCurrentWeather(latitude, longitude);
        ForecastDto forecast = weatherService.getForecast(latitude, longitude);
        AirQualityDto airQuality = weatherService.getAirQuality(latitude, longitude);

        Map<String, Object> comprehensiveData = Map.of(
                "current", currentWeather,
                "forecast", forecast,
                "airQuality", airQuality
        );
        
        return ResponseEntity.ok(ApiResponse.success("종합 날씨 정보 조회가 완료되었습니다.", comprehensiveData));
    }

    @GetMapping("/today-summary")
    @Operation(
            summary = "오늘의 날씨 요약",
            description = "UI 표시용으로 간소화된 오늘의 날씨 정보를 제공합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> getTodaySummary(
            @Parameter(description = "위도 (-90 ~ 90)", example = "37.5665")
            @RequestParam("lat") double latitude,
            @Parameter(description = "경도 (-180 ~ 180)", example = "126.9780")
            @RequestParam("lon") double longitude) {

        log.info("오늘의 날씨 요약 조회 요청 - 위도: {}, 경도: {}", latitude, longitude);

        WeatherDto currentWeather = weatherService.getCurrentWeather(latitude, longitude);
        AirQualityDto airQuality = weatherService.getAirQuality(latitude, longitude);

        Map<String, Object> todaySummary = Map.of(
                "temperature", Math.round(currentWeather.getTemperatureInCelsius()),
                "temperatureUnit", "°C",
                "condition", currentWeather.getWeatherConditionKorean(),
                "icon", currentWeather.getWeather().get(0).getIcon(),
                "humidity", currentWeather.getMain().getHumidity(),
                "airQuality", airQuality.getOverallAirQualityKorean(),
                "airQualityIndex", airQuality.getAirQualityIndex(),
                "comfortScore", currentWeather.getComfortScore(),
                "cityName", currentWeather.getCityName(),
                "datetime", currentWeather.getDateTime()
        );
        
        return ResponseEntity.ok(ApiResponse.success("오늘의 날씨 요약 조회가 완료되었습니다.", todaySummary));
    }

    @GetMapping("/health")
    @Operation(
            summary = "날씨 API 상태 확인",
            description = "날씨 API 연결 상태와 API 키 유효성을 확인합니다."
    )
    public ResponseEntity<ApiResponse<Map<String, Object>>> checkHealth() {
        log.info("날씨 API 상태 확인 요청");

        boolean apiKeyValid = weatherService.isApiKeyValid();
        
        Map<String, Object> healthStatus = Map.of(
                "apiKeyValid", apiKeyValid,
                "status", apiKeyValid ? "healthy" : "unhealthy",
                "timestamp", java.time.LocalDateTime.now()
        );

        String message = apiKeyValid ? "날씨 API 상태가 정상입니다." : "날씨 API 키가 유효하지 않습니다.";
        
        return ResponseEntity.ok(ApiResponse.success(message, healthStatus));
    }
}
package com.activityforecastbackend.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.SERVICE_UNAVAILABLE)
public class WeatherApiException extends RuntimeException {

    private final String apiType;
    private final String errorCode;

    public WeatherApiException(String message) {
        super(message);
        this.apiType = "WEATHER_API";
        this.errorCode = "API_ERROR";
    }

    public WeatherApiException(String apiType, String message) {
        super(message);
        this.apiType = apiType;
        this.errorCode = "API_ERROR";
    }

    public WeatherApiException(String apiType, String errorCode, String message) {
        super(message);
        this.apiType = apiType;
        this.errorCode = errorCode;
    }

    public WeatherApiException(String apiType, String message, Throwable cause) {
        super(message, cause);
        this.apiType = apiType;
        this.errorCode = "API_ERROR";
    }

    public WeatherApiException(String apiType, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.apiType = apiType;
        this.errorCode = errorCode;
    }

    public String getApiType() {
        return apiType;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public static WeatherApiException currentWeatherError(String message) {
        return new WeatherApiException("CURRENT_WEATHER", "CURRENT_WEATHER_ERROR", message);
    }

    public static WeatherApiException currentWeatherError(String message, Throwable cause) {
        return new WeatherApiException("CURRENT_WEATHER", "CURRENT_WEATHER_ERROR", message, cause);
    }

    public static WeatherApiException forecastError(String message) {
        return new WeatherApiException("FORECAST", "FORECAST_ERROR", message);
    }

    public static WeatherApiException forecastError(String message, Throwable cause) {
        return new WeatherApiException("FORECAST", "FORECAST_ERROR", message, cause);
    }

    public static WeatherApiException airQualityError(String message) {
        return new WeatherApiException("AIR_QUALITY", "AIR_QUALITY_ERROR", message);
    }

    public static WeatherApiException airQualityError(String message, Throwable cause) {
        return new WeatherApiException("AIR_QUALITY", "AIR_QUALITY_ERROR", message, cause);
    }

    public static WeatherApiException apiKeyError() {
        return new WeatherApiException("AUTHENTICATION", "INVALID_API_KEY", "유효하지 않은 날씨 API 키입니다.");
    }

    public static WeatherApiException rateLimitError() {
        return new WeatherApiException("RATE_LIMIT", "API_RATE_LIMIT", "날씨 API 호출 한도를 초과했습니다.");
    }

    public static WeatherApiException timeoutError() {
        return new WeatherApiException("TIMEOUT", "API_TIMEOUT", "날씨 API 응답 시간이 초과되었습니다.");
    }

    public static WeatherApiException invalidLocationError(double lat, double lon) {
        return new WeatherApiException("LOCATION", "INVALID_LOCATION", 
                String.format("유효하지 않은 위치입니다: 위도=%.6f, 경도=%.6f", lat, lon));
    }

    public static WeatherApiException parseError(String message) {
        return new WeatherApiException("RESPONSE_PARSE", "PARSE_ERROR", "날씨 API 응답 파싱 중 오류가 발생했습니다: " + message);
    }

    public static WeatherApiException parseError(String message, Throwable cause) {
        return new WeatherApiException("RESPONSE_PARSE", "PARSE_ERROR", "날씨 API 응답 파싱 중 오류가 발생했습니다: " + message, cause);
    }
}
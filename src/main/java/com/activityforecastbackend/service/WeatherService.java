package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.weather.AirQualityDto;
import com.activityforecastbackend.dto.weather.ForecastDto;
import com.activityforecastbackend.dto.weather.WeatherDto;
import com.activityforecastbackend.exception.WeatherApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;
import java.util.Map;

@Slf4j
@Service
public class WeatherService {

    private static final String OPENWEATHER_BASE_URL = "https://api.openweathermap.org";
    private static final String CURRENT_WEATHER_PATH = "/data/2.5/weather";
    private static final String FORECAST_PATH = "/data/2.5/forecast";
    private static final String AIR_POLLUTION_PATH = "/data/2.5/air_pollution";

    private final RestTemplate weatherRestTemplate;
    private final String apiKey;

    public WeatherService(
            @Qualifier("weatherRestTemplate") RestTemplate weatherRestTemplate,
            @Value("${weather.api.key}") String apiKey) {
        this.weatherRestTemplate = weatherRestTemplate;
        this.apiKey = apiKey;
    }

    public WeatherDto getCurrentWeather(double latitude, double longitude) {
        log.info("현재 날씨 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        try {
            validateCoordinates(latitude, longitude);

            URI uri = UriComponentsBuilder.fromHttpUrl(OPENWEATHER_BASE_URL + CURRENT_WEATHER_PATH)
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("appid", apiKey)
                    .queryParam("lang", "kr")
                    .build()
                    .toUri();

            log.debug("현재 날씨 API 호출 URL: {}", uri);

            WeatherDto weatherDto = weatherRestTemplate.getForObject(uri, WeatherDto.class);
            
            if (weatherDto == null) {
                throw WeatherApiException.currentWeatherError("날씨 API로부터 빈 응답을 받았습니다.");
            }

            log.info("현재 날씨 조회 완료 - 온도: {}°C, 상태: {}", 
                    weatherDto.getTemperatureInCelsius(), 
                    weatherDto.getWeatherConditionKorean());

            return weatherDto;

        } catch (HttpClientErrorException e) {
            log.error("날씨 API 클라이언트 오류: {} {}", e.getStatusCode(), e.getStatusText());
            throw handleHttpClientError(e);
        } catch (HttpServerErrorException e) {
            log.error("날씨 API 서버 오류: {} {}", e.getStatusCode(), e.getStatusText());
            throw WeatherApiException.currentWeatherError("날씨 서비스 서버 오류입니다.");
        } catch (ResourceAccessException e) {
            log.error("날씨 API 연결 실패", e);
            throw WeatherApiException.timeoutError();
        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("현재 날씨 조회 중 예외 발생", e);
            throw WeatherApiException.currentWeatherError("현재 날씨 조회 중 오류가 발생했습니다.", e);
        }
    }

    public ForecastDto getForecast(double latitude, double longitude) {
        log.info("5일 예보 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        try {
            validateCoordinates(latitude, longitude);

            URI uri = UriComponentsBuilder.fromHttpUrl(OPENWEATHER_BASE_URL + FORECAST_PATH)
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("appid", apiKey)
                    .queryParam("lang", "kr")
                    .build()
                    .toUri();

            log.debug("5일 예보 API 호출 URL: {}", uri);

            ForecastDto forecastDto = weatherRestTemplate.getForObject(uri, ForecastDto.class);
            
            if (forecastDto == null) {
                throw WeatherApiException.forecastError("예보 API로부터 빈 응답을 받았습니다.");
            }

            log.info("5일 예보 조회 완료 - 예보 항목 수: {}", 
                    forecastDto.getList() != null ? forecastDto.getList().size() : 0);

            return forecastDto;

        } catch (HttpClientErrorException e) {
            log.error("예보 API 클라이언트 오류: {} {}", e.getStatusCode(), e.getStatusText());
            throw handleHttpClientError(e);
        } catch (HttpServerErrorException e) {
            log.error("예보 API 서버 오류: {} {}", e.getStatusCode(), e.getStatusText());
            throw WeatherApiException.forecastError("예보 서비스 서버 오류입니다.");
        } catch (ResourceAccessException e) {
            log.error("예보 API 연결 실패", e);
            throw WeatherApiException.timeoutError();
        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("5일 예보 조회 중 예외 발생", e);
            throw WeatherApiException.forecastError("5일 예보 조회 중 오류가 발생했습니다.", e);
        }
    }

    public AirQualityDto getAirQuality(double latitude, double longitude) {
        log.info("대기질 조회 시작 - 위도: {}, 경도: {}", latitude, longitude);

        try {
            validateCoordinates(latitude, longitude);

            URI uri = UriComponentsBuilder.fromHttpUrl(OPENWEATHER_BASE_URL + AIR_POLLUTION_PATH)
                    .queryParam("lat", latitude)
                    .queryParam("lon", longitude)
                    .queryParam("appid", apiKey)
                    .build()
                    .toUri();

            log.debug("대기질 API 호출 URL: {}", uri);

            AirQualityDto airQualityDto = weatherRestTemplate.getForObject(uri, AirQualityDto.class);
            
            if (airQualityDto == null) {
                throw WeatherApiException.airQualityError("대기질 API로부터 빈 응답을 받았습니다.");
            }

            log.info("대기질 조회 완료 - AQI: {}, 상태: {}", 
                    airQualityDto.getAirQualityIndex(),
                    airQualityDto.getAirQualityStatusKorean());

            return airQualityDto;

        } catch (HttpClientErrorException e) {
            log.error("대기질 API 클라이언트 오류: {} {}", e.getStatusCode(), e.getStatusText());
            throw handleHttpClientError(e);
        } catch (HttpServerErrorException e) {
            log.error("대기질 API 서버 오류: {} {}", e.getStatusCode(), e.getStatusText());
            throw WeatherApiException.airQualityError("대기질 서비스 서버 오류입니다.");
        } catch (ResourceAccessException e) {
            log.error("대기질 API 연결 실패", e);
            throw WeatherApiException.timeoutError();
        } catch (WeatherApiException e) {
            throw e;
        } catch (Exception e) {
            log.error("대기질 조회 중 예외 발생", e);
            throw WeatherApiException.airQualityError("대기질 조회 중 오류가 발생했습니다.", e);
        }
    }

    public WeatherDto getCurrentWeatherWithRetry(double latitude, double longitude) {
        return getCurrentWeatherWithRetry(latitude, longitude, 3);
    }

    private WeatherDto getCurrentWeatherWithRetry(double latitude, double longitude, int retryCount) {
        for (int i = 0; i < retryCount; i++) {
            try {
                return getCurrentWeather(latitude, longitude);
            } catch (WeatherApiException e) {
                if (i == retryCount - 1) {
                    throw e;
                }
                log.warn("날씨 API 호출 실패, 재시도 {}/{}", i + 1, retryCount);
                
                try {
                    Thread.sleep(1000 * (i + 1));
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    throw WeatherApiException.currentWeatherError("재시도 중 인터럽트 발생");
                }
            }
        }
        
        throw WeatherApiException.currentWeatherError("재시도 후에도 날씨 데이터를 가져올 수 없습니다.");
    }

    private void validateCoordinates(double latitude, double longitude) {
        if (latitude < -90 || latitude > 90) {
            throw WeatherApiException.invalidLocationError(latitude, longitude);
        }
        if (longitude < -180 || longitude > 180) {
            throw WeatherApiException.invalidLocationError(latitude, longitude);
        }
    }

    private void validateApiKey() {
        if (apiKey == null || apiKey.trim().isEmpty() || "your-weather-api-key".equals(apiKey)) {
            throw WeatherApiException.apiKeyError();
        }
    }

    public boolean isApiKeyValid() {
        try {
            validateApiKey();
            return true;
        } catch (WeatherApiException e) {
            return false;
        }
    }

    public Map<String, Object> getWeatherDataForSchedule(double latitude, double longitude) {
        log.info("일정용 날씨 데이터 조회 - 위도: {}, 경도: {}", latitude, longitude);

        try {
            WeatherDto currentWeather = getCurrentWeather(latitude, longitude);
            AirQualityDto airQuality = getAirQuality(latitude, longitude);

            return Map.of(
                    "temperature", currentWeather.getTemperatureInCelsius(),
                    "weatherCondition", currentWeather.getWeatherConditionKorean(),
                    "airQualityIndex", airQuality.getAirQualityIndex(),
                    "comfortScore", currentWeather.getComfortScore(),
                    "humidity", currentWeather.getMain().getHumidity(),
                    "windSpeed", currentWeather.getWind() != null ? currentWeather.getWind().getSpeed() : 0.0,
                    "timestamp", currentWeather.getDateTime()
            );
        } catch (WeatherApiException e) {
            log.warn("일정용 날씨 데이터 조회 실패: {}", e.getMessage());
            return Map.of(
                    "temperature", 20.0,
                    "weatherCondition", "정보없음",
                    "airQualityIndex", 2,
                    "comfortScore", 0.5,
                    "humidity", 50,
                    "windSpeed", 0.0,
                    "timestamp", java.time.LocalDateTime.now()
            );
        }
    }

    public Double getComfortScoreForActivity(double latitude, double longitude, String activityType) {
        log.info("활동별 쾌적도 점수 계산 - 활동: {}, 위도: {}, 경도: {}", activityType, latitude, longitude);

        try {
            WeatherDto weather = getCurrentWeather(latitude, longitude);
            AirQualityDto airQuality = getAirQuality(latitude, longitude);

            double baseScore = weather.getComfortScore().doubleValue();
            
            if ("실외".equals(activityType)) {
                Integer aqi = airQuality.getAirQualityIndex();
                if (aqi != null && aqi > 3) {
                    baseScore -= 0.3;
                }
            } else if ("실내".equals(activityType)) {
                baseScore += 0.1;
            }

            return Math.max(0.0, Math.min(1.0, baseScore));

        } catch (WeatherApiException e) {
            log.warn("활동별 쾌적도 점수 계산 실패: {}", e.getMessage());
            return 0.5;
        }
    }

    private WeatherApiException handleHttpClientError(HttpClientErrorException e) {
        switch (e.getStatusCode().value()) {
            case 401:
                return WeatherApiException.apiKeyError();
            case 429:
                return WeatherApiException.rateLimitError();
            case 404:
                return WeatherApiException.invalidLocationError(0, 0);
            case 400:
                return new WeatherApiException("WEATHER_API", "BAD_REQUEST", "잘못된 요청 파라미터입니다.");
            default:
                return new WeatherApiException("WEATHER_API", "CLIENT_ERROR", "날씨 API 클라이언트 오류: " + e.getStatusText());
        }
    }
}
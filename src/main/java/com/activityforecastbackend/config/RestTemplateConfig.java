package com.activityforecastbackend.config;

import com.activityforecastbackend.exception.WeatherApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpStatus;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.ResponseErrorHandler;
import org.springframework.web.client.RestTemplate;

import java.io.IOException;

@Slf4j
@Configuration
public class RestTemplateConfig {

    @Bean
    @Primary
    public RestTemplate restTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(clientHttpRequestFactory());
        return restTemplate;
    }

    @Bean("weatherRestTemplate")
    public RestTemplate weatherRestTemplate() {
        RestTemplate restTemplate = new RestTemplate();
        restTemplate.setRequestFactory(weatherClientHttpRequestFactory());
        restTemplate.setErrorHandler(weatherApiErrorHandler());
        return restTemplate;
    }

    @Bean
    public ClientHttpRequestFactory clientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5000);  // 연결 타임아웃 5초
        factory.setReadTimeout(10000);    // 읽기 타임아웃 10초
        return factory;
    }

    @Bean
    public ClientHttpRequestFactory weatherClientHttpRequestFactory() {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(10000);  // 날씨 API 연결 타임아웃 10초
        factory.setReadTimeout(15000);     // 날씨 API 읽기 타임아웃 15초
        return factory;
    }

    @Bean
    public ResponseErrorHandler weatherApiErrorHandler() {
        return new WeatherApiErrorHandler();
    }

    @Slf4j
    private static class WeatherApiErrorHandler implements ResponseErrorHandler {

        @Override
        public boolean hasError(org.springframework.http.client.ClientHttpResponse response) throws IOException {
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            return status.isError();
        }

        @Override
        public void handleError(org.springframework.http.client.ClientHttpResponse response) throws IOException {
            HttpStatus status = HttpStatus.valueOf(response.getStatusCode().value());
            String statusText = response.getStatusText();

            log.error("Weather API error: {} {}", status.value(), statusText);

            switch (status) {
                case UNAUTHORIZED:
                    throw WeatherApiException.apiKeyError();
                case TOO_MANY_REQUESTS:
                    throw WeatherApiException.rateLimitError();
                case NOT_FOUND:
                    throw WeatherApiException.invalidLocationError(0, 0);
                case BAD_REQUEST:
                    throw new WeatherApiException("WEATHER_API", "BAD_REQUEST", "잘못된 요청 파라미터입니다.");
                case INTERNAL_SERVER_ERROR:
                case BAD_GATEWAY:
                case SERVICE_UNAVAILABLE:
                case GATEWAY_TIMEOUT:
                    throw new WeatherApiException("WEATHER_API", "SERVER_ERROR", "날씨 서비스 서버 오류입니다.");
                default:
                    throw new WeatherApiException("WEATHER_API", "UNKNOWN_ERROR", "알 수 없는 날씨 API 오류: " + statusText);
            }
        }
    }
}
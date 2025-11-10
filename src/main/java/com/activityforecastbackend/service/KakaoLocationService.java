package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.activity.*;
import com.activityforecastbackend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLocationService {

    private final RestTemplate restTemplate;

    @Value("${kakao.api.key}")
    private String kakaoApiKey;

    @Value("${kakao.api.origin}")
    private String kakaoOrigin;

    private static final String KAKAO_API_BASE_URL = "https://dapi.kakao.com/v2/local";

    /**
     * 활동별 카카오 카테고리 매핑
     */
    private static final Map<String, String> ACTIVITY_CATEGORY_MAP = Map.of(
            "축구", "SW8",      // 스포츠,레저 > 스포츠시설
            "농구", "SW8",
            "야구", "SW8",
            "배구", "SW8",
            "테니스", "SW8",
            "볼링", "SW8",
            "헬스", "SW8",
            "수영", "SW8",
            "골프", "SW8",
            "스키", "SW8"
    );

    /**
     * 키워드로 장소 검색
     */
    public List<KakaoPlaceDto> searchPlacesByKeyword(String keyword, BigDecimal latitude, BigDecimal longitude, Integer radius) {
        log.info("Searching places by keyword: {} at ({}, {}) within {}m",
                keyword, latitude, longitude, radius);

        try {
            URI uri = UriComponentsBuilder.fromUriString(KAKAO_API_BASE_URL + "/search/keyword.json")
                    .queryParam("query", keyword)
                    .queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .queryParam("radius", radius != null ? radius : 5000) // 기본 5km
                    .queryParam("page", 1)
                    .queryParam("size", 15)
                    .queryParam("sort", "distance")
                    .build()
                    .encode()
                    .toUri();

            HttpEntity<?> entity = new HttpEntity<>(createKakaoHeaders());

            ResponseEntity<KakaoSearchResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, KakaoSearchResponse.class);

            if (response.getBody() != null && response.getBody().getDocuments() != null) {
                log.info("Found {} places for keyword: {}",
                        response.getBody().getDocuments().size(), keyword);
                return response.getBody().getDocuments();
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Error searching places by keyword: {}", e.getMessage(), e);
            throw new BadRequestException("장소 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 카테고리로 장소 검색
     */
    public List<KakaoPlaceDto> searchPlacesByCategory(String categoryCode, BigDecimal latitude, BigDecimal longitude, Integer radius) {
        log.info("Searching places by category: {} at ({}, {}) within {}m",
                categoryCode, latitude, longitude, radius);

        try {
            URI uri = UriComponentsBuilder.fromUriString(KAKAO_API_BASE_URL + "/search/category.json")
                    .queryParam("category_group_code", categoryCode)
                    .queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .queryParam("radius", radius != null ? radius : 5000)
                    .queryParam("page", 1)
                    .queryParam("size", 15)
                    .queryParam("sort", "distance")
                    .build()
                    .encode()
                    .toUri();

            HttpEntity<?> entity = new HttpEntity<>(createKakaoHeaders());

            ResponseEntity<KakaoSearchResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, KakaoSearchResponse.class);

            if (response.getBody() != null && response.getBody().getDocuments() != null) {
                log.info("Found {} places for category: {}",
                        response.getBody().getDocuments().size(), categoryCode);
                return response.getBody().getDocuments();
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("Error searching places by category: {}", e.getMessage(), e);
            throw new BadRequestException("카테고리 장소 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 활동명으로 적합한 장소 검색
     */
    public List<KakaoPlaceDto> searchPlacesByActivity(String activityName, BigDecimal latitude, BigDecimal longitude, Integer radius) {
        log.info("Searching places for activity: {}", activityName);

        // 1. 카테고리 매핑이 있으면 카테고리로 검색
        String categoryCode = ACTIVITY_CATEGORY_MAP.get(activityName);
        if (categoryCode != null) {
            List<KakaoPlaceDto> categoryResults = searchPlacesByCategory(categoryCode, latitude, longitude, radius);
            if (!categoryResults.isEmpty()) {
                return categoryResults;
            }
        }

        // 2. 카테고리 검색 결과가 없으면 키워드로 검색
        return searchPlacesByKeyword(activityName, latitude, longitude, radius);
    }

    /**
     * 주소를 좌표로 변환 (지오코딩)
     */
    public CoordinateDto geocodeAddress(String address) {
        log.info("Geocoding address: {}", address);

        URI uri = null;
        try {
            uri = UriComponentsBuilder.fromUriString(KAKAO_API_BASE_URL + "/search/address.json")
                    .queryParam("query", address)
                    .queryParam("page", 1)
                    .queryParam("size", 1)
                    .build()
                    .encode()
                    .toUri();

            ResponseEntity<KakaoSearchResponse> response = null;

            // 1차 시도: KA 헤더 포함
            try {
                HttpEntity<?> entity = new HttpEntity<>(createKakaoHeaders());
                response = restTemplate.exchange(uri, HttpMethod.GET, entity, KakaoSearchResponse.class);
            } catch (Exception e) {
                if (e.getMessage().contains("KA Header")) {
                    log.warn("KA 헤더 방식 실패, 대체 헤더로 재시도: {}", e.getMessage());
                    // 2차 시도: 대체 헤더
                    HttpEntity<?> entity = new HttpEntity<>(createAlternativeHeaders());
                    response = restTemplate.exchange(uri, HttpMethod.GET, entity, KakaoSearchResponse.class);
                } else {
                    throw e;
                }
            }

            if (response.getBody() != null &&
                    response.getBody().getDocuments() != null &&
                    !response.getBody().getDocuments().isEmpty()) {

                KakaoPlaceDto place = response.getBody().getDocuments().get(0);

                return CoordinateDto.builder()
                        .latitude(place.getLatitude())
                        .longitude(place.getLongitude())
                        .address(place.getAddressName())
                        .roadAddress(place.getRoadAddressName())
                        .build();
            }

            // 주소 검색 실패 시 키워드 검색으로 fallback (역명 등을 위해)
            log.info("주소 검색 실패, 키워드 검색으로 재시도: {}", address);
            List<KakaoPlaceDto> keywordResults = searchPlacesByKeyword(
                    address, new BigDecimal("37.5665"), new BigDecimal("126.9780"), 20000);

            if (!keywordResults.isEmpty()) {
                KakaoPlaceDto place = keywordResults.get(0);
                return CoordinateDto.builder()
                        .latitude(place.getLatitude())
                        .longitude(place.getLongitude())
                        .address(place.getAddressName())
                        .roadAddress(place.getRoadAddressName())
                        .build();
            }

            throw new BadRequestException("해당 주소 또는 장소를 찾을 수 없습니다: " + address);

        } catch (Exception e) {
            log.error("Error geocoding address: {} | Request URI: {} | Error: {}",
                    address, uri != null ? uri.toString() : "URI not built", e.getMessage(), e);

            // HTTP 에러인 경우 상세 정보 추가
            if (e.getMessage().contains("400")) {
                log.error("카카오 API 400 오류: API 키 또는 요청 형식을 확인해주세요. API Key 유효성: {}",
                        isApiKeyValid());
            }

            throw new BadRequestException("주소 변환 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 좌표를 주소로 변환 (역지오코딩)
     */
    public CoordinateDto reverseGeocode(BigDecimal latitude, BigDecimal longitude) {
        log.info("Reverse geocoding coordinates: ({}, {})", latitude, longitude);

        try {
            URI uri = UriComponentsBuilder.fromUriString(KAKAO_API_BASE_URL + "/geo/coord2address.json")
                    .queryParam("x", longitude)
                    .queryParam("y", latitude)
                    .queryParam("input_coord", "WGS84")
                    .build()
                    .encode()
                    .toUri();

            HttpEntity<?> entity = new HttpEntity<>(createKakaoHeaders());

            // 역지오코딩은 다른 응답 형식이므로 직접 Map으로 처리
            ResponseEntity<Map> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, Map.class);

            if (response.getBody() != null) {
                Map<String, Object> body = response.getBody();
                List<Map<String, Object>> documents = (List<Map<String, Object>>) body.get("documents");

                if (documents != null && !documents.isEmpty()) {
                    Map<String, Object> document = documents.get(0);
                    Map<String, Object> address = (Map<String, Object>) document.get("address");
                    Map<String, Object> roadAddress = (Map<String, Object>) document.get("road_address");

                    return CoordinateDto.builder()
                            .latitude(latitude)
                            .longitude(longitude)
                            .address(address != null ? (String) address.get("address_name") : null)
                            .roadAddress(roadAddress != null ? (String) roadAddress.get("address_name") : null)
                            .build();
                }
            }

            throw new BadRequestException("해당 좌표의 주소를 찾을 수 없습니다");

        } catch (Exception e) {
            log.error("Error reverse geocoding: {}", e.getMessage(), e);
            throw new BadRequestException("좌표 변환 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 카카오 API 요청용 HttpHeaders 생성
     */
    private HttpHeaders createKakaoHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        headers.set("KA", String.format("sdk/1.0 os/java lang/ko-KR origin/%s", kakaoOrigin));
        headers.set("User-Agent", "ActivityForecast/1.0");
        return headers;
    }

    /**
     * 대체 헤더 (KA 없이) 생성
     */
    private HttpHeaders createAlternativeHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "KakaoAK " + kakaoApiKey);
        headers.set("User-Agent", "ActivityForecast/1.0");
        headers.set("Accept", "application/json");
        return headers;
    }

    /**
     * 카카오 API 키 유효성 검사
     */
    public boolean isApiKeyValid() {
        return kakaoApiKey != null &&
                !kakaoApiKey.isEmpty() &&
                !kakaoApiKey.equals("your-kakao-api-key") &&
                !kakaoApiKey.equals("your-rest-api-key-here");
    }

    /**
     * 애플리케이션 시작 시 카카오 API 키 테스트
     */
    @EventListener(ApplicationReadyEvent.class)
    public void testKakaoApiKey() {
        if (!isApiKeyValid()) {
            log.warn("❌ 카카오 API 키가 설정되지 않았습니다. 외부 장소 검색 기능이 제한됩니다.");
            return;
        }

        log.info("🔧 카카오 API 설정 - Origin: {}", kakaoOrigin);

        URI testUri = UriComponentsBuilder.fromUriString(KAKAO_API_BASE_URL + "/search/keyword.json")
                .queryParam("query", "카페")
                .queryParam("x", "127.027926")
                .queryParam("y", "37.498095")
                .queryParam("size", "1")
                .build()
                .encode()
                .toUri();

        // 1차 시도: KA 헤더 포함
        try {
            HttpEntity<?> entity = new HttpEntity<>(createKakaoHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    testUri, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ 카카오 API 키가 정상적으로 설정되었습니다. (KA 헤더 방식)");
                return;
            }

        } catch (Exception e) {
            log.warn("⚠️ KA 헤더 방식 실패: {}", e.getMessage());
        }

        // 2차 시도: KA 헤더 없이
        try {
            HttpEntity<?> entity = new HttpEntity<>(createAlternativeHeaders());
            ResponseEntity<String> response = restTemplate.exchange(
                    testUri, HttpMethod.GET, entity, String.class);

            if (response.getStatusCode().is2xxSuccessful()) {
                log.info("✅ 카카오 API 키가 정상적으로 설정되었습니다. (대체 헤더 방식)");
                return;
            }

        } catch (Exception e) {
            log.error("❌ 카카오 API 키 테스트 중 오류 발생: {}", e.getMessage());
            log.error("   - API 키를 카카오 개발자 콘솔에서 확인해주세요.");
            log.error("   - 플랫폼 설정에서 도메인이 올바르게 등록되었는지 확인해주세요.");
            log.error("   - REST API 키가 올바른지 확인해주세요. (JavaScript 키와 다름)");
        }
    }
}
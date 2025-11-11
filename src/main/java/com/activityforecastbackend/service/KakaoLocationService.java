package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.activity.*;
import com.activityforecastbackend.exception.BadRequestException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.core.env.Environment;
import jakarta.annotation.PostConstruct;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.math.BigDecimal;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class KakaoLocationService {

    private final RestTemplate restTemplate;
    
    @Autowired
    private Environment environment;
    
    private String kakaoApiKey;
    private String kakaoOrigin;

    private static final String KAKAO_API_BASE_URL = "https://dapi.kakao.com/v2/local";
    
    /**
     * 애플리케이션 시작 시 카카오 API 설정 초기화
     * 로컬 환경: Spring 프로퍼티 우선 사용
     * 운영 환경: 환경변수 직접 접근으로 폴백
     */
    @PostConstruct
    public void initializeKakaoApiSettings() {
        // 1순위: Spring 프로퍼티에서 로드 (로컬 환경)
        kakaoApiKey = environment.getProperty("kakao.api.key");
        kakaoOrigin = environment.getProperty("kakao.api.origin");
        
        // 2순위: 환경변수에서 직접 로드 (운영 환경 폴백)
        if (kakaoApiKey == null || kakaoApiKey.isEmpty()) {
            kakaoApiKey = System.getenv("KAKAO_API_KEY");
            log.info("Spring 프로퍼티에서 API 키 로드 실패, 환경변수에서 로드 시도");
        }
        
        if (kakaoOrigin == null || kakaoOrigin.isEmpty()) {
            kakaoOrigin = System.getenv("KAKAO_API_ORIGIN");
            if (kakaoOrigin == null || kakaoOrigin.isEmpty()) {
                kakaoOrigin = "localhost"; // 기본값
            }
        }
        
        // 초기화 상태 로깅
        log.info("=== 카카오 API 설정 초기화 완료 (PR #17 병합 후 재배포) ===");
        log.info("API 키 상태: {}", kakaoApiKey != null && !kakaoApiKey.isEmpty() ? "설정됨" : "미설정");
        log.info("API 키 소스: {}", environment.getProperty("kakao.api.key") != null ? "Spring 프로퍼티" : "환경변수");
        log.info("Origin: {}", kakaoOrigin);
        log.info("현재 프로파일: {}", String.join(",", environment.getActiveProfiles()));
        
        if (kakaoApiKey != null && !kakaoApiKey.isEmpty()) {
            log.info("API 키 앞 4자리: {}***", kakaoApiKey.substring(0, Math.min(4, kakaoApiKey.length())));
        } else {
            log.warn("❌ 카카오 API 키가 설정되지 않았습니다!");
            log.warn("  - Spring 프로퍼티: kakao.api.key = {}", environment.getProperty("kakao.api.key"));
            log.warn("  - 환경변수: KAKAO_API_KEY = {}", System.getenv("KAKAO_API_KEY"));
        }
        log.info("======================================");
    }

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
    
    // 지하철역 관련 카테고리 코드들 (카카오 API 기준)
    private static final String[] SUBWAY_CATEGORIES = {"SW8", "MT1", "CS2"};

    /**
     * 키워드로 전국 범위 장소 검색 (좌표 제한 없음)
     */
    public List<KakaoPlaceDto> searchPlacesByKeywordNationwide(String keyword) {
        return searchPlacesByKeywordWithAnalyzeType(keyword, null);
    }

    /**
     * analyze_type을 지정한 전국 범위 키워드 검색
     */
    public List<KakaoPlaceDto> searchPlacesByKeywordWithAnalyzeType(String keyword, String analyzeType) {
        String analyzeTypeStr = analyzeType != null ? analyzeType : "similar";
        log.info("전국 범위 키워드 검색 ({}): {}", analyzeTypeStr, keyword);
        
        // API 키 유효성 검사
        if (!isApiKeyValid()) {
            log.error("❌ 카카오 API 키가 설정되지 않았습니다.");
            return Collections.emptyList();
        }

        try {
            UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(KAKAO_API_BASE_URL + "/search/keyword.json")
                    .queryParam("query", keyword)
                    .queryParam("page", 1)
                    .queryParam("size", 15)
                    .queryParam("sort", "accuracy"); // 정확도 순 정렬
            
            // analyze_type이 지정된 경우에만 추가
            if (analyzeType != null) {
                uriBuilder.queryParam("analyze_type", analyzeType);
            }
            
            URI uri = uriBuilder.build().encode().toUri();

            HttpEntity<?> entity = new HttpEntity<>(createKakaoHeaders());

            ResponseEntity<KakaoSearchResponse> response = restTemplate.exchange(
                    uri, HttpMethod.GET, entity, KakaoSearchResponse.class);

            if (response.getBody() != null && response.getBody().getDocuments() != null) {
                log.info("전국 검색 결과 {}개 (정확도 순, {})", 
                        response.getBody().getDocuments().size(), analyzeTypeStr);
                return response.getBody().getDocuments();
            }

            return Collections.emptyList();

        } catch (Exception e) {
            log.error("전국 키워드 검색 중 오류 ({}): {}", analyzeTypeStr, e.getMessage(), e);
            throw new BadRequestException("장소 검색 중 오류가 발생했습니다: " + e.getMessage());
        }
    }

    /**
     * 키워드로 장소 검색
     */
    public List<KakaoPlaceDto> searchPlacesByKeyword(String keyword, BigDecimal latitude, BigDecimal longitude, Integer radius) {
        log.info("Searching places by keyword: {} at ({}, {}) within {}m",
                keyword, latitude, longitude, radius);
        
        // API 키 유효성 검사
        if (!isApiKeyValid()) {
            log.error("❌ 카카오 API 키가 설정되지 않았습니다.");
            return Collections.emptyList();
        }

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
        
        // API 키 유효성 검사
        if (!isApiKeyValid()) {
            log.error("❌ 카카오 API 키가 설정되지 않았습니다.");
            return Collections.emptyList();
        }

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
        
        // API 키 유효성 검사 먼저 수행
        if (!isApiKeyValid()) {
            log.error("❌ 카카오 API 키가 설정되지 않았거나 유효하지 않습니다. 현재 키: [{}]", 
                    kakaoApiKey == null ? "null" : (kakaoApiKey.isEmpty() ? "empty" : "***"));
            throw new BadRequestException("카카오 API 키가 설정되지 않았습니다. 관리자에게 문의하세요.");
        }

        // 지하철역 검색 우선 처리
        if (address.contains("역")) {
            log.info("지하철역 키워드 감지, 카테고리 검색 우선 적용: {}", address);
            CoordinateDto stationResult = searchSubwayStation(address);
            if (stationResult != null) {
                return stationResult;
            }
        }

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
            log.info("주소 검색 실패, 스마트 키워드 검색으로 재시도: {}", address);
            List<KakaoPlaceDto> keywordResults = searchWithSmartStrategy(address);

            if (!keywordResults.isEmpty()) {
                // 검색 결과 로깅 및 분석
                log.info("키워드 검색 결과 {}개:", keywordResults.size());
                for (int i = 0; i < Math.min(keywordResults.size(), 5); i++) {
                    KakaoPlaceDto result = keywordResults.get(i);
                    log.info("  {}. {} - ({}, {})", 
                            i+1, result.getPlaceName(), result.getLatitude(), result.getLongitude());
                }
                
                // 가장 적절한 결과 선택 (거리 기준 + 키워드 매칭)
                KakaoPlaceDto selectedPlace = selectBestPlace(keywordResults, address);
                log.info("선택된 장소: {} - ({}, {})", 
                        selectedPlace.getPlaceName(), selectedPlace.getLatitude(), selectedPlace.getLongitude());
                
                return CoordinateDto.builder()
                        .latitude(selectedPlace.getLatitude())
                        .longitude(selectedPlace.getLongitude())
                        .address(selectedPlace.getAddressName())
                        .roadAddress(selectedPlace.getRoadAddressName())
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
        
        // API 키 유효성 검사
        if (!isApiKeyValid()) {
            log.error("❌ 카카오 API 키가 설정되지 않았거나 유효하지 않습니다. 현재 키: [{}]", 
                    kakaoApiKey == null ? "null" : (kakaoApiKey.isEmpty() ? "empty" : "***"));
            throw new BadRequestException("카카오 API 키가 설정되지 않았습니다. 관리자에게 문의하세요.");
        }

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
            log.error("❌ 카카오 API 키가 설정되지 않았습니다!");
            log.error("   현재 설정값: [{}]", kakaoApiKey == null ? "null" : (kakaoApiKey.isEmpty() ? "empty" : "***"));
            log.error("   환경변수 KAKAO_API_KEY를 설정하거나 application-prod.yml에서 직접 설정하세요.");
            log.error("   외부 장소 검색 기능이 제한됩니다.");
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

    /**
     * 검색 결과에서 가장 적절한 장소 선택 (지하철역 우선)
     */
    private KakaoPlaceDto selectBestPlace(List<KakaoPlaceDto> places, String keyword) {
        if (places.isEmpty()) {
            return null;
        }
        
        // 지하철역 키워드가 포함된 경우 지하철역 우선 검색
        if (keyword.contains("역")) {
            String stationKeyword = keyword.replace("역", "").trim();
            
            // 1순위: 지하철역 관련 키워드가 포함된 정확한 매칭
            for (KakaoPlaceDto place : places) {
                String placeName = place.getPlaceName().toLowerCase();
                String categoryName = place.getCategoryName() != null ? place.getCategoryName().toLowerCase() : "";
                
                if (containsSubwayKeywords(placeName, categoryName) && 
                    containsStationKeyword(placeName, stationKeyword)) {
                    log.info("지하철역 키워드 매칭으로 선택: {}", place.getPlaceName());
                    return place;
                }
            }
        }
        
        // 2순위: 완전한 키워드 매칭 (스마트 필터링 적용)
        List<KakaoPlaceDto> completeMatches = filterByCompleteKeywordMatch(places, keyword);
        if (!completeMatches.isEmpty()) {
            log.info("완전 키워드 매칭으로 선택: {}", completeMatches.get(0).getPlaceName());
            return completeMatches.get(0);
        }
        
        // 3순위: 부분 키워드 매칭
        for (KakaoPlaceDto place : places) {
            if (place.getPlaceName().toLowerCase().contains(keyword.toLowerCase())) {
                log.info("부분 키워드 매칭으로 선택: {}", place.getPlaceName());
                return place;
            }
        }
        
        // 4순위: 첫 번째 검색 결과 (정확도 순으로 정렬됨)
        KakaoPlaceDto bestPlace = places.get(0);
        log.info("정확도 기준으로 선택: {}", bestPlace.getPlaceName());
        return bestPlace;
    }

    /**
     * 지하철역 전용 검색
     */
    private CoordinateDto searchSubwayStation(String stationName) {
        log.info("지하철역 전용 검색 시작: {}", stationName);
        
        try {
            String searchKeyword = stationName.replace("역", "").trim();
            
            // 1. 다중 검색 키워드로 시도
            String[] searchQueries = {
                searchKeyword + "역",           // 강남역
                searchKeyword + " 지하철역",     // 강남 지하철역  
                searchKeyword + " 전철역",      // 강남 전철역
                "지하철 " + searchKeyword + "역", // 지하철 강남역
                "서울지하철 " + searchKeyword + "역" // 서울지하철 강남역
            };
            
            for (String query : searchQueries) {
                log.info("지하철역 스마트 검색 시도: {}", query);
                
                List<KakaoPlaceDto> keywordResults = searchWithSmartStrategy(query);
                
                if (!keywordResults.isEmpty()) {
                    log.info("'{}' 스마트 검색 결과 {}개:", query, keywordResults.size());
                    
                    // 지하철역 관련 키워드가 포함된 결과 우선 필터링
                    for (KakaoPlaceDto place : keywordResults) {
                        String placeName = place.getPlaceName().toLowerCase();
                        String placeCategory = place.getCategoryName() != null ? place.getCategoryName().toLowerCase() : "";
                        
                        // 지하철역 관련 키워드 체크
                        if (containsSubwayKeywords(placeName, placeCategory) && 
                            containsStationKeyword(placeName, searchKeyword)) {
                            
                            log.info("지하철역 매칭 성공: {} -> {} (카테고리: {})", 
                                    stationName, place.getPlaceName(), place.getCategoryName());
                            
                            return CoordinateDto.builder()
                                    .latitude(place.getLatitude())
                                    .longitude(place.getLongitude())
                                    .address(place.getAddressName())
                                    .roadAddress(place.getRoadAddressName())
                                    .build();
                        }
                    }
                    
                    // 지하철역 키워드 매칭이 실패하면 첫 번째 결과라도 로깅
                    log.info("지하철역 키워드 매칭 실패, 상위 3개 결과:");
                    for (int i = 0; i < Math.min(3, keywordResults.size()); i++) {
                        KakaoPlaceDto result = keywordResults.get(i);
                        log.info("  {}. {} - {} (카테고리: {})", 
                                i+1, result.getPlaceName(), result.getAddressName(), result.getCategoryName());
                    }
                }
            }
            
            // 2. 모든 검색이 실패한 경우, 마지막으로 기본 스마트 검색 결과 사용
            log.info("지하철역 특화 검색 실패, 기본 스마트 검색 결과 사용: {}", stationName);
            List<KakaoPlaceDto> fallbackResults = searchWithSmartStrategy(stationName);
            
            if (!fallbackResults.isEmpty()) {
                // 역명이 포함된 첫 번째 결과 찾기
                for (KakaoPlaceDto place : fallbackResults) {
                    if (place.getPlaceName().contains(searchKeyword) || 
                        place.getPlaceName().contains(stationName)) {
                        
                        log.info("Fallback 검색으로 선택: {}", place.getPlaceName());
                        return CoordinateDto.builder()
                                .latitude(place.getLatitude())
                                .longitude(place.getLongitude())
                                .address(place.getAddressName())
                                .roadAddress(place.getRoadAddressName())
                                .build();
                    }
                }
            }
            
            log.warn("지하철역 전용 검색 완전 실패: {}", stationName);
            return null;
            
        } catch (Exception e) {
            log.error("지하철역 검색 중 오류 발생: {} - {}", stationName, e.getMessage());
            return null;
        }
    }
    
    /**
     * 스마트 키워드 검색 전략 (exact → similar)
     */
    private List<KakaoPlaceDto> searchWithSmartStrategy(String keyword) {
        log.info("스마트 검색 전략 시작: {}", keyword);
        
        // 1차 시도: exact 매칭으로 정확한 결과 찾기
        List<KakaoPlaceDto> exactResults = searchPlacesByKeywordWithAnalyzeType(keyword, "exact");
        
        if (!exactResults.isEmpty()) {
            log.info("exact 매칭 성공: {} 결과 {}개", keyword, exactResults.size());
            
            // 정확한 매칭 결과에서 키워드 완전 포함 여부 확인
            List<KakaoPlaceDto> filteredResults = filterByCompleteKeywordMatch(exactResults, keyword);
            if (!filteredResults.isEmpty()) {
                log.info("exact 매칭 + 완전 키워드 포함: {} 결과", filteredResults.size());
                return filteredResults;
            }
            
            // 완전 키워드 포함 결과가 없어도 exact 결과가 있으면 반환
            return exactResults;
        }
        
        // 2차 시도: similar 매칭으로 확장 검색
        log.info("exact 매칭 실패, similar 매칭 시도: {}", keyword);
        List<KakaoPlaceDto> similarResults = searchPlacesByKeywordWithAnalyzeType(keyword, "similar");
        
        if (!similarResults.isEmpty()) {
            log.info("similar 매칭 결과: {} 개", similarResults.size());
            
            // similar 결과에서 키워드 완전 포함 우선 필터링
            List<KakaoPlaceDto> filteredResults = filterByCompleteKeywordMatch(similarResults, keyword);
            if (!filteredResults.isEmpty()) {
                log.info("similar 매칭에서 완전 키워드 포함 결과: {} 개", filteredResults.size());
                return filteredResults;
            }
            
            // 완전 매칭이 없으면 원본 결과 반환
            return similarResults;
        }
        
        log.warn("스마트 검색 전략 완전 실패: {}", keyword);
        return Collections.emptyList();
    }
    
    /**
     * 완전한 키워드 포함 여부로 결과 필터링
     */
    private List<KakaoPlaceDto> filterByCompleteKeywordMatch(List<KakaoPlaceDto> places, String keyword) {
        List<KakaoPlaceDto> filtered = new ArrayList<>();
        String[] keywords = keyword.toLowerCase().split("\\s+");
        
        for (KakaoPlaceDto place : places) {
            String placeName = place.getPlaceName().toLowerCase();
            boolean containsAllKeywords = true;
            
            // 모든 키워드가 장소명에 포함되어 있는지 확인
            for (String kw : keywords) {
                if (!placeName.contains(kw)) {
                    containsAllKeywords = false;
                    break;
                }
            }
            
            if (containsAllKeywords) {
                filtered.add(place);
                log.debug("완전 키워드 매칭: '{}' contains all keywords from '{}'", 
                        placeName, keyword);
            } else {
                log.debug("부분 키워드 매칭 제외: '{}' does not contain all keywords from '{}'", 
                        placeName, keyword);
            }
        }
        
        return filtered;
    }

    /**
     * 지하철역 관련 키워드 포함 여부 확인 (개선된 버전)
     */
    private boolean containsSubwayKeywords(String placeName, String categoryName) {
        String combined = (placeName + " " + categoryName).toLowerCase();
        
        // 지하철역 관련 키워드들
        String[] subwayKeywords = {
            "지하철", "전철", "역", "subway", "station",
            "교통,수송", "지하철,전철", "수도권", "호선"
        };
        
        for (String keyword : subwayKeywords) {
            if (combined.contains(keyword.toLowerCase())) {
                log.debug("지하철 키워드 매칭: '{}' in '{}'", keyword, combined);
                return true;
            }
        }
        
        // 지하철 노선명 패턴 체크 (1호선, 2호선, 9호선 등)
        if (combined.matches(".*\\d+호선.*")) {
            log.debug("지하철 호선 패턴 매칭: '{}'", combined);
            return true;
        }
        
        log.debug("지하철 키워드 매칭 실패: '{}'", combined);
        return false;
    }
    
    /**
     * 역명 키워드 포함 여부 확인 (개선된 버전)
     */
    private boolean containsStationKeyword(String placeName, String stationKeyword) {
        String lowerPlaceName = placeName.toLowerCase();
        String lowerStationKeyword = stationKeyword.toLowerCase();
        
        // 정확한 역명 매칭
        boolean matched = lowerPlaceName.contains(lowerStationKeyword) || 
                         lowerPlaceName.contains(lowerStationKeyword + "역") ||
                         lowerPlaceName.equals(lowerStationKeyword + "역");
        
        if (matched) {
            log.debug("역명 키워드 매칭 성공: '{}' contains '{}'", lowerPlaceName, lowerStationKeyword);
        } else {
            log.debug("역명 키워드 매칭 실패: '{}' does not contain '{}'", lowerPlaceName, lowerStationKeyword);
        }
        
        return matched;
    }

    /**
     * 두 좌표 간의 거리 계산 (km)
     */
    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // 지구 반지름 (km)
        
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        
        return R * c;
    }
}
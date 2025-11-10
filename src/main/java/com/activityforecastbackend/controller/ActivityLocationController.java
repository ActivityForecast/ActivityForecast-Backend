package com.activityforecastbackend.controller;

import com.activityforecastbackend.dto.ApiResponse;
import com.activityforecastbackend.dto.activity.ActivityLocationCreateRequest;
import com.activityforecastbackend.dto.activity.ActivityLocationDto;
import com.activityforecastbackend.dto.activity.LocationSearchRequest;
import com.activityforecastbackend.dto.activity.GeocodeRequest;
import com.activityforecastbackend.dto.activity.CoordinateDto;
import com.activityforecastbackend.service.ActivityLocationService;
import com.activityforecastbackend.service.KakaoLocationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/locations")
@RequiredArgsConstructor
@Tag(name = "Activity Location", description = "활동 장소 관리 API")
public class ActivityLocationController {

    private final ActivityLocationService activityLocationService;
    private final KakaoLocationService kakaoLocationService;

    @Operation(summary = "모든 활동 장소 조회", description = "등록된 모든 활동 장소를 조회합니다.")
    @GetMapping
    public ResponseEntity<ApiResponse<List<ActivityLocationDto>>> getAllLocations() {
        log.info("Request to get all activity locations");
        
        List<ActivityLocationDto> locations = activityLocationService.getAllLocations();
        
        return ResponseEntity.ok(ApiResponse.success(
            "활동 장소 목록을 성공적으로 조회했습니다.",
            locations
        ));
    }

    @Operation(summary = "특정 활동 장소 조회", description = "ID로 특정 활동 장소의 상세 정보를 조회합니다.")
    @GetMapping("/{locationId}")
    public ResponseEntity<ApiResponse<ActivityLocationDto>> getLocation(
            @Parameter(description = "장소 ID", required = true, example = "1")
            @PathVariable Long locationId) {
        log.info("Request to get location: {}", locationId);
        
        ActivityLocationDto location = activityLocationService.getLocation(locationId);
        
        return ResponseEntity.ok(ApiResponse.success(
            "활동 장소 정보를 성공적으로 조회했습니다.",
            location
        ));
    }


    @Operation(summary = "키워드로 장소 검색", description = "장소명이나 주소에 포함된 키워드로 활동 장소를 검색합니다.")
    @GetMapping("/search")
    public ResponseEntity<ApiResponse<List<ActivityLocationDto>>> searchLocationsByKeyword(
            @Parameter(description = "검색 키워드", example = "축구장")
            @RequestParam(required = false) String keyword) {
        log.info("Request to search locations by keyword: {}", keyword);
        
        List<ActivityLocationDto> locations = activityLocationService.searchLocationsByKeyword(keyword);
        
        return ResponseEntity.ok(ApiResponse.success(
            String.format("키워드 '%s' 검색 결과 %d개를 찾았습니다.", 
                         keyword != null ? keyword : "전체", locations.size()),
            locations
        ));
    }

    @Operation(summary = "새 활동 장소 등록", description = "새로운 활동 장소를 등록합니다.")
    @PostMapping
    public ResponseEntity<ApiResponse<ActivityLocationDto>> createLocation(
            @Parameter(description = "장소 생성 정보", required = true)
            @Valid @RequestBody ActivityLocationCreateRequest request) {
        log.info("Request to create new location for activity: {}", request.getActivityId());
        
        ActivityLocationDto createdLocation = activityLocationService.createLocation(request);
        
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(
                    "새 활동 장소가 성공적으로 등록되었습니다.",
                    createdLocation
                ));
    }

    @Operation(summary = "활동 장소 정보 수정", description = "기존 활동 장소의 정보를 수정합니다.")
    @PutMapping("/{locationId}")
    public ResponseEntity<ApiResponse<ActivityLocationDto>> updateLocation(
            @Parameter(description = "장소 ID", required = true, example = "1")
            @PathVariable Long locationId,
            @Parameter(description = "수정할 장소 정보", required = true)
            @Valid @RequestBody ActivityLocationCreateRequest request) {
        log.info("Request to update location: {}", locationId);
        
        ActivityLocationDto updatedLocation = activityLocationService.updateLocation(locationId, request);
        
        return ResponseEntity.ok(ApiResponse.success(
            "활동 장소 정보가 성공적으로 수정되었습니다.",
            updatedLocation
        ));
    }

    @Operation(summary = "활동 장소 삭제", description = "활동 장소를 삭제합니다. (소프트 삭제)")
    @DeleteMapping("/{locationId}")
    public ResponseEntity<ApiResponse<Void>> deleteLocation(
            @Parameter(description = "장소 ID", required = true, example = "1")
            @PathVariable Long locationId) {
        log.info("Request to delete location: {}", locationId);
        
        activityLocationService.deleteLocation(locationId);
        
        return ResponseEntity.ok(ApiResponse.success(
            "활동 장소가 성공적으로 삭제되었습니다.",
            null
        ));
    }



    @Operation(summary = "외부 API 장소 검색", description = "카카오 API만을 사용하여 실시간 장소 검색을 수행합니다.")
    @PostMapping("/search/external")
    public ResponseEntity<ApiResponse<List<ActivityLocationDto>>> searchExternalLocations(
            @Parameter(description = "장소 검색 조건", required = true)
            @Valid @RequestBody LocationSearchRequest searchRequest) {
        log.info("Request to search external locations: {}", searchRequest);
        
        List<ActivityLocationDto> locations = activityLocationService.searchExternalLocations(searchRequest);
        
        return ResponseEntity.ok(ApiResponse.success(
            String.format("외부 API 검색 결과 %d개를 찾았습니다.", locations.size()),
            locations
        ));
    }

    @Operation(summary = "주소 지오코딩", description = "주소를 위도/경도 좌표로 변환합니다.")
    @PostMapping("/geocode")
    public ResponseEntity<ApiResponse<CoordinateDto>> geocodeAddress(
            @Parameter(description = "지오코딩 요청", required = true)
            @Valid @RequestBody GeocodeRequest request) {
        log.info("Request to geocode address: {}", request.getAddress());
        
        CoordinateDto coordinate = kakaoLocationService.geocodeAddress(request.getAddress());
        
        return ResponseEntity.ok(ApiResponse.success(
            "주소가 성공적으로 좌표로 변환되었습니다.",
            coordinate
        ));
    }

    @Operation(summary = "좌표 역지오코딩", description = "위도/경도 좌표를 주소로 변환합니다.")
    @GetMapping("/reverse-geocode")
    public ResponseEntity<ApiResponse<CoordinateDto>> reverseGeocode(
            @Parameter(description = "위도", required = true, example = "37.5196")
            @RequestParam BigDecimal latitude,
            @Parameter(description = "경도", required = true, example = "127.1281")
            @RequestParam BigDecimal longitude) {
        log.info("Request to reverse geocode coordinates: ({}, {})", latitude, longitude);
        
        CoordinateDto coordinate = kakaoLocationService.reverseGeocode(latitude, longitude);
        
        return ResponseEntity.ok(ApiResponse.success(
            "좌표가 성공적으로 주소로 변환되었습니다.",
            coordinate
        ));
    }
}
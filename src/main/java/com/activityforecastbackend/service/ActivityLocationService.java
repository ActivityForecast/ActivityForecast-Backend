package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.activity.ActivityLocationCreateRequest;
import com.activityforecastbackend.dto.activity.ActivityLocationDto;
import com.activityforecastbackend.dto.activity.LocationSearchRequest;
import com.activityforecastbackend.dto.activity.KakaoPlaceDto;
import com.activityforecastbackend.entity.Activity;
import com.activityforecastbackend.entity.ActivityLocation;
import com.activityforecastbackend.exception.BadRequestException;
import com.activityforecastbackend.exception.ResourceNotFoundException;
import com.activityforecastbackend.repository.ActivityLocationRepository;
import com.activityforecastbackend.repository.ActivityRepository;
import com.activityforecastbackend.util.DistanceCalculator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityLocationService {

    private final ActivityLocationRepository activityLocationRepository;
    private final ActivityRepository activityRepository;
    private final KakaoLocationService kakaoLocationService;

    /**
     * 모든 활동 장소 조회
     */
    public List<ActivityLocationDto> getAllLocations() {
        log.info("Fetching all activity locations");
        
        List<ActivityLocation> locations = activityLocationRepository.findByIsDeletedFalse();
        
        return locations.stream()
                .map(ActivityLocationDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 활동 장소 조회
     */
    public ActivityLocationDto getLocation(Long locationId) {
        log.info("Fetching location: {}", locationId);
        
        ActivityLocation location = activityLocationRepository.findByLocationIdAndIsDeletedFalse(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("ActivityLocation", "id", locationId));
        
        return ActivityLocationDto.from(location);
    }

    /**
     * 특정 활동의 모든 장소 조회
     */
    public List<ActivityLocationDto> getLocationsByActivity(Long activityId) {
        log.info("Fetching locations for activity: {}", activityId);
        
        Activity activity = activityRepository.findByActivityIdAndIsDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", "id", activityId));
        
        List<ActivityLocation> locations = activityLocationRepository.findByActivityAndIsDeletedFalse(activity);
        
        return locations.stream()
                .map(ActivityLocationDto::from)
                .collect(Collectors.toList());
    }


    /**
     * 키워드로 장소 검색
     */
    public List<ActivityLocationDto> searchLocationsByKeyword(String keyword) {
        log.info("Searching locations by keyword: {}", keyword);
        
        if (!StringUtils.hasText(keyword)) {
            return getAllLocations();
        }
        
        List<ActivityLocation> locations = activityLocationRepository.findByLocationNameContainingAndIsDeletedFalse(keyword);
        
        return locations.stream()
                .map(ActivityLocationDto::from)
                .collect(Collectors.toList());
    }

    /**
     * 새 활동 장소 생성
     */
    @Transactional
    public ActivityLocationDto createLocation(ActivityLocationCreateRequest request) {
        log.info("Creating new location for activity: {}", request.getActivityId());
        
        // 활동 존재 여부 확인
        Activity activity = activityRepository.findByActivityIdAndIsDeletedFalse(request.getActivityId())
                .orElseThrow(() -> new ResourceNotFoundException("Activity", "id", request.getActivityId()));
        
        // 동일한 좌표의 장소가 이미 존재하는지 확인
        validateDuplicateLocation(request.getLatitude(), request.getLongitude());
        
        // 새 장소 생성
        ActivityLocation location = ActivityLocation.createLocation(
                activity,
                request.getLocationName(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude()
        );
        
        ActivityLocation savedLocation = activityLocationRepository.save(location);
        
        log.info("Created location: {} (ID: {})", savedLocation.getLocationName(), savedLocation.getLocationId());
        
        return ActivityLocationDto.from(savedLocation);
    }

    /**
     * 활동 장소 정보 수정
     */
    @Transactional
    public ActivityLocationDto updateLocation(Long locationId, ActivityLocationCreateRequest request) {
        log.info("Updating location: {}", locationId);
        
        ActivityLocation location = activityLocationRepository.findByLocationIdAndIsDeletedFalse(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("ActivityLocation", "id", locationId));
        
        // 다른 장소와 좌표가 중복되는지 확인 (자기 자신 제외)
        validateDuplicateLocationForUpdate(locationId, request.getLatitude(), request.getLongitude());
        
        // 장소 정보 업데이트
        location.updateLocation(
                request.getLocationName(),
                request.getAddress(),
                request.getLatitude(),
                request.getLongitude()
        );
        
        ActivityLocation updatedLocation = activityLocationRepository.save(location);
        
        log.info("Updated location: {} (ID: {})", updatedLocation.getLocationName(), updatedLocation.getLocationId());
        
        return ActivityLocationDto.from(updatedLocation);
    }

    /**
     * 활동 장소 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteLocation(Long locationId) {
        log.info("Deleting location: {}", locationId);
        
        ActivityLocation location = activityLocationRepository.findByLocationIdAndIsDeletedFalse(locationId)
                .orElseThrow(() -> new ResourceNotFoundException("ActivityLocation", "id", locationId));
        
        // 관련된 일정이 있는지 확인
        if (!location.getSchedules().isEmpty()) {
            throw new BadRequestException("이 장소와 연관된 일정이 있어 삭제할 수 없습니다.");
        }
        
        location.softDelete();
        activityLocationRepository.save(location);
        
        log.info("Deleted location: {} (ID: {})", location.getLocationName(), location.getLocationId());
    }


    /**
     * 중복 위치 검증 (신규 생성 시)
     */
    private void validateDuplicateLocation(BigDecimal latitude, BigDecimal longitude) {
        List<ActivityLocation> allLocations = activityLocationRepository.findByIsDeletedFalse();
        
        for (ActivityLocation location : allLocations) {
            BigDecimal distance = DistanceCalculator.calculateDistance(
                    latitude, longitude,
                    location.getLatitude(), location.getLongitude()
            );
            
            // 100m 이내에 같은 장소가 있으면 중복으로 간주
            if (distance.compareTo(new BigDecimal("0.1")) < 0) {
                throw new BadRequestException("이미 같은 위치에 장소가 등록되어 있습니다: " + location.getLocationName());
            }
        }
    }

    /**
     * 중복 위치 검증 (수정 시)
     */
    private void validateDuplicateLocationForUpdate(Long locationId, BigDecimal latitude, BigDecimal longitude) {
        List<ActivityLocation> allLocations = activityLocationRepository.findByIsDeletedFalse();
        
        for (ActivityLocation location : allLocations) {
            // 자기 자신은 제외
            if (location.getLocationId().equals(locationId)) {
                continue;
            }
            
            BigDecimal distance = DistanceCalculator.calculateDistance(
                    latitude, longitude,
                    location.getLatitude(), location.getLongitude()
            );
            
            // 100m 이내에 같은 장소가 있으면 중복으로 간주
            if (distance.compareTo(new BigDecimal("0.1")) < 0) {
                throw new BadRequestException("이미 같은 위치에 장소가 등록되어 있습니다: " + location.getLocationName());
            }
        }
    }


    /**
     * 카카오 API로 외부 장소 검색
     */
    public List<ActivityLocationDto> searchExternalLocations(LocationSearchRequest searchRequest) {
        try {
            List<KakaoPlaceDto> kakaoPlaces;
            
            // 특정 활동 검색인 경우
            if (searchRequest.getActivityId() != null) {
                Activity activity = activityRepository.findByActivityIdAndIsDeletedFalse(searchRequest.getActivityId())
                        .orElseThrow(() -> new ResourceNotFoundException("Activity", "id", searchRequest.getActivityId()));
                
                kakaoPlaces = kakaoLocationService.searchPlacesByActivity(
                        activity.getActivityName(),
                        searchRequest.getLatitude(),
                        searchRequest.getLongitude(),
                        searchRequest.getRadiusKm().multiply(new BigDecimal("1000")).intValue() // km → m 변환
                );
            }
            // 키워드 검색인 경우
            else if (StringUtils.hasText(searchRequest.getKeyword())) {
                kakaoPlaces = kakaoLocationService.searchPlacesByKeyword(
                        searchRequest.getKeyword(),
                        searchRequest.getLatitude(),
                        searchRequest.getLongitude(),
                        searchRequest.getRadiusKm().multiply(new BigDecimal("1000")).intValue()
                );
            }
            // 일반적인 스포츠 시설 검색
            else {
                kakaoPlaces = kakaoLocationService.searchPlacesByCategory(
                        "SW8", // 스포츠시설 카테고리
                        searchRequest.getLatitude(),
                        searchRequest.getLongitude(),
                        searchRequest.getRadiusKm().multiply(new BigDecimal("1000")).intValue()
                );
            }
            
            // KakaoPlaceDto를 ActivityLocationDto로 변환
            return kakaoPlaces.stream()
                    .map(this::convertKakaoPlaceToActivityLocationDto)
                    .collect(Collectors.toList());
                    
        } catch (Exception e) {
            log.error("Error searching external locations: {}", e.getMessage(), e);
            return Collections.emptyList(); // 외부 API 오류 시 빈 리스트 반환
        }
    }


    /**
     * KakaoPlaceDto를 ActivityLocationDto로 변환
     */
    private ActivityLocationDto convertKakaoPlaceToActivityLocationDto(KakaoPlaceDto kakaoPlace) {
        return ActivityLocationDto.builder()
                .locationId(null) // 외부 API 결과는 ID 없음
                .locationName(kakaoPlace.getPlaceName())
                .address(kakaoPlace.getRoadAddressName() != null ? 
                        kakaoPlace.getRoadAddressName() : kakaoPlace.getAddressName())
                .latitude(kakaoPlace.getLatitude())
                .longitude(kakaoPlace.getLongitude())
                .activityId(null) // 특정 활동과 연결되지 않음
                .activityName("외부 검색") // 구분을 위한 표시
                .categoryName(kakaoPlace.getCategoryGroupName())
                .createdAt(null)
                .build();
    }

}
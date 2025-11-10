package com.activityforecastbackend.dto.activity;

import com.activityforecastbackend.entity.ActivityLocation;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "활동 장소 정보 DTO")
public class ActivityLocationDto {
    
    @Schema(description = "장소 ID", example = "1")
    private Long locationId;
    
    @Schema(description = "장소명", example = "올림픽 공원 축구장")
    private String locationName;
    
    @Schema(description = "주소", example = "서울특별시 송파구 올림픽로 424")
    private String address;
    
    @Schema(description = "위도", example = "37.5196")
    private BigDecimal latitude;
    
    @Schema(description = "경도", example = "127.1281")
    private BigDecimal longitude;
    
    @Schema(description = "활동 ID", example = "25")
    private Long activityId;
    
    @Schema(description = "활동명", example = "축구")
    private String activityName;
    
    @Schema(description = "활동 카테고리명", example = "구기스포츠")
    private String categoryName;
    
    @Schema(description = "등록 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    public static ActivityLocationDto from(ActivityLocation location) {
        return ActivityLocationDto.builder()
                .locationId(location.getLocationId())
                .locationName(location.getLocationName())
                .address(location.getAddress())
                .latitude(location.getLatitude())
                .longitude(location.getLongitude())
                .activityId(location.getActivity().getActivityId())
                .activityName(location.getActivity().getActivityName())
                .categoryName(location.getActivity().getCategory().getCategoryName())
                .createdAt(location.getCreatedAt())
                .build();
    }
}
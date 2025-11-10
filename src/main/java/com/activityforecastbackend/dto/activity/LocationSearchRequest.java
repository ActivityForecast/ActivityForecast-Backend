package com.activityforecastbackend.dto.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "장소 검색 요청 DTO")
public class LocationSearchRequest {
    
    @Schema(description = "검색 중심 위도", example = "37.5665", required = true)
    @NotNull(message = "위도는 필수입니다")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    private BigDecimal latitude;
    
    @Schema(description = "검색 중심 경도", example = "126.9780", required = true)
    @NotNull(message = "경도는 필수입니다")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    private BigDecimal longitude;
    
    @Schema(description = "검색 반경 (km)", example = "10.0", required = true)
    @NotNull(message = "검색 반경은 필수입니다")
    @Positive(message = "검색 반경은 0보다 커야 합니다")
    @DecimalMax(value = "100.0", message = "검색 반경은 100km 이하여야 합니다")
    private BigDecimal radiusKm;
    
    @Schema(description = "활동 ID (특정 활동의 장소만 검색)", example = "25")
    private Long activityId;
    
    @Schema(description = "장소명 키워드", example = "축구장")
    private String keyword;
}
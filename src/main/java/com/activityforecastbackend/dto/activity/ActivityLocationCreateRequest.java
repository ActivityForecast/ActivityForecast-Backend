package com.activityforecastbackend.dto.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "활동 장소 생성 요청 DTO")
public class ActivityLocationCreateRequest {
    
    @Schema(description = "활동 ID", example = "25", required = true)
    @NotNull(message = "활동 ID는 필수입니다")
    @Positive(message = "활동 ID는 양수여야 합니다")
    private Long activityId;
    
    @Schema(description = "장소명", example = "올림픽 공원 축구장", required = true)
    @NotBlank(message = "장소명은 필수입니다")
    @Size(max = 255, message = "장소명은 255자 이하여야 합니다")
    private String locationName;
    
    @Schema(description = "주소", example = "서울특별시 송파구 올림픽로 424")
    @Size(max = 500, message = "주소는 500자 이하여야 합니다")
    private String address;
    
    @Schema(description = "위도", example = "37.5196", required = true)
    @NotNull(message = "위도는 필수입니다")
    @DecimalMin(value = "-90.0", message = "위도는 -90 이상이어야 합니다")
    @DecimalMax(value = "90.0", message = "위도는 90 이하여야 합니다")
    private BigDecimal latitude;
    
    @Schema(description = "경도", example = "127.1281", required = true)
    @NotNull(message = "경도는 필수입니다")
    @DecimalMin(value = "-180.0", message = "경도는 -180 이상이어야 합니다")
    @DecimalMax(value = "180.0", message = "경도는 180 이하여야 합니다")
    private BigDecimal longitude;
}
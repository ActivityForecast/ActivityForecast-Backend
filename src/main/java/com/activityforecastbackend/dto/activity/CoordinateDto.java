package com.activityforecastbackend.dto.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "좌표 정보 DTO")
public class CoordinateDto {
    
    @Schema(description = "위도", example = "37.5196")
    private BigDecimal latitude;
    
    @Schema(description = "경도", example = "127.1281")
    private BigDecimal longitude;
    
    @Schema(description = "주소", example = "서울특별시 송파구 올림픽로 424")
    private String address;
    
    @Schema(description = "도로명 주소", example = "서울특별시 송파구 올림픽로 424")
    private String roadAddress;
}
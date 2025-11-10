package com.activityforecastbackend.dto.activity;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "지오코딩 요청 DTO")
public class GeocodeRequest {
    
    @Schema(description = "변환할 주소", example = "서울특별시 송파구 올림픽로 424", required = true)
    @NotBlank(message = "주소는 필수입니다")
    private String address;
}
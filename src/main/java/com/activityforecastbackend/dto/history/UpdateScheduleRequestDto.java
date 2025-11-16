package com.activityforecastbackend.dto.history;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Schema(description = "일정(Schedule) 상태 및 평점 수정 요청 DTO")
public class UpdateScheduleRequestDto {

    @Schema(description = "참여(완료) 여부", example = "true")
    private Boolean isParticipated;

    @Schema(description = "사용자 평점 (1.0 ~ 5.0)", example = "4.5")
    private BigDecimal rating;

    @Schema(description = "간단한 메모 (선택적)", example = "재미있었음!")
    private String notes;
}
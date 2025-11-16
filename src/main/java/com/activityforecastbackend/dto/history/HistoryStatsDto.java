package com.activityforecastbackend.dto.history;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Data;

import java.util.Map;

@Data
@Builder
@Schema(description = "월별 활동 통계 DTO")
public class HistoryStatsDto {

    @Schema(description = "조회 연도", example = "2025")
    private int year;

    @Schema(description = "조회 월", example = "10")
    private int month;

    @Schema(description = "총 완료 운동 횟수", example = "5")
    private long totalCompletedCount;

    @Schema(description = "활동별 완료 횟수 (차트용)")
    private Map<String, Long> activityCounts; // 예: {"런닝": 2, "축구": 2, "농구": 1}

    @Schema(description = "활동별 완료 비율 (차트용)")
    private Map<String, Double> activityPercentages; // 예: {"런닝": 40.0, "축구": 40.0, "농구": 20.0}
}
package com.activityforecastbackend.dto;

import lombok.Builder;
import lombok.Getter;
import java.util.Map;

@Getter
@Builder
public class ActivityStatisticsDto {

    // 크루에서 활동 통계표시용 DTO
    private long totalActivityCount; // 총 활동 횟수
    private Map<String, ActivityStat> activityDetails; // 활동별 상세 통계 (이름, 횟수, 비율)

    @Getter
    @Builder
    public static class ActivityStat {
        private String activityName;
        private long count; // 활동 횟수
        private double percentage; // 비율 (%)
    }
}
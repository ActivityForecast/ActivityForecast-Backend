package com.activityforecastbackend.dto.history;

import com.activityforecastbackend.dto.activity.ActivityDto;
import com.activityforecastbackend.entity.Schedule;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;

@Data
@Builder
public class ScheduleDto {

    private Long scheduleId;
    private ActivityDto activity; // 활동 상세 정보
    private String locationAddress;
    private LocalDate scheduleDate;
    private LocalTime scheduleTime;

    private Boolean isParticipated; // 완료 여부
    private BigDecimal rating; // 평점
    private String notes;

    private LocalDateTime createdAt;

    //schedule에서 dto변환
    public static ScheduleDto from(Schedule schedule) {
        return ScheduleDto.builder()
                .scheduleId(schedule.getScheduleId())
                .activity(ActivityDto.from(schedule.getActivity())) // 기존 ActivityDto 활용
                .locationAddress(schedule.getLocationAddress())
                .scheduleDate(schedule.getScheduleDate())
                .scheduleTime(schedule.getScheduleTime())
                .isParticipated(schedule.getIsParticipated())
                .rating(schedule.getRating())
                .notes(schedule.getNotes())
                .createdAt(schedule.getCreatedAt())
                .build();
    }
}
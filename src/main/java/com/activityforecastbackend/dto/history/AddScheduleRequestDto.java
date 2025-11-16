package com.activityforecastbackend.dto.history;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Schema(description = "캘린더에 일정(Schedule) 추가 요청 DTO")
public class AddScheduleRequestDto {

    @Schema(description = "활동 ID", example = "1")
    private Long activityId;

    @Schema(description = "장소명", example = "시청역 1호선")
    private String locationAddress; // Schedule 엔티티의 locationAddress 필드와 매칭

    @Schema(description = "예정된 날짜", example = "2025-11-16")
    @JsonFormat(pattern = "yyyy-MM-dd", shape = JsonFormat.Shape.STRING)
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate scheduleDate;

    @Schema(description = "예정된 시간", example = "18:00:00")
    @JsonFormat(pattern = "HH:mm:ss", shape = JsonFormat.Shape.STRING)
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime scheduleTime;

    @Schema(description = "(선택적) 이 일정을 추천한 추천 ID", example = "123")
    private Long recommendationId;
}
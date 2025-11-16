package com.activityforecastbackend.controller;

import com.activityforecastbackend.dto.history.AddScheduleRequestDto;
import com.activityforecastbackend.dto.history.HistoryStatsDto;
import com.activityforecastbackend.dto.history.ScheduleDto;
import com.activityforecastbackend.dto.history.UpdateScheduleRequestDto;
import com.activityforecastbackend.security.UserPrincipal;
import com.activityforecastbackend.service.HistoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "히스토리 API", description = "사용자 활동 내역(캘린더) 및 통계 관리 API")
@Slf4j
@RestController
@RequestMapping("/api/history")
@RequiredArgsConstructor
public class HistoryController {

    private final HistoryService historyService;

    @Operation(summary = "캘린더에 활동 일정 추가", description = "추천받거나 선택한 활동을 사용자 일정(Schedule)으로 추가")
    @PostMapping
    public ResponseEntity<ScheduleDto> addScheduleToHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody AddScheduleRequestDto requestDto) {

        log.info("일정 추가 요청: userId={}, activityId={}", currentUser.getId(), requestDto.getActivityId());
        ScheduleDto newSchedule = historyService.addSchedule(currentUser.getId(), requestDto);
        return ResponseEntity.status(HttpStatus.CREATED).body(newSchedule);
    }

    @Operation(summary = "월별 활동 내역 조회", description = "특정 연/월의 활동 내역을 조회(타임라인)")
    @GetMapping
    public ResponseEntity<List<ScheduleDto>> getHistoryByMonth(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "조회 연도", example = "2025") @RequestParam int year,
            @Parameter(description = "조회 월", example = "10") @RequestParam int month) {

        log.info("월별 히스토리 조회 요청: userId={}, year={}, month={}", currentUser.getId(), year, month);
        List<ScheduleDto> history = historyService.getSchedulesByMonth(currentUser.getId(), year, month);
        return ResponseEntity.ok(history);
    }

    @Operation(summary = "월별 활동 통계 조회", description = "특정 연/월의 활동 통계를 조회(차트)")
    @GetMapping("/stats")
    public ResponseEntity<HistoryStatsDto> getHistoryStatsByMonth(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "조회 연도", example = "2025") @RequestParam int year,
            @Parameter(description = "조회 월", example = "10") @RequestParam int month) {

        log.info("월별 히스토리 통계 요청: userId={}, year={}, month={}", currentUser.getId(), year, month);
        HistoryStatsDto stats = historyService.getScheduleStatsByMonth(currentUser.getId(), year, month);
        return ResponseEntity.ok(stats);
    }

    @Operation(summary = "활동 평점기능 (완료/평가)", description = "특정 일정의 상태(참여/불참)와 평점을 업데이트")
    @PatchMapping("/{scheduleId}")
    public ResponseEntity<ScheduleDto> updateScheduleHistory(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Parameter(description = "일정 ID") @PathVariable Long scheduleId,
            @RequestBody UpdateScheduleRequestDto requestDto) {

        log.info("활동 내역 수정 요청: userId={}, scheduleId={}, participated={}, rating={}",
                currentUser.getId(), scheduleId, requestDto.getIsParticipated(), requestDto.getRating());

        ScheduleDto updatedSchedule = historyService.updateSchedule(currentUser.getId(), scheduleId, requestDto);
        return ResponseEntity.ok(updatedSchedule);
    }
}
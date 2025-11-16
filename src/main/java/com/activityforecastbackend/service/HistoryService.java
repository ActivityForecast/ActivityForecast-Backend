package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.history.AddScheduleRequestDto;
import com.activityforecastbackend.dto.history.HistoryStatsDto;
import com.activityforecastbackend.dto.history.ScheduleDto;
import com.activityforecastbackend.dto.history.UpdateScheduleRequestDto;
import com.activityforecastbackend.entity.*;
import com.activityforecastbackend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class HistoryService {

    private final ScheduleRepository scheduleRepository;
    private final AiTrainingDataRepository aiTrainingDataRepository; // AI 피드백 저장용
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final RecommendationRepository recommendationRepository;

    //활동을 캘린더(Schedule)에 추가
    public ScheduleDto addSchedule(Long userId, AddScheduleRequestDto dto) {
        User user = findUser(userId);
        Activity activity = activityRepository.findById(dto.getActivityId())
                .orElseThrow(() -> new IllegalArgumentException("활동을 찾을 수 없습니다: " + dto.getActivityId()));

        // Schedule 엔티티의 static factory method 사용
        Schedule schedule = Schedule.createPersonalSchedule(
                user,
                activity,
                dto.getScheduleDate(),
                dto.getScheduleTime()
        );

        // 커스텀 위치 정보 설정
        schedule.setCustomLocation(null, null, dto.getLocationAddress());


        Schedule savedSchedule = scheduleRepository.save(schedule);
        log.info("캘린더 일정 추가 완료: scheduleId={}", savedSchedule.getScheduleId());

        return ScheduleDto.from(savedSchedule);
    }

    // 월별 히스토리 조회 (타임라인 기능)
    @Transactional(readOnly = true)
    public List<ScheduleDto> getSchedulesByMonth(Long userId, int year, int month) {
        User user = findUser(userId);
        LocalDate start = YearMonth.of(year, month).atDay(1);
        LocalDate end = YearMonth.of(year, month).atEndOfMonth();

        // ScheduleRepository에 추가한 쿼리 사용
        List<Schedule> schedules = scheduleRepository
                .findSchedulesForHistoryTimeline(user, start, end);

        return schedules.stream()
                .map(ScheduleDto::from)
                .collect(Collectors.toList());
    }

    // 월별 히스토리 통계
    @Transactional(readOnly = true)
    public HistoryStatsDto getScheduleStatsByMonth(Long userId, int year, int month) {
        User user = findUser(userId);
        LocalDate start = YearMonth.of(year, month).atDay(1);
        LocalDate end = YearMonth.of(year, month).atEndOfMonth();

        // 1. 총 완료 횟수 (isParticipated = true)
        // ScheduleRepository에 추가한 쿼리 사용
        long totalCount = scheduleRepository.countByUserAndScheduleDateBetweenAndIsParticipatedTrueAndIsDeletedFalse(
                user, start, end
        );

        // 2. 활동별 완료 횟수
        // ScheduleRepository에 추가한 쿼리 사용
        List<Object[]> counts = scheduleRepository.countCompletedActivitiesByNameForUserAndPeriod(
                user, start, end
        );

        Map<String, Long> activityCounts = counts.stream()
                .collect(Collectors.toMap(
                        key -> (String) key[0],
                        value -> (Long) value[1]
                ));

        // 3. 활동별 비율
        Map<String, Double> activityPercentages = activityCounts.entrySet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        entry -> (totalCount == 0) ? 0.0 :
                                BigDecimal.valueOf(entry.getValue() * 100.0 / totalCount)
                                        .setScale(1, RoundingMode.HALF_UP).doubleValue()
                ));

        return HistoryStatsDto.builder()
                .year(year)
                .month(month)
                .totalCompletedCount(totalCount)
                .activityCounts(activityCounts)
                .activityPercentages(activityPercentages)
                .build();
    }


    // 활동 일정 수정 (완료/평가 또는 취소)
    public ScheduleDto updateSchedule(Long userId, Long scheduleId, UpdateScheduleRequestDto dto) {
        User user = findUser(userId);
        // findByScheduleIdAndIsDeletedFalse 쿼리 사용 (기존에 있던 쿼리)
        Schedule schedule = scheduleRepository.findByScheduleIdAndIsDeletedFalse(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("일정을 찾을 수 없습니다: " + scheduleId));

        // 사용자 본인의 내역이 맞는지 확인
        if (!schedule.getUser().getUserId().equals(userId)) {
            throw new SecurityException("자신의 일정만 수정할 수 있습니다.");
        }

        if (dto.getIsParticipated() == null) {
            throw new IllegalArgumentException("참여 여부(isParticipated)는 필수입니다.");
        }

        if (Boolean.TRUE.equals(dto.getIsParticipated()) && dto.getRating() == null) {
            throw new IllegalArgumentException("완료(참여) 시 평점(rating)은 필수입니다.");
        }

        // Schedule 엔티티의 메서드를 사용하여 상태 업데이트
        schedule.setParticipationResult(
                dto.getIsParticipated(),
                dto.getIsParticipated() ? dto.getRating() : null, // 참여한 경우에만 평점 저장
                dto.getNotes()
        );

        Schedule updatedSchedule = scheduleRepository.save(schedule);

        // AI 피드백을 위한 학습 데이터 생성
        if (Boolean.TRUE.equals(updatedSchedule.getIsParticipated())) {
            try {
                // AiTrainingData 엔티티의 static factory method 사용
                AiTrainingData trainingData = AiTrainingData.createFromSchedule(updatedSchedule);
                aiTrainingDataRepository.save(trainingData);
                log.info("AI 학습 데이터(피드백) 저장 완료: scheduleId={}", scheduleId);
            } catch (Exception e) {
                // 피드백 저장이 실패해도 메인 로직(일정 수정)은 성공해야 함
                log.error("AI 학습 데이터 저장 실패. scheduleId: " + scheduleId, e);
            }
        }

        return ScheduleDto.from(updatedSchedule);
    }

    private User findUser(Long userId) {
        // UserRepository 사용
        return userRepository.findByUserIdAndIsDeletedFalse(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다: " + userId));
    }
}
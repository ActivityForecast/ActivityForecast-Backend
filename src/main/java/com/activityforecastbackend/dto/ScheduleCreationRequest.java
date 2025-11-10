package com.activityforecastbackend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;
import java.math.BigDecimal;

@Getter
@Setter
public class ScheduleCreationRequest {
    private Long activityId; // Activity 엔티티 ID
    private LocalDate date;    // ScheduleDate
    private LocalTime time;    // ScheduleTime
    private String equipmentList;


    // 장소 필드
    private Long locationId; // 기존 ActivityLocation 엔티티 ID
    private String locationAddress; // 사용자 지정 주소
    private BigDecimal locationLatitude; // 사용자 지정 위도
    private BigDecimal locationLongitude; // 사용자 지정 경도
}
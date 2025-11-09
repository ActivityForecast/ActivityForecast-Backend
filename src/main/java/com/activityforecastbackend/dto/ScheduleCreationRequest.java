package com.activityforecastbackend.dto;

import lombok.Getter;
import lombok.Setter;
import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@Setter
public class ScheduleCreationRequest {
    private Long activityId; // Activity 엔티티 ID
    private LocalDate date;    // ScheduleDate
    private LocalTime time;    // ScheduleTime
    private String equipmentList;
}
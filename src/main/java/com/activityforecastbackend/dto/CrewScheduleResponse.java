package com.activityforecastbackend.dto;

import com.activityforecastbackend.entity.CrewSchedule;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Builder
public class CrewScheduleResponse {
    private Long crewScheduleId;
    private Long crewId; // Crew 객체 대신 ID만
    private Long scheduleId; // Schedule 객체 대신 ID만

    // 일정의 핵심 정보 (날짜, 시간 등)는 Schedule 엔티티에서 가져와 직접 포함
    private String equipmentList;
    private LocalDateTime scheduleDate; // Schedule 엔티티의 날짜/시간 정보

    // 참가자 수 (통계용)
    private int totalParticipantCount;
    private int confirmedParticipantCount;

    public static CrewScheduleResponse from(CrewSchedule cs) {
        return CrewScheduleResponse.builder()
                .crewScheduleId(cs.getCrewScheduleId())
                .crewId(cs.getCrew().getCrewId())
                .scheduleId(cs.getSchedule().getScheduleId())
                .equipmentList(cs.getEquipmentList())
                // Schedule 엔티티의 날짜를 가져와 사용 (순환 참조 차단)
                .scheduleDate(cs.getSchedule().getScheduleDate().atTime(cs.getSchedule().getScheduleTime()))
                .totalParticipantCount(cs.getTotalParticipantCount())
                .confirmedParticipantCount(cs.getConfirmedParticipantCount())
                .build();
    }
}

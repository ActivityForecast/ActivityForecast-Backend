package com.activityforecastbackend.dto.crew;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrewCreationRequest {
    private String crewName; // 크루명
    private String description;
    private String colorCode;
    // 인원 제한을 받는 필드
    private Integer maxCapacity;
}
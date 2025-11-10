package com.activityforecastbackend.dto;

import com.activityforecastbackend.entity.Crew;

import com.activityforecastbackend.entity.CrewMember;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Setter
@Builder
public class CrewResponse {
    private Long crewId;
    private String crewName;
    private String description;
    private String colorCode;
    private String inviteCode; // 공유 링크용
    private UserResponse createdBy; // UserResponse로 대체 (리더 정보)
    private LocalDateTime createdAt;
    private Boolean isDeleted;

    private Integer maxCapacity; // 최대 인원 제한 필드

    private List<CrewMemberResponse> members;
    private int activeMemberCount;

    public static CrewResponse from(Crew crew) {
        List<CrewMemberResponse> memberResponses = crew.getMembers().stream()
                .filter(CrewMember::isActiveMember)
                .map(CrewMemberResponse::from)
                .collect(Collectors.toList());

        return CrewResponse.builder()
                .crewId(crew.getCrewId())
                .crewName(crew.getCrewName())
                .description(crew.getDescription())
                .colorCode(crew.getColorCode())
                .inviteCode(crew.getInviteCode())
                .createdBy(UserResponse.from(crew.getCreatedBy()))
                .createdAt(crew.getCreatedAt())
                .isDeleted(crew.getIsDeleted())
                .maxCapacity(crew.getMaxCapacity()) // 엔티티의 값을 DTO에 복사
                .members(memberResponses)
                .activeMemberCount(memberResponses.size())
                .build();
    }
}

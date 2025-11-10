package com.activityforecastbackend.dto;

import com.activityforecastbackend.entity.CrewMember;
import com.activityforecastbackend.entity.CrewMember.CrewRole;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class CrewMemberResponse {
    private UserResponse user;
    private CrewRole role;

    @JsonProperty("isLeader") //JSON key값 isLeader로 유지용
    private boolean isLeader;

    // Jackson이 자동으로 인식하는 Legacy Getter 메서드를 JSON 변환에서 제외
    @JsonIgnore
    public boolean isLeader() {
        return this.isLeader;
    }

    public static CrewMemberResponse from(CrewMember member) {
        return CrewMemberResponse.builder()
                .user(UserResponse.from(member.getUser()))
                .role(member.getRole())
                .isLeader(member.isLeader())
                .build();
    }
}
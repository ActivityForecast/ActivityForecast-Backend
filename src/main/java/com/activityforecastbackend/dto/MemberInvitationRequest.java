package com.activityforecastbackend.dto;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MemberInvitationRequest {
    private Long invitedUserId; // 초대할 사용자의 ID참고
}
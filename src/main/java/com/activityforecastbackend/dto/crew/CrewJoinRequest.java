package com.activityforecastbackend.dto.crew;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class CrewJoinRequest {

    //링크를 통한 참여 구현용
    private String inviteCode; // 사용자가 링크를 통해 전달받은 초대 코드
}
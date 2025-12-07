package com.activityforecastbackend.controller;

import com.activityforecastbackend.dto.crew.*;
import com.activityforecastbackend.entity.CrewMember;
import com.activityforecastbackend.entity.CrewSchedule;
import com.activityforecastbackend.service.CrewService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.activityforecastbackend.security.UserPrincipal;
import org.springframework.security.core.annotation.AuthenticationPrincipal; // JWT로

import java.util.List;
import java.util.NoSuchElementException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/crews")
@RequiredArgsConstructor
public class CrewController {
    private final CrewService crewService;

    // 예외 처리 핸들러 (모든 API 요청에 공통 적용)
    @ExceptionHandler({CrewService.UnauthorizedException.class, NoSuchElementException.class, IllegalStateException.class})
    public ResponseEntity<String> handleException(RuntimeException ex) {
        // 403 Forbidden, 404 Not Found, 400 Bad Request
        HttpStatus status = ex instanceof CrewService.UnauthorizedException ? HttpStatus.FORBIDDEN :
                ex instanceof NoSuchElementException ? HttpStatus.NOT_FOUND :
                        HttpStatus.BAD_REQUEST;
        return new ResponseEntity<>(ex.getMessage(), status);
    }

    // 1. 크루 생성 (POST /api/crews)
    @PostMapping
    public ResponseEntity<CrewResponse> createCrew( // CrewResponse로 변경
                                                    @RequestBody CrewCreationRequest request,
                                                    @AuthenticationPrincipal UserPrincipal currentUser) {
        Long currentUserId = currentUser.getId();
        CrewResponse newCrew = crewService.createCrew(request, currentUserId);
        return new ResponseEntity<>(newCrew, HttpStatus.CREATED);
    }

    // 2. 크루 일정 생성 (POST /api/crews/{crewId}/schedules) - 리더 권한 필요
    @PostMapping("/{crewId}/schedules")
    public ResponseEntity<CrewScheduleResponse> createSchedule(
            // 반환 타입 변경
            @PathVariable Long crewId,
            @RequestBody ScheduleCreationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long currentUserId = currentUser.getId();
        CrewSchedule newSchedule = crewService.createCrewSchedule(crewId, request, currentUserId);
        // DTO로 변환하여 반환
        return new ResponseEntity<>(CrewScheduleResponse.from(newSchedule), HttpStatus.CREATED);
    }

    // 3. 크루 일정 삭제 (DELETE /api/crews/{crewId}/schedules/{crewScheduleId}) - 리더 권한 필요
    @DeleteMapping("/{crewId}/schedules/{crewScheduleId}")
    public ResponseEntity<Void> deleteSchedule(
            @PathVariable Long crewId,
            @PathVariable Long crewScheduleId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long currentUserId = currentUser.getId();
        crewService.deleteCrewSchedule(crewId, crewScheduleId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // 4. 멤버 초대 (POST /api/crews/{crewId}/members/invite) - 리더 권한 필요
    @PostMapping("/{crewId}/members/invite")
    public ResponseEntity<CrewMemberResponse> inviteMember(
            @PathVariable Long crewId,
            @RequestBody MemberInvitationRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long currentUserId = currentUser.getId();
        CrewMember newMember = crewService.inviteMember(crewId, request, currentUserId);
        return new ResponseEntity<>(CrewMemberResponse.from(newMember), HttpStatus.CREATED);
    }

    // 5. 월별 일정 조회
    @GetMapping("/{crewId}/schedules")
    public ResponseEntity<List<CrewScheduleResponse>> getMonthlySchedules(
            // 반환 타입을 DTO 리스트로 변경
            @PathVariable Long crewId,
            @RequestParam int year,
            @RequestParam int month) {

        List<CrewSchedule> schedules = crewService.getMonthlySchedules(crewId, year, month);

        // 엔티티 리스트를 DTO 리스트로 변환하여 반환
        List<CrewScheduleResponse> responseList = schedules.stream()
                .map(CrewScheduleResponse::from)
                .collect(Collectors.toList());

        return ResponseEntity.ok(responseList);
    }

    // 6. 크루 상세 정보 조회 (GET /api/crews/{crewId})
    @GetMapping("/{crewId}")
    public ResponseEntity<CrewResponse> getCrewDetails(@PathVariable Long crewId) {
        CrewResponse crew = crewService.getCrewDetails(crewId);
        return ResponseEntity.ok(crew);
    }

    // 7. GET /api/crews/{crewId}/statistics (크루 활동 통계 조회 API)
    @GetMapping("/{crewId}/statistics")
    public ResponseEntity<ActivityStatisticsResponse> getCrewStatistics(@PathVariable Long crewId) {
        ActivityStatisticsResponse stats = crewService.getCrewActivityStatistics(crewId);
        return ResponseEntity.ok(stats);
    }

    // 8. GET /api/crews (사용자별 크루 목록 조회)
    @GetMapping
    public ResponseEntity<List<CrewResponse>> getAllCrews(
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long currentUserId = currentUser.getId();
        List<CrewResponse> crews = crewService.getCrewsByUserId(currentUserId);
        return ResponseEntity.ok(crews);
    }

    // 9. DELETE /api/crews/{crewId}/members/{targetUserId} (멤버 탈퇴/제명)
    @DeleteMapping("/{crewId}/members/{targetUserId}")
    public ResponseEntity<Void> removeMember(
            @PathVariable Long crewId,
            @PathVariable Long targetUserId,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long currentUserId = currentUser.getId();
        crewService.leaveOrRemoveMember(crewId, targetUserId, currentUserId);
        return ResponseEntity.noContent().build();
    }

    // 10. POST /api/crews/join (공유 링크를 통한 크루 가입)
    @PostMapping("/join")
    public ResponseEntity<CrewMemberResponse> joinCrewByLink(
            @RequestBody CrewJoinRequest request,
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long currentUserId = currentUser.getId();
        CrewMember newMembership = crewService.joinCrewByInviteCode(request.getInviteCode(), currentUserId);
        //DTO로 변경
        return new ResponseEntity<>(CrewMemberResponse.from(newMembership), HttpStatus.CREATED);
    }

    // 11. GET /api/crews/schedules (사용자 전체 크루 월별 일정 조회 API)
    // 기존 /{crewId}/schedules와 구별하기 위해 PathVariable 없이 정의
    @GetMapping("/schedules")
    public ResponseEntity<List<CrewScheduleResponse>> getCombinedMonthlySchedules(

            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam int year,
            @RequestParam int month) {

        Long currentUserId = currentUser.getId();
        List<CrewScheduleResponse> schedules = crewService.getCombinedMonthlySchedulesForUser(currentUserId, year, month);

        return ResponseEntity.ok(schedules);
    }

    // 12. 크루 일정 수정 (PUT /api/crews/{crewId}/schedules/{crewScheduleId}) - 리더 권한 필요
    @PutMapping("/{crewId}/schedules/{crewScheduleId}")
    public ResponseEntity<CrewScheduleResponse> updateSchedule(
            @PathVariable Long crewId,
            @PathVariable Long crewScheduleId,
            @RequestBody ScheduleCreationRequest request, // 생성 DTO 재사용
            @AuthenticationPrincipal UserPrincipal currentUser) {

        Long currentUserId = currentUser.getId();
        // 서비스의 updateCrewSchedule 호출
        CrewScheduleResponse updatedSchedule = crewService.updateCrewSchedule(crewId, crewScheduleId, request, currentUserId);
        return ResponseEntity.ok(updatedSchedule); // 200 OK
    }
}
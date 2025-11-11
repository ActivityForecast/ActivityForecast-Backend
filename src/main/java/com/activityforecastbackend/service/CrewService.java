package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.*;
import com.activityforecastbackend.entity.*;
import com.activityforecastbackend.entity.CrewMember.CrewRole;
import com.activityforecastbackend.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import jakarta.persistence.EntityManager; // EntityManager import
import java.lang.Number;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.stream.Collectors; // Collectors import 추가


@Service
@RequiredArgsConstructor
public class CrewService {
    private final CrewRepository crewRepository;
    private final ScheduleRepository scheduleRepository;
    private final CrewScheduleRepository crewScheduleRepository;
    private final CrewMemberRepository crewMemberRepository;
    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final CrewScheduleParticipantRepository participantRepository;
    private final ActivityLocationRepository activityLocationRepository;

    private final EntityManager em; // EntityManager 주입

    // 사용자 정의 예외
    public static class UnauthorizedException extends RuntimeException {
        public UnauthorizedException(String message) {
            super(message);
        }
    }

    // 최대 허용 인원 상수를 정의 (5명 초과 금지)
    private static final int GLOBAL_MAX_CAPACITY = 50;
    // 기본값 5명 유지
    private static final int DEFAULT_MAX_CAPACITY = 5;

    // 헬퍼 메서드: 리더 권한 확인 (CrewMember.isLeader() 사용)
    private Crew checkLeaderAuthority(Long crewId, Long currentUserId) {
        // 1. User와 Crew 엔티티 조회
        User currentUser = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NoSuchElementException("현재 사용자를 찾을 수 없습니다."));
        Crew crew = crewRepository.findByCrewIdAndIsDeletedFalse(crewId)
                .orElseThrow(() -> new NoSuchElementException("크루를 찾을 수 없습니다. ID: " + crewId));

        // 2. 해당 크루의 활성 멤버 중 리더인지 확인 (CrewMemberRepository의 메서드 활용)
        CrewMember member = crewMemberRepository.findByCrewAndUserAndIsActiveTrue(crew, currentUser)
                .orElseThrow(() -> new UnauthorizedException("크루의 활성 멤버가 아닙니다."));

        if (!member.isLeader()) {
            throw new UnauthorizedException("크루 리더만 이 작업을 수행할 수 있습니다.");
        }
        return crew;
    }

    // --- 1. 크루 생성 (리더 지정 및 멤버로 자동 추가) ---
    @Transactional
    public CrewResponse createCrew(CrewCreationRequest request, Long currentUserId) {

        // 1. 유효성 검사 추가: 입력된 인원이 GLOBAL_MAX_CAPACITY(5)를 초과하는지 확인
        if (request.getMaxCapacity() != null && request.getMaxCapacity() > GLOBAL_MAX_CAPACITY) {
            throw new IllegalArgumentException("크루의 최대 인원은 " + GLOBAL_MAX_CAPACITY + "명을 초과할 수 없습니다. (입력값: " + request.getMaxCapacity() + ")");
        }

        // null이거나 1 미만일 경우 기본값 5로 설정
        Integer finalCapacity = (request.getMaxCapacity() == null || request.getMaxCapacity() < 1)
                ? DEFAULT_MAX_CAPACITY : request.getMaxCapacity();

        User creator = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NoSuchElementException("유효하지 않은 사용자 ID입니다."));

        // finalCapacity를 팩토리 메서드에 전달하고, 팩토리 메서드에서 null 체크를 한 번 더
        Crew newCrew = Crew.createCrew(
                request.getCrewName(),
                request.getDescription(),
                request.getColorCode(),
                creator,
                finalCapacity); // 최종 검증된 Capacity 전달

        Crew savedCrew = crewRepository.save(newCrew);

        // 1. CrewMember 저장
        CrewMember leaderMember = CrewMember.createLeader(savedCrew, creator);
        crewMemberRepository.save(leaderMember);

        // (EntityManager clear 및 재조회 로직 유지)
        crewRepository.flush();
        em.clear();

        Crew updatedCrew = crewRepository.findByIdWithMembers(savedCrew.getCrewId())
                .orElseThrow(() -> new NoSuchElementException("생성된 크루를 찾을 수 없습니다."));

        return CrewResponse.from(updatedCrew);
    }

    // --- 2. 크루 일정 생성 (리더만 가능) ---
    @Transactional
    public CrewSchedule createCrewSchedule(Long crewId, ScheduleCreationRequest request, Long currentUserId) {
        // 1. 권한 확인 및 Crew 엔티티 조회
        Crew crew = checkLeaderAuthority(crewId, currentUserId);

        // 2. 필요한 엔티티 조회
        User creator = userRepository.findById(currentUserId).get();
        Activity activity = activityRepository.findById(request.getActivityId())
                .orElseThrow(() -> new NoSuchElementException("활동을 찾을 수 없습니다. ID: " + request.getActivityId()));

        // 장소 엔티티 조회
        ActivityLocation location = null;
        if (request.getLocationId() != null) {
            location = activityLocationRepository.findById(request.getLocationId())
                    .orElseThrow(() -> new NoSuchElementException("장소를 찾을 수 없습니다. ID: " + request.getLocationId()));
        }

        // 3. Schedule 엔티티 생성
        Schedule newSchedule = Schedule.createCrewSchedule(
                creator,
                activity,
                crew,
                request.getDate(),
                request.getTime());

        // Schedule 엔티티에 위치 정보 설정
        if (location != null) {
            newSchedule.setLocation(location);
        } else if (request.getLocationAddress() != null) {
            // 사용자 지정 위치 정보 설정 (Schedule 엔티티의 setCustomLocation 메서드를 사용한다고 가정)
            newSchedule.setCustomLocation(
                    request.getLocationLatitude(),
                    request.getLocationLongitude(),
                    request.getLocationAddress());
        }

        newSchedule = scheduleRepository.save(newSchedule);

        // 4. CrewSchedule 엔티티 생성
        CrewSchedule newCrewSchedule = CrewSchedule.createCrewSchedule(
                crew,
                newSchedule,
                request.getEquipmentList());
        newCrewSchedule = crewScheduleRepository.save(newCrewSchedule);

        // 5. 리더를 자동으로 참가자로 등록하고 '확정' 처리
        CrewScheduleParticipant leaderParticipant = CrewScheduleParticipant.createParticipant(
                newCrewSchedule,
                creator,
                true // isConfirmed = true
        );
        participantRepository.save(leaderParticipant);

        return newCrewSchedule;
    }

    // --- 3. 크루 일정 삭제 (리더만 가능) ---
    @Transactional
    public void deleteCrewSchedule(Long crewId, Long crewScheduleId, Long currentUserId) {
        // 1. 권한 확인
        checkLeaderAuthority(crewId, currentUserId);

        // 2. CrewSchedule 조회 및 유효성 검사
        CrewSchedule crewSchedule = crewScheduleRepository.findById(crewScheduleId)
                .orElseThrow(() -> new NoSuchElementException("크루 일정을 찾을 수 없습니다."));

        if (!Objects.equals(crewSchedule.getCrew().getCrewId(), crewId)) {
            throw new UnauthorizedException("해당 크루의 일정이 아닙니다.");
        }

        Schedule scheduleToDelete = crewSchedule.getSchedule();

        // 3. 삭제
        scheduleToDelete.softDelete(); // Schedule soft delete
        crewScheduleRepository.delete(crewSchedule);
    }

    // --- 4. 멤버 초대 (ID로 초대하는 기능), 필요없는 기능 ---
    @Transactional
    public CrewMember inviteMember(Long crewId, MemberInvitationRequest request, Long currentUserId) {
        // 1. 권한 확인 및 Crew 엔티티 조회
        Crew crew = checkLeaderAuthority(crewId, currentUserId);

        // 2. 초대할 사용자 및 중복 검사
        User invitedUser = userRepository.findById(request.getInvitedUserId())
                .orElseThrow(() -> new NoSuchElementException("초대할 사용자 ID를 찾을 수 없습니다."));

        if (crewMemberRepository.existsByCrewAndUserAndIsActiveTrue(crew, invitedUser)) {
            throw new IllegalStateException("이미 활성 크루 멤버입니다.");
        }

        // 3. 인원 제한 검사 (하드코딩된 최대 인원 제한 가정)
        // 3. Crew 엔티티의 maxCapacity를 사용하여 인원 제한 검사
        if (crewRepository.countActiveMembersByCrew(crew) >= crew.getMaxCapacity()) {
            throw new IllegalStateException("크루 인원 제한(" + crew.getMaxCapacity() + "명)을 초과하여 초대할 수 없습니다.");
        }

        // 4. 새로운 멤버를 MEMBER 권한으로 추가
        CrewMember newMember = CrewMember.createMember(crew, invitedUser, CrewRole.MEMBER);
        return crewMemberRepository.save(newMember);
    }

    // --- 5. 월별 일정 조회 ---
    @Transactional(readOnly = true)
    public List<CrewSchedule> getMonthlySchedules(Long crewId, int year, int month) {
        Crew crew = crewRepository.findByCrewIdAndIsDeletedFalse(crewId)
                .orElseThrow(() -> new NoSuchElementException("크루를 찾을 수 없습니다. ID: " + crewId));

        LocalDate startDate = LocalDate.of(year, month, 1);
        LocalDate endDate = startDate.with(TemporalAdjusters.lastDayOfMonth());

        LocalDate displayStart = startDate.with(TemporalAdjusters.previousOrSame(DayOfWeek.SUNDAY));
        LocalDate displayEnd = endDate.with(TemporalAdjusters.nextOrSame(DayOfWeek.SATURDAY));

        return crewScheduleRepository.findByCrewAndScheduleDateBetween(crew, displayStart, displayEnd);
    }

    // --- 6. 크루 상세 정보 조회 ---
    @Transactional(readOnly = true)
    public CrewResponse getCrewDetails(Long crewId) {
        Crew crew = crewRepository.findByCrewIdAndIsDeletedFalse(crewId)
                .orElseThrow(() -> new NoSuchElementException("크루 ID를 찾을 수 없습니다: " + crewId));

        // Lazy Loading 방지 및 DTO 변환 준비
        crew.getMembers().forEach(m -> m.getUser().getName());

        return CrewResponse.from(crew);
    }

    // --- 7. 크루 활동 통계 계산 (파이 차트 데이터 생성) ---
    @Transactional(readOnly = true)
    public ActivityStatisticsDto getCrewActivityStatistics(Long crewId) {
        Crew crew = crewRepository.findByCrewIdAndIsDeletedFalse(crewId)
                .orElseThrow(() -> new NoSuchElementException("크루 ID를 찾을 수 없습니다: " + crewId));

        // 쿼리 호출 변경: Crew 객체 대신 ID와 현재 날짜를 전달
        List<Object[]> crewStats = scheduleRepository.findCrewActivityStatistics(
                crew.getCrewId(),
                LocalDate.now() // Java에서 현재 날짜를 가져와 전달
        );

        long totalActivityCount = 0;
        Map<String, Long> combinedStats = new java.util.HashMap<>();

        for (Object[] stat : crewStats) {
            String activityName = stat[0] != null ? (String) stat[0] : "알 수 없는 활동";

            Long count = 0L;

            // CAST를 사용하여 쿼리 결과를 Long으로 받았다고 가정하고 처리
            try {
                if (stat[1] != null) {
                    // 쿼리에서 long으로 CAST했으므로 Long 또는 Number로 안전하게 처리 시도
                    if (stat[1] instanceof Long) {
                        count = (Long) stat[1];
                    } else if (stat[1] instanceof Number) {
                        count = ((Number) stat[1]).longValue();
                    } else {
                        // 예상치 못한 타입일 경우 디버깅 메시지를 통해 확인 가능
                        System.err.println("Warning: Unexpected type for count: " + stat[1].getClass().getName() + " with value " + stat[1]);
                    }
                }
            } catch (Exception e) {
                // 캐스팅 오류 발생 시 로깅하고 0으로 처리
                System.err.println("Error processing activity count for " + activityName + ": " + e.getMessage());
                count = 0L;
            }

            totalActivityCount += count;
            combinedStats.put(activityName, count);
        }

        Map<String, ActivityStatisticsDto.ActivityStat> detailedStats = new java.util.HashMap<>();

        for (Map.Entry<String, Long> entry : combinedStats.entrySet()) {
            String activityName = entry.getKey();
            long count = entry.getValue();

            double percentage = 0.0;
            if (totalActivityCount > 0) {
                percentage = (double) count * 100.0 / totalActivityCount;
            }

            detailedStats.put(activityName, ActivityStatisticsDto.ActivityStat.builder()
                    .activityName(activityName)
                    .count(count)
                    .percentage(percentage)
                    .build());
        }

        return ActivityStatisticsDto.builder()
                .totalActivityCount(totalActivityCount)
                .activityDetails(detailedStats)
                .build();
    }

    // --- 9. 사용자별 크루 목록 조회 ---
    @Transactional(readOnly = true)
    public List<CrewResponse> getCrewsByUserId(Long currentUserId) { // 반환 타입 List<CrewResponse>
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        List<Crew> crews = crewRepository.findCrewsByMember(user);

        // Lazy Loading 방지 및 DTO 변환
        return crews.stream()
                .map(crew -> {
                    crew.getMembers().forEach(m -> m.getUser().getName()); // 멤버 정보 강제 로드
                    return CrewResponse.from(crew);
                })
                .collect(Collectors.toList());
    }

    // --- 10. 멤버 탈퇴/제명 기능 ---
    @Transactional
    public void leaveOrRemoveMember(Long crewId, Long targetUserId, Long currentUserId) {
        // 1. Crew 및 User 엔티티 조회
        Crew crew = crewRepository.findByCrewIdAndIsDeletedFalse(crewId)
                .orElseThrow(() -> new NoSuchElementException("크루를 찾을 수 없습니다."));
        User targetUser = userRepository.findById(targetUserId)
                .orElseThrow(() -> new NoSuchElementException("대상 사용자를 찾을 수 없습니다."));

        // 2. 멤버십 엔티티 조회 (isActive=true인 경우)
        CrewMember membership = crewMemberRepository.findByCrewAndUserAndIsActiveTrue(crew, targetUser)
                .orElseThrow(() -> new NoSuchElementException("크루의 활성 멤버가 아닙니다."));

        // 3. 권한 확인 및 로직 분기
        if (targetUserId.equals(currentUserId)) {
            // A. 자기 자신 탈퇴 (Leave)
            if (membership.isLeader()) {
                // 리더 탈퇴시 크루해제 로직 호출, 프론트에서 모달로 확인을 완료했다는 전제
                disbandCrew(crewId, currentUserId);
                return;
            }
            membership.leaveCrew(); // isActive = false 처리

        } else {
            // B. 타 멤버 제명 (Remove) - 리더 권한 필요, 필요없을수 있음
            CrewMember currentUserMembership = crewMemberRepository.findByCrewAndUserAndIsActiveTrue(crew, userRepository.findById(currentUserId).get())
                    .orElseThrow(() -> new UnauthorizedException("크루 멤버가 아닙니다."));

            if (!currentUserMembership.isLeader()) {
                throw new UnauthorizedException("크루 리더만 멤버를 제명할 수 있습니다.");
            }

            // 제명 대상이 리더인 경우 (리더는 제명할 수 없습니다)
            if (membership.isLeader()) {
                throw new IllegalStateException("리더는 제명할 수 없습니다. 리더 권한을 위임한 후 탈퇴를 시도해야 합니다.");
            }

            membership.leaveCrew(); // isActive = false 처리
        }

        crewMemberRepository.save(membership);
    }

    // --- 11. 초대 코드를 통한 크루 가입 기능 ---
    @Transactional
    public CrewMember joinCrewByInviteCode(String inviteCode, Long currentUserId) {
        // 1. 현재 사용자 조회
        User user = userRepository.findById(currentUserId)
                .orElseThrow(() -> new NoSuchElementException("사용자를 찾을 수 없습니다."));

        // 2. 초대 코드로 Crew 엔티티 조회 (findByInviteCodeAndIsDeletedFalse 활용)
        Crew crew = crewRepository.findByInviteCodeAndIsDeletedFalse(inviteCode)
                .orElseThrow(() -> new NoSuchElementException("유효하지 않거나 만료된 초대 코드입니다."));

        // 3. 이미 활성 멤버인지 검사
        if (crewMemberRepository.existsByCrewAndUserAndIsActiveTrue(crew, user)) {
            throw new IllegalStateException("이미 크루의 멤버입니다.");
        }

        // 4. 인원 제한 검사 (하드코딩된 최대 인원 제한)
        // Crew 엔티티의 maxCapacity를 사용하여 인원 제한 검사
        if (crewRepository.countActiveMembersByCrew(crew) >= crew.getMaxCapacity()) {
            throw new IllegalStateException("크루 인원 제한(" + crew.getMaxCapacity() + "명)이 가득 찼습니다.");
        }

        // 5. 멤버로 추가 (일반 멤버 권한)
        CrewMember newMember = CrewMember.createMember(crew, user, CrewRole.MEMBER);
        return crewMemberRepository.save(newMember);
    }

    // --- 12. 사용자 전체 크루 월별 일정 조회 ---
    @Transactional(readOnly = true)
    public List<CrewScheduleResponse> getCombinedMonthlySchedulesForUser(Long currentUserId, int year, int month) {
        // 1. 사용자가 속한 모든 크루 목록 조회 (getCrewsByUserId 재사용)
        List<CrewResponse> userCrews = getCrewsByUserId(currentUserId);

        List<CrewScheduleResponse> combinedSchedules = new java.util.ArrayList<>();

        // 2. 각 크루별로 월별 일정 조회 메서드를 호출하여 데이터를 합침
        for (CrewResponse crewResponse : userCrews) {
            Long crewId = crewResponse.getCrewId();

            // 3. 기존의 getMonthlySchedules(CrewSchedule 엔티티 리스트 반환)를 호출
            List<CrewSchedule> schedules = getMonthlySchedules(crewId, year, month);

            // 4. 엔티티 리스트를 DTO로 변환 후 통합 리스트에 추가
            combinedSchedules.addAll(
                    schedules.stream()
                            .map(CrewScheduleResponse::from)
                            .collect(Collectors.toList())
            );
        }

        return combinedSchedules;
    }

    // --- 13. 크루 해체 (리더만 가능 - Soft Delete) ---
    @Transactional
    public void disbandCrew(Long crewId, Long currentUserId) {
        // 1. 리더 권한 확인 및 Crew 엔티티 조회
        Crew crew = checkLeaderAuthority(crewId, currentUserId);

        // 2. 크루를 Soft Delete 처리 (isDeleted = true)
        crew.softDelete();
        crewRepository.save(crew);

        // 3. 해당 크루의 모든 멤버십을 비활성화 (isActive = false)
        List<CrewMember> activeMembers = crewMemberRepository.findByCrewAndIsActiveTrue(crew);
        for (CrewMember member : activeMembers) {
            member.leaveCrew(); // isActive = false 처리
        }
        crewMemberRepository.saveAll(activeMembers); // 일괄 저장
    }
}

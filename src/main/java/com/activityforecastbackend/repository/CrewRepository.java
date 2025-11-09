package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.Crew;
import com.activityforecastbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrewRepository extends JpaRepository<Crew, Long> {

    List<Crew> findByIsDeletedFalse();

    Optional<Crew> findByCrewIdAndIsDeletedFalse(Long crewId);

    Optional<Crew> findByInviteCodeAndIsDeletedFalse(String inviteCode);

    List<Crew> findByCreatedByAndIsDeletedFalse(User createdBy);

    @Query("SELECT c FROM Crew c WHERE c.isDeleted = false AND c.crewName LIKE %:keyword%")
    List<Crew> findByCrewNameContainingAndIsDeletedFalse(@Param("keyword") String keyword);

    @Query("SELECT c FROM Crew c JOIN c.members cm WHERE cm.user = :user AND cm.isActive = true AND c.isDeleted = false")
    List<Crew> findCrewsByMember(@Param("user") User user);

    @Query("SELECT c FROM Crew c JOIN c.members cm WHERE cm.user = :user AND cm.role = 'LEADER' AND cm.isActive = true AND c.isDeleted = false")
    List<Crew> findCrewsByLeader(@Param("user") User user);

    @Query("SELECT COUNT(cm) FROM CrewMember cm WHERE cm.crew = :crew AND cm.isActive = true")
    long countActiveMembersByCrew(@Param("crew") Crew crew);

    // FETCH JOIN 쿼리에서 'cm.isActive = true' 조건을 제거하고,
    // CrewId와 isDeleted만으로 Crew와 모든 멤버십을 가져옴
    @Query("SELECT c FROM Crew c JOIN FETCH c.members cm JOIN FETCH c.createdBy WHERE c.crewId = :crewId AND c.isDeleted = false")
    Optional<Crew> findByIdWithMembers(@Param("crewId") Long crewId);

    boolean existsByInviteCodeAndIsDeletedFalse(String inviteCode);
}
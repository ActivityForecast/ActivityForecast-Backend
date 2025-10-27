package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.Crew;
import com.activityforecastbackend.entity.CrewMember;
import com.activityforecastbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrewMemberRepository extends JpaRepository<CrewMember, Long> {

    List<CrewMember> findByCrewAndIsActiveTrue(Crew crew);
    
    List<CrewMember> findByUserAndIsActiveTrue(User user);
    
    Optional<CrewMember> findByCrewAndUser(Crew crew, User user);
    
    Optional<CrewMember> findByCrewAndUserAndIsActiveTrue(Crew crew, User user);
    
    boolean existsByCrewAndUserAndIsActiveTrue(Crew crew, User user);
    
    List<CrewMember> findByCrewAndRoleAndIsActiveTrue(Crew crew, CrewMember.CrewRole role);
    
    @Query("SELECT cm FROM CrewMember cm WHERE cm.crew = :crew AND cm.role = 'LEADER' AND cm.isActive = true")
    List<CrewMember> findLeadersByCrew(@Param("crew") Crew crew);
    
    @Query("SELECT cm FROM CrewMember cm WHERE cm.crew = :crew AND cm.role = 'MEMBER' AND cm.isActive = true")
    List<CrewMember> findMembersByCrew(@Param("crew") Crew crew);
    
    @Query("SELECT COUNT(cm) FROM CrewMember cm WHERE cm.crew = :crew AND cm.isActive = true")
    long countActiveMembers(@Param("crew") Crew crew);
    
    @Query("SELECT COUNT(cm) FROM CrewMember cm WHERE cm.user = :user AND cm.isActive = true")
    long countActiveCrewsByUser(@Param("user") User user);
}
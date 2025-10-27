package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.CrewSchedule;
import com.activityforecastbackend.entity.CrewScheduleParticipant;
import com.activityforecastbackend.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CrewScheduleParticipantRepository extends JpaRepository<CrewScheduleParticipant, Long> {

    List<CrewScheduleParticipant> findByCrewSchedule(CrewSchedule crewSchedule);
    
    List<CrewScheduleParticipant> findByUser(User user);
    
    Optional<CrewScheduleParticipant> findByCrewScheduleAndUser(CrewSchedule crewSchedule, User user);
    
    List<CrewScheduleParticipant> findByCrewScheduleAndIsConfirmedTrue(CrewSchedule crewSchedule);
    
    List<CrewScheduleParticipant> findByCrewScheduleAndIsConfirmedFalse(CrewSchedule crewSchedule);
    
    boolean existsByCrewScheduleAndUser(CrewSchedule crewSchedule, User user);
    
    @Query("SELECT COUNT(csp) FROM CrewScheduleParticipant csp WHERE csp.crewSchedule = :crewSchedule AND csp.isConfirmed = true")
    long countConfirmedParticipants(@Param("crewSchedule") CrewSchedule crewSchedule);
    
    @Query("SELECT COUNT(csp) FROM CrewScheduleParticipant csp WHERE csp.crewSchedule = :crewSchedule")
    long countTotalParticipants(@Param("crewSchedule") CrewSchedule crewSchedule);
    
    @Query("SELECT csp FROM CrewScheduleParticipant csp WHERE csp.user = :user AND csp.isConfirmed = true")
    List<CrewScheduleParticipant> findConfirmedParticipationsByUser(@Param("user") User user);
}
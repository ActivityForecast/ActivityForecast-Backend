package com.activityforecastbackend.repository;

import com.activityforecastbackend.entity.Crew;
import com.activityforecastbackend.entity.CrewSchedule;
import com.activityforecastbackend.entity.Schedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface CrewScheduleRepository extends JpaRepository<CrewSchedule, Long> {

    List<CrewSchedule> findByCrew(Crew crew);
    
    Optional<CrewSchedule> findByCrewAndSchedule(Crew crew, Schedule schedule);
    
    List<CrewSchedule> findBySchedule(Schedule schedule);
    
    @Query("SELECT cs FROM CrewSchedule cs WHERE cs.crew = :crew AND cs.schedule.scheduleDate = :date AND cs.schedule.isDeleted = false")
    List<CrewSchedule> findByCrewAndScheduleDate(@Param("crew") Crew crew, @Param("date") LocalDate date);
    
    @Query("SELECT cs FROM CrewSchedule cs WHERE cs.crew = :crew AND cs.schedule.scheduleDate BETWEEN :startDate AND :endDate AND cs.schedule.isDeleted = false")
    List<CrewSchedule> findByCrewAndScheduleDateBetween(@Param("crew") Crew crew, @Param("startDate") LocalDate startDate, @Param("endDate") LocalDate endDate);
    
    @Query("SELECT COUNT(cs) FROM CrewSchedule cs WHERE cs.crew = :crew AND cs.schedule.isDeleted = false")
    long countByCrewAndScheduleIsDeletedFalse(@Param("crew") Crew crew);
    
    boolean existsByCrewAndSchedule(Crew crew, Schedule schedule);
}
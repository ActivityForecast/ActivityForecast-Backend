package com.activityforecastbackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "crew_schedules", indexes = {
        @Index(name = "idx_crew_schedule_crew_id", columnList = "crew_id"),
        @Index(name = "idx_crew_schedule_schedule_id", columnList = "schedule_id"),
        @Index(name = "idx_crew_schedule_unique", columnList = "crew_id, schedule_id", unique = true)
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewSchedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crew_schedule_id")
    private Long crewScheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private Schedule schedule;

    @Column(name = "equipment_list", columnDefinition = "TEXT")
    private String equipmentList;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "crewSchedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CrewScheduleParticipant> participants = new ArrayList<>();

    public static CrewSchedule createCrewSchedule(Crew crew, Schedule schedule, String equipmentList) {
        CrewSchedule crewSchedule = new CrewSchedule();
        crewSchedule.setCrew(crew);
        crewSchedule.setSchedule(schedule);
        crewSchedule.setEquipmentList(equipmentList);
        return crewSchedule;
    }

    public void updateEquipmentList(String equipmentList) {
        this.equipmentList = equipmentList;
    }

    public int getConfirmedParticipantCount() {
        return (int) participants.stream()
                .filter(CrewScheduleParticipant::isConfirmed)
                .count();
    }

    public int getTotalParticipantCount() {
        return participants.size();
    }
}
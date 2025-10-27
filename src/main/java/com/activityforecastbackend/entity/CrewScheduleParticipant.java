package com.activityforecastbackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "crew_schedule_participants", indexes = {
        @Index(name = "idx_crew_schedule_participant_crew_schedule_id", columnList = "crew_schedule_id"),
        @Index(name = "idx_crew_schedule_participant_unique", columnList = "crew_schedule_id, user_id", unique = true),
        @Index(name = "idx_crew_schedule_participant_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewScheduleParticipant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "participant_id")
    private Long participantId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_schedule_id", nullable = false)
    private CrewSchedule crewSchedule;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "is_confirmed", nullable = false)
    private Boolean isConfirmed = false;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    public static CrewScheduleParticipant createParticipant(CrewSchedule crewSchedule, User user, Boolean isConfirmed) {
        CrewScheduleParticipant participant = new CrewScheduleParticipant();
        participant.setCrewSchedule(crewSchedule);
        participant.setUser(user);
        participant.setIsConfirmed(isConfirmed != null ? isConfirmed : false);
        return participant;
    }

    public void confirmParticipation() {
        this.isConfirmed = true;
    }

    public void cancelParticipation() {
        this.isConfirmed = false;
    }

    public boolean isConfirmed() {
        return this.isConfirmed;
    }
}
package com.activityforecastbackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(name = "crews", indexes = {
        @Index(name = "idx_crew_invite_code", columnList = "invite_code", unique = true),
        @Index(name = "idx_crew_created_by", columnList = "created_by")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Crew {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "crew_id")
    private Long crewId;

    @Column(name = "crew_name", nullable = false, length = 100)
    private String crewName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "color_code", nullable = false, length = 7)
    private String colorCode = "#4A90E2";

    @Column(name = "invite_code", unique = true, nullable = false, length = 50)
    private String inviteCode;

    @Column(name = "max_capacity", nullable = false)
    private Integer maxCapacity; // 최대 인원 제한 필드

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by", nullable = false)
    private User createdBy;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "crew", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CrewMember> members = new ArrayList<>();

    @OneToMany(mappedBy = "crew", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "crew", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CrewSchedule> crewSchedules = new ArrayList<>();

    public static Crew createCrew(String crewName, String description, String colorCode, User creator, Integer maxCapacity) {
        Crew crew = new Crew();
        crew.setCrewName(crewName);
        crew.setDescription(description);
        crew.setColorCode(colorCode != null ? colorCode : "#4A90E2");
        crew.setCreatedBy(creator);
        crew.setInviteCode(generateInviteCode());
        crew.setMaxCapacity(maxCapacity != null ? maxCapacity : 5);
        return crew;
    }

    public void updateCrew(String crewName, String description, String colorCode) {
        this.crewName = crewName;
        this.description = description;
        this.colorCode = colorCode;
    }

    public void regenerateInviteCode() {
        this.inviteCode = generateInviteCode();
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public boolean isActive() {
        return !this.isDeleted;
    }

    public boolean isCreatedBy(User user) {
        return this.createdBy.equals(user);
    }

    private static String generateInviteCode() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, 12);
    }
}
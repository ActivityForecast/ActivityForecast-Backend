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
@Table(name = "crew_members", indexes = {
        @Index(name = "idx_crew_member_crew_id", columnList = "crew_id"),
        @Index(name = "idx_crew_member_unique", columnList = "crew_id, user_id", unique = true),
        @Index(name = "idx_crew_member_user_id", columnList = "user_id")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CrewMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "member_id")
    private Long memberId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id", nullable = false)
    private Crew crew;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", nullable = false, length = 20)
    private CrewRole role = CrewRole.MEMBER;

    @CreationTimestamp
    @Column(name = "joined_at", nullable = false)
    private LocalDateTime joinedAt;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;


    public enum CrewRole {
        LEADER, MEMBER
    }

    public static CrewMember createMember(Crew crew, User user, CrewRole role) {
        CrewMember member = new CrewMember();
        member.setCrew(crew);
        member.setUser(user);
        member.setRole(role != null ? role : CrewRole.MEMBER);
        return member;
    }

    public static CrewMember createLeader(Crew crew, User user) {
        return createMember(crew, user, CrewRole.LEADER);
    }

    public void promoteToLeader() {
        this.role = CrewRole.LEADER;
    }

    public void demoteToMember() {
        this.role = CrewRole.MEMBER;
    }

    public void leaveCrew() {
        this.isActive = false;
    }

    public void rejoinCrew() {
        this.isActive = true;
    }

    public boolean isLeader() {
        return this.role == CrewRole.LEADER;
    }

    public boolean isMember() {
        return this.role == CrewRole.MEMBER;
    }

    public boolean isActiveMember() {
        return this.isActive;
    }
}
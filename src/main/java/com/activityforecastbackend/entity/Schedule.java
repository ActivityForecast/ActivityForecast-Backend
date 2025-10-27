package com.activityforecastbackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "schedules", indexes = {
        @Index(name = "idx_schedule_user_id", columnList = "user_id"),
        @Index(name = "idx_schedule_user_date", columnList = "user_id, schedule_date"),
        @Index(name = "idx_schedule_crew_id", columnList = "crew_id"),
        @Index(name = "idx_schedule_date", columnList = "schedule_date")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Schedule {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long scheduleId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "location_id")
    private ActivityLocation location;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "crew_id")
    private Crew crew;

    @Column(name = "schedule_date", nullable = false)
    private LocalDate scheduleDate;

    @Column(name = "schedule_time")
    private LocalTime scheduleTime;

    @Column(name = "location_latitude", precision = 10, scale = 8)
    private BigDecimal locationLatitude;

    @Column(name = "location_longitude", precision = 11, scale = 8)
    private BigDecimal locationLongitude;

    @Column(name = "location_address", length = 500)
    private String locationAddress;

    @Column(name = "weather_temp", precision = 5, scale = 2)
    private BigDecimal weatherTemp;

    @Column(name = "weather_condition", length = 100)
    private String weatherCondition;

    @Column(name = "air_quality_index")
    private Integer airQualityIndex;

    @Column(name = "comfort_score", precision = 3, scale = 2)
    private BigDecimal comfortScore;

    @Column(name = "is_participated", nullable = false)
    private Boolean isParticipated = false;

    @Column(name = "rating", precision = 2, scale = 1)
    private BigDecimal rating;

    @Column(name = "notes", columnDefinition = "TEXT")
    private String notes;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<CrewSchedule> crewSchedules = new ArrayList<>();

    @OneToMany(mappedBy = "schedule", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<AiTrainingData> aiTrainingData = new ArrayList<>();

    public static Schedule createPersonalSchedule(User user, Activity activity, LocalDate scheduleDate, LocalTime scheduleTime) {
        Schedule schedule = new Schedule();
        schedule.setUser(user);
        schedule.setActivity(activity);
        schedule.setScheduleDate(scheduleDate);
        schedule.setScheduleTime(scheduleTime);
        return schedule;
    }

    public static Schedule createCrewSchedule(User user, Activity activity, Crew crew, LocalDate scheduleDate, LocalTime scheduleTime) {
        Schedule schedule = new Schedule();
        schedule.setUser(user);
        schedule.setActivity(activity);
        schedule.setCrew(crew);
        schedule.setScheduleDate(scheduleDate);
        schedule.setScheduleTime(scheduleTime);
        return schedule;
    }

    public void updateSchedule(LocalDate scheduleDate, LocalTime scheduleTime, String locationAddress) {
        this.scheduleDate = scheduleDate;
        this.scheduleTime = scheduleTime;
        this.locationAddress = locationAddress;
    }

    public void setWeatherData(BigDecimal temperature, String condition, Integer airQualityIndex, BigDecimal comfortScore) {
        this.weatherTemp = temperature;
        this.weatherCondition = condition;
        this.airQualityIndex = airQualityIndex;
        this.comfortScore = comfortScore;
    }

    public void setParticipationResult(Boolean participated, BigDecimal rating, String notes) {
        this.isParticipated = participated;
        this.rating = rating;
        this.notes = notes;
    }

    public void setCustomLocation(BigDecimal latitude, BigDecimal longitude, String address) {
        this.locationLatitude = latitude;
        this.locationLongitude = longitude;
        this.locationAddress = address;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public boolean isActive() {
        return !this.isDeleted;
    }

    public boolean isCrewSchedule() {
        return this.crew != null;
    }
}
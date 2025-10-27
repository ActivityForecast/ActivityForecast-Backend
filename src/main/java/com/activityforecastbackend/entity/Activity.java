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

@Entity
@Table(name = "activities", indexes = {
        @Index(name = "idx_activity_category_id", columnList = "category_id"),
        @Index(name = "idx_activity_type", columnList = "activity_type"),
        @Index(name = "idx_activity_name", columnList = "activity_name")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Activity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "activity_id")
    private Long activityId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "category_id", nullable = false)
    private ActivityCategory category;

    @Column(name = "activity_name", nullable = false, length = 100)
    private String activityName;

    @Column(name = "activity_type", nullable = false, length = 50)
    private String activityType;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "difficulty_level")
    private Integer difficultyLevel;

    @Enumerated(EnumType.STRING)
    @Column(name = "location_type", length = 50)
    private LocationType locationType;

    @Column(name = "equipment_needed", columnDefinition = "TEXT")
    private String equipmentNeeded;

    @Column(name = "precautions", columnDefinition = "TEXT")
    private String precautions;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<ActivityLocation> locations = new ArrayList<>();

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserPreference> userPreferences = new ArrayList<>();

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules = new ArrayList<>();

    @OneToMany(mappedBy = "activity", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Recommendation> recommendations = new ArrayList<>();

    public enum LocationType {
        INDOOR, OUTDOOR, BOTH
    }

    public static Activity createActivity(ActivityCategory category, String activityName, String activityType, 
                                        String description, Integer difficultyLevel, LocationType locationType) {
        Activity activity = new Activity();
        activity.setCategory(category);
        activity.setActivityName(activityName);
        activity.setActivityType(activityType);
        activity.setDescription(description);
        activity.setDifficultyLevel(difficultyLevel);
        activity.setLocationType(locationType);
        return activity;
    }

    public void updateActivity(String activityName, String description, Integer difficultyLevel, 
                              LocationType locationType, String equipmentNeeded, String precautions) {
        this.activityName = activityName;
        this.description = description;
        this.difficultyLevel = difficultyLevel;
        this.locationType = locationType;
        this.equipmentNeeded = equipmentNeeded;
        this.precautions = precautions;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public boolean isActive() {
        return !this.isDeleted;
    }
}
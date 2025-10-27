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
@Table(name = "activity_categories")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "category_id")
    private Long categoryId;

    @Column(name = "category_name", nullable = false, unique = true, length = 50)
    private String categoryName;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @OneToMany(mappedBy = "category", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Activity> activities = new ArrayList<>();

    public static ActivityCategory createCategory(String categoryName, String description) {
        ActivityCategory category = new ActivityCategory();
        category.setCategoryName(categoryName);
        category.setDescription(description);
        return category;
    }

    public void updateCategory(String categoryName, String description) {
        this.categoryName = categoryName;
        this.description = description;
    }
}
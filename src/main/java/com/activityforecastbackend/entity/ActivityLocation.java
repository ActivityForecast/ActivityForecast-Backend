package com.activityforecastbackend.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.CreationTimestamp;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "activity_locations", indexes = {
        @Index(name = "idx_activity_location_activity_id", columnList = "activity_id"),
        @Index(name = "idx_activity_location_coordinates", columnList = "latitude, longitude")
})
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ActivityLocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "location_id")
    private Long locationId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "activity_id", nullable = false)
    private Activity activity;

    @Column(name = "location_name", nullable = false, length = 255)
    private String locationName;

    @Column(name = "address", length = 500)
    private String address;

    @Column(name = "latitude", nullable = false, precision = 10, scale = 8)
    private BigDecimal latitude;

    @Column(name = "longitude", nullable = false, precision = 11, scale = 8)
    private BigDecimal longitude;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "is_deleted", nullable = false)
    private Boolean isDeleted = false;

    @OneToMany(mappedBy = "location", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Schedule> schedules = new ArrayList<>();

    public static ActivityLocation createLocation(Activity activity, String locationName, String address, 
                                                 BigDecimal latitude, BigDecimal longitude) {
        ActivityLocation location = new ActivityLocation();
        location.setActivity(activity);
        location.setLocationName(locationName);
        location.setAddress(address);
        location.setLatitude(latitude);
        location.setLongitude(longitude);
        return location;
    }

    public void updateLocation(String locationName, String address, BigDecimal latitude, BigDecimal longitude) {
        this.locationName = locationName;
        this.address = address;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public void softDelete() {
        this.isDeleted = true;
    }

    public boolean isActive() {
        return !this.isDeleted;
    }
}
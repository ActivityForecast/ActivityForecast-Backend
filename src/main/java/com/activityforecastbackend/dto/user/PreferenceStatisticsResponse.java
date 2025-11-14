package com.activityforecastbackend.dto.user;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.Map;

@Getter
@Setter
@Builder
public class PreferenceStatisticsResponse {
    
    private int totalPreferences;
    private Map<String, Integer> preferencesByCategory;
    private List<CategoryStatistic> categoryStatistics;
    
    @Getter
    @Setter
    @Builder
    public static class CategoryStatistic {
        private String categoryName;
        private int activityCount;
    }
}
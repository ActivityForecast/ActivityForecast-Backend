package com.activityforecastbackend.dto.activity;

import com.activityforecastbackend.entity.ActivityCategory;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "활동 카테고리 정보 DTO")
public class ActivityCategoryDto {
    
    @Schema(description = "카테고리 ID", example = "1")
    private Long categoryId;
    
    @Schema(description = "카테고리명", example = "유산소")
    private String categoryName;
    
    @Schema(description = "카테고리 설명", example = "심폐기능 향상을 위한 운동으로 걷기, 조깅, 수영 등이 포함됩니다")
    private String description;
    
    @Schema(description = "등록 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "카테고리에 속한 활동 목록")
    private List<ActivityDto> activities;
    
    public static ActivityCategoryDto from(ActivityCategory category, List<ActivityDto> activities) {
        return ActivityCategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .activities(activities)
                .build();
    }
    
    public static ActivityCategoryDto fromWithoutActivities(ActivityCategory category) {
        return ActivityCategoryDto.builder()
                .categoryId(category.getCategoryId())
                .categoryName(category.getCategoryName())
                .description(category.getDescription())
                .createdAt(category.getCreatedAt())
                .build();
    }
}
package com.activityforecastbackend.dto.activity;

import com.activityforecastbackend.entity.Activity;
import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "활동 정보 DTO")
public class ActivityDto {
    
    @Schema(description = "활동 ID", example = "1")
    private Long activityId;
    
    @Schema(description = "활동명", example = "축구")
    private String activityName;
    
    @Schema(description = "활동 유형 (AI 예측용)", example = "TEAM_SPORT")
    private String activityType;
    
    @Schema(description = "활동 설명", example = "11명이 한 팀을 이루어 하는 구기 스포츠")
    private String description;
    
    @Schema(description = "난이도 (1-5)", example = "3")
    private Integer difficultyLevel;
    
    @Schema(description = "장소 유형", example = "실내/실외")
    private String locationType;
    
    @Schema(description = "필요한 준비물")
    private String equipmentNeeded;
    
    @Schema(description = "주의사항")
    private String precautions;
    
    @Schema(description = "활동 이미지 URL")
    private String imageUrl;
    
    @Schema(description = "카테고리 ID", example = "2")
    private Long categoryId;
    
    @Schema(description = "카테고리명", example = "구기스포츠")
    private String categoryName;
    
    @Schema(description = "등록 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
    
    @Schema(description = "수정 일시")
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updatedAt;
    
    public static ActivityDto from(Activity activity) {
        return ActivityDto.builder()
                .activityId(activity.getActivityId())
                .activityName(activity.getActivityName())
                .activityType(activity.getActivityType())
                .description(activity.getDescription())
                .difficultyLevel(activity.getDifficultyLevel())
                //.locationType(activity.getLocationType().name())
                // locationType null일때 null 반환하게 수정
                .locationType(activity.getLocationType() != null ? activity.getLocationType().name() : null)
                .equipmentNeeded(activity.getEquipmentNeeded())
                .precautions(activity.getPrecautions())
                .imageUrl(activity.getImageUrl())
                .categoryId(activity.getCategory().getCategoryId())
                .categoryName(activity.getCategory().getCategoryName())
                .createdAt(activity.getCreatedAt())
                .updatedAt(activity.getUpdatedAt())
                .build();
    }
}
package com.activityforecastbackend.controller;

import com.activityforecastbackend.dto.activity.ActivityCategoryDto;
import com.activityforecastbackend.dto.activity.ActivityDto;
import com.activityforecastbackend.service.ActivityService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/api/activities")
@RequiredArgsConstructor
@Tag(name = "Activity API", description = "활동 관련 API")
public class ActivityController {

    private final ActivityService activityService;

    @GetMapping
    @Operation(summary = "모든 활동 조회", description = "시스템에 등록된 모든 활동을 조회합니다.")
    public ResponseEntity<List<ActivityDto>> getAllActivities() {
        log.info("Request to get all activities");
        List<ActivityDto> activities = activityService.getAllActivities();
        log.info("Retrieved {} activities", activities.size());
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/categories")
    @Operation(summary = "활동 카테고리별 조회", description = "활동을 카테고리별로 그룹화하여 조회합니다.")
    public ResponseEntity<List<ActivityCategoryDto>> getActivitiesByCategories() {
        log.info("Request to get activities by categories");
        List<ActivityCategoryDto> categories = activityService.getActivitiesByCategories();
        log.info("Retrieved {} categories with activities", categories.size());
        return ResponseEntity.ok(categories);
    }

    @GetMapping("/category/{categoryId}")
    @Operation(summary = "특정 카테고리 활동 조회", description = "특정 카테고리에 속한 활동들을 조회합니다.")
    public ResponseEntity<List<ActivityDto>> getActivitiesByCategory(@PathVariable Long categoryId) {
        log.info("Request to get activities for category: {}", categoryId);
        List<ActivityDto> activities = activityService.getActivitiesByCategory(categoryId);
        log.info("Retrieved {} activities for category: {}", activities.size(), categoryId);
        return ResponseEntity.ok(activities);
    }

    @GetMapping("/{activityId}")
    @Operation(summary = "활동 상세 조회", description = "특정 활동의 상세 정보를 조회합니다.")
    public ResponseEntity<ActivityDto> getActivity(@PathVariable Long activityId) {
        log.info("Request to get activity: {}", activityId);
        ActivityDto activity = activityService.getActivity(activityId);
        return ResponseEntity.ok(activity);
    }
}
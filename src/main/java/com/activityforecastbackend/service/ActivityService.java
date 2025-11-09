package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.activity.ActivityCategoryDto;
import com.activityforecastbackend.dto.activity.ActivityDto;
import com.activityforecastbackend.entity.Activity;
import com.activityforecastbackend.entity.ActivityCategory;
import com.activityforecastbackend.exception.ResourceNotFoundException;
import com.activityforecastbackend.repository.ActivityCategoryRepository;
import com.activityforecastbackend.repository.ActivityRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ActivityService {

    private final ActivityRepository activityRepository;
    private final ActivityCategoryRepository activityCategoryRepository;

    public List<ActivityDto> getAllActivities() {
        log.info("Fetching all activities");
        List<Activity> activities = activityRepository.findAllByIsDeletedFalseOrderByActivityName();
        return activities.stream()
                .map(ActivityDto::from)
                .collect(Collectors.toList());
    }

    public List<ActivityCategoryDto> getActivitiesByCategories() {
        log.info("Fetching activities grouped by categories");
        List<ActivityCategory> categories = activityCategoryRepository.findAllByOrderByCategoryNameAsc();
        
        return categories.stream()
                .map(category -> {
                    List<ActivityDto> activities = activityRepository
                            .findByCategoryAndIsDeletedFalseOrderByActivityName(category)
                            .stream()
                            .map(ActivityDto::from)
                            .collect(Collectors.toList());
                    
                    return ActivityCategoryDto.from(category, activities);
                })
                .collect(Collectors.toList());
    }

    public List<ActivityDto> getActivitiesByCategory(Long categoryId) {
        log.info("Fetching activities for category: {}", categoryId);
        
        ActivityCategory category = activityCategoryRepository.findById(categoryId)
                .orElseThrow(() -> new ResourceNotFoundException("ActivityCategory", "id", categoryId));
        
        List<Activity> activities = activityRepository
                .findByCategoryAndIsDeletedFalseOrderByActivityName(category);
        
        return activities.stream()
                .map(ActivityDto::from)
                .collect(Collectors.toList());
    }

    public ActivityDto getActivity(Long activityId) {
        log.info("Fetching activity: {}", activityId);
        
        Activity activity = activityRepository.findByActivityIdAndIsDeletedFalse(activityId)
                .orElseThrow(() -> new ResourceNotFoundException("Activity", "id", activityId));
        
        return ActivityDto.from(activity);
    }
}
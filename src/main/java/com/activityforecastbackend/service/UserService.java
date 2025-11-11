package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.user.*;
import com.activityforecastbackend.entity.Activity;
import com.activityforecastbackend.entity.User;
import com.activityforecastbackend.entity.UserPreference;
import com.activityforecastbackend.exception.BadRequestException;
import com.activityforecastbackend.exception.ResourceNotFoundException;
import com.activityforecastbackend.repository.ActivityRepository;
import com.activityforecastbackend.repository.UserPreferenceRepository;
import com.activityforecastbackend.repository.UserRepository;
import com.activityforecastbackend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class UserService {
    
    private final UserRepository userRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final ActivityRepository activityRepository;
    private final PasswordEncoder passwordEncoder;
    
    @Transactional(readOnly = true)
    public UserProfileResponse getUserProfile() {
        Long userId = getCurrentUserId();
        User user = findUserById(userId);
        return UserProfileResponse.fromEntity(user);
    }
    
    public UserProfileResponse updateUserProfile(UserProfileUpdateRequest request) {
        Long userId = getCurrentUserId();
        User user = findUserById(userId);
        
        if (!request.isPasswordMatch()) {
            throw new BadRequestException("비밀번호와 비밀번호 확인이 일치하지 않습니다");
        }
        
        user.setName(request.getName());
        
        if (request.isPasswordUpdateRequested()) {
            if (user.getProvider() != null) {
                throw new BadRequestException("소셜 로그인 사용자는 비밀번호를 변경할 수 없습니다");
            }
            user.setPassword(passwordEncoder.encode(request.getPassword()));
        }
        
        User savedUser = userRepository.save(user);
        log.info("사용자 프로필 업데이트 완료: userId={}, name={}", userId, request.getName());
        
        return UserProfileResponse.fromEntity(savedUser);
    }
    
    @Transactional(readOnly = true)
    public List<UserPreferenceResponse> getUserPreferences() {
        Long userId = getCurrentUserId();
        User user = findUserById(userId);
        List<UserPreference> preferences = userPreferenceRepository.findByUser(user);
        
        return preferences.stream()
                .map(UserPreferenceResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    public List<UserPreferenceResponse> updateUserPreferences(PreferenceUpdateRequest request) {
        Long userId = getCurrentUserId();
        User user = findUserById(userId);
        
        validatePreferences(request.getPreferences());
        
        userPreferenceRepository.deleteByUser(user);
        
        List<UserPreference> newPreferences = request.getPreferences().stream()
                .map(item -> {
                    Activity activity = activityRepository.findById(item.getActivityId())
                            .orElseThrow(() -> new ResourceNotFoundException("활동을 찾을 수 없습니다: " + item.getActivityId()));
                    
                    return UserPreference.createPreference(user, activity);
                })
                .collect(Collectors.toList());
        
        List<UserPreference> savedPreferences = userPreferenceRepository.saveAll(newPreferences);
        log.info("사용자 선호도 업데이트 완료: userId={}, count={}", userId, savedPreferences.size());
        
        return savedPreferences.stream()
                .map(UserPreferenceResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public PreferenceStatisticsResponse getPreferenceStatistics() {
        Long userId = getCurrentUserId();
        User user = findUserById(userId);
        List<UserPreference> preferences = userPreferenceRepository.findByUserWithActivityAndCategory(user);
        
        Map<String, Integer> preferencesByCategory = preferences.stream()
                .collect(Collectors.groupingBy(
                        p -> p.getActivity().getCategory().getCategoryName(),
                        Collectors.collectingAndThen(Collectors.counting(), Math::toIntExact)
                ));
        
        List<PreferenceStatisticsResponse.CategoryStatistic> categoryStatistics = preferences.stream()
                .collect(Collectors.groupingBy(p -> p.getActivity().getCategory()))
                .entrySet().stream()
                .map(entry -> {
                    String categoryName = entry.getKey().getCategoryName();
                    List<UserPreference> categoryPrefs = entry.getValue();
                    int activityCount = categoryPrefs.size();
                    
                    return PreferenceStatisticsResponse.CategoryStatistic.builder()
                            .categoryName(categoryName)
                            .activityCount(activityCount)
                            .build();
                })
                .collect(Collectors.toList());
        
        return PreferenceStatisticsResponse.builder()
                .totalPreferences(preferences.size())
                .preferencesByCategory(preferencesByCategory)
                .categoryStatistics(categoryStatistics)
                .build();
    }
    
    public void deleteUserAccount(AccountDeletionRequest request) {
        Long userId = getCurrentUserId();
        User user = findUserById(userId);
        
        if (user.getProvider() == null) {
            if (!passwordEncoder.matches(request.getPassword(), user.getPassword())) {
                throw new BadRequestException("비밀번호가 일치하지 않습니다");
            }
        }
        
        user.softDelete();
        userRepository.save(user);
        
        log.info("사용자 계정 삭제 완료: userId={}, reason={}", userId, request.getReason());
    }
    
    private Long getCurrentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            throw new BadRequestException("인증되지 않은 사용자입니다");
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        return userPrincipal.getId();
    }
    
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("사용자를 찾을 수 없습니다: " + userId));
    }
    
    private void validatePreferences(List<PreferenceUpdateRequest.PreferenceItem> preferences) {
        if (preferences.size() < 2) {
            throw new BadRequestException("선호 활동은 최소 2개 이상 설정해야 합니다");
        }
        
        if (preferences.size() > 8) {
            throw new BadRequestException("선호 활동은 최대 8개까지 설정할 수 있습니다");
        }
        
        List<Long> activityIds = preferences.stream()
                .map(PreferenceUpdateRequest.PreferenceItem::getActivityId)
                .collect(Collectors.toList());
        
        if (activityIds.size() != activityIds.stream().distinct().count()) {
            throw new BadRequestException("중복된 활동이 포함되어 있습니다");
        }
    }
}
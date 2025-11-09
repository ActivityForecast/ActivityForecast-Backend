package com.activityforecastbackend.service;

import com.activityforecastbackend.dto.auth.JwtAuthenticationResponse;
import com.activityforecastbackend.dto.auth.LoginRequest;
import com.activityforecastbackend.dto.auth.SignupRequest;
import com.activityforecastbackend.dto.auth.UserSummary;
import com.activityforecastbackend.entity.Activity;
import com.activityforecastbackend.entity.User;
import com.activityforecastbackend.entity.UserPreference;
import com.activityforecastbackend.exception.BadRequestException;
import com.activityforecastbackend.exception.ResourceNotFoundException;
import com.activityforecastbackend.repository.ActivityRepository;
import com.activityforecastbackend.repository.UserPreferenceRepository;
import com.activityforecastbackend.repository.UserRepository;
import com.activityforecastbackend.security.JwtTokenProvider;
import com.activityforecastbackend.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AuthService {

    private final UserRepository userRepository;
    private final ActivityRepository activityRepository;
    private final UserPreferenceRepository userPreferenceRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;

    @Transactional
    public JwtAuthenticationResponse login(LoginRequest loginRequest) {
        log.info("Login attempt for email: {}", loginRequest.getEmail());
        
        // 사용자 존재 여부 확인
        User user = userRepository.findByEmailAndIsDeletedFalse(loginRequest.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", loginRequest.getEmail()));

        // OAuth2 사용자인 경우 일반 로그인 불가
        if (user.getProvider() != null) {
            throw new BadRequestException("This account is registered with " + user.getProvider() + 
                    ". Please login using " + user.getProvider() + " account.");
        }

        // 인증 처리
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(
                        loginRequest.getEmail(),
                        loginRequest.getPassword()
                )
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        // 마지막 로그인 시간 업데이트
        user.updateLastLoginAt();
        userRepository.save(user);

        log.info("Login successful for user: {}", user.getEmail());

        return new JwtAuthenticationResponse(
                accessToken,
                refreshToken,
                UserSummary.from(user)
        );
    }

    @Transactional
    public JwtAuthenticationResponse signup(SignupRequest signupRequest) {
        log.info("Signup attempt for email: {}", signupRequest.getEmail());

        // 이메일 중복 확인
        if (userRepository.existsByEmailAndIsDeletedFalse(signupRequest.getEmail())) {
            throw new BadRequestException("Email address already in use!");
        }

        // 사용자 생성
        User user = User.createUser(
                signupRequest.getEmail(),
                passwordEncoder.encode(signupRequest.getPassword()),
                signupRequest.getName()
        );

        User savedUser = userRepository.save(user);

        // 선호 활동 저장
        if (signupRequest.getPreferredActivityIds() != null && !signupRequest.getPreferredActivityIds().isEmpty()) {
            log.info("Saving {} preferred activities for user: {}", 
                    signupRequest.getPreferredActivityIds().size(), savedUser.getEmail());
            
            for (Long activityId : signupRequest.getPreferredActivityIds()) {
                try {
                    Activity activity = activityRepository.findById(activityId)
                            .orElseThrow(() -> new BadRequestException("Activity not found with id: " + activityId));
                    
                    UserPreference preference = UserPreference.builder()
                            .user(savedUser)
                            .activity(activity)
                            .build();
                    
                    userPreferenceRepository.save(preference);
                    log.debug("Saved preference for activity: {} ({})", activity.getActivityName(), activityId);
                } catch (Exception e) {
                    log.warn("Failed to save preference for activity ID {}: {}", activityId, e.getMessage());
                }
            }
        }

        // 자동 로그인 처리
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                UserPrincipal.create(savedUser),
                null,
                UserPrincipal.create(savedUser).getAuthorities()
        );

        SecurityContextHolder.getContext().setAuthentication(authentication);

        // JWT 토큰 생성
        String accessToken = jwtTokenProvider.generateToken(authentication);
        String refreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        // 마지막 로그인 시간 업데이트
        savedUser.updateLastLoginAt();
        userRepository.save(savedUser);

        log.info("Signup and login successful for user: {}", savedUser.getEmail());

        return new JwtAuthenticationResponse(
                accessToken,
                refreshToken,
                UserSummary.from(savedUser)
        );
    }

    @Transactional
    public JwtAuthenticationResponse refreshToken(String refreshToken) {
        log.info("Token refresh attempt");

        // Refresh Token 검증
        if (!jwtTokenProvider.validateToken(refreshToken)) {
            throw new BadRequestException("Invalid refresh token");
        }

        // Refresh Token에서 사용자 ID 추출
        Long userId = jwtTokenProvider.getUserIdFromToken(refreshToken);
        
        // 사용자 조회
        User user = userRepository.findById(userId)
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        // 새로운 Authentication 객체 생성
        UserPrincipal userPrincipal = UserPrincipal.create(user);
        Authentication authentication = new UsernamePasswordAuthenticationToken(
                userPrincipal,
                null,
                userPrincipal.getAuthorities()
        );

        // 새로운 Access Token 생성
        String newAccessToken = jwtTokenProvider.generateToken(authentication);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(authentication);

        log.info("Token refresh successful for user: {}", user.getEmail());

        return new JwtAuthenticationResponse(
                newAccessToken,
                newRefreshToken,
                UserSummary.from(user)
        );
    }

    @Transactional
    public User processOAuth2User(String email, String name, String provider, String providerId) {
        log.info("Processing OAuth2 user: email={}, provider={}", email, provider);

        // 기존 사용자 조회 (Provider + ProviderId로)
        User existingUser = userRepository.findByProviderAndProviderIdAndIsDeletedFalse(provider, providerId)
                .orElse(null);

        if (existingUser != null) {
            // 기존 OAuth2 사용자 정보 업데이트
            existingUser.setEmail(email);
            existingUser.setName(name);
            existingUser.updateLastLoginAt();
            return userRepository.save(existingUser);
        }

        // 같은 이메일로 일반 회원가입한 사용자가 있는지 확인
        User emailUser = userRepository.findByEmailAndIsDeletedFalse(email)
                .orElse(null);

        if (emailUser != null && emailUser.getProvider() == null) {
            // 일반 회원가입 사용자가 OAuth2로 로그인 시도 시 예외 발생
            throw new BadRequestException("An account with this email already exists. Please login using your email and password.");
        }

        // 새로운 OAuth2 사용자 생성
        User newUser = User.createSocialUser(email, name, provider, providerId);
        newUser.updateLastLoginAt();
        
        User savedUser = userRepository.save(newUser);
        log.info("New OAuth2 user created: {}", savedUser.getEmail());
        
        return savedUser;
    }

    @Transactional(readOnly = true)
    public UserSummary getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !authentication.isAuthenticated() || 
            authentication.getPrincipal().equals("anonymousUser")) {
            throw new BadRequestException("No authenticated user found");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        User user = userRepository.findById(userPrincipal.getId())
                .filter(User::isActive)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userPrincipal.getId()));

        return UserSummary.from(user);
    }

    public void logout() {
        SecurityContextHolder.clearContext();
        log.info("User logged out successfully");
    }
}
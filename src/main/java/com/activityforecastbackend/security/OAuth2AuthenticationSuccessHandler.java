package com.activityforecastbackend.security;

import com.activityforecastbackend.entity.User;
import com.activityforecastbackend.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.client.authentication.OAuth2AuthenticationToken;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.security.web.authentication.SimpleUrlAuthenticationSuccessHandler;
import org.springframework.stereotype.Component;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class OAuth2AuthenticationSuccessHandler extends SimpleUrlAuthenticationSuccessHandler implements ApplicationContextAware {

    private ApplicationContext applicationContext;
    private final JwtTokenProvider jwtTokenProvider;
    
    @Value("${app.oauth2.authorized-redirect-uri:http://localhost:3000/auth/oauth2/redirect}")
    private String authorizedRedirectUri;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }
    
    private AuthService getAuthService() {
        return applicationContext.getBean(AuthService.class);
    }

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        
        String targetUrl = determineTargetUrl(request, response, authentication);

        if (response.isCommitted()) {
            log.debug("Response has already been committed. Unable to redirect to " + targetUrl);
            return;
        }

        clearAuthenticationAttributes(request, response);
        getRedirectStrategy().sendRedirect(request, response, targetUrl);
    }

    protected String determineTargetUrl(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) {
        
        String targetUrl = authorizedRedirectUri;

        try {
            OAuth2AuthenticationToken authToken = (OAuth2AuthenticationToken) authentication;
            String registrationId = authToken.getAuthorizedClientRegistrationId();
            OAuth2User oAuth2User = authToken.getPrincipal();

            log.info("OAuth2 authentication successful for provider: {}", registrationId);

            // OAuth2 사용자 정보 추출
            OAuth2UserInfo userInfo = OAuth2UserInfoFactory.getOAuth2UserInfo(registrationId, oAuth2User.getAttributes());
            
            if (userInfo.getEmail() == null || userInfo.getEmail().isEmpty()) {
                log.error("Email not found from OAuth2 provider: {}", registrationId);
                return UriComponentsBuilder.fromUriString(targetUrl)
                        .queryParam("error", "email_not_found")
                        .build().toUriString();
            }

            // 사용자 정보 처리 (회원가입 또는 로그인)
            User user = getAuthService().processOAuth2User(
                    userInfo.getEmail(),
                    userInfo.getName(),
                    registrationId,
                    userInfo.getId()
            );

            // JWT 토큰 생성
            UserPrincipal userPrincipal = UserPrincipal.create(user);
            org.springframework.security.authentication.UsernamePasswordAuthenticationToken authenticationToken = 
                new org.springframework.security.authentication.UsernamePasswordAuthenticationToken(
                    userPrincipal, null, userPrincipal.getAuthorities()
                );

            String accessToken = jwtTokenProvider.generateToken(authenticationToken);
            String refreshToken = jwtTokenProvider.generateRefreshToken(authenticationToken);

            log.info("OAuth2 authentication completed for user: {}", user.getEmail());

            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("token", accessToken)
                    .queryParam("refreshToken", refreshToken)
                    .queryParam("success", "true")
                    .build().toUriString();

        } catch (Exception ex) {
            log.error("OAuth2 authentication error: ", ex);
            return UriComponentsBuilder.fromUriString(targetUrl)
                    .queryParam("error", "authentication_failed")
                    .queryParam("message", ex.getMessage())
                    .build().toUriString();
        }
    }

    protected void clearAuthenticationAttributes(HttpServletRequest request, HttpServletResponse response) {
        super.clearAuthenticationAttributes(request);
    }
}
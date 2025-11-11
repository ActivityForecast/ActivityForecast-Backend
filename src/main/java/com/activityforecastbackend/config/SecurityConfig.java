package com.activityforecastbackend.config;

import com.activityforecastbackend.security.CustomUserDetailsService;
import com.activityforecastbackend.security.JwtAuthenticationFilter;
import com.activityforecastbackend.security.OAuth2AuthenticationFailureHandler;
import com.activityforecastbackend.security.OAuth2AuthenticationSuccessHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.context.annotation.Lazy;
import org.springframework.beans.factory.annotation.Value;

import java.util.Arrays;
import java.util.ArrayList;
import java.util.List;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity(prePostEnabled = true)
@RequiredArgsConstructor
public class SecurityConfig {

    private final CustomUserDetailsService customUserDetailsService;
    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final @Lazy OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final @Lazy OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final KakaoTokenResponseClient kakaoTokenResponseClient;

    @Value("${cors.allowed-origins:https://activityforecast.netlify.app}")
    private String allowedOrigins;

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public DaoAuthenticationProvider authenticationProvider() {
        DaoAuthenticationProvider authProvider = new DaoAuthenticationProvider();
        authProvider.setUserDetailsService(customUserDetailsService);
        authProvider.setPasswordEncoder(passwordEncoder());
        return authProvider;
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .cors(cors -> cors.configurationSource(corsConfigurationSource()))
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(session -> session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        // Public endpoints (API prefix)
                        .requestMatchers("/api/auth/**").permitAll()
                        .requestMatchers("/oauth2/**").permitAll()
                        .requestMatchers("/health/**").permitAll()
                        .requestMatchers("/actuator/health").permitAll()
                        .requestMatchers("/test/**").permitAll()
                        .requestMatchers("/api/weather/**").permitAll()

                        // Swagger endpoints (comprehensive patterns for context path support)
                        .requestMatchers("/swagger-ui/**").permitAll()
                        .requestMatchers("/v3/api-docs/**").permitAll()
                        .requestMatchers("/swagger-ui.html").permitAll()
                        .requestMatchers("/swagger-resources/**").permitAll()
                        .requestMatchers("/webjars/**").permitAll()
                        .requestMatchers("/api/swagger-ui/**").permitAll()
                        .requestMatchers("/api/v3/api-docs/**").permitAll()
                        .requestMatchers("/api/swagger-ui.html").permitAll()
                        .requestMatchers("/api/swagger-resources/**").permitAll()
                        .requestMatchers("/api/webjars/**").permitAll()
                        // Additional patterns for SpringDoc OpenAPI
                        .requestMatchers("/swagger-ui/index.html").permitAll()
                        .requestMatchers("/api/swagger-ui/index.html").permitAll()
                        .requestMatchers("/swagger-ui/swagger-ui-bundle.js").permitAll()
                        .requestMatchers("/swagger-ui/swagger-ui-standalone-preset.js").permitAll()
                        .requestMatchers("/swagger-ui/swagger-ui.css").permitAll()
                        .requestMatchers("/api/swagger-ui/swagger-ui-bundle.js").permitAll()
                        .requestMatchers("/api/swagger-ui/swagger-ui-standalone-preset.js").permitAll()
                        .requestMatchers("/api/swagger-ui/swagger-ui.css").permitAll()

                        // 사용자 프로필 및 선호도 API
                        .requestMatchers("/api/user/**").hasAnyRole("USER", "ADMIN")

                        // Admin endpoints
                        .requestMatchers("/admin/**").hasRole("ADMIN")

                        // User endpoints
                        .requestMatchers("/user/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/recommendation/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/history/**").hasAnyRole("USER", "ADMIN")
                        .requestMatchers("/crew/**").hasAnyRole("USER", "ADMIN")

                        // 크루 API 인증 설정 - USER 또는 ADMIN 권한 필요
                        .requestMatchers("/api/crews/**").hasAnyRole("USER", "ADMIN")

                        // All other requests need authentication
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .tokenEndpoint(tokenEndpoint ->
                                tokenEndpoint.accessTokenResponseClient(kakaoTokenResponseClient)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                );

        http.authenticationProvider(authenticationProvider());
        http.addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        // 허용할 Origin 패턴을 동적으로 구성
        List<String> originPatterns = new ArrayList<>();

        // 개발 환경 origins (항상 포함)
        originPatterns.addAll(Arrays.asList(
                "http://localhost:3000",
                "http://127.0.0.1:3000",
                "http://localhost:*",
                "http://127.0.0.1:*",
                "https://activityforecast.netlify.app"  // 운영용 Netlify 주소 명시적 포함
        ));

        // 운영 환경 origins (환경변수에서)
        if (allowedOrigins != null && !allowedOrigins.trim().isEmpty()) {
            String[] origins = allowedOrigins.split(",");
            for (String origin : origins) {
                String trimmedOrigin = origin.trim();
                if (!trimmedOrigin.isEmpty() && !originPatterns.contains(trimmedOrigin)) {
                    originPatterns.add(trimmedOrigin);
                }
            }
        }

        // 직접 Netlify 주소도 추가 (백업용)
        String netlifyUrl = "https://activityforecast.netlify.app";
        if (!originPatterns.contains(netlifyUrl)) {
            originPatterns.add(netlifyUrl);
        }

        configuration.setAllowedOriginPatterns(originPatterns);

        // 모든 HTTP 메서드 허용
        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS", "HEAD", "PATCH"));

        // 모든 헤더 허용
        configuration.setAllowedHeaders(Arrays.asList("*"));

        // 인증 정보 허용 (JWT 사용)
        configuration.setAllowCredentials(true);

        // preflight 요청 캐시 시간
        configuration.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
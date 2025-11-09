package com.activityforecastbackend.config;

import org.springframework.core.convert.converter.Converter;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.oauth2.client.endpoint.OAuth2AccessTokenResponseClient;
import org.springframework.security.oauth2.client.endpoint.OAuth2AuthorizationCodeGrantRequest;
import org.springframework.security.oauth2.core.OAuth2AccessToken;
import org.springframework.security.oauth2.core.OAuth2AuthorizationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Set;

@Slf4j
@Component
public class KakaoTokenResponseClient implements OAuth2AccessTokenResponseClient<OAuth2AuthorizationCodeGrantRequest> {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public KakaoTokenResponseClient() {
        this.restTemplate = new RestTemplate();
        this.objectMapper = new ObjectMapper();
    }

    @Override
    public OAuth2AccessTokenResponse getTokenResponse(OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest) {
        try {
            log.debug("카카오 토큰 요청 시작");
            
            // 요청 파라미터 구성
            MultiValueMap<String, String> parameters = createTokenRequestParameters(authorizationGrantRequest);
            
            // HTTP 헤더 설정
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
            headers.set("Accept", "application/json");
            
            // 카카오 방식: Authorization 헤더에 KakaoAK 방식 또는 Body에 client credentials
            String clientId = authorizationGrantRequest.getClientRegistration().getClientId();
            String clientSecret = authorizationGrantRequest.getClientRegistration().getClientSecret();
            
            log.debug("Client ID: {}, Client Secret 존재: {}", clientId, clientSecret != null);
            log.debug("Token URI: {}", authorizationGrantRequest.getClientRegistration().getProviderDetails().getTokenUri());
            log.debug("요청 파라미터: {}", parameters);
            
            HttpEntity<MultiValueMap<String, String>> requestEntity = new HttpEntity<>(parameters, headers);
            
            // 카카오 토큰 API 호출
            ResponseEntity<String> response = restTemplate.exchange(
                authorizationGrantRequest.getClientRegistration().getProviderDetails().getTokenUri(),
                HttpMethod.POST,
                requestEntity,
                String.class
            );
            
            log.debug("카카오 토큰 응답 상태: {}", response.getStatusCode());
            log.debug("카카오 토큰 응답 본문: {}", response.getBody());
            
            // JSON 응답 파싱
            Map<String, Object> responseBody = objectMapper.readValue(response.getBody(), Map.class);
            
            return createAccessTokenResponse(responseBody);
            
        } catch (Exception ex) {
            log.error("카카오 토큰 요청 실패: {}", ex.getMessage(), ex);
            OAuth2Error error = new OAuth2Error("invalid_token_response", 
                "카카오 토큰 요청 실패: " + ex.getMessage(), null);
            throw new OAuth2AuthorizationException(error, ex);
        }
    }

    private MultiValueMap<String, String> createTokenRequestParameters(OAuth2AuthorizationCodeGrantRequest authorizationGrantRequest) {
        MultiValueMap<String, String> parameters = new LinkedMultiValueMap<>();
        
        // OAuth2 표준 파라미터
        parameters.add(OAuth2ParameterNames.GRANT_TYPE, authorizationGrantRequest.getGrantType().getValue());
        parameters.add(OAuth2ParameterNames.CODE, authorizationGrantRequest.getAuthorizationExchange().getAuthorizationResponse().getCode());
        parameters.add(OAuth2ParameterNames.REDIRECT_URI, authorizationGrantRequest.getAuthorizationExchange().getAuthorizationRequest().getRedirectUri());
        
        // 카카오 요구사항에 따른 client credentials
        parameters.add(OAuth2ParameterNames.CLIENT_ID, authorizationGrantRequest.getClientRegistration().getClientId());
        
        String clientSecret = authorizationGrantRequest.getClientRegistration().getClientSecret();
        if (clientSecret != null && !clientSecret.isEmpty()) {
            parameters.add(OAuth2ParameterNames.CLIENT_SECRET, clientSecret);
        }
        
        return parameters;
    }

    private OAuth2AccessTokenResponse createAccessTokenResponse(Map<String, Object> responseBody) {
        // 필수 필드 추출
        String accessToken = (String) responseBody.get("access_token");
        String tokenType = (String) responseBody.get("token_type");
        Integer expiresIn = (Integer) responseBody.get("expires_in");
        String refreshToken = (String) responseBody.get("refresh_token");
        String scope = (String) responseBody.get("scope");
        
        if (accessToken == null || tokenType == null) {
            throw new OAuth2AuthorizationException(
                new OAuth2Error("invalid_token_response", "액세스 토큰 또는 토큰 타입이 누락되었습니다.", null)
            );
        }
        
        log.debug("파싱된 토큰 정보 - Access Token: {}..., Token Type: {}, Expires In: {}, Refresh Token 존재: {}", 
            accessToken.substring(0, Math.min(20, accessToken.length())), 
            tokenType, 
            expiresIn, 
            refreshToken != null);
        
        OAuth2AccessTokenResponse.Builder builder = OAuth2AccessTokenResponse.withToken(accessToken)
                .tokenType(OAuth2AccessToken.TokenType.BEARER);
        
        if (expiresIn != null) {
            builder.expiresIn(expiresIn);
        }
        
        if (refreshToken != null) {
            builder.refreshToken(refreshToken);
        }
        
        if (scope != null) {
            // 스코프 문자열을 Set으로 변환
            Set<String> scopes = Set.of(scope.split("\\s+"));
            builder.scopes(scopes);
        }
        
        // 추가 파라미터 (카카오 특화)
        if (responseBody.containsKey("refresh_token_expires_in")) {
            builder.additionalParameters(Map.of("refresh_token_expires_in", responseBody.get("refresh_token_expires_in")));
        }
        
        return builder.build();
    }
}
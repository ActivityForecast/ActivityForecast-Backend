package com.activityforecastbackend.exception;

/**
 * AI 모델 서버 연동 관련 예외 처리 클래스
 */
public class AiModelException extends RuntimeException {
    
    private final String errorCode;
    private final String errorType;
    
    public AiModelException(String errorType, String errorCode, String message) {
        super(message);
        this.errorType = errorType;
        this.errorCode = errorCode;
    }
    
    public AiModelException(String errorType, String errorCode, String message, Throwable cause) {
        super(message, cause);
        this.errorType = errorType;
        this.errorCode = errorCode;
    }
    
    public String getErrorCode() {
        return errorCode;
    }
    
    public String getErrorType() {
        return errorType;
    }
    
    // 미리 정의된 예외 생성 메서드들
    
    /**
     * AI 서버 연결 실패
     */
    public static AiModelException connectionError(String message) {
        return new AiModelException("AI_MODEL", "CONNECTION_ERROR", 
                "AI 모델 서버 연결에 실패했습니다: " + message);
    }
    
    /**
     * AI 서버 연결 실패 (원인 포함)
     */
    public static AiModelException connectionError(String message, Throwable cause) {
        return new AiModelException("AI_MODEL", "CONNECTION_ERROR", 
                "AI 모델 서버 연결에 실패했습니다: " + message, cause);
    }
    
    /**
     * AI 서버 응답 타임아웃
     */
    public static AiModelException timeoutError() {
        return new AiModelException("AI_MODEL", "TIMEOUT", 
                "AI 모델 서버 응답 시간이 초과되었습니다. 잠시 후 다시 시도해주세요.");
    }
    
    /**
     * AI 서버 잘못된 응답
     */
    public static AiModelException invalidResponseError(String message) {
        return new AiModelException("AI_MODEL", "INVALID_RESPONSE", 
                "AI 모델 서버로부터 올바르지 않은 응답을 받았습니다: " + message);
    }
    
    /**
     * 추천 생성 실패
     */
    public static AiModelException recommendationError(String message) {
        return new AiModelException("AI_MODEL", "RECOMMENDATION_FAILED", 
                "활동 추천 생성에 실패했습니다: " + message);
    }
    
    /**
     * 추천 생성 실패 (원인 포함)
     */
    public static AiModelException recommendationError(String message, Throwable cause) {
        return new AiModelException("AI_MODEL", "RECOMMENDATION_FAILED", 
                "활동 추천 생성에 실패했습니다: " + message, cause);
    }
    
    /**
     * 사용자 데이터 부족
     */
    public static AiModelException insufficientDataError(String userId) {
        return new AiModelException("AI_MODEL", "INSUFFICIENT_DATA", 
                String.format("사용자 %s의 추천을 위한 데이터가 부족합니다. 선호 활동을 설정해주세요.", userId));
    }
    
    /**
     * 위치 정보 오류
     */
    public static AiModelException locationError(String locationName) {
        return new AiModelException("AI_MODEL", "LOCATION_ERROR", 
                String.format("위치 '%s'에 대한 정보를 가져올 수 없습니다. 올바른 위치명을 입력해주세요.", locationName));
    }
    
    /**
     * 서버 내부 오류
     */
    public static AiModelException serverError(String message) {
        return new AiModelException("AI_MODEL", "SERVER_ERROR", 
                "AI 모델 서버에서 내부 오류가 발생했습니다: " + message);
    }
}
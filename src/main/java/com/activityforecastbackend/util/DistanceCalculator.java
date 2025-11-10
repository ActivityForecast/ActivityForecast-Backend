package com.activityforecastbackend.util;

import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Slf4j
public class DistanceCalculator {
    
    private static final double EARTH_RADIUS_KM = 6371.0;
    
    /**
     * Haversine 공식을 사용하여 두 지점 간의 거리를 계산합니다.
     * 
     * @param lat1 첫 번째 지점의 위도
     * @param lon1 첫 번째 지점의 경도
     * @param lat2 두 번째 지점의 위도
     * @param lon2 두 번째 지점의 경도
     * @return 두 지점 간의 거리 (km)
     */
    public static BigDecimal calculateDistance(BigDecimal lat1, BigDecimal lon1, BigDecimal lat2, BigDecimal lon2) {
        try {
            // BigDecimal을 double로 변환
            double latitude1 = lat1.doubleValue();
            double longitude1 = lon1.doubleValue();
            double latitude2 = lat2.doubleValue();
            double longitude2 = lon2.doubleValue();
            
            // 위도와 경도를 라디안으로 변환
            double lat1Rad = Math.toRadians(latitude1);
            double lon1Rad = Math.toRadians(longitude1);
            double lat2Rad = Math.toRadians(latitude2);
            double lon2Rad = Math.toRadians(longitude2);
            
            // 위도와 경도 차이 계산
            double deltaLat = lat2Rad - lat1Rad;
            double deltaLon = lon2Rad - lon1Rad;
            
            // Haversine 공식 적용
            double a = Math.sin(deltaLat / 2) * Math.sin(deltaLat / 2) +
                      Math.cos(lat1Rad) * Math.cos(lat2Rad) *
                      Math.sin(deltaLon / 2) * Math.sin(deltaLon / 2);
            
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = EARTH_RADIUS_KM * c;
            
            // BigDecimal로 변환하여 정확도 유지
            return BigDecimal.valueOf(distance).setScale(3, RoundingMode.HALF_UP);
            
        } catch (Exception e) {
            log.error("Distance calculation error: lat1={}, lon1={}, lat2={}, lon2={}", 
                      lat1, lon1, lat2, lon2, e);
            return BigDecimal.ZERO;
        }
    }
    
    /**
     * 주어진 중심점과 반경 내에 있는지 확인합니다.
     * 
     * @param centerLat 중심점 위도
     * @param centerLon 중심점 경도
     * @param pointLat 확인할 지점 위도
     * @param pointLon 확인할 지점 경도
     * @param radiusKm 반경 (km)
     * @return 반경 내에 있으면 true, 아니면 false
     */
    public static boolean isWithinRadius(BigDecimal centerLat, BigDecimal centerLon,
                                        BigDecimal pointLat, BigDecimal pointLon,
                                        BigDecimal radiusKm) {
        BigDecimal distance = calculateDistance(centerLat, centerLon, pointLat, pointLon);
        return distance.compareTo(radiusKm) <= 0;
    }
    
    /**
     * 거리를 미터 단위로 변환합니다.
     * 
     * @param distanceKm 킬로미터 단위 거리
     * @return 미터 단위 거리
     */
    public static BigDecimal kmToMeters(BigDecimal distanceKm) {
        return distanceKm.multiply(new BigDecimal("1000"));
    }
    
    /**
     * 거리를 킬로미터 단위로 변환합니다.
     * 
     * @param distanceMeters 미터 단위 거리
     * @return 킬로미터 단위 거리
     */
    public static BigDecimal metersToKm(BigDecimal distanceMeters) {
        return distanceMeters.divide(new BigDecimal("1000"), 3, RoundingMode.HALF_UP);
    }
}
package com.activityforecastbackend.dto.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카카오 장소 검색 결과 DTO")
public class KakaoPlaceDto {
    
    @Schema(description = "장소 ID", example = "8326745")
    @JsonProperty("id")
    private String id;
    
    @Schema(description = "장소명", example = "올림픽공원")
    @JsonProperty("place_name")
    private String placeName;
    
    @Schema(description = "카테고리명", example = "여행 > 관광,명소 > 공원")
    @JsonProperty("category_name")
    private String categoryName;
    
    @Schema(description = "카테고리 그룹 코드", example = "AT4")
    @JsonProperty("category_group_code")
    private String categoryGroupCode;
    
    @Schema(description = "카테고리 그룹명", example = "관광명소")
    @JsonProperty("category_group_name")
    private String categoryGroupName;
    
    @Schema(description = "전화번호", example = "02-410-1114")
    private String phone;
    
    @Schema(description = "도로명 주소", example = "서울 송파구 올림픽로 424")
    @JsonProperty("road_address_name")
    private String roadAddressName;
    
    @Schema(description = "지번 주소", example = "서울 송파구 방이동 88")
    @JsonProperty("address_name")
    private String addressName;
    
    @Schema(description = "X 좌표(경도)", example = "127.128239386781")
    @JsonProperty("x")
    private String x;
    
    @Schema(description = "Y 좌표(위도)", example = "37.5196301939547")
    @JsonProperty("y")
    private String y;
    
    @Schema(description = "장소 상세페이지 URL")
    @JsonProperty("place_url")
    private String placeUrl;
    
    @Schema(description = "중심좌표까지의 거리 (단위: meter)")
    private String distance;
    
    // 편의 메서드
    public BigDecimal getLatitude() {
        return y != null ? new BigDecimal(y) : null;
    }
    
    public BigDecimal getLongitude() {
        return x != null ? new BigDecimal(x) : null;
    }
    
    public Integer getDistanceInMeters() {
        return distance != null && !distance.isEmpty() ? Integer.valueOf(distance) : null;
    }
}
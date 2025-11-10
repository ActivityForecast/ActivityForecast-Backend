package com.activityforecastbackend.dto.activity;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "카카오 장소 검색 응답 DTO")
public class KakaoSearchResponse {
    
    @Schema(description = "검색 결과")
    @JsonProperty("documents")
    private List<KakaoPlaceDto> documents;
    
    @Schema(description = "메타 정보")
    @JsonProperty("meta")
    private Meta meta;
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Meta {
        
        @Schema(description = "검색된 총 문서 수", example = "100")
        @JsonProperty("total_count")
        private Integer totalCount;
        
        @Schema(description = "현재 페이지에서 보여지는 문서 수", example = "15")
        @JsonProperty("pageable_count")
        private Integer pageableCount;
        
        @Schema(description = "마지막 페이지 여부", example = "false")
        @JsonProperty("is_end")
        private Boolean isEnd;
        
        @Schema(description = "특정 지역을 기준으로 검색된 결과인지 여부")
        @JsonProperty("same_name")
        private SameName sameName;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SameName {
        
        @Schema(description = "질의어에서 인식된 지역의 리스트")
        private List<String> region;
        
        @Schema(description = "질의어에서 지역 정보를 제외한 키워드")
        private String keyword;
        
        @Schema(description = "인식된 지역 리스트 중, 현재 검색에 사용된 지역 정보")
        @JsonProperty("selected_region")
        private String selectedRegion;
    }
}
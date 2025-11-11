package com.activityforecastbackend.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class AirQualityDto {

    @JsonProperty("coord")
    private Coordinate coordinate;

    @JsonProperty("list")
    private List<AirQualityData> list;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coordinate {
        @JsonProperty("lat")
        private Double latitude;

        @JsonProperty("lon")
        private Double longitude;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class AirQualityData {
        @JsonProperty("main")
        private Main main;

        @JsonProperty("components")
        private Components components;

        @JsonProperty("dt")
        private Long timestamp;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {
        @JsonProperty("aqi")
        private Integer aqi;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Components {
        @JsonProperty("co")
        private Double co;

        @JsonProperty("no")
        private Double no;

        @JsonProperty("no2")
        private Double no2;

        @JsonProperty("o3")
        private Double o3;

        @JsonProperty("so2")
        private Double so2;

        @JsonProperty("pm2_5")
        private Double pm25;

        @JsonProperty("pm10")
        private Double pm10;

        @JsonProperty("nh3")
        private Double nh3;
    }

    public Integer getAirQualityIndex() {
        if (list == null || list.isEmpty() || list.get(0).getMain() == null) {
            return null;
        }
        return list.get(0).getMain().getAqi();
    }

    public Double getPm25() {
        if (list == null || list.isEmpty() || list.get(0).getComponents() == null) {
            return null;
        }
        return list.get(0).getComponents().getPm25();
    }

    public Double getPm10() {
        if (list == null || list.isEmpty() || list.get(0).getComponents() == null) {
            return null;
        }
        return list.get(0).getComponents().getPm10();
    }

    public String getAirQualityStatusKorean() {
        Integer aqi = getAirQualityIndex();
        if (aqi == null) {
            return "정보없음";
        }

        return switch (aqi) {
            case 1 -> "좋음";
            case 2 -> "보통";
            case 3 -> "나쁨";
            case 4 -> "매우나쁨";
            case 5 -> "최악";
            default -> "정보없음";
        };
    }

    public String getPm25StatusKorean() {
        Double pm25 = getPm25();
        if (pm25 == null) {
            return "정보없음";
        }

        if (pm25 <= 15) return "좋음";
        else if (pm25 <= 35) return "보통";
        else if (pm25 <= 75) return "나쁨";
        else return "매우나쁨";
    }

    public String getPm10StatusKorean() {
        Double pm10 = getPm10();
        if (pm10 == null) {
            return "정보없음";
        }

        if (pm10 <= 30) return "좋음";
        else if (pm10 <= 80) return "보통";
        else if (pm10 <= 150) return "나쁨";
        else return "매우나쁨";
    }

    public String getOverallAirQualityKorean() {
        String pm25Status = getPm25StatusKorean();
        String pm10Status = getPm10StatusKorean();

        if ("매우나쁨".equals(pm25Status) || "매우나쁨".equals(pm10Status)) {
            return "미세먼지 매우나쁨";
        } else if ("나쁨".equals(pm25Status) || "나쁨".equals(pm10Status)) {
            return "미세먼지 나쁨";
        } else if ("보통".equals(pm25Status) || "보통".equals(pm10Status)) {
            return "미세먼지 보통";
        } else if ("좋음".equals(pm25Status) && "좋음".equals(pm10Status)) {
            return "미세먼지 좋음";
        } else {
            return "미세먼지 정보없음";
        }
    }
}
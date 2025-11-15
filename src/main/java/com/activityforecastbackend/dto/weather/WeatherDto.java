package com.activityforecastbackend.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class WeatherDto {

    @JsonProperty("coord")
    private Coordinate coordinate;

    @JsonProperty("weather")
    private List<Weather> weather;

    @JsonProperty("main")
    private Main main;

    @JsonProperty("wind")
    private Wind wind;

    @JsonProperty("clouds")
    private Clouds clouds;

    @JsonProperty("rain")
    private Rain rain;

    @JsonProperty("snow")
    private Snow snow;

    @JsonProperty("dt")
    private Long timestamp;

    @JsonProperty("sys")
    private Sys sys;

    @JsonProperty("timezone")
    private Integer timezone;

    @JsonProperty("name")
    private String cityName;

    @JsonProperty("visibility")
    private Integer visibility;

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
    public static class Weather {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("main")
        private String main;

        @JsonProperty("description")
        private String description;

        @JsonProperty("icon")
        private String icon;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Main {
        @JsonProperty("temp")
        private Double temperature;

        @JsonProperty("feels_like")
        private Double feelsLike;

        @JsonProperty("temp_min")
        private Double tempMin;

        @JsonProperty("temp_max")
        private Double tempMax;

        @JsonProperty("pressure")
        private Integer pressure;

        @JsonProperty("humidity")
        private Integer humidity;

        @JsonProperty("sea_level")
        private Integer seaLevel;

        @JsonProperty("grnd_level")
        private Integer groundLevel;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Wind {
        @JsonProperty("speed")
        private Double speed;

        @JsonProperty("deg")
        private Integer degree;

        @JsonProperty("gust")
        private Double gust;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Clouds {
        @JsonProperty("all")
        private Integer all;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Rain {
        @JsonProperty("1h")
        private Double oneHour;

        @JsonProperty("3h")
        private Double threeHours;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snow {
        @JsonProperty("1h")
        private Double oneHour;

        @JsonProperty("3h")
        private Double threeHours;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sys {
        @JsonProperty("type")
        private Integer type;

        @JsonProperty("id")
        private Integer id;

        @JsonProperty("country")
        private String country;

        @JsonProperty("sunrise")
        private Long sunrise;

        @JsonProperty("sunset")
        private Long sunset;
    }

    public Double getTemperatureInCelsius() {
        return main != null ? main.getTemperature() - 273.15 : null;
    }

    public String getWeatherConditionKorean() {
        if (weather == null || weather.isEmpty()) {
            return "정보없음";
        }

        String condition = weather.get(0).getMain();
        return translateWeatherCondition(condition);
    }

    public static String translateWeatherCondition(String condition) {
        if (condition == null) return "정보없음";

        Map<String, String> conditionMap = Map.of(
                "Clear", "맑음",
                "Clouds", "흐림",
                "Rain", "비",
                "Drizzle", "이슬비",
                "Snow", "눈",
                "Mist", "안개",
                "Fog", "짙은안개",
                "Thunderstorm", "천둥번개",
                "Atmosphere", "대기"
        );

        return conditionMap.getOrDefault(condition, condition);
    }

    public LocalDateTime getDateTime() {
        if (timestamp == null) return null;
        
        // OpenWeatherMap timezone 필드 활용 (UTC 오프셋을 초 단위로 제공)
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(timezone != null ? timezone : 0);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), zoneOffset);
    }

    public LocalDateTime getSunrise() {
        if (sys == null || sys.getSunrise() == null) return null;
        
        // OpenWeatherMap timezone 필드 활용
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(timezone != null ? timezone : 0);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(sys.getSunrise()), zoneOffset);
    }

    public LocalDateTime getSunset() {
        if (sys == null || sys.getSunset() == null) return null;
        
        // OpenWeatherMap timezone 필드 활용
        ZoneOffset zoneOffset = ZoneOffset.ofTotalSeconds(timezone != null ? timezone : 0);
        return LocalDateTime.ofInstant(Instant.ofEpochSecond(sys.getSunset()), zoneOffset);
    }

    public BigDecimal getComfortScore() {
        if (main == null) return BigDecimal.ZERO;

        double temp = getTemperatureInCelsius();
        int humidity = main.getHumidity() != null ? main.getHumidity() : 50;
        double windSpeed = wind != null && wind.getSpeed() != null ? wind.getSpeed() : 0;
        boolean isRaining = rain != null && (rain.getOneHour() != null || rain.getThreeHours() != null);

        double score = 1.0;

        if (temp < 10 || temp > 30) score -= 0.3;
        else if (temp >= 18 && temp <= 25) score += 0.1;

        if (humidity > 80) score -= 0.2;
        else if (humidity < 30) score -= 0.1;

        if (windSpeed > 15) score -= 0.2;

        if (isRaining) score -= 0.4;

        return BigDecimal.valueOf(Math.max(0.0, Math.min(1.0, score)));
    }
}
package com.activityforecastbackend.dto.weather;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

@Data
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class ForecastDto {

    @JsonProperty("cod")
    private String code;

    @JsonProperty("message")
    private Integer message;

    @JsonProperty("cnt")
    private Integer count;

    @JsonProperty("list")
    private List<ForecastItem> list;

    @JsonProperty("city")
    private City city;

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ForecastItem {
        @JsonProperty("dt")
        private Long timestamp;

        @JsonProperty("main")
        private Main main;

        @JsonProperty("weather")
        private List<Weather> weather;

        @JsonProperty("clouds")
        private Clouds clouds;

        @JsonProperty("wind")
        private Wind wind;

        @JsonProperty("visibility")
        private Integer visibility;

        @JsonProperty("pop")
        private Double probabilityOfPrecipitation;

        @JsonProperty("rain")
        private Rain rain;

        @JsonProperty("snow")
        private Snow snow;

        @JsonProperty("sys")
        private Sys sys;

        @JsonProperty("dt_txt")
        private String dateTimeText;

        public Double getTemperatureInCelsius() {
            return main != null ? main.getTemperature() - 273.15 : null;
        }

        public String getWeatherConditionKorean() {
            if (weather == null || weather.isEmpty()) {
                return "정보없음";
            }

            String condition = weather.get(0).getMain();
            return WeatherDto.translateWeatherCondition(condition);
        }

        public LocalDateTime getDateTime() {
            if (timestamp == null) return null;
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(timestamp), ZoneId.systemDefault());
        }
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

        @JsonProperty("sea_level")
        private Integer seaLevel;

        @JsonProperty("grnd_level")
        private Integer groundLevel;

        @JsonProperty("humidity")
        private Integer humidity;

        @JsonProperty("temp_kf")
        private Double tempKelvinFactor;
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
    public static class Clouds {
        @JsonProperty("all")
        private Integer all;
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
    public static class Rain {
        @JsonProperty("3h")
        private Double threeHours;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Snow {
        @JsonProperty("3h")
        private Double threeHours;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Sys {
        @JsonProperty("pod")
        private String partOfDay;
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class City {
        @JsonProperty("id")
        private Integer id;

        @JsonProperty("name")
        private String name;

        @JsonProperty("coord")
        private Coordinate coordinate;

        @JsonProperty("country")
        private String country;

        @JsonProperty("population")
        private Integer population;

        @JsonProperty("timezone")
        private Integer timezone;

        @JsonProperty("sunrise")
        private Long sunrise;

        @JsonProperty("sunset")
        private Long sunset;

        public LocalDateTime getSunriseTime() {
            if (sunrise == null) return null;
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(sunrise), ZoneId.systemDefault());
        }

        public LocalDateTime getSunsetTime() {
            if (sunset == null) return null;
            return LocalDateTime.ofInstant(Instant.ofEpochSecond(sunset), ZoneId.systemDefault());
        }
    }

    @Data
    @NoArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Coordinate {
        @JsonProperty("lat")
        private Double latitude;

        @JsonProperty("lon")
        private Double longitude;
    }

    public List<ForecastItem> getTodayForecast() {
        if (list == null) return List.of();
        
        LocalDateTime now = LocalDateTime.now();
        String today = now.toLocalDate().toString();
        
        return list.stream()
                .filter(item -> {
                    LocalDateTime dateTime = item.getDateTime();
                    return dateTime != null && dateTime.toLocalDate().toString().equals(today);
                })
                .toList();
    }

    public List<ForecastItem> getTomorrowForecast() {
        if (list == null) return List.of();
        
        LocalDateTime tomorrow = LocalDateTime.now().plusDays(1);
        String tomorrowDate = tomorrow.toLocalDate().toString();
        
        return list.stream()
                .filter(item -> {
                    LocalDateTime dateTime = item.getDateTime();
                    return dateTime != null && dateTime.toLocalDate().toString().equals(tomorrowDate);
                })
                .toList();
    }

    public List<ForecastItem> getWeeklyForecast() {
        if (list == null) return List.of();
        
        return list.stream()
                .filter(item -> {
                    LocalDateTime dateTime = item.getDateTime();
                    return dateTime != null && dateTime.getHour() == 12;
                })
                .toList();
    }
}
package com.iot.airqualitymonitor.dto;

import com.iot.airqualitymonitor.model.AirQualityAlert;
import lombok.Data;
import java.util.List;

@Data
public class OpenWeatherResponse {
    private Coord coord;
    private List<Weather> weather;
    private Main main;
    private Wind wind;
    private Clouds clouds;
    private Sys sys;
    private long dt;
    private String timezone;
    private int timezone_offset;
    private List<AirQualityAlert> alerts;

    @Data
    public static class Coord {
        private double lat;
        private double lon;
    }

    @Data
    public static class Weather {
        private int id;
        private String main;
        private String description;
        private String icon;
    }

    @Data
    public static class Main {
        private double temp;
        private double feels_like;
        private double pressure;
        private double humidity;
    }

    @Data
    public static class Wind {
        private double speed;
        private int deg;
        private Double gust;
    }

    @Data
    public static class Clouds {
        private int all;
    }

    @Data
    public static class Sys {
        private long sunrise;
        private long sunset;
        private String country;
    }

    @Data
    public static class Alert {
        private String sender_name;
        private String event;
        private long start;
        private long end;
        private String description;
        private List<String> tags;
    }
}

package com.iot.airqualitymonitor.dto;

import lombok.Data;
import java.util.List;

@Data
public class AirPollutionResponse {
    private Coord coord;
    private List<AirData> list;

    @Data
    public static class Coord {
        private double lat;
        private double lon;
    }

    @Data
    public static class AirData {
        private Main main;
        private Components components;
        private long dt;
    }

    @Data
    public static class Main {
        private int aqi; // Air Quality Index: 1=Good, 2=Fair, 3=Moderate, 4=Poor, 5=Very Poor
    }

    @Data
    public static class Components {
        private double co;    // Carbon monoxide
        private double no;    // Nitric oxide
        private double no2;   // Nitrogen dioxide
        private double o3;    // Ozone
        private double so2;   // Sulphur dioxide
        private double pm2_5; // Fine particles matter
        private double pm10;  // Coarse particulate matter
        private double nh3;   // Ammonia
    }
}

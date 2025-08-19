package com.iot.airqualitymonitor.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class AirQualitySchedulerService {

    @Autowired
    private AirQualityService airQualityService;

    // Coordenadas fixas
    private final double latitude = 39.9042;
    private final double longitude = 116.4074;

    @Scheduled(fixedRate = 30000)
    public void scheduleAirQualityUpdate() {
        airQualityService.getOpenWeatherAirQuality(latitude, longitude);
    }
}
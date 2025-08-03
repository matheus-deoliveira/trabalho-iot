package com.iot.airqualitymonitor.controller;

import com.iot.airqualitymonitor.dto.AirPollutionResponse;
import com.iot.airqualitymonitor.model.AirQualityData;
import com.iot.airqualitymonitor.service.AirQualityService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AirQualityController {

    @Autowired
    private AirQualityService airQualityService;

    @GetMapping("/api/openweather/airquality")
    public AirPollutionResponse getOpenWeatherAirQuality(@RequestParam double lat, @RequestParam double lon) {
        return airQualityService.getOpenWeatherAirQuality(lat, lon);
    }
}

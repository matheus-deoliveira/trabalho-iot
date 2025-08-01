package com.iot.airqualitymonitor.controller;

import com.iot.airqualitymonitor.dto.AirPollutionResponse;
import com.iot.airqualitymonitor.model.AirQualityData;
import com.iot.airqualitymonitor.service.AirQualityService;
import com.iot.airqualitymonitor.service.ThingsBoardHttpService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class AirQualityController {

    @Autowired
    private AirQualityService airQualityService;

    private final ThingsBoardHttpService thingsBoardHttpService;

    public AirQualityController(ThingsBoardHttpService thingsBoardHttpService) {
        this.thingsBoardHttpService = thingsBoardHttpService;
    }

    @GetMapping("/api/openweather/airquality")
    public AirPollutionResponse getOpenWeatherAirQuality(@RequestParam double lat, @RequestParam double lon) {
        return airQualityService.getOpenWeatherAirQuality(lat, lon);
    }

    @GetMapping("/test-tb")
    public String testThingsBoard() {
        AirQualityData mockData = new AirQualityData();
        mockData.setPm25(12.5);
        mockData.setCo(0.8);
        mockData.setO3(28.3);
        mockData.setAqi(45);
        mockData.setTemperature(25.0);

        thingsBoardHttpService.sendTelemetry(mockData);
        return "Dados enviados ao ThingsBoard!";
    }
}

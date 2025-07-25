package com.iot.airqualitymonitor.service;

import com.iot.airqualitymonitor.dto.AirPollutionResponse;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class AirQualityService {
    @Value("${openweather.api.key}")
    private String openWeatherApiKey;

    private final RestTemplate restTemplate = new RestTemplate();

    public AirPollutionResponse getOpenWeatherAirQuality(double lat, double lon) {
        String url = String.format(
            "https://api.openweathermap.org/data/2.5/air_pollution?lat=%f&lon=%f&appid=%s",
            lat, lon, openWeatherApiKey
        );
        return restTemplate.getForObject(url, AirPollutionResponse.class);
    }
}

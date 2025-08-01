package com.iot.airqualitymonitor.service;

import com.iot.airqualitymonitor.model.AirQualityData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class ThingsBoardHttpService {

    @Value("${thingsboard.http.url}")
    private String tbUrl;

    @Value("${thingsboard.device.token}")
    private String deviceToken;

    private final RestTemplate restTemplate = new RestTemplate();

    public void sendTelemetry(AirQualityData data) {
        String url = tbUrl + "/api/v1/" + deviceToken + "/telemetry";

        String payload = String.format(
                "{\"pm25\":%.2f,\"co\":%.2f,\"o3\":%.2f,\"aqi\":%d,\"temp\":%.2f}",
                data.getPm25(), data.getCo(), data.getO3(), data.getAqi(), data.getTemperature()
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> request = new HttpEntity<>(payload, headers);
        restTemplate.postForEntity(url, request, String.class);
    }
}

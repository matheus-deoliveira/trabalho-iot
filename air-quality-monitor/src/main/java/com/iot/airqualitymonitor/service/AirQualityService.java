package com.iot.airqualitymonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.airqualitymonitor.dto.AirPollutionResponse;
import com.iot.airqualitymonitor.model.AirQualityData;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.HashMap;
import java.util.Map;

@Service
public class AirQualityService {

    @Value("${openweather.api.key}")
    private String openWeatherApiKey;

    @Value("${thingsboard.api.url}")
    private String thingsboardUrl;

    @Value("${thingsboard.device.token}")
    private String deviceToken;

    private final RestTemplate restTemplate = new RestTemplate();

    private final ObjectMapper objectMapper = new ObjectMapper();

    public AirPollutionResponse getOpenWeatherAirQuality(double lat, double lon) {
        // 1. Obter dados da API OpenWeather
        String openWeatherUrl = String.format(
                "http://api.openweathermap.org/data/2.5/air_pollution?lat=%f&lon=%f&appid=%s",
                lat, lon, openWeatherApiKey
        );

        AirPollutionResponse response = restTemplate.getForObject(
                openWeatherUrl,
                AirPollutionResponse.class
        );

        // 2. Enviar dados para o ThingsBoard
        if (response != null) {
            sendToThingsBoard(convertToAirQualityData(response, lat, lon));
        }

        return response;
    }

    private void sendToThingsBoard(AirQualityData data) {
        if (data.getPm25() == null || data.getCo() == null ||
                data.getO3() == null || data.getAqi() == null) {
            logger.warn("Dados incompletos - não enviando ao ThingsBoard");
            return;
        }

        try {
            String url = thingsboardUrl + "/api/v1/" + deviceToken + "/telemetry";

            // Criar payload com Jackson para garantir JSON válido
            Map<String, Object> payloadMap = new HashMap<>();
            payloadMap.put("pm25", data.getPm25());
            payloadMap.put("co", data.getCo());
            payloadMap.put("o3", data.getO3());
            payloadMap.put("aqi", data.getAqi());

            String payload = objectMapper.writeValueAsString(payloadMap);

            // Log para depuração
            System.out.println("Payload enviado ao ThingsBoard: " + payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(payload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);

            System.out.println("Resposta do ThingsBoard: " + response.getStatusCode());

        } catch (JsonProcessingException e) {
            System.err.println("Erro ao serializar JSON: " + e.getMessage());
        } catch (Exception e) {
            System.err.println("Erro na comunicação com ThingsBoard: " + e.getMessage());
        }
    }

    private AirQualityData convertToAirQualityData(AirPollutionResponse response, double lat, double lon) {
        if (response == null || response.getList() == null || response.getList().isEmpty()) {
            System.err.println("AirPollutionResponse list is null or empty - cannot convert to AirQualityData");
            return null;
        }
        AirQualityData data = new AirQualityData();
        data.setPm25(response.getList().get(0).getComponents().getPm2_5());
        data.setCo(response.getList().get(0).getComponents().getCo());
        data.setO3(response.getList().get(0).getComponents().getO3());
        data.setAqi(response.getList().get(0).getMain().getAqi());
        data.setLatitude(lat);
        data.setLongitude(lon);
        return data;
    }
}
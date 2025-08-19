package com.iot.airqualitymonitor.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.iot.airqualitymonitor.dto.AirPollutionResponse;
import com.iot.airqualitymonitor.model.AirQualityData;
import com.iot.airqualitymonitor.model.enums.AqiCategory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.Map;

@Service
@EnableScheduling
public class AirQualityService {

    private static final Logger logger = LoggerFactory.getLogger(AirQualityService.class);

    @Value("${openweather.api.key}")
    private String openWeatherApiKey;

    @Value("${thingsboard.api.url}")
    private String thingsboardUrl;

    @Value("${thingsboard.device.token}")
    private String deviceToken;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public AirQualityService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public AirPollutionResponse getOpenWeatherAirQuality(double lat, double lon) {
        String openWeatherUrl = String.format(
                "http://api.openweathermap.org/data/2.5/air_pollution?lat=%f&lon=%f&appid=%s",
                lat, lon, openWeatherApiKey
        );

        try {
            AirPollutionResponse response = restTemplate.getForObject(openWeatherUrl, AirPollutionResponse.class);

            if (response != null) {
                AirQualityData airData = convertToAirQualityData(response, lat, lon);
                if (airData != null) {
                    airData.calculateAndUpdateAqi(); // Calcula AQI e categoria
                    sendEnhancedAirQualityData(airData);
                } else {
                    logger.warn("Falha ao converter resposta em AirQualityData - dados inválidos ou incompletos.");
                }
            }

            return response;
        } catch (Exception e) {
            logger.error("Erro ao obter dados da OpenWeather API", e);
            throw new RuntimeException("Falha na comunicação com OpenWeather API", e);
        }
    }

    private void sendEnhancedAirQualityData(AirQualityData data) {
        if (!isValidAirQualityData(data)) {
            logger.warn("Dados inválidos - não enviando ao ThingsBoard");
            return;
        }

        try {
            Map<String, Object> payload = buildEnhancedPayload(data);
            String jsonPayload = objectMapper.writeValueAsString(payload);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("ThingsBoard-Integration", "Java-SpringBoot");

            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    thingsboardUrl + "/api/v1/" + deviceToken + "/telemetry",
                    request,
                    String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Dados enviados com sucesso para ThingsBoard. AQI: {}", data.getAqi());
            } else {
                logger.warn("Resposta inesperada do ThingsBoard: {}", response.getStatusCode());
            }

        } catch (JsonProcessingException e) {
            logger.error("Erro ao serializar payload JSON", e);
        } catch (Exception e) {
            logger.error("Erro na comunicação com ThingsBoard", e);
        }
    }

    private Map<String, Object> buildEnhancedPayload(AirQualityData data) {
        Map<String, Object> payload = new HashMap<>();

        // Dados básicos de poluição
        payload.put("pm2_5", data.getPm25());
        payload.put("co", data.getCo());
        payload.put("o3", data.getO3());
        payload.put("no2", data.getNo2());
        payload.put("so2", data.getSo2());
        payload.put("pm10", data.getPm10());

        // Informações consolidadas
        payload.put("aqi", data.getAqi());
        payload.put("aqi_category", data.getAqiCategory().name());
        payload.put("aqi_description", data.getAqiCategory().getDescription());
        payload.put("aqi_color", data.getAqiCategory().getColor());

        // Metadados
        payload.put("latitude", data.getLatitude());
        payload.put("longitude", data.getLongitude());
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("source", "OpenWeatherMap");

        // Adicionando recomendações baseadas no AQI
        payload.put("health_recommendation", getHealthRecommendation(data.getAqiCategory()));

        return payload;
    }

    private String getHealthRecommendation(AqiCategory category) {
        return switch (category) {
            case GOOD -> "Qualidade do ar satisfatória, risco mínimo";
            case MODERATE -> "Qualidade aceitável, pode afetar pessoas sensíveis";
            case UNHEALTHY_SENSITIVE -> "Evitar atividades prolongadas ao ar livre";
            case UNHEALTHY -> "Evitar atividades ao ar livre, usar máscara";
            case VERY_UNHEALTHY -> "Limitar atividades externas, risco significativo";
            case HAZARDOUS -> "Evitar sair de casa, risco grave à saúde";
        };
    }

    private boolean isValidAirQualityData(AirQualityData data) {
        return data != null &&
                data.getPm25() != null &&
                data.getCo() != null &&
                data.getO3() != null &&
                data.getAqi() != null;
    }

    private AirQualityData convertToAirQualityData(AirPollutionResponse response, double lat, double lon) {
        AirQualityData data = new AirQualityData();

        if (response != null && response.getList() != null && !response.getList().isEmpty()) {
            data.setPm25(response.getList().get(0).getComponents().getPm2_5());
            data.setCo(response.getList().get(0).getComponents().getCo());
            data.setO3(response.getList().get(0).getComponents().getO3());
            data.setNo2(response.getList().get(0).getComponents().getNo2());
            data.setSo2(response.getList().get(0).getComponents().getSo2());
            data.setAqi(response.getList().get(0).getMain().getAqi());
            data.setPm10(response.getList().get(0).getComponents().getPm10());
        }

        data.setLatitude(lat);
        data.setLongitude(lon);
        data.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")));

        return data;
    }
}
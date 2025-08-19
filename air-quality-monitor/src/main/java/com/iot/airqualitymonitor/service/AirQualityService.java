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
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects; // Importe a classe Objects

@Service
public class AirQualityService {

    private static final Logger logger = LoggerFactory.getLogger(AirQualityService.class);

    @Value("${openweather.api.key}")
    private String openWeatherApiKey;
    @Value("${thingsboard.api.url}")
    private String thingsboardUrl;
    @Value("${thingsboard.device.token}")
    private String deviceToken;
    @Value("${telegram.api.token}")
    private String telegramApiToken;
    @Value("${telegram.chat.id}")
    private String telegramChatId;

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;
    private AirQualityData lastNotifiedData;

    public AirQualityService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    @Scheduled(fixedRate = 600000)
    public void scheduleAirQualityFetch() {
        logger.info("Executando tarefa agendada: buscando dados de qualidade do ar...");
        try {
            double lat = -20.3194; // Vitória, ES
            double lon = -40.3378;
            getOpenWeatherAirQuality(lat, lon);
            logger.info("Tarefa agendada concluída com sucesso para Vitória, ES.");
        } catch (Exception e) {
            logger.error("Erro ao executar a tarefa agendada", e);
        }
    }

    public AirPollutionResponse getOpenWeatherAirQuality(double lat, double lon) {
        String openWeatherUrl = String.format(Locale.US, "http://api.openweathermap.org/data/2.5/air_pollution?lat=%f&lon=%f&appid=%s", lat, lon, openWeatherApiKey);
        try {
            AirPollutionResponse response = restTemplate.getForObject(openWeatherUrl, AirPollutionResponse.class);
            if (response != null) {
                AirQualityData airData = convertToAirQualityData(response, lat, lon);
                if (airData != null) {
                    airData.calculateAndUpdateAqi();
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
            HttpEntity<String> request = new HttpEntity<>(jsonPayload, headers);
            ResponseEntity<String> response = restTemplate.postForEntity(
                    thingsboardUrl + "/api/v1/" + deviceToken + "/telemetry", request, String.class
            );

            if (response.getStatusCode().is2xxSuccessful()) {
                logger.info("Dados enviados com sucesso para ThingsBoard. AQI: {}, Categoria: {}", data.getAqi(), data.getAqiCategory());
                if (shouldNotify(data)) {
                    sendTelegramNotification(data, this.lastNotifiedData);
                    this.lastNotifiedData = data;
                } else {
                    logger.info("Qualidade do ar não mudou significativamente (AQI e Categoria iguais). Notificação não enviada.");
                }
            } else {
                logger.warn("Resposta inesperada do ThingsBoard: {}", response.getStatusCode());
            }
        } catch (Exception e) {
            logger.error("Erro na comunicação com ThingsBoard", e);
        }
    }

    // =====================================================================================
    // MÉTODO ATUALIZADO PARA VERIFICAR MUDANÇA NO AQI OU NA CATEGORIA
    // =====================================================================================
    private boolean shouldNotify(AirQualityData currentData) {
        if (this.lastNotifiedData == null) {
            logger.info("Primeira leitura, enviando notificação inicial.");
            return true;
        }

        // Verifica se a categoria é diferente
        boolean categoryChanged = !currentData.getAqiCategory().equals(this.lastNotifiedData.getAqiCategory());

        // Verifica se o valor do AQI é diferente
        boolean aqiValueChanged = !Objects.equals(currentData.getAqi(), this.lastNotifiedData.getAqi());

        if(aqiValueChanged && !categoryChanged){
            logger.info("AQI mudou de {} para {}. Enviando notificação.", this.lastNotifiedData.getAqi(), currentData.getAqi());
        }

        return categoryChanged || aqiValueChanged;
    }

    private void sendTelegramNotification(AirQualityData newData, AirQualityData oldData) {
        String messageText = formatTelegramMessage(newData, oldData);
        String telegramUrlTemplate = "https://api.telegram.org/bot{token}/sendMessage?chat_id={chat_id}&text={text}&parse_mode=Markdown";
        Map<String, String> uriVariables = new HashMap<>();
        uriVariables.put("token", telegramApiToken);
        uriVariables.put("chat_id", telegramChatId);
        uriVariables.put("text", messageText);
        try {
            restTemplate.getForEntity(telegramUrlTemplate, String.class, uriVariables);
            logger.info("Notificação enviada para o Telegram.");
        } catch (Exception e) {
            logger.error("Erro ao enviar notificação para o Telegram", e);
        }
    }

    private String formatTelegramMessage(AirQualityData newData, AirQualityData oldData) {
        AqiCategory newCategory = newData.getAqiCategory();
        String statusEmoji;
        String titleEmoji;

        switch (newCategory) {
            case GOOD: statusEmoji = "✅"; titleEmoji = "🌬️"; break;
            case MODERATE: statusEmoji = "🙂"; titleEmoji = "☁️"; break;
            case UNHEALTHY_SENSITIVE: statusEmoji = "😐"; titleEmoji = "⚠️"; break;
            case UNHEALTHY: statusEmoji = "😷"; titleEmoji = "⚠️"; break;
            case VERY_UNHEALTHY: statusEmoji = "🤢"; titleEmoji = "🚨"; break;
            case HAZARDOUS: statusEmoji = "☠️"; titleEmoji = "🚨"; break;
            default: statusEmoji = ""; titleEmoji = "ℹ️";
        }

        String statusLine;
        if (oldData == null) {
            statusLine = String.format("*Status Atual: %s* %s", newCategory.getDescription(), statusEmoji);
        } else if (!newCategory.equals(oldData.getAqiCategory())) {
            statusLine = String.format("*Status alterado de %s para %s* %s",
                    oldData.getAqiCategory().getDescription(),
                    newCategory.getDescription(),
                    statusEmoji
            );
        } else {
            statusLine = String.format("*Status: %s (Atualizado)* %s", newCategory.getDescription(), statusEmoji);
        }

        String mapUrl = String.format(Locale.US, "https://www.google.com/maps/search/?api=1&query=%f,%f", newData.getLatitude(), newData.getLongitude());
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("'de' dd/MM/yyyy 'às' HH:mm");
        String formattedTimestamp = newData.getTimestamp().format(formatter);

        return String.format(
                "%s *Alerta de Qualidade do Ar* %s\n\n" +
                        "%s\n\n" +
                        "O nível de poluição na sua área foi atualizado para *%s*.\n\n" +
                        "📈 *Índice (AQI):* `%d`\n" +
                        "📍 *Localização:* [Ver no Mapa](%s)\n\n" +
                        "*Recomendação de Saúde:*\n%s\n\n" +
                        "_%s_",
                titleEmoji, titleEmoji,
                statusLine,
                newCategory.getDescription().toLowerCase(),
                newData.getAqi(),
                mapUrl,
                getHealthRecommendation(newCategory),
                "Leitura " + formattedTimestamp
        );
    }

    private Map<String, Object> buildEnhancedPayload(AirQualityData data) {
        Map<String, Object> payload = new HashMap<>();
        payload.put("pm2_5", data.getPm25());
        payload.put("co", data.getCo());
        payload.put("o3", data.getO3());
        payload.put("no2", data.getNo2());
        payload.put("so2", data.getSo2());
        payload.put("aqi", data.getAqi());
        payload.put("aqi_category", data.getAqiCategory().name());
        payload.put("aqi_description", data.getAqiCategory().getDescription());
        payload.put("aqi_color", data.getAqiCategory().getColor());
        payload.put("latitude", data.getLatitude());
        payload.put("longitude", data.getLongitude());
        payload.put("timestamp", LocalDateTime.now().toString());
        payload.put("source", "OpenWeatherMap");
        payload.put("health_recommendation", getHealthRecommendation(data.getAqiCategory()));
        return payload;
    }

    private String getHealthRecommendation(AqiCategory category) {
        return switch (category) {
            case GOOD -> "Qualidade do ar satisfatória. Risco mínimo ou inexistente.";
            case MODERATE -> "Qualidade aceitável. Pessoas sensíveis podem sentir algum efeito.";
            case UNHEALTHY_SENSITIVE -> "Grupos sensíveis podem ser afetados. O público geral não deve ser afetado.";
            case UNHEALTHY -> "Evite atividades prolongadas ao ar livre. Todos podem começar a sentir efeitos na saúde.";
            case VERY_UNHEALTHY -> "Alerta de saúde. Limite as atividades externas. Risco significativo para todos.";
            case HAZARDOUS -> "Risco grave à saúde. Evite sair de casa e qualquer atividade externa.";
        };
    }

    private boolean isValidAirQualityData(AirQualityData data) {
        return data != null && data.getPm25() != null && data.getCo() != null && data.getO3() != null && data.getAqi() != null;
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
        }
        data.setLatitude(lat);
        data.setLongitude(lon);
        data.setTimestamp(LocalDateTime.now(ZoneId.of("UTC")));
        return data;
    }
}
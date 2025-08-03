package com.iot.airqualitymonitor.util;

import com.iot.airqualitymonitor.dto.OpenWeatherResponse;
import com.iot.airqualitymonitor.model.AirQualityData;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.util.stream.Collectors;

public class OpenWeatherConverter {

    public static AirQualityData convertToAirQualityData(OpenWeatherResponse response, String locationName) {
        AirQualityData data = new AirQualityData();

        // Localização
        data.setLocation(locationName);
        data.setLatitude(response.getCoord().getLat());
        data.setLongitude(response.getCoord().getLon());
        data.setTimezone(response.getTimezone());
        data.setTimezoneOffset(response.getTimezone_offset());

        // Timestamps (convertendo Unix para LocalDateTime)
        data.setTimestamp(LocalDateTime.ofInstant(Instant.ofEpochSecond(response.getDt()), ZoneOffset.UTC));
        data.setSunrise(LocalDateTime.ofInstant(Instant.ofEpochSecond(response.getSys().getSunrise()), ZoneOffset.UTC));
        data.setSunset(LocalDateTime.ofInstant(Instant.ofEpochSecond(response.getSys().getSunset()), ZoneOffset.UTC));

        // Dados meteorológicos
        if (response.getMain() != null) {
            data.setTemperature(response.getMain().getTemp());
            data.setHumidity(response.getMain().getHumidity());
            data.setPressure(response.getMain().getPressure());
        } else {
            data.setTemperature(0.0); // Default value
            data.setHumidity(0.0);     // Default value
            data.setPressure(0.0);     // Default value
        }
        data.setWindSpeed(response.getWind() != null ? response.getWind().getSpeed() : 0.0);

        // Alertas (convertendo lista para JSON)
        if (response.getAlerts() != null) {
            String alertsJson = response.getAlerts().stream()
                    .map(alert -> String.format(
                            "{\"type\":\"%s\",\"message\":\"%s\",\"triggeredAt\":%s}",
                            alert.getType(),
                            alert.getMessage(),
                            alert.getTriggeredAt()
                    ))
                    .collect(Collectors.joining(", ", "[", "]"));
            data.setAlerts(response.getAlerts());
        }

        return data;
    }
}

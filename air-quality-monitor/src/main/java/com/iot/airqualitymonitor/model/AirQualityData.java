package com.iot.airqualitymonitor.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AirQualityData {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // Localização
    private String location;
    private Double latitude;
    private Double longitude;
    private String timezone;
    private Integer timezoneOffset;

    // Dados principais de qualidade do ar
    private Double pm25;      // Partículas finas (será extraído de 'current.air_pollution' em outra API)
    private Double co;        // Monóxido de Carbono (em µg/m³)
    private Double o3;        // Ozônio (em µg/m³)
    private Integer aqi;      // Índice de Qualidade do Ar (1-5)

    // Dados meteorológicos complementares
    private Double temperature;
    private Double humidity;
    private Double pressure;
    private Double windSpeed;
    private Integer cloudiness;

    // Timestamps
    private LocalDateTime timestamp;
    private LocalDateTime sunrise;
    private LocalDateTime sunset;

    // Alertas (armazenados como JSON ou texto)
    @Column(columnDefinition = "TEXT")
    private String alerts;
}

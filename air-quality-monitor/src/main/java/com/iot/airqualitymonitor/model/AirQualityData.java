package com.iot.airqualitymonitor.model;

import com.iot.airqualitymonitor.model.enums.AqiCategory;
import com.iot.airqualitymonitor.util.AqiCalculator;
import jakarta.persistence.*;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Data
@NoArgsConstructor
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

    // Poluentes
    @Min(0) private Double pm25;  // µg/m³
    @Min(0) private Double co;    // µg/m³
    @Min(0) private Double o3;    // µg/m³
    private Double no2;    // Dióxido de Nitrogênio (µg/m³)
    private Double so2;    // Dióxido de Enxofre (µg/m³)

    // AQI
    @Column(name = "aqi_index")
    @Min(0) @Max(500)
    private Integer aqi;

    @Enumerated(EnumType.STRING)
    private AqiCategory aqiCategory;

    // Meteorologia
    private Double temperature;  // °C
    private Double humidity;     // %
    private Double windSpeed;    // m/s
    private Double pressure;    // hPa

    // Timestamps
    private LocalDateTime timestamp;
    private LocalDateTime sunrise;
    private LocalDateTime sunset;

    @OneToMany(mappedBy = "airQualityData", cascade = CascadeType.ALL)
    private List<AirQualityAlert> alerts = new ArrayList<>();

    public void calculateAndUpdateAqi() {
        this.aqi = AqiCalculator.calculate(this.pm25, this.co, this.o3);
        this.aqiCategory = AqiCategory.fromAqi(this.aqi);
        checkAlerts();
    }

    private void checkAlerts() {
        if (this.aqi >= 150) {
            AirQualityAlert alert = new AirQualityAlert();
            alert.setType(AlertType.AQI_HIGH.name());
            alert.setMessage("Qualidade do ar insalubre: " + aqiCategory.getDescription());
            alert.setTriggeredAt(LocalDateTime.now());
            this.alerts.add(alert);
        }
    }

    // Método de negócio
    public void updateAqi() {
        calculateAndUpdateAqi();
    }
}
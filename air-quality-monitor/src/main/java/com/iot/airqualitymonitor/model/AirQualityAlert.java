package com.iot.airqualitymonitor.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Data
public class AirQualityAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String type; // "PM2.5", "OZONE", etc.
    private String message;
    private LocalDateTime triggeredAt;
    private boolean resolved;

    @ManyToOne
    @JoinColumn(name = "air_quality_data_id")
    private AirQualityData airQualityData;
}
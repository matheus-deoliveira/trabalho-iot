package com.iot.airqualitymonitor.model.enums;

import lombok.Getter;

@Getter
public enum AqiCategory {
    GOOD("Bom", 0, 50, "#4CAF50"),
    MODERATE("Moderado", 51, 100, "#FFEB3B"),
    UNHEALTHY_SENSITIVE("Insalubre p/ grupos sensíveis", 101, 150, "#FF9800"),
    UNHEALTHY("Insalubre", 151, 200, "#F44336"),
    VERY_UNHEALTHY("Muito Insalubre", 201, 300, "#9C27B0"),
    HAZARDOUS("Perigoso", 301, 500, "#795548");

    private final String description;
    private final int min;
    private final int max;
    private final String color;

    AqiCategory(String description, int min, int max, String color) {
        this.description = description;
        this.min = min;
        this.max = max;
        this.color = color;
    }

    // Método para obter categoria baseada no valor AQI
    public static AqiCategory fromAqi(int aqi) {
        for (AqiCategory category : values()) {
            if (aqi >= category.min && aqi <= category.max) {
                return category;
            }
        }
        return HAZARDOUS;
    }
}

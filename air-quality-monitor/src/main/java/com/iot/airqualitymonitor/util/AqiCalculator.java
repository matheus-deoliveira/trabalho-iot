package com.iot.airqualitymonitor.util;

public class AqiCalculator {

    public static int calculate(double pm25, double co, double o3) {
        // Calcular sub-índices para cada poluente
        int aqiPm25 = calculatePm25Aqi(pm25);
        int aqiCo = calculateCoAqi(co);
        int aqiO3 = calculateO3Aqi(o3);

        // O AQI final é o maior sub-índice
        return Math.max(Math.max(aqiPm25, aqiCo), aqiO3);
    }

    private static int calculatePm25Aqi(double concentration) {
        // Tabela de conversão PM2.5 para AQI (µg/m³)
        return calculateSubIndex(
                concentration,
                new double[]{0, 12.0, 35.4, 55.4, 150.4, 250.4, 350.4, 500.4},
                new int[]{0, 50, 100, 150, 200, 300, 400, 500}
        );
    }

    private static int calculateCoAqi(double concentration) {
        // CO em ppm (partes por milhão)
        double ppm = concentration / 1000; // Convertendo µg/m³ para ppm
        return calculateSubIndex(
                ppm,
                new double[]{0, 4.4, 9.4, 12.4, 15.4, 30.4, 40.4, 50.4},
                new int[]{0, 50, 100, 150, 200, 300, 400, 500}
        );
    }

    private static int calculateO3Aqi(double concentration) {
        // O3 em ppb (partes por bilhão)
        double ppb = concentration; // Assumindo que já está em ppb
        return calculateSubIndex(
                ppb,
                new double[]{0, 54, 70, 85, 105, 200, 300, 400},
                new int[]{0, 50, 100, 150, 200, 300, 400, 500}
        );
    }

    private static int calculateSubIndex(double concentration, double[] breaks, int[] aqiValues) {
        for (int i = 0; i < breaks.length - 1; i++) {
            if (concentration <= breaks[i+1]) {
                return linearInterpolation(
                        concentration,
                        breaks[i], breaks[i+1],
                        aqiValues[i], aqiValues[i+1]
                );
            }
        }
        return 500; // Valor máximo
    }

    private static int linearInterpolation(double c, double cLow, double cHigh, int aqiLow, int aqiHigh) {
        return (int) Math.round(
                ((aqiHigh - aqiLow) / (cHigh - cLow)) * (c - cLow) + aqiLow
        );
    }
}
package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class ComparisonResponseDTO {
    private List<HourlyComparisonDTO> hourly;

    @Data
    @AllArgsConstructor
    public static class HourlyComparisonDTO {
        private int hour;
        private Double workdayAvgSpeed;
        private Double holidayAvgSpeed;
        private Double makeupWorkdayAvgSpeed;
        private Double workdayCongestion;
        private Double holidayCongestion;
        private Double makeupWorkdayCongestion;
    }
}
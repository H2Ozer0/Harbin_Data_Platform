package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class TrendResponseDTO {
    private List<DailyTrendDTO> daily;

    @Data
    @AllArgsConstructor
    public static class DailyTrendDTO {
        private String dt;
        private Double avgCongestionIndex;
        private Double avgSpeedKmh;
        private long totalTrips;
    }
}
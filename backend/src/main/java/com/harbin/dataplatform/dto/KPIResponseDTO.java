package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class KPIResponseDTO {
    private String dt;
    private long totalVehicles;
    private long totalTrips;
    private Double avgSpeedKmh;
    private List<CongestedRoadDTO> top5Congested;
    private int mostActiveHour;

    @Data
    @AllArgsConstructor
    public static class CongestedRoadDTO {
        private String roadName;
        private Double congestionIndex;
    }
}
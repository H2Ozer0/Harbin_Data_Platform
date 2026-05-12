package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class CongestionHeatmapDTO {
    private Long roadSegmentId;
    private String roadName;
    private Double avgSpeedKmh;
    private Double congestionIndex;
    private Double deviationPct;
    private Integer tripCount;
    private Double lon;
    private Double lat;
    private String geometry;
}
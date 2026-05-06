package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class HeatmapResponse {
    private List<CongestionHeatmapDTO> segments;
    private int total;
}
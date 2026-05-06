package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverRestHeatmapResponse {
    private List<DriverRestLocationDTO> restLocations;
    private long totalRests;
}

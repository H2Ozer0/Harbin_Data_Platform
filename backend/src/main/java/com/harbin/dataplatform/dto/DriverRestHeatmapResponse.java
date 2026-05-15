package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class DriverRestHeatmapResponse {
    private List<DriverRestLocationDTO> restLocations;
    private long totalRests;

    /** 前端请求的 limit，-1 表示不限 */
    private int limit = -1;

    public DriverRestHeatmapResponse(List<DriverRestLocationDTO> restLocations, long totalRests) {
        this.restLocations = restLocations;
        this.totalRests = totalRests;
    }

    public DriverRestHeatmapResponse(List<DriverRestLocationDTO> restLocations, long totalRests, int limit) {
        this.restLocations = restLocations;
        this.totalRests = totalRests;
        this.limit = limit;
    }
}

package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotspotGridDTO {
    private String gridId;
    private Double lon;
    private Double lat;
    private Integer eventCount;
    private Double hotspotScore;
    private Integer rankInHour;
    private String eventType;
}

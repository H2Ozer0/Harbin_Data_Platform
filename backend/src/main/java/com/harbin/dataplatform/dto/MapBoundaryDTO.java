package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MapBoundaryDTO {
    private Long gid;
    private Long osmId;
    private Integer classId;
    private Double length;
    private Double reverse;
    private Integer maxspeedForward;
    private Integer maxspeedBackward;
    private Double priority;
    private String geojson;
}

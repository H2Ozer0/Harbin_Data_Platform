package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TrajectorySliceDTO {
    private String devid;
    private Double lon;
    private Double lat;
    private Long timestamp;
    private int seqIndex;
}

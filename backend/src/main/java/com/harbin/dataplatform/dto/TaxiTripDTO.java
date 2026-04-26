package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TaxiTripDTO {
    private Long id;
    private String devid;
    private List<Double> lonSeq;
    private List<Double> latSeq;
    private List<Long> tmsSeq;
    private Double startTime;
    private Double endTime;
}

package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ShiftDistributionDTO {
    private String pattern;
    private Integer driverCount;
    private Double ratio;
}

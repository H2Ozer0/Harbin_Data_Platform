package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverRestLocationDTO {
    private String devid;
    private Double lon;
    private Double lat;
    private Double restMinutes;
    private String restStart;
    private String shiftPattern;
}

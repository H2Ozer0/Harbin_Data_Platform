package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AvgActiveMinutesDTO {
    private String pattern;
    private Double avgMinutes;
}

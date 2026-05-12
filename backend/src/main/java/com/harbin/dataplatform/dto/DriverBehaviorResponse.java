package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DriverBehaviorResponse {
    private List<ShiftDistributionDTO> shiftDistribution;
    private List<AvgActiveMinutesDTO> avgActiveMinutesByPattern;
    private long totalDrivers;
}

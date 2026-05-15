package com.harbin.dataplatform.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrajectoryRequest {
    @NotNull
    private String startTime;

    @NotNull
    private String endTime;

    private String deviceId;

    @Min(1)
    @Max(1000)
    @Builder.Default
    private Integer limit = 100;

    private Double minLon;
    private Double maxLon;
    private Double minLat;
    private Double maxLat;
}

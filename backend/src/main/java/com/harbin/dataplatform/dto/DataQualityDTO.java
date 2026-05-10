package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DataQualityDTO {
    private String tableName;
    private String schema;
    private Long rowCount;
    private Integer fieldCount;
    private String lastUpdated;
    private Double completenessPct;
    private String status;
}

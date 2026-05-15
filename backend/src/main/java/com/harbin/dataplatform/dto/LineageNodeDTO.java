package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageNodeDTO {
    private String id;
    private String label;
    private String layer;
    private Long rowCount;
    private Integer fieldCount;
}

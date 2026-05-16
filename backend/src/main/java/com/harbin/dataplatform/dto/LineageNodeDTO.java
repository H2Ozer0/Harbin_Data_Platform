package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageNodeDTO {
    private String id;
    private String label;
    private String layer;
    private String schema;
    private Long rowCount;
    private Integer fieldCount;
    private List<String> columns;
}

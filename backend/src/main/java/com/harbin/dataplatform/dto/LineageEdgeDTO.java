package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LineageEdgeDTO {
    private String source;
    private String target;
    private String label;
}

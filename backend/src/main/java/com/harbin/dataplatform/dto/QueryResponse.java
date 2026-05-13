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
public class QueryResponse {
    private List<String> columns;
    private List<List<Object>> rows;
    private Long totalRows;
    private String queryId;
}

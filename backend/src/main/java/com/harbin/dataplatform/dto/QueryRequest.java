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
public class QueryRequest {
    @NotNull
    private String tableName;

    private List<String> fields;

    private List<FilterCondition> filters;

    @Min(1)
    @Max(1000)
    @Builder.Default
    private Integer limit = 100;
}

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

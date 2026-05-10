package com.harbin.dataplatform.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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
    @NotBlank
    private String tableName;

    private List<String> fields;

    private List<FilterCondition> filters;

    @NotNull
    @jakarta.validation.constraints.Min(1)
    @jakarta.validation.constraints.Max(1000)
    private Integer limit;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FilterCondition {
        @NotBlank
        private String field;

        @NotBlank
        private String operator;

        private Object value;
    }
}

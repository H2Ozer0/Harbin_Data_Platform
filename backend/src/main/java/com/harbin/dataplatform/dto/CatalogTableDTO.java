package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CatalogTableDTO {
    private String schema;
    private String tableName;
    private Long rowCount;
    private Integer fieldCount;
}

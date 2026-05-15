package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FieldDetailDTO {
    private String name;
    private String type;
    private Boolean nullable;
    private String description;
}

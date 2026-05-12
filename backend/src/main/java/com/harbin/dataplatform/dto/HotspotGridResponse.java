package com.harbin.dataplatform.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class HotspotGridResponse {
    private List<HotspotGridDTO> grids;
    private long totalGrids;
}

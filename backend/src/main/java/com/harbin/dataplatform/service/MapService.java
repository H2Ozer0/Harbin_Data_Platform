package com.harbin.dataplatform.service;

import com.harbin.dataplatform.dto.MapBoundaryDTO;
import com.harbin.dataplatform.dto.PageResponse;

import java.util.List;

public interface MapService {

    PageResponse<MapBoundaryDTO> getBoundariesInBBOX(
            double minLon, double minLat, double maxLon, double maxLat,
            int page, int size
    );

    PageResponse<MapBoundaryDTO> getAllBoundaries(int page, int size);

    PageResponse<MapBoundaryDTO> getBoundariesByType(
            String highwayType, int page, int size
    );

    List<MapBoundaryDTO> getFullMapBoundary();
}

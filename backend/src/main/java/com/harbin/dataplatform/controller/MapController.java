package com.harbin.dataplatform.controller;

import com.harbin.dataplatform.dto.MapBoundaryDTO;
import com.harbin.dataplatform.dto.PageResponse;
import com.harbin.dataplatform.service.MapService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/map")
@RequiredArgsConstructor
@CrossOrigin
public class MapController {

    private final MapService mapService;

    @GetMapping("/boundary")
    public ResponseEntity<PageResponse<MapBoundaryDTO>> getBoundaries(
            @RequestParam(defaultValue = "126.0") double minLon,
            @RequestParam(defaultValue = "45.4") double minLat,
            @RequestParam(defaultValue = "127.2") double maxLon,
            @RequestParam(defaultValue = "46.2") double maxLat,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "200") int size
    ) {
        validatePageSize(page, size);
        validateBbox(minLon, maxLon, minLat, maxLat);
        return ResponseEntity.ok(mapService.getBoundariesInBBOX(
                minLon, minLat, maxLon, maxLat, page, size
        ));
    }

    @GetMapping("/full")
    public ResponseEntity<List<MapBoundaryDTO>> getFullMap() {
        return ResponseEntity.ok(mapService.getFullMapBoundary());
    }

    @GetMapping("/roads")
    public ResponseEntity<PageResponse<MapBoundaryDTO>> getRoadsByClass(
            @RequestParam String classId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "100") int size
    ) {
        validatePageSize(page, size);
        return ResponseEntity.ok(mapService.getBoundariesByType(
                classId, page, size
        ));
    }

    private void validatePageSize(int page, int size) {
        if (page < 0) {
            throw new IllegalArgumentException("page must be >= 0");
        }
        if (size <= 0 || size > 5000) {
            throw new IllegalArgumentException("size must be between 1 and 5000");
        }
    }

    private void validateBbox(double minLon, double maxLon, double minLat, double maxLat) {
        if (minLon > maxLon) {
            throw new IllegalArgumentException("minLon must be <= maxLon");
        }
        if (minLat > maxLat) {
            throw new IllegalArgumentException("minLat must be <= maxLat");
        }
    }
}

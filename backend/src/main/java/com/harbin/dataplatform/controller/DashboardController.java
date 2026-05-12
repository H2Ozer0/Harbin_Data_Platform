package com.harbin.dataplatform.controller;

import com.harbin.dataplatform.dto.*;
import com.harbin.dataplatform.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {

    private final DashboardService dashboardService;

    // ============== P3: Catalog + Lineage Modules ==============

    @GetMapping("/catalog/tables")
    public ResponseEntity<List<CatalogTableDTO>> getCatalogTables() {
        return ResponseEntity.ok(dashboardService.getCatalogTables());
    }

    @GetMapping("/catalog/fields/{tableName}")
    public ResponseEntity<CatalogFieldsResponse> getCatalogFields(@PathVariable String tableName) {
        return ResponseEntity.ok(dashboardService.getCatalogFields(tableName));
    }

    @PostMapping("/catalog/query")
    public ResponseEntity<QueryResponse> queryCatalog(@Valid @RequestBody QueryRequest request) {
        return ResponseEntity.ok(dashboardService.queryCatalog(request));
    }

    @GetMapping("/catalog/hot-fields")
    public ResponseEntity<List<HotFieldDTO>> getHotFields() {
        return ResponseEntity.ok(dashboardService.getHotFields());
    }

    @GetMapping("/lineage")
    public ResponseEntity<LineageResponse> getLineage() {
        return ResponseEntity.ok(dashboardService.getLineage());
    }

    @GetMapping("/quality")
    public ResponseEntity<QualityResponse> getQuality() {
        return ResponseEntity.ok(dashboardService.getQuality());
    }

    @GetMapping("/trajectory")
    public ResponseEntity<TrajectoryResponse> getTrajectory(
            @RequestParam String startTime,
            @RequestParam String endTime,
            @RequestParam(required = false) String deviceId,
            @RequestParam(required = false, defaultValue = "100") Integer limit,
            @RequestParam(required = false) Double minLon,
            @RequestParam(required = false) Double maxLon,
            @RequestParam(required = false) Double minLat,
            @RequestParam(required = false) Double maxLat) {
        TrajectoryRequest request = TrajectoryRequest.builder()
                .startTime(startTime)
                .endTime(endTime)
                .deviceId(deviceId)
                .limit(limit)
                .minLon(minLon)
                .maxLon(maxLon)
                .minLat(minLat)
                .maxLat(maxLat)
                .build();
        return ResponseEntity.ok(dashboardService.getTrajectory(request));
    }
}

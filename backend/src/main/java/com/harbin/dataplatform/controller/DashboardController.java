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
}

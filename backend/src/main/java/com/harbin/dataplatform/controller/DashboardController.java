package com.harbin.dataplatform.controller;

import com.harbin.dataplatform.dto.*;
import com.harbin.dataplatform.service.DashboardService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {

    private final DashboardService dashboardService;

    private static final Set<String> VALID_DAY_TYPES = Set.of("workday", "weekend", "holiday", "makeup_workday");

    // ========== P1: Congestion APIs ==========

    @GetMapping("/congestion/heatmap")
    public ResponseEntity<HeatmapResponse> getHeatmap(
            @RequestParam String dt,
            @RequestParam int hour,
            @RequestParam String day_type
    ) {
        validateDate(dt);
        validateHour(hour);
        validateDayType(day_type);

        log.info("GET /congestion/heatmap?dt={}&hour={}&day_type={}", dt, hour, day_type);
        HeatmapResponse response = dashboardService.getHeatmap(dt, hour, day_type);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/kpi")
    public ResponseEntity<KPIResponseDTO> getKPI(
            @RequestParam String dt
    ) {
        validateDate(dt);

        log.info("GET /kpi?dt={}", dt);
        KPIResponseDTO response = dashboardService.getKPI(dt);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/congestion/comparison")
    public ResponseEntity<ComparisonResponseDTO> getComparison() {
        log.info("GET /congestion/comparison");
        ComparisonResponseDTO response = dashboardService.getComparison();
        return ResponseEntity.ok(response);
    }

    @GetMapping("/congestion/trend")
    public ResponseEntity<TrendResponseDTO> getTrend(
            @RequestParam String start_dt,
            @RequestParam String end_dt
    ) {
        validateDate(start_dt);
        validateDate(end_dt);

        log.info("GET /congestion/trend?start_dt={}&end_dt={}", start_dt, end_dt);
        TrendResponseDTO response = dashboardService.getTrend(start_dt, end_dt);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/congestion/road-type-speed")
    public ResponseEntity<List<Map<String, Object>>> getRoadTypeSpeed(
            @RequestParam String dt
    ) {
        validateDate(dt);

        log.info("GET /congestion/road-type-speed?dt={}", dt);
        List<Map<String, Object>> response = dashboardService.getRoadTypeSpeed(dt);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/congestion/duration-ranking")
    public ResponseEntity<List<Map<String, Object>>> getCongestionDuration(
            @RequestParam String dt
    ) {
        validateDate(dt);

        log.info("GET /congestion/duration-ranking?dt={}", dt);
        List<Map<String, Object>> response = dashboardService.getCongestionDurationRanking(dt);
        return ResponseEntity.ok(response);
    }

    // ========== P2: Hotspot & Driver APIs ==========

    @GetMapping("/hotspot/map")
    public ResponseEntity<HotspotGridResponse> getHotspotMap(
            @RequestParam String dt,
            @RequestParam int hour,
            @RequestParam(name = "event_type") String eventType
    ) {
        return ResponseEntity.ok(dashboardService.getHotspotGrid(dt, hour, eventType));
    }

    @GetMapping("/driver/behavior")
    public ResponseEntity<DriverBehaviorResponse> getDriverBehavior(
            @RequestParam String dt
    ) {
        return ResponseEntity.ok(dashboardService.getDriverBehavior(dt));
    }

    @GetMapping("/driver/rest-heatmap")
    public ResponseEntity<DriverRestHeatmapResponse> getDriverRestHeatmap(
            @RequestParam String dt,
            @RequestParam(required = false, defaultValue = "-1") int limit
    ) {
        return ResponseEntity.ok(dashboardService.getDriverRestHeatmap(dt, limit));
    }

    // ========== P3: Catalog + Lineage APIs ==========

    @GetMapping("/catalog/tables")
    public ResponseEntity<List<CatalogTableDTO>> getCatalogTables() {
        return ResponseEntity.ok(dashboardService.getCatalogTables());
    }

    @GetMapping("/catalog/fields/{schema}/{tableName}")
    public ResponseEntity<CatalogFieldsResponse> getCatalogFields(
            @PathVariable String schema, @PathVariable String tableName) {
        return ResponseEntity.ok(dashboardService.getCatalogFields(schema, tableName));
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

    // ========== Validation ==========

    private void validateDate(String dt) {
        if (dt == null || !dt.matches("\\d{4}-\\d{2}-\\d{2}")) {
            throw new IllegalArgumentException("Invalid date format: " + dt + ". Expected YYYY-MM-DD");
        }
    }

    private void validateHour(int hour) {
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("Hour must be between 0 and 23, got: " + hour);
        }
    }

    private void validateDayType(String dayType) {
        if (dayType == null || !VALID_DAY_TYPES.contains(dayType)) {
            throw new IllegalArgumentException("Invalid day_type: " + dayType
                    + ". Must be one of: " + String.join(", ", VALID_DAY_TYPES));
        }
    }
}

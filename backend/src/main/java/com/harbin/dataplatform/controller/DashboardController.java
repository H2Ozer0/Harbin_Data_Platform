package com.harbin.dataplatform.controller;

import com.harbin.dataplatform.dto.ComparisonResponseDTO;
import com.harbin.dataplatform.dto.HeatmapResponse;
import com.harbin.dataplatform.dto.KPIResponseDTO;
import com.harbin.dataplatform.dto.TrendResponseDTO;
import com.harbin.dataplatform.service.DashboardService;
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
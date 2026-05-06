package com.harbin.dataplatform.controller;

import com.harbin.dataplatform.dto.DriverBehaviorResponse;
import com.harbin.dataplatform.dto.DriverRestHeatmapResponse;
import com.harbin.dataplatform.dto.HotspotGridResponse;
import com.harbin.dataplatform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
@CrossOrigin
public class DashboardController {

    private final DashboardService dashboardService;

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
            @RequestParam String dt
    ) {
        return ResponseEntity.ok(dashboardService.getDriverRestHeatmap(dt));
    }
}

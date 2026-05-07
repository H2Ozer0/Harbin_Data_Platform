package com.harbin.dataplatform.service;

import com.harbin.dataplatform.dto.DriverBehaviorResponse;
import com.harbin.dataplatform.dto.DriverRestHeatmapResponse;
import com.harbin.dataplatform.dto.HotspotGridResponse;

public interface DashboardService {
    HotspotGridResponse getHotspotGrid(String dt, int hour, String eventType);

    DriverBehaviorResponse getDriverBehavior(String dt);

    DriverRestHeatmapResponse getDriverRestHeatmap(String dt);

    DriverRestHeatmapResponse getDriverRestHeatmap(String dt, int limit);
}

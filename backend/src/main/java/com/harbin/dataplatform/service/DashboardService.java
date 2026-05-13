package com.harbin.dataplatform.service;

import com.harbin.dataplatform.dto.*;

import java.util.List;
import java.util.Map;

public interface DashboardService {

    // P1: Congestion
    HeatmapResponse getHeatmap(String dt, int hour, String dayType);

    KPIResponseDTO getKPI(String dt);

    ComparisonResponseDTO getComparison();

    TrendResponseDTO getTrend(String startDt, String endDt);

    List<Map<String, Object>> getRoadTypeSpeed(String dt);

    List<Map<String, Object>> getCongestionDurationRanking(String dt);

    // P2: Hotspot & Driver
    HotspotGridResponse getHotspotGrid(String dt, int hour, String eventType);

    DriverBehaviorResponse getDriverBehavior(String dt);

    DriverRestHeatmapResponse getDriverRestHeatmap(String dt);

    DriverRestHeatmapResponse getDriverRestHeatmap(String dt, int limit);

    // P3: Catalog + Lineage
    List<CatalogTableDTO> getCatalogTables();

    CatalogFieldsResponse getCatalogFields(String schema, String tableName);

    QueryResponse queryCatalog(QueryRequest request);

    List<HotFieldDTO> getHotFields();

    LineageResponse getLineage();

    QualityResponse getQuality();

    TrajectoryResponse getTrajectory(TrajectoryRequest request);
}

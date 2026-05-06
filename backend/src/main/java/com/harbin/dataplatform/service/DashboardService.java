package com.harbin.dataplatform.service;

import com.harbin.dataplatform.dto.ComparisonResponseDTO;
import com.harbin.dataplatform.dto.ComparisonResponseDTO.HourlyComparisonDTO;
import com.harbin.dataplatform.dto.CongestionHeatmapDTO;
import com.harbin.dataplatform.dto.HeatmapResponse;
import com.harbin.dataplatform.dto.KPIResponseDTO;
import com.harbin.dataplatform.dto.KPIResponseDTO.CongestedRoadDTO;
import com.harbin.dataplatform.dto.TrendResponseDTO;
import com.harbin.dataplatform.dto.TrendResponseDTO.DailyTrendDTO;
import com.harbin.dataplatform.repository.DashboardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DashboardRepository dashboardRepository;

    /**
     * 获取拥堵热力图数据
     */
    public HeatmapResponse getHeatmap(String dt, int hour, String dayType) {
        List<Map<String, Object>> rows = dashboardRepository.findHeatmapByDtHourAndDayType(dt, hour, dayType);

        List<CongestionHeatmapDTO> segments = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long segId = toLong(row.get("road_segment_id"));
            String roadName = toStringOrNull(row.get("road_name"));
            Double avgSpeed = toDouble(row.get("avg_speed_kmh"));
            Double ci = toDouble(row.get("congestion_index"));
            Double deviation = toDouble(row.get("deviation_pct"));
            String geometry = toStringOrNull(row.get("geometry"));

            segments.add(new CongestionHeatmapDTO(segId, roadName, avgSpeed, ci, deviation, null, null, geometry));
        }

        return new HeatmapResponse(segments, segments.size());
    }

    /**
     * 获取关键指标
     */
    public KPIResponseDTO getKPI(String dt) {
        List<Map<String, Object>> kpiRows = dashboardRepository.findKPIByDt(dt);
        Map<String, Object> kpiRow = kpiRows.isEmpty() ? Map.of() : kpiRows.get(0);

        long totalVehicles = toLong(kpiRow.get("total_vehicles"), 0L);
        long totalTrips = toLong(kpiRow.get("total_trips"), 0L);
        Double avgSpeedKmh = toDouble(kpiRow.get("avg_speed_kmh"));
        int mostActiveHour = toInt(kpiRow.get("most_active_hour"), 0);

        // Top5 congestion
        List<Map<String, Object>> topRows = dashboardRepository.findTop5CongestedByDt(dt);
        List<CongestedRoadDTO> top5 = topRows.stream()
                .map(r -> new CongestedRoadDTO(toStringOrNull(r.get("road_name")), toDouble(r.get("congestion_index"))))
                .toList();

        return new KPIResponseDTO(dt, totalVehicles, totalTrips, avgSpeedKmh, top5, mostActiveHour);
    }

    /**
     * 获取5天拥堵趋势
     */
    public TrendResponseDTO getTrend(String startDt, String endDt) {
        List<Map<String, Object>> rows = dashboardRepository.findTrendByDateRange(startDt, endDt);

        List<DailyTrendDTO> daily = rows.stream()
                .map(r -> new DailyTrendDTO(
                        toStringOrNull(r.get("dt")),
                        toDouble(r.get("avg_congestion_index")),
                        toDouble(r.get("avg_speed_kmh")),
                        toLong(r.get("total_trips"), 0L)
                ))
                .toList();

        return new TrendResponseDTO(daily);
    }

    /**
     * 获取日类型对比数据
     */
    public ComparisonResponseDTO getComparison() {
        List<Map<String, Object>> rows = dashboardRepository.findComparisonByHour();

        List<HourlyComparisonDTO> hourly = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            hourly.add(new HourlyComparisonDTO(
                    toInt(row.get("hour_of_day"), 0),
                    toDouble(row.get("workday_avg_speed")),
                    toDouble(row.get("holiday_avg_speed")),
                    toDouble(row.get("makeup_workday_avg_speed")),
                    toDouble(row.get("workday_congestion")),
                    toDouble(row.get("holiday_congestion")),
                    toDouble(row.get("makeup_workday_congestion"))
            ));
        }

        return new ComparisonResponseDTO(hourly);
    }

    private Long toLong(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.longValue();
        return Long.parseLong(obj.toString());
    }

    private long toLong(Object obj, long defaultValue) {
        Long v = toLong(obj);
        return v != null ? v : defaultValue;
    }

    private int toInt(Object obj, int defaultValue) {
        if (obj == null) return defaultValue;
        if (obj instanceof Number n) return n.intValue();
        try { return Integer.parseInt(obj.toString()); } catch (Exception e) { return defaultValue; }
    }

    private Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.doubleValue();
        return Double.parseDouble(obj.toString());
    }

    private String toStringOrNull(Object obj) {
        return obj != null ? obj.toString() : null;
    }
}
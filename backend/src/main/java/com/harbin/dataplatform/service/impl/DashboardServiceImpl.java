package com.harbin.dataplatform.service.impl;

import com.harbin.dataplatform.dto.*;
import com.harbin.dataplatform.dto.ComparisonResponseDTO.HourlyComparisonDTO;
import com.harbin.dataplatform.dto.KPIResponseDTO.CongestedRoadDTO;
import com.harbin.dataplatform.dto.TrendResponseDTO.DailyTrendDTO;
import com.harbin.dataplatform.repository.DashboardRepository;
import com.harbin.dataplatform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository dashboardRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ========== P1: Congestion ==========

    @Override
    public HeatmapResponse getHeatmap(String dt, int hour, String dayType) {
        List<Map<String, Object>> rows = dashboardRepository.findHeatmapByDtHourAndDayType(dt, hour, dayType);

        List<CongestionHeatmapDTO> segments = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long segId = toLong(row.get("road_segment_id"));
            String roadName = toStringOrNull(row.get("road_name"));
            Double avgSpeed = toDouble(row.get("avg_speed_kmh"));
            Double ci = toDouble(row.get("congestion_index"));
            Double deviation = toDouble(row.get("deviation_pct"));
            Integer tripCount = toInteger(row.get("trip_count"));
            String geometry = toStringOrNull(row.get("geometry"));

            segments.add(new CongestionHeatmapDTO(segId, roadName, avgSpeed, ci, deviation, tripCount, null, null, geometry));
        }

        return new HeatmapResponse(segments, segments.size());
    }

    @Override
    public KPIResponseDTO getKPI(String dt) {
        List<Map<String, Object>> kpiRows = dashboardRepository.findKPIByDt(dt);
        Map<String, Object> kpiRow = kpiRows.isEmpty() ? Map.of() : kpiRows.get(0);

        long totalVehicles = toLong(kpiRow.get("total_vehicles"), 0L);
        long totalTrips = toLong(kpiRow.get("total_trips"), 0L);
        Double avgSpeedKmh = toDouble(kpiRow.get("avg_speed_kmh"));
        int mostActiveHour = toInt(kpiRow.get("most_active_hour"), 0);

        List<Map<String, Object>> topRows = dashboardRepository.findTop5CongestedByDt(dt);
        List<CongestedRoadDTO> top5 = topRows.stream()
                .map(r -> new CongestedRoadDTO(toStringOrNull(r.get("road_name")), toDouble(r.get("congestion_index"))))
                .toList();

        return new KPIResponseDTO(dt, totalVehicles, totalTrips, avgSpeedKmh, top5, mostActiveHour);
    }

    @Override
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

    @Override
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

    @Override
    public List<Map<String, Object>> getRoadTypeSpeed(String dt) {
        return dashboardRepository.findRoadTypeSpeedByHour(dt);
    }

    @Override
    public List<Map<String, Object>> getCongestionDurationRanking(String dt) {
        return dashboardRepository.findCongestionDurationRanking(dt);
    }

    // ========== P2: Hotspot & Driver ==========

    @Override
    public HotspotGridResponse getHotspotGrid(String dt, int hour, String eventType) {
        validateDate(dt);
        if (hour < 0 || hour > 23) {
            throw new IllegalArgumentException("hour must be between 0 and 23");
        }
        if (!"pickup".equalsIgnoreCase(eventType) && !"dropoff".equalsIgnoreCase(eventType)) {
            throw new IllegalArgumentException("event_type must be pickup or dropoff");
        }

        List<Map<String, Object>> rows = dashboardRepository.findHotspotGrids(dt, hour, eventType.toLowerCase());
        List<HotspotGridDTO> grids = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            grids.add(new HotspotGridDTO(
                    (String) row.get("grid_id"),
                    toDouble(row.get("lon")),
                    toDouble(row.get("lat")),
                    toInt(row.get("event_count")),
                    toDouble(row.get("hotspot_score")),
                    toInt(row.get("rank_in_hour")),
                    (String) row.get("event_type")
            ));
        }
        long total = dashboardRepository.countHotspotGrids(dt, hour, eventType.toLowerCase());
        return new HotspotGridResponse(grids, total);
    }

    @Override
    public DriverBehaviorResponse getDriverBehavior(String dt) {
        validateDate(dt);
        List<Map<String, Object>> rows = dashboardRepository.findDriverBehavior(dt);

        long totalDrivers = dashboardRepository.countDrivers(dt);
        List<ShiftDistributionDTO> distribution = new ArrayList<>();
        List<AvgActiveMinutesDTO> avgActive = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            String pattern = (String) row.get("shift_pattern");
            Integer count = toInt(row.get("driver_count"));
            Double avgMinutes = toDouble(row.get("avg_active_minutes"));
            double ratio = totalDrivers == 0 ? 0 : (count == null ? 0 : count.doubleValue() / totalDrivers);

            distribution.add(new ShiftDistributionDTO(pattern, count, ratio));
            avgActive.add(new AvgActiveMinutesDTO(pattern, avgMinutes));
        }

        return new DriverBehaviorResponse(distribution, avgActive, totalDrivers);
    }

    @Override
    public DriverRestHeatmapResponse getDriverRestHeatmap(String dt) {
        return getDriverRestHeatmap(dt, -1);
    }

    @Override
    public DriverRestHeatmapResponse getDriverRestHeatmap(String dt, int limit) {
        validateDate(dt);
        List<Map<String, Object>> rows = dashboardRepository.findRestLocations(dt, limit);
        List<DriverRestLocationDTO> locations = new ArrayList<>();

        for (Map<String, Object> row : rows) {
            locations.add(new DriverRestLocationDTO(
                    (String) row.get("devid"),
                    toDouble(row.get("lon")),
                    toDouble(row.get("lat")),
                    toDouble(row.get("rest_minutes")),
                    row.get("rest_start") == null ? null : row.get("rest_start").toString(),
                    (String) row.get("shift_pattern")
            ));
        }

        long total = dashboardRepository.countRestLocations(dt);
        DriverRestHeatmapResponse resp = new DriverRestHeatmapResponse(locations, total);
        resp.setLimit(limit);
        return resp;
    }

    // ========== Helpers ==========

    private void validateDate(String dt) {
        try {
            LocalDate.parse(dt, DATE_FORMATTER);
        } catch (Exception ex) {
            throw new IllegalArgumentException("dt must be yyyy-MM-dd");
        }
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

    private Integer toInt(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.intValue();
        return Integer.parseInt(obj.toString());
    }

    private Double toDouble(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.doubleValue();
        return Double.parseDouble(obj.toString());
    }

    private Integer toInteger(Object obj) {
        if (obj == null) return null;
        if (obj instanceof Number n) return n.intValue();
        return Integer.parseInt(obj.toString());
    }

    private String toStringOrNull(Object obj) {
        return obj != null ? obj.toString() : null;
    }
}

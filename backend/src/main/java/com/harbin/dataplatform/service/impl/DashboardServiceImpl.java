package com.harbin.dataplatform.service.impl;

import com.harbin.dataplatform.dto.AvgActiveMinutesDTO;
import com.harbin.dataplatform.dto.DriverBehaviorResponse;
import com.harbin.dataplatform.dto.DriverRestHeatmapResponse;
import com.harbin.dataplatform.dto.DriverRestLocationDTO;
import com.harbin.dataplatform.dto.HotspotGridDTO;
import com.harbin.dataplatform.dto.HotspotGridResponse;
import com.harbin.dataplatform.dto.ShiftDistributionDTO;
import com.harbin.dataplatform.repository.DashboardRepository;
import com.harbin.dataplatform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository dashboardRepository;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

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
        validateDate(dt);
        List<Map<String, Object>> rows = dashboardRepository.findRestLocations(dt);
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
        return new DriverRestHeatmapResponse(locations, total);
    }

    private void validateDate(String dt) {
        try {
            LocalDate.parse(dt, DATE_FORMATTER);
        } catch (Exception ex) {
            throw new IllegalArgumentException("dt must be yyyy-MM-dd");
        }
    }

    private Integer toInt(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.intValue();
        }
        return Integer.parseInt(value.toString());
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        return Double.parseDouble(value.toString());
    }
}

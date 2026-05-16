package com.harbin.dataplatform.service.impl;

import com.harbin.dataplatform.dto.*;
import com.harbin.dataplatform.dto.ComparisonResponseDTO.HourlyComparisonDTO;
import com.harbin.dataplatform.dto.KPIResponseDTO.CongestedRoadDTO;
import com.harbin.dataplatform.dto.TrendResponseDTO.DailyTrendDTO;
import com.harbin.dataplatform.repository.DashboardRepository;
import com.harbin.dataplatform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final DashboardRepository dashboardRepository;
    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    // ========== P1: Congestion ==========

    @Override
    @Cacheable(value = "heatmap", key = "#dt + '_' + #hour + '_' + #dayType")
    public HeatmapResponse getHeatmap(String dt, int hour, String dayType) {
        List<Map<String, Object>> rows = dashboardRepository.findHeatmapByDtHourAndDayType(dt, hour, dayType);

        List<CongestionHeatmapDTO> segments = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Long segId = toLong(row.get("road_segment_id"));
            String roadName = toStringOrNull(row.get("road_name"));
            String roadType = toStringOrNull(row.get("road_type"));
            Double avgSpeed = toDouble(row.get("avg_speed_kmh"));
            Double ci = toDouble(row.get("congestion_index"));
            Double deviation = toDouble(row.get("deviation_pct"));
            Integer tripCount = toInteger(row.get("trip_count"));
            String geometry = toStringOrNull(row.get("geometry"));

            segments.add(new CongestionHeatmapDTO(segId, roadName, roadType, avgSpeed, ci, deviation, tripCount, null, null, geometry));
        }

        return new HeatmapResponse(segments, segments.size());
    }

    @Override
    @Cacheable(value = "kpi", key = "#dt")
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
    @Cacheable(value = "trend", key = "#startDt + '_' + #endDt")
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
    @Cacheable(value = "comparison", key = "'all'")
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
    @Cacheable(value = "roadTypeSpeed", key = "#dt")
    public List<Map<String, Object>> getRoadTypeSpeed(String dt) {
        return dashboardRepository.findRoadTypeSpeedByHour(dt);
    }

    @Override
    @Cacheable(value = "durationRanking", key = "#dt")
    public List<Map<String, Object>> getCongestionDurationRanking(String dt) {
        return dashboardRepository.findCongestionDurationRanking(dt);
    }

    // ========== P2: Hotspot & Driver ==========

    @Override
    @Cacheable(value = "hotspotGrid", key = "#dt + '_' + #hour + '_' + #eventType")
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
    @Cacheable(value = "driverBehavior", key = "#dt")
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
    @Cacheable(value = "restHeatmap", key = "#dt + '_' + #limit")
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

    // ========== P3: Catalog + Lineage ==========

    @Override
    public List<CatalogTableDTO> getCatalogTables() {
        // 展示大屏数据链路中实际使用的 9 张表（ODS→DW→TDM→ADS）
        String[][] displayTables = {
                {"ods", "ods_taxi_trips_raw"},
                {"dw", "fact_congestion_seg_hour"},
                {"tdm", "congestion_baseline_5day"},
                {"tdm", "driver_shift_pattern"},
                {"tdm", "grid_hotspot_score"},
                {"ads", "congestion_by_segment_hour"},
                {"ads", "hotspot_grid_enriched"},
                {"ads", "driver_behavior_summary"},
                {"ads", "driver_rest_enriched"},
        };

        List<CatalogTableDTO> result = new ArrayList<>();
        for (String[] t : displayTables) {
            String schema = t[0];
            String table = t[1];
            try {
                Long rowCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + schema + "." + table, Long.class);
                Integer fieldCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name=?",
                        Integer.class, schema, table);
                result.add(CatalogTableDTO.builder()
                        .schema(schema)
                        .tableName(table)
                        .rowCount(rowCount)
                        .fieldCount(fieldCount)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to get info for {}.{}: {}", schema, table, e.getMessage());
            }
        }
        return result;
    }

    @Override
    public CatalogFieldsResponse getCatalogFields(String schema, String tableName) {
        if (!schema.matches("^[a-z]+$") || !tableName.matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid schema or table name");
        }

        String fullTable = schema + "." + tableName;

        String fieldSql = """
            SELECT
                column_name,
                data_type,
                is_nullable,
                COALESCE(column_default, '') AS default_value
            FROM information_schema.columns
            WHERE table_schema = ? AND table_name = ?
            ORDER BY ordinal_position
            """;

        List<FieldDetailDTO> fields = jdbcTemplate.query(fieldSql,
                (rs, rowNum) -> FieldDetailDTO.builder()
                        .name(rs.getString("column_name"))
                        .type(rs.getString("data_type"))
                        .nullable("YES".equals(rs.getString("is_nullable")))
                        .description("")
                        .build(),
                schema, tableName);

        String previewSql = "SELECT * FROM " + fullTable + " LIMIT 5";
        List<Map<String, Object>> preview = jdbcTemplate.queryForList(previewSql);

        return CatalogFieldsResponse.builder()
                .table(tableName)
                .fields(fields)
                .preview(preview)
                .build();
    }

    @Override
    public QueryResponse queryCatalog(QueryRequest request) {
        String schema = request.getSchema() != null ? request.getSchema() : "ads";
        if (!schema.matches("^[a-z]+$") || !request.getTableName().matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid schema or table name");
        }

        String fullTable = schema + "." + request.getTableName();
        int limit = Math.min(request.getLimit() != null ? request.getLimit() : 100, 1000);
        List<String> columns;
        StringBuilder sql = new StringBuilder("SELECT ");

        List<String> requestFields = request.getFields();
        if (requestFields == null || requestFields.isEmpty()) {
            sql.append("* FROM ").append(fullTable);
            columns = getTableColumns(schema, request.getTableName());
        } else {
            columns = requestFields;
            sql.append(String.join(", ", requestFields)).append(" FROM ").append(fullTable);
        }

        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            sql.append(" WHERE ");
            sql.append(request.getFilters().stream().map(this::buildCondition).collect(Collectors.joining(" AND ")));
        }
        sql.append(" LIMIT ").append(limit);

        List<Map<String, Object>> maps = jdbcTemplate.queryForList(sql.toString());
        List<List<Object>> rows = maps.stream()
                .map(m -> columns.stream().map(m::get).toList())
                .toList();

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM ").append(fullTable);
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            countSql.append(" WHERE ");
            countSql.append(request.getFilters().stream().map(this::buildCondition).collect(Collectors.joining(" AND ")));
        }
        Long totalRows = jdbcTemplate.queryForObject(countSql.toString(), Long.class);

        String queryId = "q_" + System.currentTimeMillis();

        // 记录字段访问日志（异步，不影响主查询）
        try {
            for (String col : columns) {
                jdbcTemplate.update(
                        "INSERT INTO ads.data_field_access_log (query_id, table_name, field_name) VALUES (?, ?, ?)",
                        queryId, fullTable, col
                );
            }
        } catch (Exception e) {
            log.debug("Failed to log field access: {}", e.getMessage());
        }

        return QueryResponse.builder()
                .columns(columns)
                .rows(rows)
                .totalRows(totalRows)
                .queryId(queryId)
                .build();
    }

    @Override
    public List<HotFieldDTO> getHotFields() {
        try {
            String sql = """
                SELECT
                    field_name,
                    COUNT(*) AS query_count,
                    ROW_NUMBER() OVER (ORDER BY COUNT(*) DESC) AS rank
                FROM ads.data_field_access_log
                GROUP BY field_name
                ORDER BY query_count DESC
                LIMIT 20
                """;

            return jdbcTemplate.query(sql, (rs, rowNum) -> HotFieldDTO.builder()
                    .fieldName(rs.getString("field_name"))
                    .queryCount(rs.getInt("query_count"))
                    .rank(rs.getInt("rank"))
                    .build());
        } catch (Exception e) {
            log.warn("Failed to get hot fields: {}", e.getMessage());
            return List.of();
        }
    }

    @Override
    public LineageResponse getLineage() {
        List<Map<String, Object>> lineageRows;
        try {
            String lineageSql = """
                SELECT source_schema, source_table, target_schema, target_table, relationship_type
                FROM ads.data_lineage
                ORDER BY source_schema, source_table, target_schema, target_table
                """;
            lineageRows = jdbcTemplate.queryForList(lineageSql);
        } catch (Exception e) {
            log.warn("ads.data_lineage not available, using built-in lineage: {}", e.getMessage());
            lineageRows = List.of();
        }

        if (lineageRows.isEmpty()) {
            lineageRows = buildBuiltinLineage();
        }

        Set<String> allTables = new LinkedHashSet<>();
        for (Map<String, Object> row : lineageRows) {
            allTables.add(row.get("source_schema") + "." + row.get("source_table"));
            allTables.add(row.get("target_schema") + "." + row.get("target_table"));
        }

        List<LineageNodeDTO> nodes = new ArrayList<>();
        for (String fullTable : allTables) {
            String[] parts = fullTable.split("\\.");
            String schema = parts[0];
            String table = parts[1];

            long rowCount = 0;
            int fieldCount = 0;
            List<String> columns = List.of();
            try {
                rowCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + schema + "." + table, Long.class);
            } catch (Exception ignored) {}
            try {
                fieldCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name=?",
                        Integer.class, schema, table);
            } catch (Exception ignored) {}
            try {
                columns = getTableColumns(schema, table);
            } catch (Exception ignored) {}

            nodes.add(LineageNodeDTO.builder()
                    .id(table)
                    .label(formatTableLabel(schema, table))
                    .layer(getLayerName(schema))
                    .schema(schema)
                    .rowCount(rowCount)
                    .fieldCount(fieldCount)
                    .columns(columns)
                    .build());
        }

        List<LineageEdgeDTO> edges = lineageRows.stream()
                .map(row -> LineageEdgeDTO.builder()
                        .source((String) row.get("source_table"))
                        .target((String) row.get("target_table"))
                        .label((String) row.get("relationship_type"))
                        .build())
                .collect(Collectors.toList());

        return LineageResponse.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
    }

    @Override
    public QualityResponse getQuality() {
        List<DataQualityDTO> tables = new ArrayList<>();

        String[][] targets = {
                // ODS
                {"ods", "ods_taxi_trips_raw"},
                // DW
                {"dw", "fact_congestion_seg_hour"},
                // TDM
                {"tdm", "congestion_baseline_5day"},
                {"tdm", "driver_shift_pattern"},
                {"tdm", "grid_hotspot_score"},
                // ADS — 实际被大屏使用的表
                {"ads", "congestion_by_segment_hour"},
                {"ads", "hotspot_grid_enriched"},
                {"ads", "driver_behavior_summary"},
                {"ads", "driver_rest_enriched"},
        };

        for (String[] t : targets) {
            String schema = t[0];
            String table = t[1];
            try {
                Long rowCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + schema + "." + table, Long.class);
                Integer fieldCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name=?",
                        Integer.class, schema, table);

                String firstCol = jdbcTemplate.queryForObject(
                        "SELECT column_name FROM information_schema.columns WHERE table_schema=? AND table_name=? ORDER BY ordinal_position LIMIT 1",
                        String.class, schema, table);
                Double completeness = 100.0;
                try {
                    long nonNull = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM " + schema + "." + table + " WHERE \"" + firstCol + "\" IS NOT NULL",
                            Long.class);
                    if (rowCount != null && rowCount > 0) {
                        completeness = Math.round(nonNull * 10000.0 / rowCount) / 100.0;
                    }
                } catch (Exception ignored) {}

                String status = completeness >= 95 ? "healthy" : completeness >= 90 ? "warning" : "error";

                tables.add(DataQualityDTO.builder()
                        .tableName(table)
                        .schema(schema)
                        .rowCount(rowCount != null ? rowCount : 0L)
                        .fieldCount(fieldCount != null ? fieldCount : 0)
                        .lastUpdated("2025-01-01T00:00:00")
                        .completenessPct(completeness)
                        .status(status)
                        .build());
            } catch (Exception e) {
                log.warn("Cannot get quality for {}.{}: {}", schema, table, e.getMessage());
            }
        }

        return QualityResponse.builder().tables(tables).build();
    }

    @Override
    public TrajectoryResponse getTrajectory(TrajectoryRequest request) {
        // P3 trajectory — 简化实现，直接返回空
        return TrajectoryResponse.builder().points(List.of()).build();
    }

    // --- P3 helpers ---

    private String buildCondition(QueryRequest.FilterCondition filter) {
        String field = filter.getField();
        String operator = filter.getOperator();
        Object value = filter.getValue();

        return switch (operator) {
            case "=" -> field + " = '" + value + "'";
            case "!=" -> field + " != '" + value + "'";
            case ">" -> field + " > " + value;
            case ">=" -> field + " >= " + value;
            case "<" -> field + " < " + value;
            case "<=" -> field + " <= " + value;
            case "LIKE" -> field + " LIKE '%" + value + "%'";
            case "IN" -> field + " IN (" + value + ")";
            default -> throw new IllegalArgumentException("Unsupported operator: " + operator);
        };
    }

    private List<String> getTableColumns(String schema, String tableName) {
        String sql = """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = ? AND table_name = ?
            ORDER BY ordinal_position
            """;
        return jdbcTemplate.queryForList(sql, String.class, schema, tableName);
    }

    private List<Map<String, Object>> buildBuiltinLineage() {
        String[][] rels = {
                // P1: 拥堵链路 ODS → DW → TDM → ADS
                {"ods", "ods_taxi_trips_raw", "dw", "fact_congestion_seg_hour", "ETL聚合"},
                {"dw", "fact_congestion_seg_hour", "tdm", "congestion_baseline_5day", "5天滑动窗口"},
                {"tdm", "congestion_baseline_5day", "ads", "congestion_by_segment_hour", "指标增强"},
                // P2-热点链路 ODS → TDM → ADS
                {"ods", "ods_taxi_trips_raw", "tdm", "grid_hotspot_score", "空间聚合"},
                {"tdm", "grid_hotspot_score", "ads", "hotspot_grid_enriched", "字段丰富"},
                // P2-司机链路 ODS → TDM → ADS
                {"ods", "ods_taxi_trips_raw", "tdm", "driver_shift_pattern", "模式识别"},
                {"tdm", "driver_shift_pattern", "ads", "driver_behavior_summary", "行为统计"},
                {"tdm", "driver_shift_pattern", "ads", "driver_rest_enriched", "休息点推断"},
        };
        List<Map<String, Object>> rows = new ArrayList<>();
        for (String[] r : rels) {
            Map<String, Object> map = new LinkedHashMap<>();
            map.put("source_schema", r[0]);
            map.put("source_table", r[1]);
            map.put("target_schema", r[2]);
            map.put("target_table", r[3]);
            map.put("relationship_type", r[4]);
            rows.add(map);
        }
        return rows;
    }

    private String getLayerName(String schema) {
        return switch (schema) {
            case "ods" -> "ODS";
            case "dw" -> "DW";
            case "tdm" -> "TDM";
            case "ads" -> "ADS";
            default -> schema.toUpperCase();
        };
    }

    private String formatTableLabel(String schema, String table) {
        return switch (schema) {
            case "ods" -> "GPS行程记录";
            case "dw" -> "拥堵事实表";
            case "tdm" -> getTdmLabel(table);
            case "ads" -> getAdsLabel(table);
            default -> table;
        };
    }

    private String getTdmLabel(String table) {
        return switch (table) {
            case "congestion_baseline_5day" -> "拥堵基线指标";
            case "driver_shift_pattern" -> "司机排班模式";
            case "grid_hotspot_score" -> "热点网格评分";
            default -> table;
        };
    }

    private String getAdsLabel(String table) {
        return switch (table) {
            case "congestion_by_segment_hour" -> "拥堵路段小时";
            case "hotspot_grid_enriched" -> "热点网格丰富";
            case "driver_behavior_summary" -> "司机行为统计";
            case "driver_rest_enriched" -> "司机休息推断";
            default -> table;
        };
    }
}

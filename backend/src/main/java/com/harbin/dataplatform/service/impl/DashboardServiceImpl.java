package com.harbin.dataplatform.service.impl;

import com.harbin.dataplatform.dto.*;
import com.harbin.dataplatform.service.DashboardService;
import com.harbin.dataplatform.service.TaxiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private static final DateTimeFormatter TRAJECTORY_TIME =
            DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss");

    private final JdbcTemplate jdbcTemplate;
    private final TaxiService taxiService;

    @Override
    public List<CatalogTableDTO> getCatalogTables() {
        String sql = """
            SELECT
                'ads' AS schema_name,
                t.table_name,
                COALESCE((SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'ads' AND table_name = t.table_name), 0) AS field_count,
                COALESCE(pg_total_relation_size('ads.' || t.table_name), 0) AS table_size_bytes
            FROM information_schema.tables t
            WHERE t.table_schema = 'ads' AND t.table_type = 'BASE TABLE'
            ORDER BY t.table_name
            """;

        return jdbcTemplate.query(sql, (rs) -> {
            List<CatalogTableDTO> result = new ArrayList<>();
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                String fullTable = "ads." + tableName;
                try {
                    Long rowCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM " + fullTable, Long.class);
                    result.add(CatalogTableDTO.builder()
                            .schema("ads")
                            .tableName(tableName)
                            .rowCount(rowCount)
                            .fieldCount(rs.getInt("field_count"))
                            .build());
                } catch (Exception e) {
                    log.warn("Failed to get row count for table {}: {}", tableName, e.getMessage());
                }
            }
            return result;
        });
    }

    @Override
    public CatalogFieldsResponse getCatalogFields(String tableName) {
        if (!tableName.matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name");
        }

        String fullTable = "ads." + tableName;

        String fieldSql = """
            SELECT
                column_name,
                data_type,
                is_nullable,
                COALESCE(column_default, '') AS default_value
            FROM information_schema.columns
            WHERE table_schema = 'ads' AND table_name = ?
            ORDER BY ordinal_position
            """;

        List<FieldDetailDTO> fields = jdbcTemplate.query(fieldSql,
                (rs, rowNum) -> FieldDetailDTO.builder()
                        .name(rs.getString("column_name"))
                        .type(rs.getString("data_type"))
                        .nullable("YES".equals(rs.getString("is_nullable")))
                        .description("")
                        .build(),
                tableName);

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
        if (!request.getTableName().matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name");
        }

        int limit = Math.min(request.getLimit() != null ? request.getLimit() : 100, 1000);
        List<String> columns;
        StringBuilder sql = new StringBuilder("SELECT ");

        List<String> requestFields = request.getFields();
        if (requestFields == null || requestFields.isEmpty()) {
            sql.append("* FROM ads.").append(request.getTableName());
            columns = getTableColumns(request.getTableName());
        } else {
            columns = requestFields;
            sql.append(String.join(", ", requestFields)).append(" FROM ads.").append(request.getTableName());
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

        StringBuilder countSql = new StringBuilder("SELECT COUNT(*) FROM ads.").append(request.getTableName());
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            countSql.append(" WHERE ");
            countSql.append(request.getFilters().stream().map(this::buildCondition).collect(Collectors.joining(" AND ")));
        }
        Long totalRows = jdbcTemplate.queryForObject(countSql.toString(), Long.class);

        String queryId = "q_" + System.currentTimeMillis();
        logQuery(request.getTableName(), columns, request.getFilters(), rows.size(), queryId);

        return QueryResponse.builder()
                .columns(columns)
                .rows(rows)
                .totalRows(totalRows)
                .queryId(queryId)
                .build();
    }

    @Override
    public TrajectoryResponse getTrajectory(TrajectoryRequest request) {
        LocalDateTime start = LocalDateTime.parse(request.getStartTime(), TRAJECTORY_TIME);
        LocalDateTime end = LocalDateTime.parse(request.getEndTime(), TRAJECTORY_TIME);
        int cap = request.getLimit() != null ? request.getLimit() : 100;
        List<TrajectorySliceDTO> points = taxiService.getTrajectorySlice(
                start, end,
                request.getMinLon(), request.getMaxLon(), request.getMinLat(), request.getMaxLat()
        );
        if (points.size() > cap) {
            points = new ArrayList<>(points.subList(0, cap));
        }
        return TrajectoryResponse.builder().points(points).build();
    }

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

    private List<String> getTableColumns(String tableName) {
        String sql = """
            SELECT column_name
            FROM information_schema.columns
            WHERE table_schema = 'ads' AND table_name = ?
            ORDER BY ordinal_position
            """;
        return jdbcTemplate.queryForList(sql, String.class, tableName);
    }

    private void logQuery(String tableName, List<String> fields,
                          List<QueryRequest.FilterCondition> filters,
                          int rowCount, String queryId) {
        try {
            String sql = """
                INSERT INTO ads.data_field_access_log (query_id, table_name, field_name, query_time, row_count, user_id)
                VALUES (?, ?, ?, ?, 'dashboard_user')
                """;
            for (String field : fields) {
                jdbcTemplate.update(sql, queryId, tableName, field,
                        java.time.LocalDateTime.now().format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")), rowCount);
            }
        } catch (Exception e) {
            log.warn("Failed to log query: {}", e.getMessage());
        }
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
        // 1. 尝试从 ads.data_lineage 查询，若表不存在则使用内置血缘数据
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

        // 如果 DB 无数据，使用内置血缘关系
        if (lineageRows.isEmpty()) {
            lineageRows = buildBuiltinLineage();
        }

        // 2. 收集所有涉及的 schema.table
        Set<String> allTables = new LinkedHashSet<>();
        for (Map<String, Object> row : lineageRows) {
            allTables.add(row.get("source_schema") + "." + row.get("source_table"));
            allTables.add(row.get("target_schema") + "." + row.get("target_table"));
        }

        // 3. 为每张表查元数据
        List<LineageNodeDTO> nodes = new ArrayList<>();
        for (String fullTable : allTables) {
            String[] parts = fullTable.split("\\.");
            String schema = parts[0];
            String table = parts[1];

            long rowCount = 0;
            int fieldCount = 0;
            try {
                rowCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM " + schema + "." + table, Long.class);
            } catch (Exception e) {
                log.warn("Cannot count rows for {}.{}: {}", schema, table, e.getMessage());
            }
            try {
                fieldCount = jdbcTemplate.queryForObject(
                        "SELECT COUNT(*) FROM information_schema.columns WHERE table_schema=? AND table_name=?",
                        Integer.class, schema, table);
            } catch (Exception e) {
                log.warn("Cannot count fields for {}.{}: {}", schema, table, e.getMessage());
            }

            nodes.add(LineageNodeDTO.builder()
                    .id(table)
                    .label(formatTableLabel(schema, table))
                    .layer(getLayerName(schema))
                    .rowCount(rowCount)
                    .fieldCount(fieldCount)
                    .build());
        }

        // 4. 构建边
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

    private List<Map<String, Object>> buildBuiltinLineage() {
        String[][] rels = {
                {"ods", "ods_taxi_trips_raw", "dw", "fact_congestion_seg_hour", "ETL聚合"},
                {"ods", "ods_taxi_trips_raw", "tdm", "driver_shift_pattern", "模式识别"},
                {"ods", "ods_taxi_trips_raw", "tdm", "grid_hotspot_score", "空间聚合"},
                {"dw", "fact_congestion_seg_hour", "tdm", "congestion_baseline_5day", "5天滑动窗口"},
                {"tdm", "congestion_baseline_5day", "ads", "congestion_by_segment_hour", "指标增强"},
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
            case "ods" -> table.contains("trip") ? "GPS行程记录" : "原始数据";
            case "dw" -> "拥堵事实表";
            case "tdm" -> getTdmLabel(table);
            case "ads" -> "应用展示数据";
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

    @Override
    public QualityResponse getQuality() {
        // 动态查询各层核心表的质量信息
        List<DataQualityDTO> tables = new ArrayList<>();

        String[][] targets = {
                {"ods", "ods_taxi_trips_raw"},
                {"dw", "fact_congestion_seg_hour"},
                {"tdm", "congestion_baseline_5day"},
                {"tdm", "driver_shift_pattern"},
                {"tdm", "grid_hotspot_score"},
                {"ads", "ads_congestion_by_segment_hour"},
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

                // 计算完整度：取非 NULL 行占比（抽样第一列）
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
}

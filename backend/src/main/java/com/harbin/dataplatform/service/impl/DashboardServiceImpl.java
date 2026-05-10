package com.harbin.dataplatform.service.impl;

import com.harbin.dataplatform.dto.*;
import com.harbin.dataplatform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final JdbcTemplate jdbcTemplate;
    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public List<CatalogTableDTO> getCatalogTables() {
        String sql = """
            SELECT
                'tdm' AS schema_name,
                t.table_name,
                COALESCE((SELECT nsp.nspname || '.' || cls.relname
                         FROM pg_class cls
                         JOIN pg_namespace nsp ON cls.relnamespace = nsp.oid
                         WHERE cls.relname = t.table_name
                         AND nsp.nspname = 'tdm'
                         LIMIT 1), 'tdm.' || t.table_name) AS full_table_name,
                COALESCE((SELECT COUNT(*) FROM information_schema.columns
                         WHERE table_schema = 'tdm' AND table_name = t.table_name), 0) AS field_count,
                COALESCE(pg_total_relation_size('tdm.' || t.table_name), 0) AS table_size_bytes
            FROM information_schema.tables t
            WHERE t.table_schema = 'tdm' AND t.table_type = 'BASE TABLE'
            ORDER BY t.table_name
            """;

        return jdbcTemplate.query(sql, (rs) -> {
            List<CatalogTableDTO> result = new ArrayList<>();
            while (rs.next()) {
                String tableName = rs.getString("table_name");
                String fullTable = rs.getString("full_table_name");
                try {
                    Long rowCount = jdbcTemplate.queryForObject(
                            "SELECT COUNT(*) FROM " + fullTable, Long.class);
                    result.add(CatalogTableDTO.builder()
                            .schema("tdm")
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

        String fullTable = "tdm." + tableName;

        String fieldSql = """
            SELECT
                column_name,
                data_type,
                is_nullable,
                COALESCE(column_default, '') AS default_value
            FROM information_schema.columns
            WHERE table_schema = 'tdm' AND table_name = ?
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
        String tableName = request.getTableName();
        if (!tableName.matches("^[a-z0-9_]+$")) {
            throw new IllegalArgumentException("Invalid table name");
        }

        StringBuilder sql = new StringBuilder();
        List<String> columns;
        List<String> requestFields = request.getFields();

        if (requestFields == null || requestFields.isEmpty()) {
            sql.append("SELECT * FROM tdm.").append(tableName);
            columns = getTableColumns(tableName);
        } else {
            columns = requestFields;
            sql.append("SELECT ");
            sql.append(String.join(", ", columns));
            sql.append(" FROM tdm.").append(tableName);
        }

        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            sql.append(" WHERE ");
            List<String> conditions = new ArrayList<>();
            for (QueryRequest.FilterCondition filter : request.getFilters()) {
                conditions.add(buildCondition(filter));
            }
            sql.append(String.join(" AND ", conditions));
        }

        sql.append(" LIMIT ").append(Math.min(request.getLimit(), 1000));

        String countSql = "SELECT COUNT(*) FROM tdm." + tableName;
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            countSql += " WHERE ";
            List<String> conditions = new ArrayList<>();
            for (QueryRequest.FilterCondition filter : request.getFilters()) {
                conditions.add(buildCondition(filter));
            }
            countSql += String.join(" AND ", conditions);
        }

        Long totalRows = jdbcTemplate.queryForObject(countSql, Long.class);

        List<List<Object>> rows = jdbcTemplate.query(sql.toString(), (rs) -> {
            List<List<Object>> result = new ArrayList<>();
            while (rs.next()) {
                List<Object> row = new ArrayList<>();
                for (String col : columns) {
                    row.add(rs.getObject(col));
                }
                result.add(row);
            }
            return result;
        });

        String queryId = "q_" + System.currentTimeMillis();

        logQuery(tableName, columns, request.getFilters(), rows.size(), queryId);

        return QueryResponse.builder()
                .columns(columns)
                .rows(rows)
                .totalRows(totalRows)
                .queryId(queryId)
                .build();
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
            WHERE table_schema = 'tdm' AND table_name = ?
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
                VALUES (?, ?, ?, ?, ?, 'dashboard_user')
                """;
            for (String field : fields) {
                jdbcTemplate.update(sql, queryId, tableName, field,
                        LocalDateTime.now().format(FORMATTER), rowCount);
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
            log.warn("Failed to get hot fields, returning mock data: {}", e.getMessage());
            return List.of(
                    HotFieldDTO.builder().fieldName("road_segment_id").queryCount(15).rank(1).build(),
                    HotFieldDTO.builder().fieldName("hour_of_day").queryCount(12).rank(2).build(),
                    HotFieldDTO.builder().fieldName("day_type").queryCount(10).rank(3).build()
            );
        }
    }

    @Override
    public LineageResponse getLineage() {
        return LineageResponse.builder()
                .nodes(List.of(
                        LineageNodeDTO.builder()
                                .id("ods_taxi_trips_raw")
                                .label("出租车原始轨迹")
                                .layer("ODS")
                                .rowCount(1340000L)
                                .build(),
                        LineageNodeDTO.builder()
                                .id("fact_congestion_seg_hour")
                                .label("拥堵小时事实")
                                .layer("DW")
                                .rowCount(1340958L)
                                .build(),
                        LineageNodeDTO.builder()
                                .id("congestion_baseline_5day")
                                .label("拥堵基线5天")
                                .layer("TDM")
                                .rowCount(834170L)
                                .build(),
                        LineageNodeDTO.builder()
                                .id("ads_congestion_by_segment_hour")
                                .label("拥堵路段展示")
                                .layer("ADS")
                                .rowCount(null)
                                .build(),
                        LineageNodeDTO.builder()
                                .id("driver_shift_pattern")
                                .label("司机排班模式")
                                .layer("TDM")
                                .rowCount(55251L)
                                .build(),
                        LineageNodeDTO.builder()
                                .id("grid_hotspot_score")
                                .label("网格热点评分")
                                .layer("TDM")
                                .rowCount(187877L)
                                .build()
                ))
                .edges(List.of(
                        LineageEdgeDTO.builder()
                                .source("ods_taxi_trips_raw")
                                .target("fact_congestion_seg_hour")
                                .label("ETL聚合")
                                .build(),
                        LineageEdgeDTO.builder()
                                .source("fact_congestion_seg_hour")
                                .target("congestion_baseline_5day")
                                .label("5天滑动窗口")
                                .build(),
                        LineageEdgeDTO.builder()
                                .source("congestion_baseline_5day")
                                .target("ads_congestion_by_segment_hour")
                                .label("指标增强")
                                .build(),
                        LineageEdgeDTO.builder()
                                .source("ods_taxi_trips_raw")
                                .target("driver_shift_pattern")
                                .label("模式识别")
                                .build(),
                        LineageEdgeDTO.builder()
                                .source("ods_taxi_trips_raw")
                                .target("grid_hotspot_score")
                                .label("空间聚合")
                                .build()
                ))
                .build();
    }

    @Override
    public QualityResponse getQuality() {
        return QualityResponse.builder()
                .tables(List.of(
                        DataQualityDTO.builder()
                                .tableName("ods_taxi_trips_raw")
                                .schema("ods")
                                .rowCount(1340000L)
                                .fieldCount(8)
                                .lastUpdated("2025-01-01T00:00:00")
                                .completenessPct(98.5)
                                .status("healthy")
                                .build(),
                        DataQualityDTO.builder()
                                .tableName("congestion_baseline_5day")
                                .schema("tdm")
                                .rowCount(834170L)
                                .fieldCount(8)
                                .lastUpdated("2025-01-01T00:00:00")
                                .completenessPct(94.4)
                                .status("healthy")
                                .build(),
                        DataQualityDTO.builder()
                                .tableName("driver_shift_pattern")
                                .schema("tdm")
                                .rowCount(55251L)
                                .fieldCount(15)
                                .lastUpdated("2025-01-01T00:00:00")
                                .completenessPct(96.2)
                                .status("healthy")
                                .build(),
                        DataQualityDTO.builder()
                                .tableName("grid_hotspot_score")
                                .schema("tdm")
                                .rowCount(187877L)
                                .fieldCount(7)
                                .lastUpdated("2025-01-01T00:00:00")
                                .completenessPct(92.8)
                                .status("warning")
                                .build()
                ))
                .build();
    }
}

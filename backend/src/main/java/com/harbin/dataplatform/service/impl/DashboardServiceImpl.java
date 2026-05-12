package com.harbin.dataplatform.service.impl;

import com.harbin.dataplatform.dto.*;
import com.harbin.dataplatform.service.DashboardService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class DashboardServiceImpl implements DashboardService {

    private final JdbcTemplate jdbcTemplate;

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
        List<String> columns;
        List<String> requestFields = request.getFields();

        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");

        if (requestFields == null || requestFields.isEmpty()) {
            sql.append("* FROM ads.").append(request.getTableName());
            columns = getTableColumns(request.getTableName());
        } else {
            columns = requestFields;
            sql.append("SELECT ");
            sql.append(String.join(", ", requestFields));
            sql.append(" FROM ads.").append(request.getTableName());
        }

        List<String> conditions = new ArrayList<>();
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            for (QueryRequest.FilterCondition filter : request.getFilters()) {
                conditions.add(buildCondition(filter));
            }
            sql.append(" WHERE ");
            sql.append(String.join(" AND ", conditions));
        }

        sql.append(" LIMIT ").append(Math.min(request.getLimit(), 1000));

        String countSql = "SELECT COUNT(*) FROM ads." + request.getTableName();
        if (request.getFilters() != null && !request.getFilters().isEmpty()) {
            countSql += " WHERE ";
            List<String> conditions = new ArrayList<>();
            for (QueryRequest.FilterCondition filter : request.getFilters()) {
                conditions.add(buildCondition(filter));
            }
            countSql += String.join(" AND ", conditions);
        }

        Object[] params = new Object[request.getFilters() != null ? request.getFilters().size() + 1 : 0];
        int paramIndex = 0;
        if (request.getStartTime() != null) {
            params[paramIndex++] = request.getStartTime();
        }
        if (request.getEndTime() != null) {
            params[paramIndex++] = request.getEndTime();
        }
        if (request.getFilters() != null) {
            for (QueryRequest.FilterCondition filter : request.getFilters()) {
                Object value = filter.getValue();
                if (value instanceof Double || value instanceof Integer) {
                    params[paramIndex++] = value;
                } else if (value instanceof String) {
                    params[paramIndex++] = value;
                }
            }
        }
        params[params.length - 1] = request.getLimit();

        Long totalRows = jdbcTemplate.queryForObject(countSql, Long.class, params);

        List<List<Object>> rows = jdbcTemplate.query(sql.toString(), params);

        String queryId = "q_" + System.currentTimeMillis();

        logQuery(request.getTableName(), columns, request.getFilters(), rows.size(), queryId);

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
        String sql = """
            SELECT
                vl.source_table || '.' || vl.source_schema AS source_id,
                vl.target_table || '.' || vl.target_schema AS target_id,
                vl.relationship_type,
                vl.source_row_count,
                vl.target_row_count
            FROM ads.vw_data_lineage vl
            ORDER BY vl.relationship_type
            """;

        List<Map<String, Object>> lineageRows = jdbcTemplate.queryForList(sql);

        Set<String> allTables = new HashSet<>();
        for (Map<String, Object> row : lineageRows) {
            String source = (String) row.get("source_id");
            String target = (String) row.get("target_id");
            allTables.add(source);
            allTables.add(target);
        }

        String tableSql = """
            SELECT
                schemaname || '.' || tablename AS full_name,
                schemaname AS schema_name,
                tablename AS table_name,
                COALESCE((SELECT COUNT(*) FROM schemaname || '.' || tablename), 0) AS row_count
            FROM information_schema.tables
            WHERE (schemaname || '.' || tablename) IN (%s)
            AND table_type = 'BASE TABLE'
            ORDER BY schemaname, tablename
            """.formatted(allTables.stream()
                    .map(t -> "'" + t + "'")
                    .collect(Collectors.join(", ")));

        List<Map<String, Object>> tableRows = jdbcTemplate.queryForList(tableSql);

        Map<String, Map<String, Object>> tableMap = tableRows.stream()
                .collect(Collectors.toMap(
                        row -> (String) row.get("full_name"),
                        row -> row
                ));

        List<LineageNodeDTO> nodes = tableMap.entrySet().stream()
                .map(entry -> {
                    String schema = (String) entry.getValue().get("schema_name");
                    String tableName = (String) entry.getValue().get("table_name");
                    return LineageNodeDTO.builder()
                            .id(tableName)
                            .label(formatTableLabel(schema, tableName))
                            .layer(getLayerName(schema))
                            .rowCount(((Number) entry.getValue().get("row_count")).longValue())
                            .build();
                })
                .collect(Collectors.toList());

        List<LineageEdgeDTO> edges = lineageRows.stream()
                .map(row -> {
                    String source = extractTableName((String) row.get("source_id"));
                    String target = extractTableName((String) row.get("target_id"));
                    return LineageEdgeDTO.builder()
                            .source(source)
                            .target(target)
                            .label((String) row.get("relationship_type"))
                            .build();
                })
                .collect(Collectors.toList());

        return LineageResponse.builder()
                .nodes(nodes)
                .edges(edges)
                .build();
    }

    private String extractTableName(String fullId) {
        return fullId.contains(".") ? fullId.substring(fullId.lastIndexOf(".") + 1) : fullId;
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
            case "dw" -> "事实数据";
            case "tdm" -> getTdmLabel(table);
            case "ads" -> "大屏展示数据";
            default -> table;
        };
    }

    private String getTdmLabel(String table) {
        return switch (table) {
            case "congestion_baseline_5day" -> "拥堵基线指标";
            case "driver_shift_pattern" -> "司机班次特征";
            case "grid_hotspot_score" -> "热点区域评分";
            default -> table;
        };
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

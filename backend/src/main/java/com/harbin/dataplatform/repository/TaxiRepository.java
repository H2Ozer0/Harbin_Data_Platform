package com.harbin.dataplatform.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class TaxiRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public TaxiRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findByTimeRange(
            double startTms, double endTms, int limit, int offset
    ) {
        String sql = "SELECT id, devid, " +
                "array_to_string(lon_seq, ',') AS lon_seq, " +
                "array_to_string(lat_seq, ',') AS lat_seq, " +
                "array_to_string(tms_seq::text, ',') AS tms_seq, " +
                "(tms_seq)[1] AS start_tms, " +
                "(tms_seq)[array_length(tms_seq, 1)] AS end_tms " +
                "FROM ods.ods_taxi_trips_raw " +
                "WHERE (tms_seq)[1] >= ? " +
                "  AND (tms_seq)[1] <= ? " +
                "ORDER BY (tms_seq)[1] " +
                "LIMIT ? OFFSET ?";
        return jdbcTemplate.queryForList(sql, startTms, endTms, limit, offset);
    }

    public long countByTimeRange(double startTms, double endTms) {
        String sql = "SELECT COUNT(*) FROM ods.ods_taxi_trips_raw " +
                "WHERE (tms_seq)[1] >= ? AND (tms_seq)[1] <= ?";
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, startTms, endTms);
        return count != null ? count : 0;
    }

    public List<Map<String, Object>> findAllPaginated(int limit, int offset) {
        String sql = "SELECT id, devid, " +
                "array_to_string(lon_seq, ',') AS lon_seq, " +
                "array_to_string(lat_seq, ',') AS lat_seq, " +
                "array_to_string(tms_seq::text, ',') AS tms_seq " +
                "FROM ods.ods_taxi_trips_raw " +
                "ORDER BY id " +
                "LIMIT ? OFFSET ?";
        return jdbcTemplate.queryForList(sql, limit, offset);
    }

    public Map<String, Object> findById(Long id) {
        String sql = "SELECT id, devid, " +
                "array_to_string(lon_seq, ',') AS lon_seq, " +
                "array_to_string(lat_seq, ',') AS lat_seq, " +
                "array_to_string(tms_seq::text, ',') AS tms_seq " +
                "FROM ods.ods_taxi_trips_raw WHERE id = ?";
        List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql, id);
        return rows.isEmpty() ? null : rows.get(0);
    }

    public List<Map<String, Object>> findTrajectorySlice(
            double startTms, double endTms,
            double minLon, double maxLon, double minLat, double maxLat,
            int maxPoints
    ) {
        String sql = "SELECT t.id, t.devid, " +
                "       p.idx, " +
                "       p.lon, " +
                "       p.lat, " +
                "       p.tms " +
                "FROM ods.ods_taxi_trips_raw t, " +
                "LATERAL unnest(t.lon_seq, t.lat_seq, t.tms_seq) " +
                "  WITH ORDINALITY AS p(lon, lat, tms, idx) " +
                "WHERE p.tms >= ? " +
                "  AND p.tms <= ? " +
                "  AND p.lon >= ? " +
                "  AND p.lon <= ? " +
                "  AND p.lat >= ? " +
                "  AND p.lat <= ? " +
                "LIMIT ?";
        return jdbcTemplate.queryForList(sql,
                startTms, endTms, minLon, maxLon, minLat, maxLat, maxPoints);
    }
}

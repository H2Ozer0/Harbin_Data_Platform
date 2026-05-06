package com.harbin.dataplatform.repository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    @Autowired
    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<Map<String, Object>> findHotspotGrids(String dt, int hour, String eventType) {
        String sql = """
            SELECT grid_id, lon, lat, event_count, hotspot_score, rank_in_hour, event_type
            FROM ads.hotspot_grid_enriched
            WHERE dt = ?::date AND hour_of_day = ? AND event_type = ?
            ORDER BY rank_in_hour ASC
        """;
        return jdbcTemplate.queryForList(sql, dt, hour, eventType);
    }

    public long countHotspotGrids(String dt, int hour, String eventType) {
        String sql = """
            SELECT COUNT(*)
            FROM ads.hotspot_grid_enriched
            WHERE dt = ?::date AND hour_of_day = ? AND event_type = ?
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, dt, hour, eventType);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> findDriverBehavior(String dt) {
        String sql = """
            SELECT shift_pattern, driver_count, avg_active_minutes
            FROM ads.driver_behavior_summary
            WHERE dt = ?::date
            ORDER BY driver_count DESC
        """;
        return jdbcTemplate.queryForList(sql, dt);
    }

    public long countDrivers(String dt) {
        String sql = """
            SELECT SUM(driver_count)
            FROM ads.driver_behavior_summary
            WHERE dt = ?::date
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, dt);
        return count == null ? 0 : count;
    }

    public List<Map<String, Object>> findRestLocations(String dt) {
        String sql = """
            SELECT r.devid, r.lon, r.lat, r.rest_minutes, r.rest_start, s.shift_pattern
            FROM tdm.driver_rest_location r
            LEFT JOIN tdm.driver_shift_pattern s
              ON r.devid = s.devid AND r.dt = s.dt
                        WHERE r.dt = ?::date
        """;
        return jdbcTemplate.queryForList(sql, dt);
    }

    public long countRestLocations(String dt) {
        String sql = """
            SELECT COUNT(*)
            FROM tdm.driver_rest_location
            WHERE dt = ?::date
        """;
        Integer count = jdbcTemplate.queryForObject(sql, Integer.class, dt);
        return count == null ? 0 : count;
    }
}

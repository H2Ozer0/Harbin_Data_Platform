package com.harbin.dataplatform.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public class DashboardRepository {

    private final JdbcTemplate jdbcTemplate;

    public DashboardRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /**
     * 拥堵热力图：查询指定日期+小时+日类型的路段级拥堵数据
     */
    public List<Map<String, Object>> findHeatmapByDtHourAndDayType(String dt, int hour, String dayType) {
        String sql = """
            SELECT
                c.road_segment_id,
                c.road_name,
                c.avg_speed_kmh,
                c.congestion_index,
                c.deviation_pct,
                f.trip_count,
                ST_AsGeoJSON(r.geom) AS geometry
            FROM ads.congestion_by_segment_hour c
            JOIN dw.dim_road_segment r ON r.road_segment_id = c.road_segment_id
            JOIN dw.fact_congestion_seg_hour f
              ON f.road_segment_id = c.road_segment_id
             AND f.dt = c.dt
             AND f.hour_of_day = c.hour_of_day
            WHERE c.dt = ?::date
              AND c.hour_of_day = ?
              AND c.day_type = ?
              AND f.trip_count >= 10
              AND r.length_m >= 100
            ORDER BY c.congestion_index DESC
        """;
        return jdbcTemplate.queryForList(sql, dt, hour, dayType);
    }

    /**
     * KPI：查询指定日期的关键指标
     */
    public List<Map<String, Object>> findKPIByDt(String dt) {
        String sql = """
            WITH daily_stats AS (
                SELECT
                    COUNT(DISTINCT CONCAT(c.road_segment_id, '_', c.hour_of_day)) AS total_trips,
                    AVG(c.avg_speed_kmh) AS avg_speed_kmh,
                    AVG(c.congestion_index) AS avg_congestion
                FROM ads.congestion_by_segment_hour c
                WHERE c.dt = ?::date
            ),
            top_congested AS (
                SELECT c.road_name, c.congestion_index
                FROM ads.congestion_by_segment_hour c
                WHERE c.dt = ?::date AND c.road_name IS NOT NULL
                ORDER BY c.congestion_index DESC
                LIMIT 5
            ),
            most_active_hour AS (
                SELECT c.hour_of_day
                FROM ads.congestion_by_segment_hour c
                WHERE c.dt = ?::date
                GROUP BY c.hour_of_day
                ORDER BY COUNT(*) DESC
                LIMIT 1
            ),
            vehicle_count AS (
                SELECT COUNT(DISTINCT t.devid) AS total_vehicles
                FROM ods.ods_taxi_trips_raw t
                WHERE (t.tms_seq)[1] >= EXTRACT(EPOCH FROM (?::date::timestamp))
                  AND (t.tms_seq)[1] < EXTRACT(EPOCH FROM (?::date::timestamp + INTERVAL '1 day'))
            )
            SELECT
                COALESCE(v.total_vehicles, 0) AS total_vehicles,
                COALESCE(s.total_trips, 0) AS total_trips,
                s.avg_speed_kmh,
                h.hour_of_day AS most_active_hour
            FROM daily_stats s
            CROSS JOIN most_active_hour h
            CROSS JOIN vehicle_count v
        """;
        return jdbcTemplate.queryForList(sql, dt, dt, dt, dt, dt);
    }

    /**
     * 获取拥堵路段Top5（供KPI使用）
     */
    public List<Map<String, Object>> findTop5CongestedByDt(String dt) {
        String sql = """
            SELECT c.road_name, c.congestion_index
            FROM ads.congestion_by_segment_hour c
            WHERE c.dt = ?::date AND c.road_name IS NOT NULL
            ORDER BY c.congestion_index DESC
            LIMIT 5
        """;
        return jdbcTemplate.queryForList(sql, dt);
    }

    /**
     * 5天拥堵趋势：按日期聚合
     */
    public List<Map<String, Object>> findTrendByDateRange(String startDt, String endDt) {
        String sql = """
            SELECT
                c.dt::text,
                AVG(c.congestion_index) AS avg_congestion_index,
                AVG(c.avg_speed_kmh) AS avg_speed_kmh,
                COUNT(*) AS total_trips
            FROM ads.congestion_by_segment_hour c
            WHERE c.dt >= ?::date AND c.dt <= ?::date
            GROUP BY c.dt
            ORDER BY c.dt
        """;
        return jdbcTemplate.queryForList(sql, startDt, endDt);
    }

    /**
     * 日类型对比：工作日 vs 节假日 vs 调休工作日，按小时聚合
     */
    public List<Map<String, Object>> findComparisonByHour() {
        String sql = """
            SELECT
                c.hour_of_day,
                AVG(CASE WHEN c.day_type = 'workday' THEN c.avg_speed_kmh END) AS workday_avg_speed,
                AVG(CASE WHEN c.day_type IN ('holiday', 'weekend') THEN c.avg_speed_kmh END) AS holiday_avg_speed,
                AVG(CASE WHEN c.day_type = 'makeup_workday' THEN c.avg_speed_kmh END) AS makeup_workday_avg_speed,
                AVG(CASE WHEN c.day_type = 'workday' THEN c.congestion_index END) AS workday_congestion,
                AVG(CASE WHEN c.day_type IN ('holiday', 'weekend') THEN c.congestion_index END) AS holiday_congestion,
                AVG(CASE WHEN c.day_type = 'makeup_workday' THEN c.congestion_index END) AS makeup_workday_congestion
            FROM ads.congestion_by_segment_hour c
            GROUP BY c.hour_of_day
            ORDER BY c.hour_of_day
        """;
        return jdbcTemplate.queryForList(sql);
    }

    /**
     * 道路类型 × 24h 速度曲线：按道路类型和小时聚合平均速度
     */
    public List<Map<String, Object>> findRoadTypeSpeedByHour(String dt) {
        String sql = """
            SELECT
                c.hour_of_day,
                c.road_type,
                ROUND(AVG(c.avg_speed_kmh)::numeric, 1) AS avg_speed,
                COUNT(*) AS segment_count
            FROM ads.congestion_by_segment_hour c
            JOIN dw.fact_congestion_seg_hour f
              ON f.road_segment_id = c.road_segment_id
             AND f.dt = c.dt
             AND f.hour_of_day = c.hour_of_day
            WHERE c.dt = ?::date
              AND c.road_type IN ('trunk', 'primary', 'secondary', 'residential')
              AND f.trip_count >= 10
            GROUP BY c.hour_of_day, c.road_type
            ORDER BY c.hour_of_day, c.road_type
        """;
        return jdbcTemplate.queryForList(sql, dt);
    }

    /**
     * 拥堵持续时间排行：统计每条路段在指定日期有多少小时处于拥堵状态
     * 拥堵定义：deviation_pct < -20% 且 avg_speed_kmh < 20
     */
    public List<Map<String, Object>> findCongestionDurationRanking(String dt) {
        String sql = """
            SELECT
                c.road_segment_id,
                c.road_name,
                c.road_type,
                COUNT(*) AS congestion_hours,
                ROUND(AVG(c.avg_speed_kmh)::numeric, 1) AS avg_speed_when_congested,
                ROUND(MIN(c.deviation_pct)::numeric, 1) AS worst_deviation
            FROM ads.congestion_by_segment_hour c
            JOIN dw.fact_congestion_seg_hour f
              ON f.road_segment_id = c.road_segment_id
             AND f.dt = c.dt
             AND f.hour_of_day = c.hour_of_day
            WHERE c.dt = ?::date
              AND c.road_name IS NOT NULL
              AND c.deviation_pct < -20
              AND c.avg_speed_kmh < 20
              AND f.trip_count >= 10
            GROUP BY c.road_segment_id, c.road_name, c.road_type
            ORDER BY congestion_hours DESC, avg_speed_when_congested ASC
            LIMIT 10
        """;
        return jdbcTemplate.queryForList(sql, dt);
    }
}
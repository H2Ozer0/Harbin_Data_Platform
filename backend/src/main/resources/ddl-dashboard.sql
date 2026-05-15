-- ADS Dashboard DDL
-- Drop 5 old ADS tables, create 7 new ADS tables

-- ============================================
-- DROP old ADS tables
-- ============================================
DROP TABLE IF EXISTS ads.ads_congestion_heatmap_ts;
DROP TABLE IF EXISTS ads.ads_driver_activity_ts;
DROP TABLE IF EXISTS ads.ads_driver_shift_pattern_day;
DROP TABLE IF EXISTS ads.ads_holiday_vs_workday_compare;
DROP TABLE IF EXISTS ads.ads_pickup_dropoff_hotspot_ts;

-- ============================================
-- CREATE new ADS tables
-- ============================================

-- 1. Congestion by segment and hour with baseline deviation
CREATE TABLE IF NOT EXISTS ads.congestion_by_segment_hour (
    dt              DATE,
    hour_of_day     INTEGER,
    road_segment_id BIGINT,
    road_name       TEXT,
    road_type       TEXT,
    avg_speed_kmh   DOUBLE PRECISION,
    p50_speed_kmh   DOUBLE PRECISION,
    p85_speed_kmh   DOUBLE PRECISION,
    congestion_index DOUBLE PRECISION,
    baseline_speed_kmh DOUBLE PRECISION,
    deviation_pct   DOUBLE PRECISION,
    day_type        TEXT,
    trip_count      INTEGER DEFAULT 0,
    length_m        DOUBLE PRECISION,
    geometry        geometry,
    PRIMARY KEY (dt, hour_of_day, road_segment_id)
);

-- 2. Dashboard KPI summary
CREATE TABLE IF NOT EXISTS ads.dashboard_kpi (
    dt              DATE,
    total_vehicles  INTEGER,
    total_trips     INTEGER,
    avg_speed_kmh   DOUBLE PRECISION,
    top5_congested  JSONB,
    most_active_hour INTEGER,
    created_at      TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (dt)
);

-- 3. Holiday vs workday comparison
CREATE TABLE IF NOT EXISTS ads.holiday_workday_comparison (
    hour_of_day          INTEGER,
    day_type             TEXT,
    avg_speed_kmh        DOUBLE PRECISION,
    avg_congestion_index DOUBLE PRECISION,
    total_trips          INTEGER,
    PRIMARY KEY (hour_of_day, day_type)
);

-- 4. Enriched hotspot grid with supply-demand imbalance
CREATE TABLE IF NOT EXISTS ads.hotspot_grid_enriched (
    dt                DATE,
    hour_of_day       INTEGER,
    event_type        TEXT,
    grid_id           TEXT,
    lon               DOUBLE PRECISION,
    lat               DOUBLE PRECISION,
    event_count       INTEGER,
    hotspot_score     DOUBLE PRECISION,
    rank_in_hour      INTEGER,
    supply_demand_ratio DOUBLE PRECISION,
    day_type          TEXT,
    PRIMARY KEY (dt, hour_of_day, event_type, grid_id)
);

-- 5. Driver behavior summary
CREATE TABLE IF NOT EXISTS ads.driver_behavior_summary (
    dt                DATE,
    shift_pattern     TEXT,
    pattern_score     DOUBLE PRECISION,
    avg_active_minutes DOUBLE PRECISION,
    avg_rest_minutes  DOUBLE PRECISION,
    rest_event_count  INTEGER,
    driver_count      INTEGER,
    created_at        TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (dt, shift_pattern)
);

-- 6. Data field access log
CREATE TABLE IF NOT EXISTS ads.data_field_access_log (
    id              SERIAL PRIMARY KEY,
    query_id        TEXT,
    table_name      TEXT,
    field_name      TEXT,
    queried_at      TIMESTAMP DEFAULT NOW(),
    user_identifier TEXT DEFAULT 'anonymous'
);

-- 7. Data field value score ranking
CREATE TABLE IF NOT EXISTS ads.data_field_value_score (
    id           SERIAL PRIMARY KEY,
    table_name   TEXT,
    field_name   TEXT,
    query_count  INTEGER DEFAULT 0,
    rank_value   INTEGER,
    updated_at   TIMESTAMP DEFAULT NOW()
);

-- 8. Driver rest location enriched (ADS 层替代 tdm 两表 JOIN)
CREATE TABLE IF NOT EXISTS ads.driver_rest_enriched (
    dt              DATE,
    devid           TEXT,
    lon             DOUBLE PRECISION,
    lat             DOUBLE PRECISION,
    rest_minutes    DOUBLE PRECISION,
    rest_start      TIMESTAMP,
    shift_pattern   TEXT,
    PRIMARY KEY (dt, devid, rest_start)
);

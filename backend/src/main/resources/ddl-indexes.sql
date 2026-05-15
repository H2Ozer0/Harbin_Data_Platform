-- Performance indexes for dashboard queries (ADS tables only)
-- Auto-executed on backend startup via DatabaseIndexInitializer

-- congestion_by_segment_hour: heatmap / KPI / trend filter
CREATE INDEX IF NOT EXISTS idx_congestion_dt_hour_daytype
    ON ads.congestion_by_segment_hour (dt, hour_of_day, day_type);

-- hotspot_grid_enriched: hotspot page filter
CREATE INDEX IF NOT EXISTS idx_hotspot_dt_hour_event
    ON ads.hotspot_grid_enriched (dt, hour_of_day, event_type);

-- driver_behavior_summary: driver behavior filter
CREATE INDEX IF NOT EXISTS idx_driver_behavior_dt
    ON ads.driver_behavior_summary (dt);

-- driver_rest_enriched: rest heatmap filter
CREATE INDEX IF NOT EXISTS idx_driver_rest_enriched_dt
    ON ads.driver_rest_enriched (dt);

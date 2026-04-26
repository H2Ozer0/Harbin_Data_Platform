-- Performance optimization for ods_taxi_trips_raw
-- Run these once to add indexes for time-range queries

-- Index on first timestamp element for time range filtering
CREATE INDEX IF NOT EXISTS idx_taxi_trips_start_tms
ON ods.ods_taxi_trips_raw (((tms_seq)[1]));

-- Index for trajectory slice: spatial bounding box on first point
CREATE INDEX IF NOT EXISTS idx_taxi_trips_first_point
ON ods.ods_taxi_trips_raw (((lon_seq)[1]), ((lat_seq)[1]));

-- ==========================================
-- 血缘关系表初始化
-- 用于存储数据表之间的依赖关系
-- ==========================================

-- 创建血缘关系表
CREATE TABLE IF NOT EXISTS ads.data_lineage (
    source_table VARCHAR(255) NOT NULL,
    source_schema VARCHAR(50) NOT NULL,
    target_table VARCHAR(255) NOT NULL,
    target_schema VARCHAR(50) NOT NULL,
    relationship_type VARCHAR(100) NOT NULL,
    created_at TIMESTAMP DEFAULT NOW(),
    PRIMARY KEY (source_table, source_schema, target_table, target_schema)
);

-- 插入初始血缘关系数据
INSERT INTO ads.data_lineage (source_table, source_schema, target_table, target_schema, relationship_type)
VALUES
-- ODS -> DW
('ods_taxi_trips_raw', 'ods', 'fact_congestion_seg_hour', 'dw', 'ETL聚合'),
-- ODS -> TDM
('ods_taxi_trips_raw', 'ods', 'driver_shift_pattern', 'tdm', '模式识别'),
('ods_taxi_trips_raw', 'ods', 'grid_hotspot_score', 'tdm', '空间聚合'),
-- DW -> TDM
('fact_congestion_seg_hour', 'dw', 'congestion_baseline_5day', 'tdm', '5天滑动窗口'),
-- TDM -> ADS
('congestion_baseline_5day', 'tdm', 'ads_congestion_by_segment_hour', 'ads', '指标增强')
ON CONFLICT DO NOTHING;

-- 查询血缘关系的视图（包含关系类型，行数由后端动态查询）
CREATE OR REPLACE VIEW ads.vw_data_lineage AS
SELECT
    sl.source_schema || '.' || sl.source_table AS source_id,
    sl.target_schema || '.' || sl.target_table AS target_id,
    sl.relationship_type
FROM ads.data_lineage sl
ORDER BY sl.source_schema, sl.source_table;

-- 创建字段访问日志表（用于热度统计）
CREATE TABLE IF NOT EXISTS ads.data_field_access_log (
    query_id VARCHAR(100) NOT NULL,
    table_name VARCHAR(255) NOT NULL,
    field_name VARCHAR(255) NOT NULL,
    query_time TIMESTAMP NOT NULL,
    row_count INTEGER NOT NULL,
    user_id VARCHAR(100),
    created_at TIMESTAMP DEFAULT NOW()
);

-- 创建索引优化查询
CREATE INDEX IF NOT EXISTS idx_lineage_source ON ads.data_lineage(source_table, source_schema);
CREATE INDEX IF NOT EXISTS idx_lineage_target ON ads.data_lineage(target_table, target_schema);
CREATE INDEX IF NOT EXISTS idx_access_log_field ON ads.data_field_access_log(field_name);
CREATE INDEX IF NOT EXISTS idx_access_log_time ON ads.data_field_access_log(query_time);

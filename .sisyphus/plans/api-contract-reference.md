# API 合同参考文档

> **用途**：此文档的最终版本应由 Sisyphus 写入 `docs/api-contract.md`。
> **当前状态**：已提前定义完成，P1/P2/P3 直接按此格式实现即可，无需讨论。

---

## 通用约定

- **基础路径**：`/api/dashboard`
- **响应格式**：统一 JSON，成功返回 HTTP 200 + body，失败返回 400/500 + 错误消息
- **日期格式**：`YYYY-MM-DD`（如 `2015-01-05`）
- **小时格式**：整数 0~23
- **坐标**：经度在前，纬度在后 `[lon, lat]`

---

## 1. 拥堵热力图

```
GET /api/dashboard/congestion/heatmap?dt=2015-01-05&hour=12&day_type=workday
```

**响应：**
```json
{
  "segments": [
    {
      "road_segment_id": 123,
      "road_name": "裕虹路",
      "avg_speed_kmh": 35.2,
      "congestion_index": 0.72,
      "deviation_pct": -15.3,
      "lon": 126.62,
      "lat": 45.75
    }
  ],
  "total": 1500
}
```

**P1 实现，P1 消费**

---

## 2. 关键指标

```
GET /api/dashboard/kpi?dt=2015-01-05
```

**响应：**
```json
{
  "dt": "2015-01-05",
  "total_vehicles": 1250,
  "total_trips": 8420,
  "avg_speed_kmh": 38.5,
  "top5_congested": [
    { "road_name": "裕虹路", "congestion_index": 0.92 },
    { "road_name": "延安高架路", "congestion_index": 0.88 }
  ],
  "most_active_hour": 17
}
```

**P1 实现，P1 + P3 消费**

---

## 3. 5 天趋势

```
GET /api/dashboard/congestion/trend?start_dt=2015-01-03&end_dt=2015-01-07
```

**响应：**
```json
{
  "daily": [
    { "dt": "2015-01-03", "avg_congestion_index": 0.35, "avg_speed_kmh": 45.2, "total_trips": 7200 }
  ]
}
```

**P1 实现，P1 消费**

---

## 4. 日类型对比

```
GET /api/dashboard/congestion/comparison
```

**响应：**
```json
{
  "hourly": [
    {
      "hour": 8,
      "workday_avg_speed": 32.1, "holiday_avg_speed": 45.2, "makeup_workday_avg_speed": 35.0,
      "workday_congestion": 0.78, "holiday_congestion": 0.45, "makeup_workday_congestion": 0.65
    }
  ]
}
```

**P1 实现，P1 消费**

---

## 5. 上下车热点地图

```
GET /api/dashboard/hotspot/map?dt=2015-01-05&hour=12&event_type=pickup
```

**响应：**
```json
{
  "grids": [
    {
      "grid_id": "126.65_45.775_500m",
      "lon": 126.65, "lat": 45.775,
      "event_count": 42, "hotspot_score": 0.85, "rank_in_hour": 1,
      "event_type": "pickup"
    }
  ],
  "total_grids": 120
}
```

**P2 实现，P2 消费**

---

## 6. 司机行为

```
GET /api/dashboard/driver/behavior?dt=2015-01-05
```

**响应：**
```json
{
  "shift_distribution": [
    { "pattern": "full_day", "driver_count": 320, "ratio": 0.35 },
    { "pattern": "dual_peak", "driver_count": 250, "ratio": 0.27 },
    { "pattern": "morning_peak", "driver_count": 180, "ratio": 0.20 },
    { "pattern": "evening_peak", "driver_count": 120, "ratio": 0.13 },
    { "pattern": "night_owl", "driver_count": 50, "ratio": 0.05 }
  ],
  "avg_active_minutes_by_pattern": [
    { "pattern": "full_day", "avg_minutes": 520 },
    { "pattern": "dual_peak", "avg_minutes": 480 },
    { "pattern": "morning_peak", "avg_minutes": 320 },
    { "pattern": "evening_peak", "avg_minutes": 350 },
    { "pattern": "night_owl", "avg_minutes": 420 }
  ],
  "total_drivers": 920
}
```

**P2 实现，P2 消费**

---

## 7. 司机休息地段

```
GET /api/dashboard/driver/rest-heatmap?dt=2015-01-05
```

**响应：**
```json
{
  "rest_locations": [
    {
      "devid": "TAXI_0001",
      "lon": 126.62, "lat": 45.75,
      "rest_minutes": 45,
      "rest_start": "2015-01-05T12:30:00",
      "shift_pattern": "full_day"
    }
  ],
  "total_rests": 1200
}
```

**P2 实现，P2 消费**

---

## 8. 数据目录 — 表列表

```
GET /api/dashboard/catalog/tables
```

**响应：**
```json
{
  "tables": [
    { "schema": "tdm", "table_name": "congestion_baseline_5day", "row_count": 834170, "field_count": 8 },
    { "schema": "tdm", "table_name": "driver_shift_pattern", "row_count": 55251, "field_count": 15 },
    { "schema": "tdm", "table_name": "grid_hotspot_score", "row_count": 187877, "field_count": 7 }
  ]
}
```

**P3 实现，P3 消费**

---

## 9. 数据目录 — 字段详情

```
GET /api/dashboard/catalog/fields/congestion_baseline_5day
```

**响应：**
```json
{
  "table": "congestion_baseline_5day",
  "fields": [
    { "name": "road_segment_id", "type": "bigint", "nullable": false, "description": "路段ID" },
    { "name": "hour_of_day", "type": "integer", "nullable": false, "description": "小时(0-23)" },
    { "name": "day_type", "type": "text", "nullable": false, "description": "日类型" },
    { "name": "baseline_speed_kmh", "type": "float8", "nullable": true, "description": "基准速度" },
    { "name": "baseline_ci", "type": "float8", "nullable": true, "description": "置信区间" },
    { "name": "sample_days", "type": "int4", "nullable": true, "description": "采样天数" },
    { "name": "model_version", "type": "text", "nullable": false, "description": "模型版本" },
    { "name": "feature_date", "type": "date", "nullable": false, "description": "特征日期" }
  ],
  "preview": [
    { "road_segment_id": 2089, "hour_of_day": 11, "day_type": "workday", "baseline_speed_kmh": null, "model_version": "v1", "feature_date": "2015-01-07" }
  ]
}
```

**P3 实现，P3 消费**

---

## 10. 数据取数查询

```
POST /api/dashboard/catalog/query
Body:
{
  "table_name": "congestion_baseline_5day",
  "fields": ["road_segment_id", "hour_of_day", "baseline_speed_kmh"],
  "filters": [
    { "field": "day_type", "operator": "=", "value": "workday" },
    { "field": "hour_of_day", "operator": ">=", "value": 8 },
    { "field": "hour_of_day", "operator": "<=", "value": 18 }
  ],
  "limit": 20
}
```

**响应：**
```json
{
  "columns": ["road_segment_id", "hour_of_day", "baseline_speed_kmh"],
  "rows": [[2089, 11, null], [2090, 11, 42.5]],
  "total_rows": 150,
  "query_id": "q_20250101_123456"
}
```

**P3 实现，P3 消费。每次查询需记录日志到 `ads.data_field_access_log`。**

---

## 11. 字段热度排行

```
GET /api/dashboard/catalog/hot-fields
```

**响应：**
```json
{
  "hot_fields": [
    { "field_name": "road_segment_id", "query_count": 15, "rank": 1 },
    { "field_name": "hour_of_day", "query_count": 12, "rank": 2 },
    { "field_name": "day_type", "query_count": 10, "rank": 3 }
  ]
}
```

**P3 实现，P3 消费**

---

## 12. 数据血缘

```
GET /api/dashboard/lineage
```

**响应：**
```json
{
  "nodes": [
    { "id": "ods_taxi_trips_raw", "label": "出租车原始轨迹", "layer": "ODS", "row_count": 1340000 },
    { "id": "fact_congestion_seg_hour", "label": "拥堵小时事实", "layer": "DW", "row_count": 1340958 },
    { "id": "congestion_baseline_5day", "label": "拥堵基线5天", "layer": "TDM", "row_count": 834170 },
    { "id": "ads.congestion_by_segment_hour", "label": "拥堵路段展示", "layer": "ADS" }
  ],
  "edges": [
    { "source": "ods_taxi_trips_raw", "target": "fact_congestion_seg_hour", "label": "ETL聚合" },
    { "source": "fact_congestion_seg_hour", "target": "congestion_baseline_5day", "label": "5天滑动窗口" },
    { "source": "congestion_baseline_5day", "target": "ads.congestion_by_segment_hour", "label": "指标增强" }
  ]
}
```

**P3 实现，P3 消费。数据硬编码即可，不需要动态查询。**

---

## 13. 数据质量

```
GET /api/dashboard/quality
```

**响应：**
```json
{
  "tables": [
    {
      "table_name": "ods.ods_taxi_trips_raw", "schema": "ods", "row_count": 1340000,
      "field_count": 8, "last_updated": "2025-01-01T00:00:00", "completeness_pct": 98.5, "status": "healthy"
    },
    {
      "table_name": "tdm.congestion_baseline_5day", "schema": "tdm", "row_count": 834170,
      "field_count": 8, "last_updated": "2025-01-01T00:00:00", "completeness_pct": 94.4, "status": "healthy"
    }
  ]
}
```

**P3 实现，P3 消费**

---

## 各人消费关系

| # | API | 实现者 | 消费者 |
|---|-----|--------|--------|
| 1 | 拥堵热力图 | P1 | P1(CongestionPage), P3(KPI) |
| 2 | 关键指标 | P1 | P1, P3 |
| 3 | 5天趋势 | P1 | P1(TrendPage) |
| 4 | 日类型对比 | P1 | P1(TrendPage) |
| 5 | 上下车热点 | P2 | P2(HotspotPage) |
| 6 | 司机行为 | P2 | P2(DriverPage) |
| 7 | 休息地段 | P2 | P2(DriverPage) |
| 8 | 目录-表列表 | P3 | P3(CatalogPage) |
| 9 | 目录-字段 | P3 | P3(CatalogPage) |
| 10 | 取数查询 | P3 | P3(CatalogPage) |
| 11 | 字段热度 | P3 | P3(CatalogPage) |
| 12 | 数据血缘 | P3 | P3(LineagePage) |
| 13 | 数据质量 | P3 | P3(LineagePage) |

> **注意**：P1 和 P2 的 API 只有自己消费，不需要等别人。P3 的 API 也只有自己消费。
> 因此三人完全可以并行开发，互不阻塞。
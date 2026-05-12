# P1 拥堵分析模块 — 页面制作思路文档

## 目录

1. [整体架构](#1-整体架构)
2. [拥堵热力图页面 (CongestionPage)](#2-拥堵热力图页面-congestionpage)
3. [趋势分析页面 (TrendPage)](#3-趋势分析页面-trendpage)
4. [后端 API 设计](#4-后端-api-设计)
5. [数据流与状态管理](#5-数据流与状态管理)
6. [关键技术决策](#6-关键技术决策)

---

## 1. 整体架构

### 1.1 技术栈

| 层级 | 技术选型 | 用途 |
|------|---------|------|
| 前端框架 | Vue 3 Composition API (`<script setup>`) | 响应式 UI |
| 地图引擎 | MapLibre GL JS 4.7 | 矢量瓦片渲染、GeoJSON 叠加、交互事件 |
| 图表库 | ECharts 6 | Top10 排行柱状图、24h 速度曲线、sparkline |
| HTTP 客户端 | Axios | API 请求、AbortController 取消 |
| 后端框架 | Spring Boot 3 + JdbcTemplate | REST API、SQL 查询 |
| 数据库 | PostgreSQL 15.4 + PostGIS 3.3.4 | 空间查询（ST_AsGeoJSON） |
| 状态管理 | Pinia (dashboardStore) | 共享 selectedDate、selectedHour |

### 1.2 数据分层

```
ODS (ods_taxi_trips_raw, 134万行原始轨迹)
  ↓ Julia ETL
DW  (dim_road_segment + fact_congestion_seg_hour)
  ↓ 聚合脚本 (fill_congestion_ads.py)
TDM (congestion_baseline_5day, 83万行历史基线)
  ↓
ADS (congestion_by_segment_hour, 路段×小时级聚合)
  ↓ Spring Boot API
前端 (MapLibre + ECharts 可视化)
```

### 1.3 文件清单

| 文件 | 职责 | 行数 |
|------|------|------|
| `frontend/src/pages/CongestionPage.vue` | 拥堵热力图 + Top10 + sparkline | 1031 |
| `frontend/src/pages/TrendPage.vue` | 速度曲线 + 高峰地图 + 时长排行 | 892 |
| `frontend/src/services/dashboardApi.js` | API 调用封装 | 40 |
| `frontend/src/stores/dashboardStore.js` | 全局状态（日期、小时） | ~80 |
| `backend/.../controller/DashboardController.java` | 6 个 REST 端点 | ~120 |
| `backend/.../service/DashboardService.java` | 业务逻辑、类型转换 | 156 |
| `backend/.../repository/DashboardRepository.java` | 5 条 SQL 查询 | 198 |
| `backend/.../dto/CongestionHeatmapDTO.java` | 9 字段 DTO | ~60 |

---

## 2. 拥堵热力图页面 (CongestionPage)

### 2.1 页面布局

```
┌─────────────────────────────────┬──────────────────────┐
│                                 │  Top 10 排行         │
│                                 │  ┌──────────────────┐ │
│                                 │  │  排行模式切换 Tab │ │
│         MapLibre 地图           │  ├──────────────────┤ │
│         (65% 宽度)              │  │  横向柱状图       │ │
│                                 │  │  (flex: 1 1 0)   │ │
│  ┌──────────┐                   │  └──────────────────┘ │
│  │ 图例     │                   │  ┌──────────────────┐ │
│  │ 畅通     │                   │  │ 统计卡片 (3列)   │ │
│  │ 正常     │                   │  │ 均速|拥堵路段|降幅│ │
│  │ 偏慢     │                   │  └──────────────────┘ │
│  │ 异常降速 │                   │  ┌──────────────────┐ │
│  │ 真拥堵   │                   │  │ 24h Sparkline    │ │
│  └──────────┘                   │  │ (点击路段后显示) │ │
│                                 │  └──────────────────┘ │
└─────────────────────────────────┴──────────────────────┘
```

- **左侧 (65%)**：MapLibre 地图，CARTO 浅色底图 + 7 层道路拥堵渲染
- **右侧 (35%)**：Top10 排行（3 种模式切换）+ 统计卡片 + 24h sparkline

### 2.2 地图渲染策略

#### 2.2.1 底图选择

使用 CARTO Light 矢量底图（`https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png`），浅色背景突出道路颜色层次，图例使用白底半透明面板适配浅色底图。

#### 2.2.2 七层渲染架构

MapLibre 不支持在 paint 表达式中动态计算颜色（性能限制），因此采用 **预计算颜色 + filter 分层** 的策略：

```
Layer 1: road-bg          — 蓝色骨架（所有路段），z-index 最低
Layer 2: road-green-dim   — 浅绿色（25-40 km/h 正常行驶）
Layer 3: road-green       — 亮绿色（≥ 40 km/h 畅通）
Layer 4: road-yellow      — 黄色（偏慢/轻微拥堵）
Layer 5: road-orange      — 橙色（异常降速）
Layer 6: road-hot         — 红色（真拥堵，速度<15）
Layer 7: road-hot-glow    — 红色发光层（blur+低透明度）
```

每层通过 `filter: ['==', ['get', '_color'], '#hex']` 过滤，颜色在 JS 端预先计算好写入 GeoJSON properties。

**线宽插值**：根据 zoom 级别动态调整线宽（zoom 9→15，线宽 1.5px→8px），保证不同缩放级别下的可视性。

#### 2.2.3 交互

- **Hover Tooltip**：鼠标悬停在路段上，MapLibre Popup 显示道路名称、当前速度、拥堵等级
- **Click 选择**：点击路段 → `selectedSegment` 更新 → 触发 sparkline 渲染
- **点击空白取消**：`map.on('click')` 时 queryRenderedFeatures 检查是否命中路段层

### 2.3 三维度拥堵着色算法

这是整个页面的核心算法，基于三个维度判断拥堵状态：

```javascript
function congestionColor(speed, deviation, tripCount)
```

#### 输入参数

| 参数 | 含义 | 来源 |
|------|------|------|
| `speed` | 路段当前平均速度 (km/h) | `ads.congestion_by_segment_hour.avg_speed_kmh` |
| `deviation` | 与历史基线的偏差百分比 | `deviation_pct = (avg_speed - baseline) / baseline * 100` |
| `tripCount` | 该时段经过该路段的车辆数 | `dw.fact_congestion_seg_hour.trip_count` |

#### 动态偏差阈值

核心洞察：**采样量越少，统计结果越不可靠，需要更强的信号才判定为拥堵**。

```
devThreshold = min(35, 20 + max(0, (50 - tripCount) × 0.5))
```

| tripCount | devThreshold | 含义 |
|-----------|-------------|------|
| ≥ 50 | 20% | 数据充分，-20% 偏差即可判定异常 |
| 30 | 25% | 中等置信度 |
| 10 | 30% | 数据较少，需要更强的信号 |
| 5 | 32.5% | 极少样本，几乎不标拥堵 |
| 无数据 | 25% | 保守默认值 |

#### 着色逻辑

```
                    deviation < -threshold?
                   /                        \
                 YES                         NO
            speed < 15?                 speed ≥ 40? → 🟢 #00e676 畅通
           /         \                 speed ≥ 25? → 🟢 #66bb6a 正常
          YES        NO               speed ≥ 10? → 🟡 #ffd700 偏慢
          ↓           ↓               speed < 10? → 🔴 #ff3d3d 极慢
    🔴 #ff3d3d   speed < 25?
    真拥堵       /         \
               YES        NO
               ↓           ↓
         🟠 #ff8c00   🟡 #ffd700
         异常降速     轻微拥堵
```

**关键设计决策**：
- 绝对速度 + 相对偏差 **双维度**：纯速度会误判"本身就很窄的路"为拥堵，纯偏差会忽略"低速路段进一步恶化"的情况
- `tripCount` 动态阈值：凌晨 2-4 点只有 3-5 辆车经过，随机性大；早高峰 50+ 辆车统计可靠
- 白天/夜间 **统一标准**：不做日间/夜间阈值差异化，因为偏差率本身就是相对指标

#### 数据过滤

前端 `validSegments` computed 属性进行二次过滤：

```javascript
// 采样少 + 速度极低 = 短暂停车/等灯，不是拥堵
if (tripCount < 10 && avgSpeedKmh < 10) → 排除
if (avgSpeedKmh == null) → 排除
```

后端 SQL 也做了预过滤：`f.trip_count >= 10 AND r.length_m >= 100`，确保只返回有统计意义的路段。

### 2.4 24h 预加载缓存

#### 问题

拖动时间轴切换小时时，每次触发 API 请求需要 ~2 秒，无法实现"丝滑滑动"。

#### 解决方案

```
页面加载 → 立即请求当前小时数据（用户立刻看到）
        → 后台并行加载其余 23 小时（每批 4 个请求）
        → 存入 hourCache (Map<hour, segments[]>)
        → 切换小时时直接从缓存取，零网络延迟
```

#### 实现细节

```javascript
const hourCache = new globalThis.Map()  // hour(0-23) → segments[]
let prefetchAbort = null                // AbortController，换日期时取消
```

**注意**：使用 `globalThis.Map` 而非 `new Map()`，因为文件顶部 `import { Map } from 'maplibre-gl'` 覆盖了 JS 原生 Map 构造函数。

**预加载流程**：
1. `prefetchAllHours(date, currentHour)` 被调用
2. 先取消上次未完成的预加载（`prefetchAbort.abort()`）
3. 优先加载当前小时，设置 `segments.value`，用户立即看到地图
4. 其余 23 小时分批加载（`batchSize = 4`，避免浏览器 HTTP 连接耗尽）
5. 每批完成后 `hourCache.set(hour, segments)`

**切换小时**：
```javascript
function switchHour(hour) {
  const cached = hourCache.get(hour)
  if (cached) {
    segments.value = cached  // 零延迟
    updateMap()
    updateChart()
    return true
  }
  return false  // 缓存未命中，回退到单次请求
}
```

**日期变更**：清空 `hourCache`，重新执行 `prefetchAllHours`。

**内存开销**：24 小时 × ~200 路段 × ~200 字节/路段 ≈ 1 MB，完全可接受。

### 2.5 Top10 排行榜

#### 三种排行模式

| 模式 | 排序依据 | 过滤条件 |
|------|---------|---------|
| 速度降幅 (deviation) | `deviationPct` 升序 | `deviationPct < -5` |
| 拥堵指数 (congestion) | `congestionIndex` 降序 | `congestionIndex != null` |
| 最慢路段 (slowest) | `avgSpeedKmh` 升序 | — |

数据来源为当前小时的 `validSegments`，与地图同步。

#### ECharts 横向柱状图

- Y 轴：道路名称（超长截断 `width: 120px, overflow: 'truncate'`）
- X 轴：数值（速度模式用 km/h，拥堵指数模式用 0-1）
- 柱子颜色：根据速度值渐变（与地图颜色一致），使用 `LinearGradient`
- Tooltip：显示速度 + 拥堵等级文字标签

### 2.6 统计卡片

三个卡片实时计算当前小时的汇总指标：

| 卡片 | 计算逻辑 |
|------|---------|
| 平均速度 | `sum(avgSpeedKmh) / count(validSegments)` |
| 拥堵路段 | `count(deviationPct < -20) / count(total)`，显示数量和百分比 |
| 最大降幅 | `min(deviationPct)`，显示百分比值 |

### 2.7 路段点击 24h Sparkline

#### 交互流程

1. 用户点击地图上的路段 → `selectedSegment = { id, name }`
2. 触发 `watch(selectedSegment)` → `nextTick(() => updateSparkline())`
3. `sparklineData` computed 从 `hourCache` 中提取该路段 24 小时速度数据（零网络请求）
4. 渲染 ECharts 折线图，红色虚线标记当前小时

#### 布局处理

使用 `v-show` 而非 `v-if` 保证 DOM 容器始终存在（ECharts 需要固定尺寸的容器）。Sparkline 区域 `height: 150px; min-height: 150px; flex-shrink: 0`，排行图区域 `flex: 1 1 0` 自动让出空间。

---

## 3. 趋势分析页面 (TrendPage)

### 3.1 页面布局

```
┌────────────────────┬───────────────────────────┐
│ 左列 (45%)         │ 右列 (55%)                │
│                    │                           │
│ ┌────────────────┐ │  ┌─────────────────────┐  │
│ │ Panel 1:       │ │  │ Panel 2:            │  │
│ │ 道路类型 24h   │ │  │ 高峰拥堵空间对比    │  │
│ │ 速度曲线       │ │  │                     │  │
│ │ (ECharts 折线) │ │  │ MapLibre 地图       │  │
│ └────────────────┘ │  │                     │  │
│ ┌────────────────┐ │  │ [早高峰 7-9]        │  │
│ │ Panel 3:       │ │  │ [晚高峰 17-19]      │  │
│ │ 拥堵持续时间   │ │  │                     │  │
│ │ Top10 排行     │ │  │ 持续拥堵=粗红线     │  │
│ │ (横向柱状图)   │ │  │ 短暂拥堵=细橙线     │  │
│ └────────────────┘ │  └─────────────────────┘  │
└────────────────────┴───────────────────────────┘
```

三个面板 **各自独立** 拥有 loading/error 状态，互不影响。

### 3.2 Panel 1：道路类型 24h 速度曲线

#### 数据来源

API：`GET /api/dashboard/congestion/road-type-speed?dt={date}`

后端 SQL 按道路类型（trunk/primary/secondary/residential）和小时聚合：
```sql
SELECT hour_of_day, road_type, ROUND(AVG(avg_speed_kmh)::numeric, 1) AS avg_speed
FROM ads.congestion_by_segment_hour c
JOIN dw.fact_congestion_seg_hour f ON ...
WHERE c.dt = ? AND c.road_type IN ('trunk','primary','secondary','residential')
  AND f.trip_count >= 10
GROUP BY hour_of_day, road_type
```

返回 96 行（4 类型 × 24 小时），前端按 `road_type` 分组后生成 4 条折线。

#### 可视化设计

- **4 条平滑曲线**，每条对应一种道路类型
- **颜色编码**：主干道=绿、一级=黄、二级=橙、居民区=红（直觉：道路等级越低颜色越"热"）
- **面积填充**：每条线下方有 `rgba` 渐变面积，增强视觉层次
- **Y 轴范围**：固定 15-35 km/h，突出不同道路类型的速度差异
- **X 轴**：0-23 小时，每 3 小时显示刻度
- **交互**：Tooltip 显示该小时所有道路类型的速度，十字线辅助定位

#### 分析价值

通过 4 条曲线的对比，可以清晰看到：
- 主干道全天速度最高，波动最小
- 居民区道路速度最低，但早晚高峰降幅明显
- 速度最低谷通常出现在 8:00 和 18:00 左右
- 不同道路类型的高峰时段有微小错位（居民区更早进入拥堵）

### 3.3 Panel 2：高峰拥堵空间对比

#### 数据来源

API：`GET /api/dashboard/congestion/heatmap`（复用拥堵页面的接口）

分别请求早高峰（7、8、9 时）和晚高峰（17、18、19 时）共 6 次请求，前端合并。

#### 路段合并算法

```javascript
function mergePeakSegments(hourSegmentsArrays) {
  const segMap = new globalThis.Map()
  hourSegmentsArrays.forEach(segments => {
    segments.forEach(seg => {
      if (!segMap.has(id)) → 创建条目 { _appearCount: 0, _minSpeed: ∞, _maxDeviation: 0 }
      entry._appearCount++
      entry._minSpeed = min(entry._minSpeed, seg.avgSpeedKmh)
      entry._maxDeviation = min(entry._maxDeviation, seg.deviationPct)
    })
  })
  return Array.from(segMap.values())
}
```

合并后每个路段有 `_appearCount`（在几个小时内出现）、`_minSpeed`（最低速度）、`_maxDeviation`（最大降幅）。

#### 拥堵分类

```
_minSpeed < 20 && _maxDeviation < -20%?
├── YES → _appearCount >= 2?
│         ├── YES → persistent（持续拥堵，3 小时中出现 2+ 次）
│         └── NO  → transient（短暂拥堵，仅出现 1 次）
└── NO  → normal（正常路段）
```

#### 地图渲染

4 层结构：
1. `road-bg` — 灰色底层（所有路段）
2. `road-persistent` — 粗红线（持续拥堵，zoom 9→15: 3px→8px）
3. `road-persistent-glow` — 红色发光层（blur: 8, opacity: 0.15）
4. `road-transient` — 细橙线（短暂拥堵）

#### 早晚高峰切换

- 两个按钮：`[早高峰 7-9]` `[晚高峰 17-19]`
- 切换时直接从 `peakCache` 读取数据，不重新请求
- 早晚高峰数据在页面加载时 **并行预加载**，切换零延迟

#### 分析价值

- 直观对比早晚高峰的空间分布差异
- 区分"持续拥堵"（瓶颈路段）和"短暂拥堵"（偶发拥堵）
- 红色发光层突出持续拥堵路段，便于定位城市交通瓶颈

### 3.4 Panel 3：拥堵持续时间 Top10

#### 数据来源

API：`GET /api/dashboard/congestion/duration-ranking?dt={date}`

后端 SQL：
```sql
SELECT road_segment_id, road_name, road_type,
       COUNT(*) AS congestion_hours,
       ROUND(AVG(avg_speed_kmh)::numeric, 1) AS avg_speed_when_congested,
       ROUND(MIN(deviation_pct)::numeric, 1) AS worst_deviation
FROM ads.congestion_by_segment_hour c
JOIN dw.fact_congestion_seg_hour f ON ...
WHERE c.dt = ? AND c.road_name IS NOT NULL
  AND c.deviation_pct < -20
  AND c.avg_speed_kmh < 20
  AND f.trip_count >= 10
GROUP BY road_segment_id, road_name, road_type
ORDER BY congestion_hours DESC, avg_speed_when_congested ASC
LIMIT 10
```

拥堵定义：`deviation_pct < -20% AND avg_speed_kmh < 20`，即速度低于 20 km/h 且比历史基线低 20% 以上。

#### 可视化设计

- 横向柱状图，#1 排名在最上方
- 柱子颜色按 `road_type` 映射（与 Panel 1 颜色一致）
- X 轴最大 24 小时
- Tooltip 显示：拥堵时长、拥堵时均速、最大降幅

#### 分析价值

- 直接回答"哪些路段一天堵了最久"
- 结合 road_type 可以发现：持续时间最长的往往是二级/居民区道路，而非主干道
- 这说明主干道虽然车流量大，但通行效率相对稳定；小路一旦拥堵很难自行缓解

---

## 4. 后端 API 设计

### 4.1 端点清单

| 端点 | 方法 | 用途 | 调用页面 |
|------|------|------|---------|
| `/api/dashboard/congestion/heatmap` | GET | 路段级拥堵数据 | CongestionPage、TrendPage |
| `/api/dashboard/kpi` | GET | 关键指标汇总 | DashboardLayout |
| `/api/dashboard/congestion/trend` | GET | 5 天趋势 | (原始设计) |
| `/api/dashboard/congestion/comparison` | GET | 日类型对比 | (原始设计) |
| `/api/dashboard/congestion/road-type-speed` | GET | 道路类型×24h速度 | TrendPage |
| `/api/dashboard/congestion/duration-ranking` | GET | 拥堵持续时间Top10 | TrendPage |

### 4.2 核心查询：拥堵热力图

```sql
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
  AND f.trip_count >= 10      -- 过滤低采样
  AND r.length_m >= 100       -- 过滤极短路段
ORDER BY c.congestion_index DESC
```

**关键设计**：
- 三表 JOIN：ADS 聚合表（速度/偏差）+ DW 维度表（空间几何）+ DW 事实表（采样量）
- `trip_count >= 10`：排除采样不足的路段，避免统计噪声
- `length_m >= 100`：排除极短路段（如路口连接线），这些路段速度计算不稳定
- `ST_AsGeoJSON(r.geom)`：将 PostGIS 几何转为 GeoJSON 字符串，前端直接解析

### 4.3 DTO 结构

```java
// CongestionHeatmapDTO — 9 个字段
{
  roadSegmentId: Long,      // 路段 ID
  roadName: String,         // 道路名称
  avgSpeedKmh: Double,      // 平均速度
  congestionIndex: Double,  // 拥堵指数 = 1 - speed/40
  deviationPct: Double,     // 与基线偏差百分比
  tripCount: Integer,       // 采样车辆数
  roadType: String,         // 道路类型（未使用于热力图）
  dayType: String,          // 日类型（未使用于热力图）
  geometry: String          // GeoJSON 几何字符串
}
```

---

## 5. 数据流与状态管理

### 5.1 全局状态 (Pinia)

`dashboardStore` 维护两个核心状态：

```javascript
selectedDate: '2015-01-05',  // 当前选择日期
selectedHour: 8,             // 当前选择小时 (0-23)
```

这两个值由 `DashboardLayout.vue` 中的时间轴控件修改，所有页面通过 `watch` 响应变化。

### 5.2 CongestionPage 数据流

```
DashboardLayout 时间轴变更
  → store.selectedHour 变化
  → CongestionPage watch 触发
    → 判断日期是否变化
      → 日期变了：prefetchAllHours(date, hour) → 重新加载 24h
      → 日期没变：switchHour(hour)
        → 缓存命中：segments.value = cached → updateMap() + updateChart()
        → 缓存未命中：fetchSingleHour() → 回退到单次请求
```

### 5.3 TrendPage 数据流

```
DashboardLayout 时间轴变更
  → store.selectedDate 变化
  → TrendPage watch 触发
    → 并行请求：fetchSpeedData() + fetchPeakData() + fetchRankData()
    → 全部完成后：updateSpeedChart() + updateRankChart() + updatePeakMap()
```

TrendPage **不监听 selectedHour**（趋势分析以天为单位），通过 `DashboardLayout.vue` 中对 `/trend` 路由隐藏时间轴实现。

---

## 6. 关键技术决策

### 6.1 为什么不用 MapLibre 表达式着色？

MapLibre GL 支持 `paint` 属性中使用表达式（如 `['case', ['<', ['get', 'speed'], 15], '#ff3d3d', ...]`），但：

1. **不支持三维度逻辑**：需要同时判断 speed + deviation + tripCount 动态阈值，表达式会极其复杂
2. **性能问题**：每帧渲染时 MapLibre 需要为每个 feature 执行表达式求值，JS 端预计算一次更高效
3. **调试困难**：MapLibre 表达式语法不易调试

**选择**：JS 端预计算 `_color` 属性，通过 filter 分层渲染。

### 6.2 为什么用 `globalThis.Map`？

```javascript
import { Map } from 'maplibre-gl'  // 覆盖了原生 Map
const hourCache = new Map()         // ❌ 创建的是 maplibre Map 实例
const hourCache = new globalThis.Map() // ✅ 正确的 JS Map
```

这是一个实际踩过的坑。maplibre-gl 的 `Map` 构造函数签名与 JS `Map` 不同，调用方式也不同，运行时会抛出难以定位的错误。

### 6.3 为什么 congestion_index 不是核心指标？

`congestion_index = 1 - avg_speed_kmh / 40` 本质上只是速度的线性映射，提供的信息量等于零。但它作为保留指标是因为：
- 排行榜中"拥堵指数"比"速度倒数"更直观
- 业务方习惯使用 0-1 的指数化指标

**实际用于判断拥堵的是 `deviation_pct`**，它反映的是"当前速度与历史基线的偏差"，才能区分"本来就很窄的路"和"突然堵了的路"。

### 6.4 为什么趋势页改为 3 面板？

原始设计（5 天趋势 + 日类型对比）的数据只有 5 天（1月3日-1月7日），其中工作日只有 2 天，节假日 3 天。日类型对比的统计意义不足。

改为 3 面板后：
- **Panel 1**（道路类型 24h 曲线）：一天内 96 个数据点，统计充分
- **Panel 2**（高峰空间对比）：3 小时合并后仍然信息丰富
- **Panel 3**（持续时间排行）：直接关联业务价值

### 6.5 Sparkline 为什么用 `v-show` 而非 `v-if`？

ECharts 初始化需要容器有确定的 DOM 尺寸。使用 `v-if` 会导致：
1. 容器在 `selectedSegment` 变更后才挂载
2. `nextTick` 时容器尺寸可能尚未确定
3. ECharts 初始化失败或渲染到 0×0 容器

`v-show` 保证容器始终存在于 DOM 中，只是 `display: none` 隐藏。当需要显示时直接 `resize()` + `setOption()` 即可。

---

## 附录：性能指标

| 操作 | 延迟 | 原因 |
|------|------|------|
| 首次加载当前小时 | ~800ms | 后端 SQL 查询 + 网络传输 |
| 切换小时（缓存命中） | < 50ms | 纯前端赋值 + MapLibre setData |
| 切换小时（缓存未命中） | ~800ms | 单次 API 请求 |
| 切换日期 | ~800ms（首小时） | 预加载当前小时 + 后台加载其余 |
| 点击路段显示 sparkline | < 30ms | 纯缓存数据 + ECharts 渲染 |
| TrendPage 完整加载 | ~1.5s | 3 个并行 API + 6 次热力图请求 |

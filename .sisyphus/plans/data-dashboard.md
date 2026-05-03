# 交通数据大屏 — 完整实现计划

## TL;DR

> **Quick Summary**: 基于现有 PostgreSQL 交通数据（哈尔滨出租车轨迹，2015-01-03~2015-01-07），构建包含 6 个页面的数据大屏，3 人分工并行开发。
>
> **Deliverables**:
> - 前端：6 个数据大屏页面（MapLibre + deck.gl 地图 + ECharts 统计图表）
> - 后端：新的 Dashboard API 接口 + ADS 层数据表
> - 数据：ADS 层全面重构 + 新增司机休息地段 ETL
>
> **Estimated Effort**: 中大型 — 3 人 × 5 天
> **Parallel Execution**: YES — 3 人并行开发，Phase 2 集成
> **Critical Path**: API 合同定义 → ADS DDL 创建 → 各人开发 → 集成测试

---

## Context

### Original Request
基于 TDM 层数据，做一个数据大屏，包含：
1. 拥堵情况热力图（地段/时间轴联动）
2. 打车上车/下车高峰地段热力图
3. 司机休息地段/休息时长/活跃时长热力图
4. 数据取数界面（取 TDM 层数据 + 字段热度追踪）
5. 数据血缘展示

### Interview Summary

**Key Discussions**:
- 场景：领导汇报/课程演示，给老师展示
- 技术栈：MapLibre + deck.gl（地图）+ ECharts（统计图表），不改现有前端版本
- ADS 层：完全清除旧表，重新设计
- 分工：3 人 × 2 页面 = 6 页面，方案 B3（每人一个功能闭环）
- 司机休息地段：新增 Julia ETL 从 `ods_taxi_trips_raw` 中基于时间间隔推断
- 额外功能：数据血缘展示（课程加分）
- 测试：需要基本 API 测试
- 前端需升级为多页大屏（添加 vue-router）

**Research Findings**:
- TDM 三张表均存在：congestion_baseline_5day（83万行），driver_shift_pattern（5.5万），grid_hotspot_score（18.8万）✅
- osmuser 有 ads/tdm schema 的 CREATE 权限 ✅
- PostGIS 3.3.4 可用 ✅
- bfmap_ways 无 name 列，但 public.ways.tags（hstore）存有道路名（如："name"=>"裕虹路"）
- 现有后端零引用 ADS 表 → 可安全清除重建
- 时区 UTC，与代码一致

### Metis Review

**Identified Gaps** (addressed):
- ✅ API 合同前置定义 → Task 0
- ✅ bfmap_ways 无道路名 → JOIN public.ways.tags->'name'
- ✅ 前端单页→多页重构 → 添加 vue-router
- ✅ 3 人并行依赖协调 → P3 先框架 + mock 数据
- ✅ Git 分支策略 → 每人独立 feature 分支
- ✅ 时间偏移风险 → 确认时区 UTC，无偏差

---

## Work Objectives

### Core Objective
构建 6 页数据大屏，展示哈尔滨交通拥堵、上下车热点、司机行为、数据血缘等可视化分析能力。

### Concrete Deliverables
- 6 个数据大屏页面（前端）
- 7 个新 API 端点（后端）
- 7 张新 ADS/TDM 表（数据层）
- 1 个 Julia ETL 脚本（司机休息地段推断）

### Definition of Done
- [ ] 6 页均可独立访问，全局导航正常
- [ ] 时间轴联动所有图表同步刷新
- [ ] 所有 API 请求返回 200 + 有效 JSON
- [ ] 取数界面可查询字段、记录日志、展示热度排行
- [ ] 数据血缘页面展示 ODS→DW→TDM→ADS 流向
- [ ] 现有地图页面功能不受影响

### Must Have
- 拥堵热力图 + 时间轴联动
- 上下车热点地图切换
- 司机班次分布 + 休息地段展示
- 数据资产取数界面（仅 TDM 层）
- 数据血缘与质量监控页
- 字段热度追踪与价值排行榜

### Must NOT Have (Guardrails)
- **不修改**现有 TaxiController / MapController / TaxiRepository / MapRepository 及相关 Service
- **不修改**现有 frontend/src/services/api.js / stores/mapStore.js — 新页面新建独立的
- **不升级**前端依赖版本（maplibre-gl 4.7.1, deck.gl 8.9.36）
- **不新增** Spring Boot 依赖（pom.xml 不变）
- **不追查**全量 DAG 血缘引擎 — 静态展示 ODS→DW→TDM→ADS 即可
- **不做**实时数据模拟 — 明确是历史数据分析
- **不做**粒子动画 / 自定义 CSS 动画 — 仅 ECharts 基本动画

---

## Verification Strategy

> **ZERO HUMAN INTERVENTION** — ALL verification is agent-executed. No exceptions.

### Test Decision
- **Infrastructure exists**: YES (Spring Boot test)
- **Automated tests**: TESTS-AFTER (basic API smoke tests)
- **Framework**: Spring Boot Test + Playwright
- **Coverage**: API 端点的 HTTP 200 验证 + 前端页面加载验证

### QA Policy
Every task MUST include agent-executed QA scenarios. Evidence saved to `.sisyphus/evidence/task-{N}-{scenario-slug}.{ext}`.

- **API**: Use Bash (curl) — Send requests, assert status 200 + valid JSON fields
- **Frontend**: Use Playwright — Navigate, wait for charts/maps to render, screenshot
- **Database**: Use Bash (python psycopg2) — Verify row counts > 0

---

# ============================================================
# ★★★ 团队协作指南：三人如何各自推进 ★★★
# ============================================================

## 一、前提条件（开始前先确认）

### 每个人都需要：
- [ ] 能连接数据库 `101.35.234.65:5432`（PostgreSQL 15.4，用户 osmuser / 密码 pass）
- [ ] 能在本地启动后端（已运行过 `mvn spring-boot:run`）
- [ ] 能在本地启动前端（已运行过 `npm run dev`）
- [ ] 已拉取最新代码

### 项目根目录结构（记住这几个重要位置）：
```
Data_Platform-main/
├── backend/src/main/java/com/harbin/dataplatform/
│   ├── controller/          # API 端点
│   │   └── DashboardController.java  ← 你在这里加新 API（新建文件）
│   ├── service/             # 业务逻辑
│   │   ├── DashboardService.java     ← 接口
│   │   └── impl/DashboardServiceImpl.java  ← 实现
│   ├── repository/          # 数据库查询
│   │   └── DashboardRepository.java  ← 写 SQL 的地方（JdbcTemplate）
│   └── entity/              # JPA 实体（不需要，全用 JdbcTemplate）
├── backend/src/main/resources/
│   └── ddl-dashboard.sql    ← ADS 表建表 SQL
├── frontend/src/
│   ├── router/index.js      ← 路由配置（P3 负责）
│   ├── services/
│   │   └── dashboardApi.js  ← 前端 API 调用（你在这里加）
│   ├── stores/
│   │   └── dashboardStore.js← Pinia 状态管理（你在这里加）
│   └── pages/               ← 你的页面组件放这里
│       ├── CongestionPage.vue   ← P1
│       ├── TrendPage.vue        ← P1
│       ├── HotspotPage.vue      ← P2
│       ├── DriverPage.vue       ← P2
│       ├── CatalogPage.vue      ← P3
│       └── LineagePage.vue      ← P3
├── scripts/
│   ├── fill_congestion_ads.py   ← P1 填充 ADS 数据
│   ├── fill_hotspot_driver_ads.py ← P2 填充 ADS 数据
│   └── smoke-dashboard.ps1   ← API 测试
├── julia/
│   └── driver_rest_etl.jl    ← P2 休息地段 ETL
└── docs/
    └── api-contract.md       ← API 合同（Task 0 创建）
```

---

## 二、Git 分支策略（必须遵守）

### 分支命名
```
main              ← 主分支，只接受 squash-merge
feature/p1        ← Person 1 的分支
feature/p2        ← Person 2 的分支
feature/p3        ← Person 3 的分支
```

### 每个人的工作流

```bash
# 1. 从 main 创建自己的分支（仅执行一次）
git checkout main
git pull
git checkout -b feature/p1   # P1 示例，P2/P3 同理

# 2. 每次开始前
git add .
git commit -m "type(scope): description"   # 先提交本地改动
git pull origin main                       # 拉取 main 的更新
# 如果有冲突，解决冲突后 git commit（无冲突则跳过）

# 3. 提交代码
git add <涉及的文件>
git commit -m "feat(api): add congestion heatmap endpoint"

# 4. 推送你的分支到远程（随时可以推，不怕冲突）
git push origin feature/p1

# 5. 集成阶段（Phase 2 开始时）
# P3 通知所有人，大家把 feature 分支合入 main
# 在 GitHub/GitLab 上 PR: feature/p1 → main
# 使用 squash-merge（所有 commit 压缩成一个）
```

### 千万不要
- ❌ 直接在 main 上开发
- ❌ 用 `git push --force`（除非你确定只有自己在用这个分支）
- ❌ 合并别人的分支到自己的分支（各干各的，最后合 main）

---

## 三、Phase 0：先做基础工作（所有人）

### Step 1：P3 先创建前段框架

P3 在 `feature/p3` 分支上：
```bash
cd frontend
npm install vue-router echarts
```
然后：
- 修改 `App.vue` → 加 `<router-view>`
- 创建 `router/index.js`
- 创建 `DashboardLayout.vue`（导航栏 + 侧边栏）
- 创建 6 个占位页面组件（内容写个标题和"数据加载中..."即可）
- 推送到 `feature/p3` → 合并到 `main`

### Step 2：P1 创建 ADS 表

P1 在 `feature/p1` 分支上：
- 写 `ddl-dashboard.sql`（DROP 旧表 + CREATE 7 张新表）
- 在 IntelliJ Database 或 psql 中执行该 SQL
- 推送到 `feature/p1` → 合并到 `main`

### Step 3：所有人执行 API 合同

**三个人一起讨论**，在 `docs/api-contract.md` 中定义每个 API 的请求/响应格式。

**为什么要先做这个？**
因为 P3 需要知道 API 返回什么数据才能写前端 mock，P1/P2 需要知道要返回什么数据才知道 ADS 表要存什么。

---

## 四、每个人具体做什么（按天分配）

### Person 1 — 拥堵分析（2 个页面）

| 天数 | 做什么 | 产出 |
|------|--------|------|
| **第 1 天** | Phase 0：写 API 合同、执行 ADS DDL | docs/api-contract.md |
| **第 2 天** | 写 `fill_congestion_ads.py` 填充 ADS 表 | 数据就绪 |
| **第 3 天** | 写 `DashboardController` + `DashboardService` + `DashboardRepository` | API 可用 |
| **第 4 天** | 写 `CongestionPage.vue`（地图热力图 + Top10 排行 + 时间轴） | 页面 1 可用 |
| **第 5 天** | 写 `TrendPage.vue`（5 天趋势折线图 + 日类型对比柱状图）+ 烟雾测试 | 页面 2 可用 |

**开发步骤（详细）：**

```bash
# 第 1 天
git checkout main && git pull
git checkout -b feature/p1

# 在第 2-3 天，后端文件结构：
# backend/src/main/java/com/harbin/dataplatform/
#   controller/DashboardController.java
#   service/DashboardService.java
#   service/impl/DashboardServiceImpl.java
#   repository/DashboardRepository.java
#   dto/CongestionHeatmapDTO.java
#   dto/KPIDTO.java
#   dto/TrendDTO.java
#   dto/ComparisonDTO.java

# 第 4-5 天，前端文件结构：
# frontend/src/
#   services/dashboardApi.js    ← 调用后端 API
#   stores/dashboardStore.js    ← 管理拥堵数据状态
#   pages/CongestionPage.vue    ← 拥堵热力图页面
#   pages/TrendPage.vue         ← 趋势对比页面

# 提交
git add .
git commit -m "feat(api): add congestion endpoints"
git push origin feature/p1
```

**后端参考：** 照着现有 `TaxiRepository.java` 写，用 `JdbcTemplate.query()` 查 `ads.congestion_by_segment_hour`

**前端参考：** 照着现有 `MapContainer.vue` 用 `maplibre-gl` + `deck.gl`，ECharts 部分看 echarts 官方文档

**重要：道路名称从哪来**
```sql
SELECT w.tags->'name' AS road_name
FROM ads.congestion_by_segment_hour c
LEFT JOIN public.ways w ON w.osm_id = c.road_segment_id
-- 需要确认 road_segment_id 和 osm_id 的对应关系
```

**什么时候需要 P3 帮助：**
- 你的页面需要放到导航栏？→ 告诉 P3 在 `router/index.js` 加路由
- 全局时间轴联动？→ P3 会在 store 中提供时间状态，你用 `store.dashboardStore.timeState` 获取

---

### Person 2 — 上下车热点 + 司机行为（2 个页面）

| 天数 | 做什么 | 产出 |
|------|--------|------|
| **第 1 天** | Phase 0：写 API 合同 | docs/api-contract.md |
| **第 2 天** | 写 `julia/driver_rest_etl.jl`（从轨迹推断停留点） | tdm.driver_rest_location 表 |
| **第 3 天** | 写 `fill_hotspot_driver_ads.py` 填充 ADS 表 | 数据就绪 |
| **第 4 天** | 在 DashboardRepository 加热点 + 司机 API | API 可用 |
| **第 4-5 天** | 写 `HotspotPage.vue` + `DriverPage.vue` | 2 个页面可用 |

**Julia ETL 核心逻辑：**
```julia
# jilia/driver_rest_etl.jl
# 1. 从 ods.ods_taxi_trips_raw 读取数据
# 2. 按 devid 分组，按 tms_seq[1] 排序
# 3. 逐点比较时间差
# 4. 如果 gap > 120 秒（MAX_GAP_SECONDS），记录为休息事件
#    休息地点 = 前一个点的 (lon_seq[i], lat_seq[i])
#    休息时长 = 时间差
# 5. 写入 tdm.driver_rest_location
```

**热点地图逻辑：**
- 从 `tdm.grid_hotspot_score` 读取数据
- grid_id 格式：`126.65_45.775_500m` → 解析为 grid_lon, grid_lat
- deck.gl HeatmapLayer 渲染
- 切换按钮：pickup / dropoff / both

**什么时候需要别人帮助：**
- 需要确认 `ods_taxi_trips_raw` 的数据格式？→ 在数据库中查 `SELECT * FROM ods.ods_taxi_trips_raw LIMIT 5`
- 需要 P3 帮你加路由？→ 告诉 P3 你的页面路径

---

### Person 3 — 数据资产取数 + 血缘质量（2 个页面 + 集成）

| 天数 | 做什么 | 产出 |
|------|--------|------|
| **第 1 天** | Phase 0：装 vue-router + echarts + 改路由 + 提供 mock 数据框架 | 前段框架可用 |
| **第 2 天** | 写 Catalog API + 前端取数界面 | 页面 5 可用 |
| **第 3 天** | 写 Lineage API + 前端血缘 DAG 图 + 质量卡片 | 页面 6 可用 |
| **第 4 天** | 写 smoke 测试 + 等待 P1/P2 完成 | 测试可用 |
| **第 5 天** | 集成：替换 mock 为真实 API + 协调回归测试 | 大屏完成 |

**路由配置（你创建）：**
```javascript
// frontend/src/router/index.js
const routes = [
  { path: '/', component: DashboardLayout, redirect: '/congestion',
    children: [
      { path: 'congestion', component: CongestionPage },
      { path: 'trend',       component: TrendPage },
      { path: 'hotspot',     component: HotspotPage },
      { path: 'driver',     component: DriverPage },
      { path: 'catalog',    component: CatalogPage },
      { path: 'lineage',    component: LineagePage },
    ]
  },
  { path: '/map', component: OriginalMapPage },  // 保留原地图
]
```

**数据血缘 DAG 图（硬编码）：**
```javascript
// GET /api/dashboard/lineage 返回
{
  nodes: [
    { id: 'ods_taxi_trips_raw', layer: 'ODS', rows: 1340000 },
    { id: 'fact_congestion_seg_hour', layer: 'DW', rows: 1340958 },
    { id: 'congestion_baseline_5day', layer: 'TDM', rows: 834170 },
    { id: 'ads_congestion_by_segment_hour', layer: 'ADS', rows: 1340958 },
    // ...更多节点
  ],
  edges: [
    { source: 'ods_taxi_trips_raw', target: 'fact_congestion_seg_hour' },
    { source: 'fact_congestion_seg_hour', target: 'congestion_baseline_5day' },
    // ...更多边
  ]
}
```

**Mock 数据（让 P1/P2 能独立测试）：**
```javascript
// frontend/src/services/dashboardApi.js
// 在真实 API 未就绪时，返回 mock 数据
const USE_MOCK = true;

export async function getCongestionHeatmap(dt, hour) {
  if (USE_MOCK) {
    return { segments: [...mockData] };
  }
  return axios.get(`/api/dashboard/congestion/heatmap`, { params: { dt, hour } });
}

// P1/P2 开发期间 USE_MOCK = true
// 集成时 USE_MOCK = false
```

**你在第 4-5 天的工作：**
1. 确认 P1/P2 的 API 已返回真实数据
2. 设置 `USE_MOCK = false`
3. 验证所有页面能正常加载
4. 跑 smoke 测试
5. 跑回归测试（原地图还能用）
6. 告诉所有人可以推送到 main

---

## 五、API 合同模板（Task 0）

```markdown
# docs/api-contract.md

## GET /api/dashboard/congestion/heatmap
请求参数: dt=2015-01-05, hour=12
响应格式:
```json
{
  "segments": [
    {
      "road_segment_id": 123,
      "road_name": "裕虹路",
      "avg_speed_kmh": 35.2,
      "congestion_index": 0.72,
      "deviation_pct": -15.3,
      "geom": "POINT(...)"
    }
  ]
}
```

## GET /api/dashboard/hotspot/map
请求参数: dt=2015-01-05, hour=12, event_type=pickup|dropoff
响应格式:
```json
{
  "grids": [
    {
      "grid_id": "126.65_45.775_500m",
      "lon": 126.65,
      "lat": 45.775,
      "event_count": 42,
      "hotspot_score": 0.85
    }
  ]
}
```

... 其余 API 类似
```

**P1/P2/P3 必须一起讨论确定所有 API 格式，写进这个文件后就不要再改。**

---

## 六、调试技巧

### 后端
```bash
# 查看后端日志
cd backend
mvn spring-boot:run  # 报错会直接打印

# 测试 API（后端跑着的时候另开终端）
curl http://localhost:8081/api/dashboard/congestion/heatmap?dt=2015-01-05&hour=12
# 如果返回 200 + JSON → 成功
# 如果返回 404 → 检查路由 /api/dashboard 是否在 Controller 上
# 如果返回 500 → 看后端终端上的错误堆栈
```

### 前端
```bash
# 启动前端
cd frontend && npm run dev
# 浏览器打开 http://localhost:5173
# F12 → Console：看是否有 API 报错
# F12 → Network：看 API 请求是否发到正确地址
```

### 数据库
```bash
# 检查数据是否就绪
python -c "
import psycopg2
conn = psycopg2.connect(host='101.35.234.65', port=5432, dbname='postgres', user='osmuser', password='pass')
cur = conn.cursor()
cur.execute('SELECT COUNT(*) FROM ads.congestion_by_segment_hour')
print('行数:', cur.fetchone()[0])
cur.close()
conn.close()
"
```

### 常见问题

**Q：后端启动报 8081 端口占用**
```bash
powershell -ExecutionPolicy Bypass -File .\scripts\stop-backend.ps1
# 或用 start-backend.ps1 -KillPortOwner
```

**Q：前端 API 请求返回 404**
- 确认后端已启动
- 确认 vite.config.js 代理配置了 `/api` → `http://localhost:8081`
- 检查 Controller 上的 `@RequestMapping("/api/dashboard")`

**Q：路由加了但页面不显示**
- 确认在 `router/index.js` 中注册了
- 确认 `DashboardLayout.vue` 中加了 `<router-view>`

**Q：合并冲突**
- 各自在 feature 分支上开发，最后合 main
- 如果两个人改了同一个文件：
  1. `git pull origin main`
  2. Git 会提示冲突文件
  3. 手动打开冲突文件，删掉 `<<<<<<<` / `=======` / `>>>>>>>`，保留你要的代码
  4. `git add <文件>` → `git commit` → `git push`

---

## 七、最终集成检查清单

**Phase 2 开始时（P3 主导）：**

- [ ] P1 的 API 全部可用（返回 200）
- [ ] P2 的 API 全部可用（返回 200）
- [ ] P3 的 API 全部可用（返回 200）
- [ ] P1 页面在 P3 的框架中能正常渲染
- [ ] P2 页面在 P3 的框架中能正常渲染
- [ ] 原 `/map` 路径还能正常访问
- [ ] smoke-dashboard.ps1 全部通过
- [ ] 演示脚本写好

**然后：**
```bash
# 各人在自己分支上
git add . && git commit -m "final: ready for integration"
git push origin feature/p1   # P2/P3 同理

# 在 GitHub/GitLab 上创建 PR，squash-merge 到 main
```

---

# ============================================================
# ★★★ 以下是原始计划内容，上面是协作指南 ★★★
# ============================================================

---

## Execution Strategy

### Phase Overview

```
Phase 0 (Foundation — 全部并行):
├── Task 0: API 合同文档 (P1/P2/P3 协作)
├── Task 1: ADS DDL + 旧表清除 (P1)
├── Task 8: 前端框架升级 (vue-router) (P3)
└── Task 2: npm install echarts (P3)

Phase 1 (Parallel — 3 人同时开工):
Person 1 (P1):
├── Task 3: ADS 拥堵表填充 (python SQL)
├── Task 4: 拥堵 API 端点
├── Task 5: 拥堵热力图前端组件
├── Task 6: 拥堵 API 测试
└── Task 7: 拥堵趋势/对比 API + 前端

Person 2 (P2):
├── Task 9: 司机休息 ETL (Julia)
├── Task 10: ADS 热力/司机表填充
├── Task 11: 热点 + 司机 API 端点
├── Task 12: 上下车热点前端组件
└── Task 13: 司机行为前端组件

Person 3 (P3):
├── Task 14: 数据取数系统 (API + 前后端)
├── Task 15: 数据血缘 + 质量 API
├── Task 16: 数据血缘 + 质量前端
└── Task 17: 各页面 API 测试

Phase 2 (Integration):
├── Task 18: 大屏框架拼装 + 全局时间轴
├── Task 19: mock 数据替换为真实 API
├── Task 20: 回归测试
└── Task 21: 演示彩排
```

### Dependency Matrix
- **0**: - → 1, 8, 9, 10, 14, 15
- **1**: 0 → 3
- **3**: 1 → 4
- **4**: 3 → 5, 6
- **5**: 4, 8 → 18
- **8**: 0 → 18
- **9**: 0 → 10
- **10**: 9 → 11
- **11**: 10 → 12, 13
- **12**: 11, 8 → 18
- **13**: 11, 8 → 18
- **14**: 0 → 18
- **15**: 0 → 16
- **16**: 15, 8 → 18
- **17**: 14, 15 → 18
- **18**: 5, 7, 12, 13, 14, 16, 17 → 19
- **19**: 18 → 20
- **20**: 19 → 21

---

## TODOs

### Phase 0: Foundation Tasks (ALL 3 人协作)

- [ ] 0. **API 合同文档定义 (P1/P2/P3 协作)**

  **What to do**:
  - 创建 `docs/api-contract.md`，定义所有新 Dashboard API 的请求/响应 JSON Schema
  - 定义以下 API 的数据格式：
    - `GET /api/dashboard/congestion/heatmap?dt=&hour=`
    - `GET /api/dashboard/congestion/trend?start_dt=&end_dt=`
    - `GET /api/dashboard/hotspot/map?dt=&hour=&event_type=`
    - `GET /api/dashboard/driver/behavior?dt=`
    - `GET /api/dashboard/driver/rest-heatmap?dt=`
    - `GET /api/dashboard/kpi?dt=`
    - `GET /api/dashboard/catalog/tables`
    - `GET /api/dashboard/catalog/fields/{table}`
    - `POST /api/dashboard/catalog/query`
    - `GET /api/dashboard/catalog/hot-fields`
    - `GET /api/dashboard/lineage`
    - `GET /api/dashboard/quality`

  **Must NOT do**:
  - 不修改现有 API 文档
  - 不涉及实现细节

  **Recommended Agent Profile**:
  > - Category: `unspecified-low` — 文档编写，轻量任务
  > - Skills: 无

  **Parallelization**: YES — Parallel Group: Phase 0 (with Task 1, 8, 2)
  **Blocks**: Task 1, 3, 4, 8, 9, 10, 14, 15
  **Blocked By**: None

  **Acceptance Criteria**:
  - [ ] docs/api-contract.md 创建完成
  - [ ] 包含全部 12 个 API 的 JSON Schema

  **Evidence to Capture:**
  - [ ] docs/api-contract.md 文件存在

  **Commit**: YES
  - Message: `docs(api): define dashboard API contract`
  - Files: `docs/api-contract.md`

---

- [ ] 1. **ADS DDL — 清除旧表，创建新表 (P1)**

  **What to do**:
  - 创建 SQL 脚本 `backend/src/main/resources/ddl-dashboard.sql`
  - DROP 5 张旧 ADS 表：
    ```sql
    DROP TABLE IF EXISTS ads.ads_congestion_heatmap_ts;
    DROP TABLE IF EXISTS ads.ads_driver_activity_ts;
    DROP TABLE IF EXISTS ads.ads_driver_shift_pattern_day;
    DROP TABLE IF EXISTS ads.ads_holiday_vs_workday_compare;
    DROP TABLE IF EXISTS ads.ads_pickup_dropoff_hotspot_ts;
    ```
  - CREATE 新 ADS 表（共 7 张）：
    - `ads.congestion_by_segment_hour` — 路段级拥堵含基线偏离度
    - `ads.dashboard_kpi` — 关键指标汇总
    - `ads.holiday_workday_comparison` — 日类型对比
    - `ads.hotspot_grid_enriched` — 上下车热点含供需失衡
    - `ads.driver_behavior_summary` — 司机行为汇总
    - `ads.data_field_access_log` — 数据取用日志
    - `ads.data_field_value_score` — 字段价值排行
  - 在数据库中执行该脚本

  **Must NOT do**:
  - 不删除 dw, ods, public 层任何表
  - 不删除 tdm 层任何表

  **Recommended Agent Profile**:
  > - Category: `unspecified-low` — SQL DDL，标准作业
  > - Skills: 无

  **Parallelization**: YES — Parallel Group: Phase 0
  **Blocks**: Task 3, 10
  **Blocked By**: Task 0

  **QA Scenarios**:
  ```
  Scenario: ADS 表创建验证
    Tool: Bash (python psycopg2)
    Steps:
      1. 执行 SQL 脚本
      2. psycopg2 连接数据库
      3. 查询 information_schema.tables WHERE table_schema='ads'
    Expected Result: 7 张新表都存在
    Evidence: .sisyphus/evidence/task-1-ads-tables.txt
  ```

  **Evidence to Capture:**
  - [ ] backend/src/main/resources/ddl-dashboard.sql 存在
  - [ ] 数据库验证 7 张新表创建成功

  **Commit**: YES
  - Message: `feat(ddl): recreate ADS tables for dashboard`
  - Files: `backend/src/main/resources/ddl-dashboard.sql`

---

- [ ] 8. **前端框架升级：vue-router + 布局重构 (P3)**

  **What to do**:
  - `npm install vue-router`（现有项目未用路由，需添加）
  - 创建 `frontend/src/router/index.js`，定义 7 条路由：
    - `/` → DashboardLayout（大屏框架）
    - `/congestion` → 拥堵页面组件（待 P1 填充）
    - `/trend` → 趋势对比页面（待 P1 填充）
    - `/hotspot` → 上下车热点页面（待 P2 填充）
    - `/driver` → 司机行为页面（待 P2 填充）
    - `/catalog` → 数据资产取数页面（P3 填充）
    - `/lineage` → 数据血缘页面（P3 填充）
  - 重构 `App.vue`：从全屏地图改为 `<router-view>` 布局
  - 创建 `DashboardLayout.vue`：统一的顶部导航栏 + 侧边栏 + 内容区域
  - 创建 6 个占位页面组件（内容为"数据加载中..."）
  - 现有地图页面保留至 `/map` 路由
  - 创建临时测试容器页面（P3 提供，让 P1/P2 独立测试各自组件）：
    - `TestContainer.vue`：可传递 mock props，显示组件标题 + 组件预览

  **Must NOT do**:
  - 不修改现有 `api.js`、`mapStore.js`
  - 不删除原有 `App.vue` 的地图逻辑（改为 `/map` 路由）

  **Recommended Agent Profile**:
  > - Category: `visual-engineering` — 前端重构，UI 布局
  > - Skills: 无

  **Parallelization**: YES — Parallel Group: Phase 0
  **Blocks**: Task 5, 7, 12, 13, 16, 18
  **Blocked By**: Task 0

  **QA Scenarios**:
  ```
  Scenario: 路由导航正常
    Tool: Playwright
    Steps:
      1. 导航到 http://localhost:5173/
      2. 点击导航栏中每个链接
    Expected Result: 路由切换正常，各页显示占位内容
    Evidence: .sisyphus/evidence/task-8-routing.gif

  Scenario: 现有地图页不受影响
    Tool: Playwright
    Steps:
      1. 导航到 http://localhost:5173/map
    Expected Result: 原来全屏地图正常显示
    Evidence: .sisyphus/evidence/task-8-existing-map.png
  ```

  **Evidence to Capture:**
  - [ ] router/index.js 存在
  - [ ] DashboardLayout.vue 存在
  - [ ] 路由可正常切换

  **Commit**: YES
  - Message: `feat(frontend): add vue-router and dashboard layout`
  - Files: `frontend/src/router/*`, `frontend/src/App.vue`, `frontend/src/layouts/*`, `frontend/src/pages/*`

---

- [ ] 2. **安装 ECharts 依赖 (P3)**

  **What to do**:
  - `npm install echarts vue-echarts` 或直接使用 `echarts` CDN
  - 在 `frontend/src/main.js` 中全局注册
  - 创建一个 `utils/chartTheme.js`，定义大屏暗色主题（匹配现有 `#0a0e1a` 背景，`#0cf` 强调色）

  **Must NOT do**:
  - 不升级现有 maplibre-gl / deck.gl 版本

  **Recommended Agent Profile**:
  > - Category: `quick` — npm 安装 + 配置
  > - Skills: 无

  **Parallelization**: YES — Parallel Group: Phase 0
  **Blocks**: Task 5, 7, 13, 16
  **Blocked By**: None

  **QA Scenarios**:
  ```
  Scenario: ECharts 安装验证
    Tool: Bash
    Steps:
      1. 检查 frontend/package.json 中 echarts 版本
    Expected Result: echarts 在 dependencies 中
    Evidence: .sisyphus/evidence/task-2-echarts.txt
  ```

  **Evidence to Capture:**
  - [ ] package.json 包含 echarts

  **Commit**: YES (groups with Task 8)
  - Message: `chore(deps): install echarts for dashboard charts`
  - Files: `frontend/package.json`, `frontend/src/utils/chartTheme.js`

---

### Phase 1: P1 继续 (拥堵分析) — 任务 3-7

- [ ] 3. **填充 ADS 拥堵相关表 (P1)**

  **What to do**:
  - 创建 Python 脚本 `scripts/fill_congestion_ads.py`，从 dw/tdm 填充 ADS 表：
  - `ads.congestion_by_segment_hour`：
    从 `dw.fact_congestion_seg_hour` + `tdm.congestion_baseline_5day` 插入，含偏差度列
  - `ads.dashboard_kpi`：按日期聚合总活跃车辆数、平均速度、总行程数、最拥堵路段 Top5
  - `ads.holiday_workday_comparison`：按 day_type + hour 聚合统计

  **Must NOT do**: 不修改 dw/tdm 表

  **Parallelization**: YES — Phase 1 (with Task 9)
  **Blocks**: Task 4 | **Blocked By**: Task 1, 0

  **QA Scenarios**:
  ```
  Scenario: ADS 拥堵表数据验证
    Tool: Bash (python psycopg2)
    Steps: SELECT COUNT(*) FROM ads.congestion_by_segment_hour
    Expected Result: > 0 行
    Evidence: .sisyphus/evidence/task-3-congestion-count.txt
  ```
  **Commit**: YES — `feat(etl): populate ADS congestion tables`

---

- [ ] 4. **拥堵分析 API 端点 (P1)**

  **What to do**:
  - 创建 `DashboardController.java`、`DashboardService.java`、`DashboardRepository.java`
  - 实现：`GET /api/dashboard/congestion/heatmap`, `/kpi`, `/trend`, `/comparison`

  **Must NOT do**: 不修改 TaxiController 或现有 API

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 5, 6 | **Blocked By**: Task 3, 0

  **QA Scenarios**:
  ```
  Scenario: 热力图 API
    Tool: Bash (curl)
    Steps: curl http://localhost:8081/api/dashboard/congestion/heatmap?dt=2015-01-05&hour=12
    Expected Result: 200 + JSON
    Evidence: .sisyphus/evidence/task-4-heatmap-api.json
  ```
  **Commit**: YES — `feat(api): add congestion dashboard endpoints`

---

- [ ] 5. **拥堵热力图 + Top10 排行前端 (P1)**

  **What to do**:
  - 创建 `dashboardApi.js`、`dashboardStore.js`
  - 创建 `CongestionPage.vue`：MapLibre 地图 + deck.gl HeatmapLayer + 侧边栏 Top10 ECharts 排行 + 时间轴 slider

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 18 | **Blocked By**: Task 4, 8, 2

  **QA Scenarios**:
  ```
  Scenario: 页面加载
    Tool: Playwright
    Steps: 导航到 /congestion，拖动时间轴到 hour=12
    Expected Result: 地图显示热力图，侧边栏 Top10
    Evidence: .sisyphus/evidence/task-5-congestion-page.png
  ```
  **Commit**: YES — `feat(frontend): add congestion heatmap page`

---

- [ ] 6. **拥堵模块 API 测试 (P1)**

  **What to do**:
  - 创建 `scripts/smoke-dashboard.ps1`，测试所有 Dashboard API 返回 200

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 18 | **Blocked By**: Task 4

  **QA Scenarios**:
  ```
  Scenario: 烟雾测试
    Tool: Bash (PowerShell)
    Steps: 执行 smoke-dashboard.ps1
    Expected Result: 全部 ✓
    Evidence: .sisyphus/evidence/task-6-smoke.txt
  ```
  **Commit**: YES (with 5) — `test(dashboard): add smoke test script`

---

- [ ] 7. **拥堵趋势 + 日类型对比页面 (P1)**

  **What to do**:
  - 创建 `TrendPage.vue`：上半 5 天折线图 + 下半工作日/节假日分组柱状图

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 18 | **Blocked By**: Task 4, 8, 2

  **QA Scenarios**:
  ```
  Scenario: 趋势页面
    Tool: Playwright
    Steps: 导航到 /trend
    Expected Result: 折线图和柱状图正常渲染
    Evidence: .sisyphus/evidence/task-7-trend-page.png
  ```
  **Commit**: YES (with 5) — `feat(frontend): add trend and comparison page`

---

### Phase 1: P3 (数据平台展示) — 任务 14-17

- [ ] 14. **数据资产取数系统 (P3)**

  **What to do**:
  - **数据层**：填充 `ads.data_field_access_log` 和 `ads.data_field_value_score`
    - `access_log`：记录每次查询的用户、查询的表、选择的字段、时间戳
    - `value_score`：按字段聚合查询次数 × 下游引用数 = 价值评分
  - **后端 API**（在 DashboardController 中添加）：
    - `GET /api/dashboard/catalog/tables` — TDM 层表列表（含行数、字段数）
    - `GET /api/dashboard/catalog/fields/{table}` — 指定表的字段详情 + 数据预览（前 5 行）
    - `POST /api/dashboard/catalog/query` — 取数请求，记录日志，返回数据
    - `GET /api/dashboard/catalog/hot-fields` — 热度排行榜
  - **前端** — 创建 `CatalogPage.vue`：
    - 左侧：TDM 表树形目录（表名 → 展开 → 字段列表）
    - 右上：选择字段后的数据预览表格
    - 右下：ECharts 柱状图（字段热度价值排行 Top10）

  **Must NOT do**:
  - 不暴露 ODS/DW/ADS 层数据，仅限 TDM 层

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 18, 17 | **Blocked By**: Task 0

  **QA Scenarios**:
  ```
  Scenario: 目录 API
    Tool: Bash (curl)
    Steps:
      1. curl http://localhost:8081/api/dashboard/catalog/tables
      2. curl http://localhost:8081/api/dashboard/catalog/fields/congestion_baseline_5day
    Expected Result: 返回 TDM 表列表和各字段详情
    Evidence: .sisyphus/evidence/task-14-catalog-api.json

  Scenario: 取数操作
    Tool: Playwright
    Steps: 导航到 /catalog → 展开 TDM 表 → 勾选字段 → 点击查询 → 查看预览
    Expected Result: 数据预览正常展示
    Evidence: .sisyphus/evidence/task-14-query-page.png
  ```
  **Commit**: YES — `feat(catalog): add data asset catalog with field tracking`

---

- [ ] 15. **数据血缘 + 质量 API (P3)**

  **What to do**:
  - **后端**（在 DashboardController/Repository 中添加）：
    - `GET /api/dashboard/lineage` — 返回血缘关系数据（nodes: 各层表名 + edges: 依赖关系）
    - `GET /api/dashboard/quality` — 返回各表质量指标（行数、空值率、覆盖率、最后更新）
  - **血缘数据定义**（硬编码，非动态查询）：
    - ODS: ods_taxi_trips_raw, ods_trip_event_raw, ods_trip_match_raw → DW
    - DW: fact_congestion_seg_hour, dim_calendar, dim_grid, dim_road_segment → TDM
    - TDM: congestion_baseline_5day, driver_shift_pattern, grid_hotspot_score → ADS
    - ADS: 7 张新表

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 16, 17 | **Blocked By**: Task 0

  **QA Scenarios**:
  ```
  Scenario: 血缘 API
    Tool: Bash (curl)
    Steps: curl http://localhost:8081/api/dashboard/lineage
    Expected Result: 返回 JSON 包含 nodes 和 edges
    Evidence: .sisyphus/evidence/task-15-lineage-api.json
  ```
  **Commit**: YES — `feat(api): add data lineage and quality endpoints`

---

- [ ] 16. **数据血缘 + 质量前端页面 (P3)**

  **What to do**:
  - 创建 `LineagePage.vue`：
    - 上半：数据血缘 DAG 图（使用 ECharts Graph 或 D3.js 力导向图）
      - 4 层（ODS/DW/TDM/ADS），每层水平排列
      - 节点 = 表名，边 = 数据流向，箭头表示方向
      - 悬浮显示表行数等元信息
    - 下半：数据质量仪表盘
      - 每张表的卡片形式，显示：表名、Schema、行数、字段数、最后更新时间
      - 空值率 > 5% 的字段标黄，> 20% 标红

  **Must NOT do**:
  - 不做动态 DAG 引擎，用硬编码的 nodes/edges

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 18 | **Blocked By**: Task 15, 8, 2

  **QA Scenarios**:
  ```
  Scenario: 血缘页面
    Tool: Playwright
    Steps: 导航到 /lineage
    Expected Result: DAG 图显示 4 层数据流向，质量卡片显示各表信息
    Evidence: .sisyphus/evidence/task-16-lineage-page.png

  Scenario: 卡片颜色标注
    Tool: Playwright
    Steps: 检查有空值率 > 5% 的卡片
    Expected Result: 对应字段标黄或标红
    Evidence: .sisyphus/evidence/task-16-quality-cards.png
  ```
  **Commit**: YES — `feat(frontend): add data lineage and quality dashboard page`

---

- [ ] 17. **全量 Dashboard API 烟雾测试 (P3)**

  **What to do**:
  - 补充 `scripts/smoke-dashboard.ps1`，加入 P2 + P3 的 API 测试：
    - `/dashboard/hotspot/map`
    - `/dashboard/driver/behavior`
    - `/dashboard/driver/rest-heatmap`
    - `/dashboard/catalog/tables`
    - `/dashboard/catalog/fields/{table}`
    - `/dashboard/catalog/hot-fields`
    - `/dashboard/lineage`
    - `/dashboard/quality`
  - 回归测试现有 API：
    - `/api/map/boundary`
    - `/api/taxi/stats`

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 18 | **Blocked By**: Task 14, 15

  **QA Scenarios**:
  ```
  Scenario: 全量烟雾测试
    Tool: Bash (PowerShell)
    Steps: 执行 scripts/smoke-dashboard.ps1
    Expected Result: 全部 ✓，包括回归测试
    Evidence: .sisyphus/evidence/task-17-full-smoke.txt
  ```
  **Commit**: YES (with 14/16) — `test(dashboard): complete smoke tests for all dashboard APIs`

### Phase 2: 集成 (P3 主导，P1/P2 配合)

- [ ] 18. **大屏框架拼装 + 全局时间轴 (P3)**

  **What to do**:
  - 从 P1/P2 拿到各页面组件，放入 DashboardLayout
  - 统一导航栏：顶部页面 Tab 切换（拥堵 / 趋势 / 热点 / 司机 / 数据资产 / 数据血缘）
  - 全局时间轴控制器：
    - 置于大屏顶部或底部固定栏
    - 包含：日期选择（2015-01-03 ~ 2015-01-07）+ 小时 slider（0-23）
    - 使用 Pinia store 管理全局时间状态
    - P1/P2 各页面通过 store 响应时间变化并刷新数据
  - 将 P3 的 mock 数据替换为真实 API 调用
  - KPI 指标卡片带自动增长动画

  **Must NOT do**:
  - 不修改 P1/P2 页面内部逻辑（通过 props/store 传递全局状态）

  **Parallelization**: NO — Sequential (依赖所有 Phase 1 完成)
  **Blocks**: Task 19 | **Blocked By**: Task 5, 7, 12, 13, 14, 16, 17

  **QA Scenarios**:
  ```
  Scenario: 全页面导航
    Tool: Playwright
    Steps: 从首页开始，依次点击每个 Tab
    Expected Result: 各页面组件正常加载，无空白或报错
    Evidence: .sisyphus/evidence/task-18-all-pages.gif

  Scenario: 全局时间轴联动
    Tool: Playwright
    Steps: 选择 2015-01-05, hour=12，然后切换到不同页面
    Expected Result: 所有页面数据响应同一时间选择
    Evidence: .sisyphus/evidence/task-18-timeline-sync.png
  ```
  **Commit**: YES — `feat(dashboard): integrate all pages with global timeline control`

---

- [ ] 19. **回归测试 + 性能验证 (P3)**

  **What to do**:
  - 确认现有地图页面（/map）不受影响
  - 确认所有 7 条路由可访问
  - 确认无控制台报错

  **Parallelization**: NO
  **Blocks**: Task 20 | **Blocked By**: Task 18

  **QA Scenarios**:
  ```
  Scenario: 回归测试
    Tool: Playwright
    Steps: 导航到 /map，确认地图正常
    Expected Result: 全屏地图正常显示，原轨迹查询功能可用
    Evidence: .sisyphus/evidence/task-19-regression.png

  Scenario: 全流程演示
    Tool: Playwright
    Steps: 按演示顺序 / → /congestion → /trend → /hotspot → /driver → /catalog → /lineage
    Expected Result: 每页加载 < 3秒，无错误
    Evidence: .sisyphus/evidence/task-19-demo-flow.gif
  ```
  **Commit**: YES — `fix(dashboard): regression fixes and final polish`

---

- [ ] 20. **演示彩排 (全部 3 人)**

  **What to do**:
  - 编写演示脚本（5-10 分钟）：
    1. 首页 KPI 概览（30s）
    2. 拥堵热力图 + 时间轴演示（1min）
    3. 拥堵趋势 + 日类型对比（1min）
    4. 上车/下车热点 + 切换（1min）
    5. 司机行为分析（班次、休息地段）（1min）
    6. 数据资产取数 + 热度排行（1min30s）
    7. 数据血缘 + 质量监控（1min）
  - 确认每页在教师演示场景下的显示效果
  - 确认所有数据加载正常

  **Parallelization**: NO
  **Blocks**: None | **Blocked By**: Task 19

  **Evidence to Capture:**
  - [ ] 演示脚本文档存在
  - [ ] 彩排全程无错误

  **Commit**: NO

---

### Phase 1: P2 (司机 + 热点) — 任务 9-13

- [ ] 9. **司机休息地段 ETL (P2)**

  **What to do**:
  - 创建 Julia 脚本 `julia/driver_rest_etl.jl`
  - 算法：对每辆车的轨迹按 `tms` 排序 → 计算前后点的时间差 → 差 > 120 秒（使用 `parameters.jl:MAX_GAP_SECONDS=120`）记为休息事件
  - 对每次休息事件：取最后活跃点的位置作为休息地点，记录休息时长（分钟）、开始时间、结束时间
  - 输出到 `tdm.driver_rest_location`（需先创建该表）
  - 处理边缘情况：跨日、司机无休息（连续工作）

  **Must NOT do**: 不修改 ods 原表

  **Parallelization**: YES — Phase 1 (with Task 3)
  **Blocks**: Task 10 | **Blocked By**: Task 0

  **QA Scenarios**:
  ```
  Scenario: ETL 执行
    Tool: Bash
    Steps: 运行 julia driver_rest_etl.jl
    Expected Result: tdm.driver_rest_location 表有数据
    Evidence: .sisyphus/evidence/task-9-rest-location-count.txt
  ```
  **Commit**: YES — `feat(etl): add driver rest location inference script`

---

- [ ] 10. **填充 ADS 热点 + 司机表 (P2)**

  **What to do**:
  - 创建 `scripts/fill_hotspot_driver_ads.py`
  - `ads.hotspot_grid_enriched`：从 `tdm.grid_hotspot_score` 加入供需失衡指标（上车/下车数比值）
  - `ads.driver_behavior_summary`：从 `tdm.driver_shift_pattern` + `tdm.driver_rest_location` 汇聚

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 11 | **Blocked By**: Task 9, 1, 0

  **QA Scenarios**:
  ```
  Scenario: ADS 热点表
    Tool: Bash (python)
    Steps: SELECT COUNT(*) FROM ads.hotspot_grid_enriched
    Expected Result: > 0 行
    Evidence: .sisyphus/evidence/task-10-hotspot-count.txt
  ```
  **Commit**: YES — `feat(etl): populate ADS hotspot and driver tables`

---

- [ ] 11. **热点 + 司机 API 端点 (P2)**

  **What to do**:
  - 在 DashboardController/Service/Repository 中添加端点：
    - `GET /api/dashboard/hotspot/map?dt=&hour=&event_type=`（event_type: pickup/dropoff）
    - `GET /api/dashboard/driver/behavior?dt=`（班次分布数据）
    - `GET /api/dashboard/driver/rest-heatmap?dt=`（休息地段数据）

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 12, 13 | **Blocked By**: Task 10, 0

  **QA Scenarios**:
  ```
  Scenario: 热点 API
    Tool: Bash (curl)
    Steps: curl http://localhost:8081/api/dashboard/hotspot/map?dt=2015-01-05&hour=12&event_type=pickup
    Expected Result: 200 + JSON
    Evidence: .sisyphus/evidence/task-11-hotspot-api.json
  ```
  **Commit**: YES — `feat(api): add hotspot and driver behavior endpoints`

---

- [ ] 12. **上下车热点地图页面 (P2)**

  **What to do**:
  - 创建 `HotspotPage.vue`：
    - MapLibre + deck.gl HeatmapLayer 展示热点
    - 切换按钮：上车(pickup)/下车(dropoff)/叠加
    - 侧边栏：ECharts Top10 热点区域排行

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 18 | **Blocked By**: Task 11, 8, 2

  **QA Scenarios**:
  ```
  Scenario: 热点页面
    Tool: Playwright
    Steps: 导航到 /hotspot，切换上车/下车
    Expected Result: 地图热力图随切换变化
    Evidence: .sisyphus/evidence/task-12-hotspot-page.png
  ```
  **Commit**: YES — `feat(frontend): add hotspot map page`

---

- [ ] 13. **司机行为分析页面 (P2)**

  **What to do**:
  - 创建 `DriverPage.vue`：
    - 左：ECharts 饼图（班次模式分布：full_day/dual_peak/morning_peak/evening_peak/night_owl）
    - 右上：ECharts 箱线图/柱状图（各模式的平均活跃时长分布）
    - 右下：MapLibre + deck.gl ScatterplotLayer 展示休息地段分布

  **Parallelization**: YES — Phase 1
  **Blocks**: Task 18 | **Blocked By**: Task 11, 8, 2

  **QA Scenarios**:
  ```
  Scenario: 司机页面
    Tool: Playwright
    Steps: 导航到 /driver
    Expected Result: 饼图、活跃时长图、休息地段地图均渲染
    Evidence: .sisyphus/evidence/task-13-driver-page.png
  ```
  **Commit**: YES — `feat(frontend): add driver behavior analysis page`

---

## Final Verification Wave

> 4 review agents run in PARALLEL. ALL must APPROVE. Present consolidated results to user and get explicit "okay" before completing.

- [ ] F1. **Plan Compliance Audit** — `oracle`
  Read the plan end-to-end. For each "Must Have": verify implementation exists (read file, curl endpoint, run command). For each "Must NOT Have": search codebase for forbidden patterns — reject with file:line if found. Check evidence files exist in .sisyphus/evidence/. Compare deliverables against plan.
  Output: `Must Have [N/N] | Must NOT Have [N/N] | Tasks [N/N] | VERDICT: APPROVE/REJECT`

- [ ] F2. **Code Quality Review** — `unspecified-high`
  Run `tsc --noEmit` + linter + basic checks. Review all changed files for: `as any`/`@ts-ignore`, empty catches, console.log in prod, commented-out code, unused imports. Check AI slop: excessive comments, over-abstraction, generic names (data/result/item/temp).
  Output: `Build [PASS/FAIL] | Lint [PASS/FAIL] | Tests [N pass/N fail] | VERDICT`

- [ ] F3. **Real Manual QA** — `unspecified-high` (+ `playwright` skill)
  Start from clean state. Execute EVERY QA scenario from EVERY task — follow exact steps, capture evidence. Test cross-task integration (all pages working together, global timeline syncing). Test edge cases: missing backend, empty data state. Save to `.sisyphus/evidence/final-qa/`.
  Output: `Scenarios [N/N pass] | Integration [N/N] | Edge Cases [N tested] | VERDICT`

- [ ] F4. **Scope Fidelity Check** — `deep`
  For each task: read "What to do", read actual diff (git log/diff). Verify 1:1 — everything in spec was built (no missing), nothing beyond spec was built (no creep). Check "Must NOT do" compliance. Detect cross-task contamination: Task N touching Task M's files.
  Output: `Tasks [N/N compliant] | Contamination [CLEAN/N issues] | VERDICT`

---

## Commit Strategy

> Each person commits to their own feature branch. Squash-merge to main at Phase 2 integration.

### Commit Message Convention
```
type(scope): description
```
- `feat(dashboard-api): add congestion heatmap endpoint`
- `feat(dashboard-frontend): add hotspot map component`
- `feat(etl): add driver rest location inference script`
- `chore(deps): install echarts and vue-router`
- `fix(api): handle null congestion baseline values`
- `test(dashboard): add smoke tests for dashboard endpoints`

---

## Success Criteria

### Verification Commands
```bash
# API smoke tests
curl http://localhost:8081/api/dashboard/congestion/heatmap?dt=2015-01-05&hour=12
curl http://localhost:8081/api/dashboard/hotspot/map?dt=2015-01-05&hour=12&event_type=pickup
curl http://localhost:8081/api/dashboard/driver/behavior?dt=2015-01-05
curl http://localhost:8081/api/dashboard/catalog/tables
curl http://localhost:8081/api/dashboard/lineage

# ADS table verification
python -c "import psycopg2; conn=psycopg2.connect(...); cur=conn.cursor(); cur.execute('SELECT COUNT(*) FROM ads.congestion_by_segment_hour'); print(cur.fetchone())"

# Regression test - existing endpoints still work
curl http://localhost:8081/api/map/boundary?page=0&size=1
curl http://localhost:8081/api/taxi/stats?startTime=2015-01-03T00:00:00&endTime=2015-01-03T00:30:00
```

### Final Checklist
- [ ] 所有 6 页可正常访问
- [ ] 时间轴联动生效
- [ ] 取数界面可完整操作
- [ ] 数据血缘图正确展示各层流向
- [ ] 现有地图页面不受影响
- [ ] 所有 API smoke test 通过
- [ ] 演示流程走通（15 分钟全覆盖）
# 哈尔滨交通数据中台（Data Platform）— 数据大屏

本仓库用于出租车轨迹数据的离线处理与在线可视化。当前扩展为包含 **6 页数据大屏**，供课程演示/领导汇报使用。

## 项目结构

```
Data_Platform-main/
├── backend/                      # Spring Boot 后端 API
│   └── src/main/java/com/harbin/dataplatform/
│       ├── controller/
│       │   ├── TaxiController.java        # 原有地图/轨迹 API
│       │   ├── MapController.java         # 原有地图边界 API
│       │   └── DashboardController.java   # 新增大屏 API（各人添加）
│       ├── service/                       # 业务逻辑
│       ├── repository/
│       │   ├── TaxiRepository.java        # 原有
│       │   └── DashboardRepository.java   # 新增大屏 SQL（各人添加）
│       └── dto/                           # 新增大屏 DTO
├── frontend/                     # Vue 3 前端
│   └── src/
│       ├── router/index.js                # 7 条路由（6 个大屏页 + 1 个原地图）
│       ├── layouts/DashboardLayout.vue    # 大屏框架（导航栏 + 时间轴）
│       ├── pages/                         # 7 个页面
│       │   ├── CongestionPage.vue         # P1: 拥堵热力图
│       │   ├── TrendPage.vue              # P1: 5 天趋势
│       │   ├── HotspotPage.vue            # P2: 上下车热点
│       │   ├── DriverPage.vue             # P2: 司机行为
│       │   ├── CatalogPage.vue            # P3: 数据资产取数
│       │   ├── LineagePage.vue            # P3: 数据血缘 + 质量
│       │   └── OriginalMapPage.vue        # 保留原地图
│       ├── services/dashboardApi.js       # 新增加 API 调用
│       ├── stores/dashboardStore.js       # 新增时间状态管理
│       └── utils/chartTheme.js            # ECharts 暗色主题
├── scripts/
│   ├── start-backend.ps1          # 启动后端
│   ├── stop-backend.ps1           # 停止后端
│   ├── smoke-backend.ps1          # 原有 API 冒烟测试
│   ├── smoke-dashboard.ps1        # 新增大屏 API 冒烟测试（各人补充）
│   ├── fill_congestion_ads.py     # P1: 填充 ADS 拥堵表
│   └── fill_hotspot_driver_ads.py # P2: 填充 ADS 热点 + 司机表
├── julia/
│   ├── driver_rest_etl.jl         # P2: 司机休息地段推断 ETL
│   └── (原有脚本)
├── docs/
│   └── api-contract.md            # API 合同（已提前定义好）
└── .sisyphus/plans/               # 开发计划（含三份分工文档）
   ├── README-NEW-PLAN.md                # 总体规划
   ├── p1-execution-plan.md              # P1 执行计划
   ├── p2-execution-plan.md              # P2 执行计划
   ├── p3-execution-plan.md              # P3 执行计划
   └── api-contract-reference.md         # API 合同参考
```

## 3 人分工

| 角色 | 负责页面 | 核心工作 |
|------|---------|---------|
| **Person 1** | 拥堵热力图 + 5天趋势对比 | 填充 ADS 表 → 后端 API → 前端地图+ECharts |
| **Person 2** | 上下车热点 + 司机行为分析 | Julia ETL 推断休息 → 填充 ADS → 后端 API → 前端 |
| **Person 3** | 数据资产取数 + 血缘质量 + 集成 | 取数系统 → 血缘展示 → 最后合并所有人代码 |

每人都有自己的 `.sisyphus/plans/p1-execution-plan.md`、`.sisyphus/plans/p2-execution-plan.md`、`.sisyphus/plans/p3-execution-plan.md` 文件，里面包含按顺序执行的任务和代码示例。

## 技术栈

- **后端**：Java 17、Spring Boot 3、JdbcTemplate、PostgreSQL 15.4（+ PostGIS）
- **前端**：Vue 3、Vite 5、Pinia、vue-router 4、ECharts 6、MapLibre GL 4.7、deck.gl 8.9
- **离线**：Julia（LibPQ + DataFrames）
- **数据库分层**：ODS（原始）→ DW（建模）→ TDM（算法模型）→ ADS（展示）

## 快速启动

```powershell
# 1. 启动后端
powershell -ExecutionPolicy Bypass -File .\scripts\start-backend.ps1 -KillPortOwner

# 2. 启动前端
cd frontend
npm install
npm run dev

# 3. 打开浏览器
# http://localhost:5173/congestion  ← 大屏首页
# http://localhost:5173/map          ← 原地图

# 4. 冒烟测试
powershell -ExecutionPolicy Bypass -File .\scripts\smoke-backend.ps1
```

## 数据库

| 信息 | 值 |
|------|-----|
| 类型 | PostgreSQL 15.4 + PostGIS 3.3.4 |
| 地址 | `101.35.234.65:5432` |
| 数据库 | `postgres` |
| 用户 | `osmuser` |
| 密码 | `pass` |

### 数据分层

| 层 | 说明 | 示例表 |
|----|------|--------|
| **ODS** | 原始数据（134 万行轨迹） | `ods_taxi_trips_raw` |
| **DW** | 维度建模 | `fact_congestion_seg_hour`、`dim_grid` 等 |
| **TDM** | 算法模型 | `congestion_baseline_5day`(83万) `driver_shift_pattern`(5.5万) `grid_hotspot_score`(18.8万) |
| **ADS** | 展示层（已重建，7 张新表） | `congestion_by_segment_hour`、`hotspot_grid_enriched` 等 |

### 数据说明

- 数据范围：**2015-01-03 ~ 2015-01-07**（共 5 天，含节假日到工作日）
- 空间范围：哈尔滨市区（lon 126.0~127.2, lat 45.4~46.2）
- 时区：UTC（代码和数据一致）

## 数据大屏 6 个页面

| 页码 | 路由 | P1 | P2 | P3 |
|------|------|----|----|----|
| 1 | `/congestion` | 拥堵热力图 + Top10 排行 | - | - |
| 2 | `/trend` | 5 天趋势 + 日类型对比 | - | - |
| 3 | `/hotspot` | - | 上下车热点切换 + 排行 | - |
| 4 | `/driver` | - | 班次饼图 + 活跃时长 + 休息地图 | - |
| 5 | `/catalog` | - | - | 取数界面 + 字段热度 |
| 6 | `/lineage` | - | - | 血缘 DAG + 质量卡片 |

## 常见问题

1. **后端启动失败，8081 端口占用**
   ```powershell
   .\scripts\start-backend.ps1 -KillPortOwner
   ```

2. **前端请求不到数据**
   - 确认后端已启动（`curl http://localhost:8081/api/map/boundary`）
   - 确认 `frontend/vite.config.js` 中 `/api` 代理指向 `http://localhost:8081`

3. **数据库连不上**
   - 先 ping `101.35.234.65` 确认网络可达
   - 课程展示时建议连校园网或 VPN

4. **三份分工文档在哪？**
   - `.sisyphus/plans/p1-execution-plan.md` — 给 P1
   - `.sisyphus/plans/p2-execution-plan.md` — 给 P2
   - `.sisyphus/plans/p3-execution-plan.md` — 给 P3
   - `.sisyphus/plans/api-contract-reference.md` — API 合同参考
   - `docs/api-contract.md` — 完整 API 合同（由 Sisyphus 写入）

## 开发文档

更完整的启动顺序、排障方法和脚本说明见：`docs/DEVELOPMENT.md`
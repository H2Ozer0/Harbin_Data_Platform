# 👤 Person 1 —— 拥堵分析模块执行计划

**项目**：交通数据大屏  
**负责人**：Person 1  
**周期**：7 个工作日（步骤式执行）  
**总工时**：20小时  
**页面**：`/congestion`（热力图）、`/trend`（趋势分析）

---

## 📊 任务清单

| # | 任务 | 工时 | 完成状态 | 优先级 | 依赖 |
|---|------|------|---------|--------|------|
| **T1** | 环境准备 + 分支创建 | 1h | ⬜ | 🔴 | API合同 |
| **T2** | Python脚本填充拥堵表 | 3h | ⬜ | 🔴 | ADS表就绪 |
| **T3** | 拥堵API实现 | 4h | ⬜ | 🔴 | T2完成 |
| **T4** | /congestion 页面 | 6h | ⬜ | 🔴 | T3完成 |
| **T5** | /trend 页面 | 4h | ⬜ | 🟡 | T3完成 |
| **T6** | API烟雾测试脚本 | 1h | ⬜ | 🟡 | T3完成 |
| **T7** | 集成冒烟修复 | 1h | ⬜ | 🟡 | P3反馈 |

**总计**：20h

---

## 📅 执行步骤

### 步骤 1：准备环境并创建分支 [1h]

```
08:00-09:00 [1h]  环境验证 + Git分支 
  □ 验证 node >= 18, java >= 17, python >= 3.9
  □ git checkout main && git pull origin main
  □ git checkout -b feature/p1
  □ 确认后续所有提交都在 feature/p1 完成
  □ 克隆一份api-contract-reference.md本地参考
  □ 与P3确认dashboardStore共用框架
  □ 与P3确认DashboardController所有权规则
```

**输出**：分支 `feature/p1` 就绪，可开发

---

### 步骤 2：填充拥堵表并开始后端 API [8h]

```
08:00-09:30 [1.5h] T2: 填充拥堵表 (Python脚本)
  □ 创建文件: scripts/fill_congestion_ads.py
  □ 实现逻辑：
    - 连接PostgreSQL
    - 从 dw.fact_congestion_seg_hour 提取数据
    - LEFT JOIN tdm.congestion_baseline_5day 获取基线速度
    - 计算 deviation_pct = (avg_speed - baseline) / baseline * 100
    - 处理NULL情况（5.6% baseline_speed为NULL）
    - 填充到 ads.congestion_by_segment_hour
  □ 测试脚本：python scripts/fill_congestion_ads.py
  □ 验证行数：SELECT COUNT(*) FROM ads.congestion_by_segment_hour

09:30-09:45 [0.25h] 休息

09:45-12:30 [2.75h] T3: 拥堵API实现（开始）
  □ 创建文件: backend/src/main/java/com/harbin/dataplatform/controller/DashboardController.java
  □ 实现方法（参考 api-contract-reference.md）:
    - GET /api/dashboard/congestion/heatmap?dt=2015-01-05&hour=12&day_type=workday
    - GET /api/dashboard/kpi?dt=2015-01-05
    - GET /api/dashboard/congestion/comparison
    - GET /api/dashboard/congestion/trend?start_dt=2015-01-03&end_dt=2015-01-07
  □ 创建对应的DTO、Service、Repository
  □ 通过Postman测试前2个端点
  □ 提交代码到feature/p1分支

12:30-13:30 [1h] 午餐

13:30-17:00 [3.5h] T4: /congestion 前端页面（开始）
  □ 创建文件: frontend/src/pages/CongestionPage.vue
  □ 设计布局：上方地图热力图 + 下方Top10排行榜
  □ 集成dashboardApi中的API stub
  □ 实现时间轴联动逻辑
  □ 使用ECharts展示排行榜（暗色主题）
  □ 基本样式完成，但数据用mock（P3冒烟时才用真实API）
```

**输出**：
- ✅ 拥堵表数据已填充
- ⏳ API基本实现，待烟雾测试
- ⏳ 前端框架搭建，待功能完善

---

### 步骤 3：完善拥堵 API 和前端页面 [8h]

```
08:00-09:00 [1h]  T3: 拥堵API完善
  □ 补充异常处理（日期格式验证、day_type合法性等）
  □ 完整测试所有4个拥堵API端点
  □ 与P3讨论是否需要修改DashboardController基类（若需要通知P3）

09:00-09:15 [0.25h] 休息

09:15-12:30 [3.25h] T4: /congestion 页面完善
  □ 集成实际地图库（MapLibre + deck.gl）
  □ 完成拥堵热力图可视化
  □ 完成排行榜Top10展示
  □ 时间轴选择器与热力图联动
  □ 错误处理（API超时、无数据等）

12:30-13:30 [1h] 午餐

13:30-17:00 [3.5h] T5: /trend 页面
  □ 创建文件: frontend/src/pages/TrendPage.vue
  □ 上半部分：5天折线趋势图
  □ 下半部分：工作日/节假日对比柱状图
  □ ECharts暗色主题配置
  □ 与时间轴联动
```

**输出**：
- ✅ API全部实现+测试
- ✅ /congestion 页面可用
- ⏳ /trend 页面框架完成，待细节调整

---

### 步骤 4：等待 P3 冒烟反馈 [1h]

```
08:00-09:00 [1h]  等待P3冒烟反馈
  □ 不主动提交代码到main
  □ 保留feature/p1分支
  □ 监听P3的冒烟测试反馈
  □ 如有即时bug，可小范围修复
```

**输出**：P3冒烟测试完成，记录问题清单

---

### 步骤 5：完善并修复遗留问题 [1h]

```
08:00-09:00 [1h]  基于P3冒烟反馈修复
  □ 查看P3的冒烟问题清单
  □ 修复影响P1功能的bug
  □ 完善/trend页面细节
  □ 本地验证修复后的功能
  □ 提交最新代码到feature/p1
```

**输出**：P1功能自测通过

---

### 步骤 6：配合回归测试 [1h]

```
08:00-09:00 [1h]  配合P3进行回归测试
  □ 跟随P3的回归测试清单
  □ 对P1相关功能进行端到端测试
  □ 记录任何遗留问题
  □ 与P3/P2沟通问题优先级
```

**输出**：P1功能回归通过

---

### 步骤 7：等待最终合并 [0h]

```
08:00-09:00  等待P3最终合并
  □ 不需要做新工作
  □ feature/p1分支由P3审核合并到main
```

**总结**：项目完成 ✅

---

## 🔧 代码目录结构

### 后端代码（Java）

```
backend/src/main/java/com/harbin/dataplatform/
├── controller/
│   └── DashboardController.java          ← 你负责这个文件的拥堵部分
│                                           (其他P2/P3负责各自部分)
├── service/
│   ├── DashboardService.java             ← 业务逻辑
│   └── CongestionService.java (可选)      ← 拥堵专用服务
├── repository/
│   └── DashboardRepository.java          ← 数据层
└── dto/
    ├── CongestionHeatmapDTO.java
    ├── KPIResponseDTO.java
    ├── TrendResponseDTO.java
    └── ComparisonResponseDTO.java
```

### 前端代码（Vue）

```
frontend/src/
├── pages/
│   ├── CongestionPage.vue                ← 拥堵热力图+排行榜
│   ├── TrendPage.vue                     ← 5天趋势+日类型对比
│   └── ...
├── services/
│   └── dashboardApi.js                   ← 由P3维护，包含13个API stub
├── stores/
│   └── dashboardStore.js                 ← 由P3维护，全局时间状态
└── utils/
    └── chartTheme.js                     ← ECharts暗色主题（已就绪）
```

### 数据处理（Python）

```
scripts/
└── fill_congestion_ads.py                ← 填充拥堵ADS表
```

---

## 🚀 API实现细节

### 1. GET /api/dashboard/congestion/heatmap

```java
@GetMapping("/congestion/heatmap")
public ResponseEntity<HeatmapResponse> getHeatmap(
    @RequestParam String dt,          // YYYY-MM-DD
    @RequestParam int hour,           // 0-23
    @RequestParam String day_type     // workday/weekend/holiday
) {
    // 查询条件
    // dt = 日期
    // hour_of_day = hour
    // day_type = day_type
    
    // 返回格式（见api-contract-reference.md）
    HeatmapResponse response = new HeatmapResponse();
    response.setSegments(segments);
    response.setTotal(total);
    return ResponseEntity.ok(response);
}
```

### 2. GET /api/dashboard/kpi

```java
@GetMapping("/kpi")
public ResponseEntity<KPIResponse> getKPI(
    @RequestParam String dt  // YYYY-MM-DD
) {
    // 查询单天的关键指标
    // 返回格式见api-contract-reference.md
}
```

### 3. GET /api/dashboard/congestion/comparison

```java
@GetMapping("/congestion/comparison")
public ResponseEntity<ComparisonResponse> getComparison() {
    // 工作日 vs 周末 vs 节假日，按小时对比
    // 返回24个小时 × 3个日类型
}
```

### 4. GET /api/dashboard/congestion/trend

```java
@GetMapping("/congestion/trend")
public ResponseEntity<TrendResponse> getTrend(
    @RequestParam String start_dt,  // YYYY-MM-DD
    @RequestParam String end_dt     // YYYY-MM-DD
) {
    // 查询5天的日级聚合数据
    // 返回日均拥堵指数、日均速度、日总行程数
}
```

---

## ❓ 常见问题 & 故障排查

### Q1: 拥堵表填充失败 —— 字段不匹配

**症状**：
```
ERROR: column "road_segment_id" not found
```

**原因**：fact_congestion_seg_hour 中的字段名不对

**解决**：
```bash
# 检查实际字段
psql -h 101.35.234.65 -U osmuser -d postgres
postgres=# \d dw.fact_congestion_seg_hour
```
根据实际字段名修改 fill_congestion_ads.py

---

### Q2: deviation_pct 计算错误

**症状**：deviation_pct 全是NULL

**原因**：baseline_speed_kmh 为NULL（5.6%情况）

**解决**：
```sql
-- 在填充脚本中处理
CASE 
    WHEN b.baseline_speed_kmh IS NOT NULL AND b.baseline_speed_kmh > 0
    THEN (f.avg_speed_kmh - b.baseline_speed_kmh) / b.baseline_speed_kmh * 100
    ELSE NULL
END AS deviation_pct
```

---

### Q3: 前端地图无法显示热力图

**症状**：页面加载但地图是空白

**原因**：MapLibre或deck.gl初始化失败

**解决**：
1. 检查浏览器console是否有JS错误
2. 验证dashboardApi.js中的API URL是否正确
3. 确认后端API是否返回200且数据不为空

---

### Q4: ECharts排行榜显示不正常

**症状**：柱子显示但没有颜色/太小

**原因**：主题配置或数据格式不对

**解决**：
```javascript
// 检查是否正确加载暗色主题
import { chartTheme } from '@/utils/chartTheme'
const myChart = echarts.init(dom, null, { renderer: 'canvas' })
myChart.setOption({
  ...chartTheme,
  ...yourSeriesData
})
```

---

## 📋 代码规范 & 注意事项

### 1. 代码分工

**你只负责**：
- DashboardController 中以 `/congestion/*` 开头的方法
- DashboardService 中拥堵相关方法
- DashboardRepository 中拥堵相关查询
- 前端 CongestionPage.vue 和 TrendPage.vue

**你不要改**：
- 其他已存在的Controller/Service/Repository
- dashboardStore.js（由P3维护，需要新state请先通知P3）
- 现有的TaxiController/MapController及相关代码

### 2. PR提交要求（给P3）

**何时提交PR**：
- T3 API全部实现后
- T4/T5 前端页面完成后

**PR包含内容**：
```
标题：[P1] Congestion & Trend APIs + Pages

描述：
- 实现了4个拥堵API端点
- 实现了/congestion和/trend两个前端页面
- 所有API通过Postman测试
- 前端通过本地mock数据验证

修改文件：
- backend/src/main/java/com/harbin/dataplatform/controller/DashboardController.java (拥堵部分)
- backend/src/main/java/com/harbin/dataplatform/service/CongestionService.java
- backend/src/main/java/com/harbin/dataplatform/repository/DashboardRepository.java (拥堵部分)
- frontend/src/pages/CongestionPage.vue
- frontend/src/pages/TrendPage.vue
- scripts/fill_congestion_ads.py

依赖：无（API合同已定，ADS表已创建）

可能冲突：DashboardController, DashboardService, DashboardRepository （P2/P3也在改，需P3协调）
```

### 3. Git提交消息格式

```
[P1-T2] Fill congestion ADS tables from DW/TDM

- Implemented fill_congestion_ads.py script
- Handles NULL baseline_speed cases
- Verified data integrity with COUNT(*)

Related: #API-3.1
```

---

## ✅ 自测检查表

### T2: 拥堵表填充完成

- [ ] 脚本执行无错误
- [ ] SELECT COUNT(*) FROM ads.congestion_by_segment_hour > 0
- [ ] 随机采样10条记录，验证字段完整

### T3: 拥堵API完成

- [ ] Postman可调用4个端点，返回200
- [ ] 响应JSON格式与api-contract-reference.md一致
- [ ] 异常情况处理（无数据/错误日期）

### T4: /congestion 页面完成

- [ ] 页面可访问（localhost:5173/congestion）
- [ ] 地图热力图可见
- [ ] 排行榜Top10可见
- [ ] 时间轴选择器可交互

### T5: /trend 页面完成

- [ ] 页面可访问（localhost:5173/trend）
- [ ] 5天折线图可见
- [ ] 日类型对比柱状图可见
- [ ] 图表响应式布局

### T6: 烟雾测试脚本

- [ ] 脚本可调用所有4个API
- [ ] 记录API响应时间
- [ ] 标记任何异常返回值

---

## 📞 沟通事项

### 与P3的协调

**步骤 1**：
- 讨论DashboardController所有权和merge规则
- 确认dashboardStore.js中的时间状态字段

**步骤 2-3**：
- 如需修改DashboardController基类，提前通知P3

**提前冒烟阶段**：
- 接收P3的冒烟测试反馈

**后续修复阶段**：
- 配合P3回归测试

### 与P2的协调

**无直接依赖**（除非需要共用工具类）

---

## 🎯 Definition of Done

任务完成标准：

```
✅ T1  分支 feature/p1 已创建，本地环境验证通过
✅ T2  拥堵表数据已填充，行数 > 100,000
✅ T3  4个拥堵API端点已实现，Postman全通过
✅ T4  /congestion 页面可访问，热力图+排行榜显示正常
✅ T5  /trend 页面可访问，折线图+柱状图显示正常
✅ T6  烟雾测试脚本已编写，可调用所有API
✅ T7  冒烟反馈已修复，feature/p1 ready for merge

最终：P3 merge feature/p1 到 main，拥堵模块完成 ✅
```

---

## 参考资源

- API合同：[api-contract-reference.md](api-contract-reference.md)
- 项目总规划：[README-NEW-PLAN.md](README-NEW-PLAN.md)
- 数据库连接：
  ```
  地址: 101.35.234.65:5432
  数据库: postgres
  用户: osmuser
  密码: pass
  ```

# 👤 Person 2 —— 热点分析 + 司机行为模块执行计划

**项目**：交通数据大屏  
**负责人**：Person 2  
**周期**：7 个工作日（步骤式执行）  
**总工时**：30小时（含Julia ETL扩展工时）  
**页面**：`/hotspot`（上下车热点地图）、`/driver`（司机行为分析）

---

## ⚠️ 关键提醒

**你的最关键任务是 Task 9（Julia ETL）**，从6小时扩展到**10小时**。这不是小调整！

```
旧方案：6h（严重低估）
新方案：10h（现实评估）
  - 阶段 1：1.5h（理解 ODS 结构）
  - 阶段 2：3.5h（实现算法）
  - 阶段 3：3h（测试 + 边界情况）
  - 阶段 4：2h（集成冒烟验证）
```

**如果Julia ETL延期，整个项目进度受影响。**

---

## 📊 任务清单

| # | 任务 | 工时 | 完成状态 | 优先级 | 依赖 |
|---|------|------|---------|--------|------|
| **T9** | Julia ETL — 司机休息推断 | **10h** | ⬜ | 🔴 | ODS表就绪 |
| **T10** | Python脚本填充热点表 | 3h | ⬜ | 🔴 | T9完成 |
| **T11** | 热点+司机API实现 | 4h | ⬜ | 🔴 | T10完成 |
| **T12** | /hotspot 页面 | 5h | ⬜ | 🟡 | T11完成 |
| **T13** | /driver 页面 | 5h | ⬜ | 🟡 | T11完成 |
| **T14** | API烟雾测试 | 1h | ⬜ | 🟡 | T11完成 |
| **T15** | 集成冒烟修复 | 1h | ⬜ | 🟡 | P3反馈 |

**总计**：30h

---

## 📅 执行步骤

### 步骤 1：准备环境、创建分支并启动 Julia 研究 [2.5h]

```
08:00-09:00 [1h]  环境准备
  □ 验证 node >= 18, java >= 17, python >= 3.9, julia >= 1.8
  □ git checkout main && git pull origin main
  □ git checkout -b feature/p2
  □ 确认后续开发与提交都在 feature/p2 完成
  □ 与P3确认dashboardStore共用框架
  □ 与P3确认DashboardController所有权规则

09:00-09:15 [0.25h] 休息

09:15-12:30 [1.5h] T9开始：Julia环境 + ODS数据探索
  □ Julia环境检查：
    julia -e "import LibPQ; import DataFrames"
  □ 连接数据库，验证ODS数据：
    SELECT COUNT(*) FROM ods.ods_taxi_trips_raw
  □ 检查原始数据格式：
    SELECT * FROM ods.ods_taxi_trips_raw LIMIT 1
    → 查看 tms_seq, lon_seq, lat_seq 字段结构（数组类型）
  □ 了解task 9核心算法：
    - 按设备ID分组
    - 遍历轨迹点，检测时间间隔 > 120秒 → 休息事件
    - 保存休息位置、开始时间、结束时间、休息时长
    - 写入 tdm.driver_rest_location

12:30-13:30 [1h] 午餐
```

**输出**：Julia环境验证，ODS数据理解，准备实现

---

### 步骤 2：完成 Julia ETL 核心开发 [8h]

```
08:00-09:00 [1h]  T9: 建表 + 初始化
  □ 在Julia中执行建表SQL：
    CREATE TABLE IF NOT EXISTS tdm.driver_rest_location (
      id SERIAL PRIMARY KEY,
      devid TEXT NOT NULL,
      dt DATE NOT NULL,
      rest_start TIMESTAMP NOT NULL,
      rest_end TIMESTAMP NOT NULL,
      rest_minutes DOUBLE PRECISION NOT NULL,
      lon DOUBLE PRECISION NOT NULL,
      lat DOUBLE PRECISION NOT NULL,
      created_at TIMESTAMP DEFAULT NOW()
    )
  □ TRUNCATE旧数据（防止重复）

09:00-09:15 [0.25h] 休息

09:15-12:30 [3.25h] T9: 算法实现（核心逻辑）
  □ 创建 julia/driver_rest_etl.jl
  □ 编写以下逻辑：
    1. 读取 ods.ods_taxi_trips_raw，按devid/tms排序
    2. unnest 数组字段转成单行
    3. 计算相邻行的时间差
    4. 时间差 > 120秒 → 标记为休息开始点
    5. 收集休息事件（devid, dt, lon, lat, rest_minutes）
    6. 批量INSERT to tdm.driver_rest_location
  □ 代码示意：
    ```julia
    # 这只是伪代码框架，你需要用真实Julia语法实现
    for devid in unique_drivers
      vehicle_trips = filter_by_devid(devid)
      sort_by_timestamp!(vehicle_trips)
      
      for i in 2:length(vehicle_trips)
        time_gap = vehicle_trips[i].tms - vehicle_trips[i-1].tms
        if time_gap > 120
          rest_location = (vehicle_trips[i-1].lon, vehicle_trips[i-1].lat)
          # 记录休息事件
      end
    end
    ```
  □ 边界情况处理：
    - 第一个点没有前一个点，跳过
    - 同一设备的多个轨迹点时间戳可能相同，按处理
    - devid为空、坐标为NULL的数据要过滤

12:30-13:30 [1h] 午餐

13:30-17:00 [3.5h] T9: 性能优化 + 测试
  □ 性能检查：
    - 数据量：ods_taxi_trips_raw 可能有百万级轨迹点
    - 逐个遍历可能很慢，考虑向量化操作
    - 使用 @time 测量执行时间
  □ 本地测试小数据集：
    - 对单个devid运行ETL，验证逻辑
    - 检查输出的 rest_location 是否合理
  □ 数据质量检查：
    SELECT COUNT(*) FROM tdm.driver_rest_location
    → 应该有数百到数千条休息事件
```

**输出**：Julia ETL脚本基本完成，能处理部分数据

---

### 步骤 3：完成 Julia 测试并填充热点表 [8h]

```
08:00-09:00 [1h]  T9: 完整数据验证
  □ 在全量数据上运行Julia ETL
  □ 验证结果合理性：
    SELECT dt, COUNT(*), AVG(rest_minutes) 
    FROM tdm.driver_rest_location 
    GROUP BY dt
  □ 检查异常：
    - 休息时长 < 0 或 > 480分钟（8小时）？
    - 坐标范围是否在哈尔滨市（lon: 126-127, lat: 45-46）？
    - 某些设备的休息地点是否过于集中或分散？

09:00-09:15 [0.25h] 休息

09:15-12:30 [3.25h] T10: Python脚本填充热点表
  □ 创建 scripts/fill_hotspot_ads.py
  □ 实现逻辑：
    - 从 ods.ods_taxi_trips_raw 的pickup/dropoff 位置
    - 聚合为网格热点 (lon_grid, lat_grid)
    - 计算上车数、下车数、供需失衡指标
    - 填充到 ads.hotspot_grid_enriched 和 ads.driver_behavior_summary
  □ 供需失衡计算：
    imbalance = (pickups - dropoffs) / (pickups + dropoffs)
    范围：[-1, 1]
    + 正值 = 上车多（供应不足）
    - 负值 = 下车多（需求不足）
  □ 测试脚本执行
  □ 验证行数

12:30-13:30 [1h] 午餐

13:30-17:00 [3.5h] T11: 热点+司机API实现
  □ 创建/修改文件：
    backend/src/main/java/com/harbin/dataplatform/controller/DashboardController.java
    backend/src/main/java/com/harbin/dataplatform/service/HotspotService.java
    backend/src/main/java/com/harbin/dataplatform/repository/DashboardRepository.java
  □ 实现4个API端点（参考api-contract-reference.md）：
    - GET /api/dashboard/hotspot/grid?dt=2015-01-05&type=pickup
    - GET /api/dashboard/hotspot/grid?dt=2015-01-05&type=dropoff
    - GET /api/dashboard/driver/behavior?dt=2015-01-05
    - GET /api/dashboard/driver/rest_location?dt=2015-01-05
  □ Postman测试前2个端点
```

**输出**：
- ✅ Julia ETL完整验证
- ✅ Python热点脚本完成
- ⏳ API实现开始

---

### 步骤 4：等待 P3 冒烟反馈 [1h]

```
08:00-09:00 [1h]  等待P3冒烟反馈
  □ 不主动提交代码到main
  □ 保留feature/p2分支
  □ 监听P3的冒烟测试反馈
```

**输出**：P3冒烟测试完成，记录问题清单

---

### 步骤 5：完成页面开发并修复问题 [6h]

```
08:00-09:00 [1h]  T11: 4个API完成 + 测试
  □ 补充API的异常处理
  □ 全量测试4个API端点
  □ 修复冒烟发现的问题

09:00-09:15 [0.25h] 休息

09:15-12:30 [3.25h] T12: /hotspot 页面
  □ 创建 frontend/src/pages/HotspotPage.vue
  □ 上半部分：地图 + 热点层（两个切换：Pickup/Dropoff）
  □ 下半部分：热点表格或图表展示
  □ 与时间轴联动
  □ 基本样式完成，使用mock数据

12:30-13:30 [1h] 午餐

13:30-17:00 [3.5h] T13: /driver 页面
  □ 创建 frontend/src/pages/DriverPage.vue
  □ 上半部分：司机班次分布饼图
  □ 中间部分：活跃时长分布
  □ 下半部分：休息地段散点图（地图）
  □ ECharts暗色主题
```

**输出**：
- ✅ API全部完成+测试
- ✅ /hotspot 和 /driver 框架完成

---

### 步骤 6：执行回归测试 [3h]

```
08:00-09:00 [1h]  T14: 烟雾测试脚本
  □ 编写脚本调用4个热点API
  □ 记录响应时间和数据量

09:00-09:15 [0.25h] 休息

09:15-12:30 [3.25h] 配合P3回归测试
  □ 跟随P3的回归测试清单
  □ 对热点/司机功能进行端到端测试
```

**输出**：烟雾测试脚本完成，P2功能回归通过

---

### 步骤 7：等待最终合并 [0h]

```
08:00-09:00  等待P3最终合并
  □ 不需要做新工作
  □ feature/p2分支由P3审核合并到main
```

**总结**：项目完成 ✅

---

## 🔧 代码目录结构

### Julia脚本

```
julia/
├── driver_rest_etl.jl                   ← 核心：司机休息推断ETL
├── parameters.jl                        ← 参数定义（MAX_GAP_SECONDS等）
├── Trip.jl                              ← Trip数据结构（已存在）
└── util.jl                              ← 通用工具（已存在）
```

### 后端代码（Java）

```
backend/src/main/java/com/harbin/dataplatform/
├── controller/
│   └── DashboardController.java         ← 你负责的 /hotspot/* 和 /driver/* 部分
├── service/
│   ├── DashboardService.java
│   └── HotspotService.java (可选)
├── repository/
│   └── DashboardRepository.java         ← 热点查询部分
└── dto/
    ├── HotspotGridDTO.java
    ├── DriverBehaviorDTO.java
    ├── DriverRestLocationDTO.java
    └── ...
```

### 前端代码（Vue）

```
frontend/src/
├── pages/
│   ├── HotspotPage.vue                  ← 上下车热点地图
│   ├── DriverPage.vue                   ← 司机行为分析
│   └── ...
└── ...
```

### 数据处理（Python）

```
scripts/
├── fill_congestion_ads.py               ← P1负责
├── fill_hotspot_ads.py                  ← 你负责（新文件）
└── fill_driver_ads.py                   ← 你负责（新文件）
```

---

## 🚀 关键技术点

### Julia ETL核心

**为什么有10小时？**
```
理解ODS数据结构    1.5h  ← 不能跳过，数据错了一切都错
实现休息推断算法  2h    ← 逻辑需要反复验证
测试+边界处理    3h    ← 百万级数据处理，性能/正确性都要验证
性能优化         2h    ← Julia初学者经常忽视，向量化很重要
集成验证         1.5h  ← 与后续Python脚本/API集成测试
```

**最常见的错误**：
- ❌ 按行遍历百万级数据 (太慢)
- ✅ 改用 DataFrames 的向量化操作
- ❌ 计算时间差用秒数浮点数（精度丢失）
- ✅ 用 UNIX 时间戳或 DateTime 对象
- ❌ 休息事件写入数据库不验证数据合理性
- ✅ 先 SELECT COUNT(), 查看数据分布

### Python脚本（热点/司机表填充）

**网格热点聚合**：
```python
# ads.hotspot_grid_enriched 需要这样的逻辑
SELECT 
    DATE_TRUNC('hour', pickup_time) AS hour_bucket,
    grid_id (lon_grid || '_' || lat_grid),
    COUNT(CASE WHEN trip_type = 'pickup' THEN 1 END) AS pickup_count,
    COUNT(CASE WHEN trip_type = 'dropoff' THEN 1 END) AS dropoff_count,
    -- 供需失衡 = (pickup - dropoff) / (pickup + dropoff)
    (COUNT(CASE WHEN trip_type = 'pickup' THEN 1 END) - 
     COUNT(CASE WHEN trip_type = 'dropoff' THEN 1 END)) /
    CAST(COUNT(*) AS FLOAT) AS imbalance_ratio
FROM ods.ods_taxi_trips_raw
GROUP BY 1, 2
```

### API实现

```java
// GET /api/dashboard/hotspot/grid?dt=2015-01-05&type=pickup
@GetMapping("/hotspot/grid")
public ResponseEntity<HotspotGridResponse> getHotspotGrid(
    @RequestParam String dt,
    @RequestParam String type  // "pickup" or "dropoff"
) {
    // 从 ads.hotspot_grid_enriched 查询
    // 返回热点列表 + 坐标 + 密度值
}

// GET /api/dashboard/driver/rest_location?dt=2015-01-05
@GetMapping("/driver/rest_location")
public ResponseEntity<DriverRestResponse> getRestLocation(
    @RequestParam String dt
) {
    // 从 tdm.driver_rest_location 查询
    // 返回所有司机的休息地段散点
}
```

---

## ❓ 常见问题 & 故障排查

### Q1: Julia脚本报错 —— LibPQ连接失败

**症状**：
```
ERROR: LoadError: cannot connect to database
```

**原因**：数据库连接字符串错误

**解决**：
```julia
conn = LibPQ.Connection(
    "host=101.35.234.65 port=5432 dbname=postgres user=osmuser password=pass"
)
# 测试连接是否成功
status(conn)  # 应该返回 POSTGRESQL_CONNECTION_OK
```

---

### Q2: Julia脚本很慢 —— 处理百万级数据超时

**症状**：运行1小时还没完成

**原因**：用了行级循环而不是向量化操作

**解决**：
```julia
# 错误方式（慢）
for i in 1:nrow(df)
    # ...
end

# 正确方式（快）
df_sorted = sort(df, :tms)
diffs = diff(df_sorted.tms)
rest_points = findall(x -> x > 120, diffs)
# 用这些索引直接提取休息事件
```

---

### Q3: 休息推断逻辑有问题 —— 休息地点全是同一个位置

**症状**：SELECT DISTINCT(lon, lat) 只有3-5个位置

**原因**：可能没有正确处理devid分组，导致所有设备的数据混在一起

**解决**：
```julia
# 确保按devid分组，不同设备的轨迹点不能混合
for devid in unique(df.devid)
    vehicle_trips = df[df.devid .== devid, :]  # 只选这个设备
    # 然后再进行时间排序和休息推断
end
```

---

### Q4: Python脚本填充表失败 —— 供需失衡NaN

**症状**：imbalance_ratio 全是NaN或Infinity

**原因**：分母为0（pickup + dropoff = 0）

**解决**：
```python
imbalance = (pickups - dropoffs) / (pickups + dropoffs + 1e-6)  # 加个小数防止除以0
# 或
if pickups + dropoffs > 0:
    imbalance = ...
else:
    imbalance = None
```

---

### Q5: 前端热力图显示不出来 —— 地图空白

**症状**：页面加载但看不到热点

**原因**：
1. API数据为空
2. 坐标范围超出地图视图
3. deck.gl 热力图层配置错误

**解决**：
1. 检查API是否返回数据
2. 确认坐标范围 (lon: 126-127, lat: 45-46)
3. 查看浏览器控制台JS错误

---

## 📋 代码规范 & 注意事项

### 1. Julia代码规范

**命名约定**：
```julia
# 好
function extract_rest_events(trips_df::DataFrame)
    # 处理逻辑
end

rest_minutes = 120

# 避免
function extractRestEvents()  # Python风格，不符合Julia习惯
end

REST_MINUTES = 120  # 全大写常量在Julia较少见
```

**性能考虑**：
```julia
# ✅ 推荐
using DataFrames
df = sort(df, :tms)
grouped = groupby(df, :devid)

# ❌ 避免
for i in 1:nrow(df)
    # 行级循环极慢
end
```

### 2. 代码分工

**你只负责**：
- DashboardController 中以 `/hotspot/*` 和 `/driver/*` 开头的方法
- DashboardService 中热点/司机相关方法
- DashboardRepository 中热点/司机查询
- 前端 HotspotPage.vue 和 DriverPage.vue
- Julia ETL 脚本和 Python填充脚本

**你不要改**：
- P1的拥堵相关代码
- P3的取数/血缘相关代码
- dashboardStore.js （由P3维护）

### 3. PR提交要求

**第一个PR**（中期后）：
```
标题：[P2] Julia ETL for Driver Rest Location

描述：
- 实现了司机休息地段推断算法
- 处理了100万+轨迹点，耗时 ~5分钟
- 生成了 ~50,000 条休息事件
- 已验证数据合理性

修改文件：
- julia/driver_rest_etl.jl
- scripts/fill_hotspot_ads.py (可选，后来补上)

验证：
- Julia脚本在全量数据上成功运行
- SELECT COUNT(*) FROM tdm.driver_rest_location → ~50,000
```

**第二个PR**（后期后）：
```
标题：[P2] Hotspot & Driver APIs + Pages

描述：
- 实现了4个热点/司机API端点
- 实现了/hotspot和/driver两个前端页面
- 所有API通过Postman测试

修改文件：
- backend/src/main/java/com/harbin/dataplatform/controller/DashboardController.java (/hotspot/*, /driver/*)
- backend/src/main/java/com/harbin/dataplatform/service/HotspotService.java
- backend/src/main/java/com/harbin/dataplatform/repository/DashboardRepository.java (热点查询部分)
- frontend/src/pages/HotspotPage.vue
- frontend/src/pages/DriverPage.vue
- scripts/fill_hotspot_ads.py
- scripts/fill_driver_ads.py

可能冲突：DashboardController (P1/P3也在改)
```

---

## ✅ 自测检查表

### T9: Julia ETL完成

- [ ] driver_rest_etl.jl 脚本存在且无语法错误
- [ ] 脚本在全量ODS数据上成功执行
- [ ] SELECT COUNT(*) FROM tdm.driver_rest_location 返回数字 > 1000
- [ ] 随机采样数据，休息时长 30-480 分钟范围合理
- [ ] 坐标范围在哈尔滨市边界内

### T10: Python热点脚本完成

- [ ] 脚本执行无错误
- [ ] SELECT COUNT(*) FROM ads.hotspot_grid_enriched > 0
- [ ] 供需失衡指标在 [-1, 1] 范围内

### T11: 热点+司机API完成

- [ ] Postman可调用4个端点，返回200
- [ ] 响应JSON格式与api-contract-reference.md一致
- [ ] 异常处理（无数据/错误日期）

### T12: /hotspot 页面完成

- [ ] 页面可访问（localhost:5173/hotspot）
- [ ] 地图热点可见
- [ ] Pickup/Dropoff 切换功能可用

### T13: /driver 页面完成

- [ ] 页面可访问（localhost:5173/driver）
- [ ] 司机班次饼图可见
- [ ] 休息地段散点图可见

---

## 📞 沟通事项

### 与P3的协调

**步骤 1**：
- 讨论Julia脚本的集成（输入ODS、输出TDM）
- 确认dashboardStore.js 需要的新字段

**步骤 2-3**：
- Julia ETL进度同步（如果遇到性能问题，可能需要P3帮忙调试Java部分）

**提前冒烟阶段**：
- 接收P3的冒烟测试反馈

**后续修复阶段**：
- 配合P3回归测试

---

## 🎯 Definition of Done

任务完成标准：

```
✅ T9   Julia ETL脚本完成，生成50k+休息事件
✅ T10  Python热点脚本完成，填充热点表
✅ T11  4个热点/司机API端点实现，Postman全通过
✅ T12  /hotspot 页面可访问，热点地图显示正常
✅ T13  /driver 页面可访问，各图表显示正常
✅ T14  烟雾测试脚本编写，覆盖所有API
✅ T15  冒烟反馈修复完成，feature/p2 ready for merge

最终：P3 merge feature/p2 到 main，热点+司机模块完成 ✅
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
- Julia文档：https://docs.julialang.org/
- LibPQ.jl 文档：https://invenia.github.io/LibPQ.jl/

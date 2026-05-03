# 👤 Person 3 —— 数据资产 + 血缘质量 + 集成执行计划

**项目**：交通数据大屏  
**负责人**：Person 3（项目整合人）  
**周期**：7 个工作日（步骤式执行）  
**总工时**：28小时  
**页面**：`/catalog`（数据资产取数）、`/lineage`（数据血缘质量）  
**特殊职责**：框架所有权 + 集成主导 + 质量把关

---

## 🎯 P3的独特角色

你不仅要完成自己的2个页面，还要：

1. **步骤 1**：设计共用框架（DashboardController/Service/Repository 结构）
2. **中期**：进行**提前集成冒烟测试**，发现问题
3. **后续阶段**：收集反馈、协调修复、进行回归测试
4. **收口阶段**：最终合并所有分支、验收项目

**这是项目成功的关键路径。**

---

## 📊 任务清单

| # | 任务 | 工时 | 完成状态 | 优先级 | 说明 |
|---|------|------|---------|--------|------|
| **T0** | 框架设计 + 代码所有权规范 | 1h | ⬜ | 🔴 | 步骤 1 完成 |
| **T16** | 数据资产API | 4h | ⬜ | 🔴 | 后端实现 |
| **T17** | /catalog 页面 | 8h | ⬜ | 🔴 | 前端取数界面 |
| **T18** | 数据血缘+质量API | 3h | ⬜ | 🟡 | 后端实现 |
| **T19** | /lineage 页面 | 4h | ⬜ | 🟡 | 前端DAG展示 |
| **T20** | 提前集成冒烟测试 | 2.5h | ⬜ | 🔴 | **中期关键** |
| **T21** | 集成协调 + 修复 | 2.5h | ⬜ | 🔴 | 反馈处理 |
| **T22** | 回归测试 + 文档 | 3h | ⬜ | 🟡 | 后期验收 |

**总计**：28h

---

## 📅 执行步骤

### 步骤 1：设计框架并创建分支 [2h]

```
08:00-09:00 [1h]  T0: 框架所有权设计
  □ 创建 DashboardController 基类/结构
  □ 设计 DashboardService 接口和基类
  □ 设计 DashboardRepository 接口
  □ 定义统一的异常处理、响应包装、日期转换等工具类
  □ 创建文档（见下面的"代码规范"章节）
  □ 与P1/P2沟通此框架
  
09:00-09:15 [0.25h] 休息

09:15-10:30 [1.25h] 环境准备 + 分支创建
  □ 验证 node >= 18, java >= 17, python >= 3.9
  □ git checkout main && git pull origin main
  □ git checkout -b feature/p3
  □ 确认后续代码提交、合并和最终收口都在 feature/p3 进行
  □ 创建文档：
    - DashboardController 代码所有权映射表
    - dashboardStore.js 共用框架（定义所有6页需要的状态）

10:30-10:45 [0.25h] 休息

10:45-12:00 [1.25h] 后端框架搭建
  □ 创建基础项目结构
  □ 生成第一版DashboardController、Service、Repository
  □ 配置异常处理和响应格式
  □ 提交commit：[P3-T0] Dashboard framework setup
```

**输出**：框架文档 + 基础代码 + P1/P2可以按框架开发

---

### 步骤 2：完成取数系统后端 [8h]

```
08:00-09:00 [1h]  T16: 数据资产API - 列表接口
  □ 实现 GET /api/dashboard/catalog/tables
    返回TDM层所有表的元数据
  □ 实现分页、排序、过滤
  □ 对应的Service和Repository

09:00-09:15 [0.25h] 休息

09:15-12:30 [3.25h] T16: 数据资产API - 详情+字段接口
  □ 实现 GET /api/dashboard/catalog/tables/{tableName}/fields
    返回指定表的字段详情
  □ 实现 GET /api/dashboard/catalog/tables/{tableName}/preview
    返回表的前5行数据（仅SELECT + LIMIT，安全处理）
  □ 处理敏感表（如果有密码字段，不返回）
  □ Postman测试

12:30-13:30 [1h] 午餐

13:30-17:00 [3.5h] T16: 数据资产API - 取数+热度接口
  □ 实现 GET /api/dashboard/catalog/export
    允许用户按字段列表导出数据（仅TDM，最多1000行）
  □ 实现 GET /api/dashboard/catalog/field-hotness
    返回字段热度排行（基于导出次数、查询次数等）
  □ 记录导出日志到审计表
  □ 全量测试4个API端点
  □ 提交代码到feature/p3
```

**输出**：
- ✅ 4个取数API实现完成
- ⏳ 前端待开发

---

### 步骤 3：完成 /catalog 与 /lineage 页面 [8h]

```
08:00-09:00 [1h]  T17: /catalog 页面 - 布局搭建
  □ 创建 frontend/src/pages/CatalogPage.vue
  □ 上部分：表格搜索 + 列表
  □ 中部分：表详情 + 字段展示
  □ 下部分：数据导出 + 热度排行

09:00-09:15 [0.25h] 休息

09:15-12:30 [3.25h] T17: /catalog 页面 - 功能完成
  □ 表格浏览、搜索、分页
  □ 字段列表展示 + 类型提示
  □ 数据预览（前5行）
  □ 导出功能集成（调用 /api/dashboard/catalog/export）
  □ 导出热度排行榜展示
  □ 基于dashboardStore的时间轴联动（可选）

12:30-13:30 [1h] 午餐

13:30-17:00 [3.5h] T18+T19: 血缘+质量API + /lineage 页面
  □ 实现 GET /api/dashboard/lineage/dag
    返回 ODS→DW→TDM→ADS 的静态DAG
  □ 实现 GET /api/dashboard/lineage/quality
    返回各表的质量指标（null率、更新时间等）
  □ 创建 frontend/src/pages/LineagePage.vue
  □ 上部分：DAG图展示（使用现有库或简单SVG）
  □ 下部分：质量指标卡片展示
  □ 本地验证（mock数据）
```

**输出**：
- ✅ 取数API全部完成
- ✅ /catalog 和 /lineage 页面框架完成
- ⏳ 等待P1/P2API完成，进行集成冒烟

---

### 步骤 4：提前集成冒烟测试 [2.5h]

**这是最关键的一天**。你要进行提前的集成冒烟测试，发现问题。

```
08:00-09:30 [1.5h] T20: 合并P1/P2分支，尝试编译
  □ 创建临时分支：feature/integration-smoke
  □ 从 feature/integration-smoke 出发：
    git merge feature/p1 --no-commit  （先不提交，看冲突）
    git merge feature/p2 --no-commit
  □ 查看冲突文件：git status | grep "both"
  □ 快速修复明显冲突（通常在DashboardController中）：
    - 三方冲突：选择保留 P1 的 /congestion/* + P2 的 /hotspot/* + P3 的 /catalog/*
    - 导入冲突：确保所有imports都在
    - 方法签名冲突：检查是否有重名方法
  □ 尝试 mvn clean compile
  □ 记录编译错误清单

09:30-09:45 [0.25h] 休息

09:45-12:00 [2.25h] T20: 冒烟测试 + 问题记录
  □ 如果后端编译成功：
    mvn spring-boot:run
    测试所有API端点（使用curl或Postman）
    - P1的4个拥堵API
    - P2的4个热点/司机API
    - P3的4个取数/血缘API
    记录响应时间、数据完整性、错误情况
  □ 前端冒烟：
    npm run dev
    访问6个页面（虽然还是mock数据）
    - /congestion / /trend / /hotspot / /driver / /catalog / /lineage
    检查是否可访问、有无JS错误
  □ 生成冒烟测试报告：
    ```
    冒烟测试报告 - 2026-05-06
    
    [✅] 后端编译：成功
    [⚠️] 拥堵API：/api/dashboard/kpi 返回500
    [✅] 热点API：全部200，数据完整
    [✅] 取数API：全部200
    [✅] 前端：6个页面可访问，无JS错误
    
    问题清单：
    1. DashboardController 第87行，拥堵KPI查询异常
    2. /trend 页面偶现样式错误
    3. dashboardStore 时间状态未被 /hotspot 正确同步
    
    优先级：
    🔴 高：问题1（API返回500）
    🟡 中：问题2（样式）、问题3（状态管理）
    ```
  □ 提交commit：[P3-T20] Integration smoke test results
  □ 通知P1/P2冒烟结果
```

**输出**：冒烟测试报告 + 问题清单 + 反馈给P1/P2

---

### 步骤 5：集成协调与修复 [2.5h]

```
08:00-09:00 [1h]  收集P1/P2的修复反馈
  □ P1/P2基于冒烟测试反馈进行修复（他们各自在自己分支上修）
  □ 你同时修复P3发现的自己代码的问题
  □ 例如：/catalog 页面样式、/lineage DAG展示逻辑等

09:00-09:15 [0.25h] 休息

09:15-12:30 [3.25h] 再次集成验证
  □ 等待P1/P2提交最新代码到各自分支
  □ 再次尝试合并：
    git checkout feature/integration-smoke
    git reset --hard origin/main  (重置到main)
    git merge feature/p1 --no-ff
    git merge feature/p2 --no-ff
  □ 再次处理冲突（应该比第一次少很多）
  □ 再次编译 + 跑冒烟测试
  □ 验证冒烟问题是否得到解决
  □ 记录遗留问题

12:30-13:30 [1h] 午餐

13:30-17:00 [3.5h] 精细化测试
  □ 如果冒烟全通过，开始精细化测试：
    - 时间轴联动：改变 /congestion 的时间，检查其他页面是否同步
    - API响应时间：记录各端点P50/P95延迟
    - 数据准确性：随机抽查数据是否与DB一致
    - 前端交互：按钮、下拉框、图表交互
  □ 提交commit：[P3-T21] Integration refinement
```

**输出**：所有API基本可用，主要bug已修复

---

### 步骤 6：回归测试与文档收尾 [3h]

```
08:00-09:00 [1h]  T22: 编制回归测试用例
  □ 创建测试文档：regression-test-checklist.md
  □ 包括：
    - 6个页面各自的访问/交互检查
    - 12个API的功能验证（参考api-contract-reference.md）
    - 边界情况（无数据/错误日期/大数据量等）
    - 性能指标（API响应时间 < 2s）

09:00-09:15 [0.25h] 休息

09:15-12:30 [3.25h] T22: 回归测试执行
  □ 与P1/P2一起执行回归测试
  □ 每个页面：
    - 访问是否正常
    - 时间轴选择是否有效
    - 与其他页面的交互是否正常
  □ 每个API：
    - 返回200且JSON有效
    - 数据格式与合同一致
    - 边界情况处理正确
  □ 记录遗留bug（如有）
  □ 优先级排序（P0修复，P1 nice to have）

12:30-13:30 [1h] 午餐

13:30-17:00 [3.5h] 最终修复 + 文档完善
  □ 修复P0级遗留bug
  □ 完善项目文档：
    - README.md 更新部署/运行说明
    - API文档补充（docs/api-contract-final.md）
    - 架构文档（如需要）
  □ 验证所有修复
```

**输出**：回归测试通过，遗留bug清单（若有）

---

### 步骤 7：最终验收与合并 [1h]

```
08:00-09:00 [1h]  最终检查 + 合并
  □ 最后一次检查6个页面是否都能访问
  □ 所有12个API是否都返回200
  □ 没有明显错误或异常
  □ 代码有comment和文档
  □ 提交最后的commit：[P3-T22] Final verification and merge
  □ 准备merge到main：
    - 确保 feature/p1, feature/p2, feature/p3 都是最新
    - 从 feature/p3 merge 到 main
    - 或者由项目主管进行最终merge
  
  项目完成 ✅
```

**输出**：项目验收通过，所有功能就绪

---

## 🔧 代码目录结构与所有权

### 后端所有权映射

```
backend/src/main/java/com/harbin/dataplatform/

DashboardController.java
├── /api/dashboard/congestion/*     → P1 实现
├── /api/dashboard/kpi/*            → P1 实现
├── /api/dashboard/comparison/*     → P1 实现
├── /api/dashboard/congestion/trend → P1 实现
├── /api/dashboard/hotspot/*        → P2 实现
├── /api/dashboard/driver/*         → P2 实现
├── /api/dashboard/rest_location/*  → P2 实现
├── /api/dashboard/catalog/*        → P3 实现 ✅
└── /api/dashboard/lineage/*        → P3 实现 ✅

DashboardService.java
├── CongestionService 方法          → P1 实现
├── HotspotService 方法             → P2 实现
├── HotspotDriverService 方法       → P2 实现
└── CatalogService 方法             → P3 实现 ✅
    LineageService 方法             → P3 实现 ✅

DashboardRepository.java
├── congestion 查询                 → P1 实现
├── hotspot 查询                    → P2 实现
├── driver 查询                     → P2 实现
└── catalog 查询                    → P3 实现 ✅
    lineage 查询                    → P3 实现 ✅
```

### 前端所有权映射

```
frontend/src/

pages/
├── CongestionPage.vue       → P1 负责
├── TrendPage.vue            → P1 负责
├── HotspotPage.vue          → P2 负责
├── DriverPage.vue           → P2 负责
├── CatalogPage.vue          → P3 负责 ✅
└── LineagePage.vue          → P3 负责 ✅

services/
└── dashboardApi.js          → P3 维护（所有人都会修改）

stores/
└── dashboardStore.js        → P3 维护（所有人都会使用）
```

---

## 📋 框架设计文档（步骤 1 产出）

### DashboardController 结构

```java
@RestController
@RequestMapping("/api/dashboard")
@Validated
public class DashboardController {
    
    @Autowired
    private DashboardService dashboardService;
    
    // ============== P1: 拥堵模块 ==============
    @GetMapping("/congestion/heatmap")
    public ResponseEntity<HeatmapResponse> getHeatmap(...) { }
    
    @GetMapping("/kpi")
    public ResponseEntity<KPIResponse> getKPI(...) { }
    
    // ... 其他P1方法
    
    // ============== P2: 热点+司机模块 ==============
    @GetMapping("/hotspot/grid")
    public ResponseEntity<HotspotGridResponse> getHotspotGrid(...) { }
    
    // ... 其他P2方法
    
    // ============== P3: 取数+血缘模块 ==============
    @GetMapping("/catalog/tables")
    public ResponseEntity<CatalogTablesResponse> getCatalogTables(...) { }
    
    // ... 其他P3方法
}
```

### dashboardStore.js 共用状态（步骤 1 产出）

```javascript
// frontend/src/stores/dashboardStore.js
import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDashboardStore = defineStore('dashboard', () => {
    // 全局时间状态（所有页面共用）
    const selectedDate = ref('2015-01-05')
    const selectedHour = ref(12)
    const selectedDayType = ref('workday')  // workday/weekend/holiday
    
    // 建议：各页面在需要新状态前，先通知P3，由P3统一添加
    // 以避免状态名冲突
    
    return {
        selectedDate,
        selectedHour,
        selectedDayType
    }
})
```

---

## ✅ 自测检查表

### T0: 框架设计完成

- [ ] DashboardController 结构已定义，所有权清晰
- [ ] DashboardService 和 Repository 接口已设计
- [ ] dashboardStore.js 共用状态已列出
- [ ] P1/P2都已确认框架

### T16-T19: API和页面完成

- [ ] GET /api/dashboard/catalog/tables 可用
- [ ] GET /api/dashboard/catalog/tables/{name}/fields 可用
- [ ] GET /api/dashboard/catalog/export 可用
- [ ] GET /api/dashboard/lineage/dag 可用
- [ ] /catalog 页面可访问
- [ ] /lineage 页面可访问

### T20: 冒烟测试完成

- [ ] feature/integration-smoke 分支已创建
- [ ] P1/P2的代码成功合并
- [ ] 后端编译无错误
- [ ] 12个API全部返回200
- [ ] 6个页面全部可访问
- [ ] 冒烟测试报告已生成

### T21-T22: 集成和回归完成

- [ ] P1/P2反馈的bug已修复
- [ ] 时间轴联动功能验证通过
- [ ] 所有API响应时间 < 2s
- [ ] 回归测试清单全部通过
- [ ] 文档已更新

---

## 🚀 API实现指南（P3示例）

### 1. GET /api/dashboard/catalog/tables

```java
@GetMapping("/catalog/tables")
public ResponseEntity<Page<CatalogTableDTO>> getCatalogTables(
    @RequestParam(defaultValue = "0") int page,
    @RequestParam(defaultValue = "10") int size,
    @RequestParam(required = false) String keyword
) {
    // 从 information_schema.tables 查询
    // 过滤：schema='tdm' 且 table_name LIKE '%keyword%'
    // 返回分页结果
    
    String sql = """
        SELECT 'tdm' AS schema_name, table_name,
               (SELECT COUNT(*) FROM information_schema.columns 
                WHERE table_schema='tdm' AND table_name=t.table_name) AS field_count,
               pg_total_relation_size('tdm.' || table_name) AS table_size_bytes
        FROM information_schema.tables t
        WHERE table_schema='tdm' AND table_type='BASE TABLE'
        AND (? IS NULL OR table_name LIKE ?)
        ORDER BY table_name
        LIMIT ? OFFSET ?
    """;
    
    // 分页 + 排序
    Page<CatalogTableDTO> result = new PageImpl<>(
        rows, PageRequest.of(page, size), totalCount
    );
    
    return ResponseEntity.ok(result);
}
```

### 2. GET /api/dashboard/catalog/tables/{tableName}/preview

```java
@GetMapping("/catalog/tables/{tableName}/preview")
public ResponseEntity<CatalogPreviewDTO> getTablePreview(
    @PathVariable String tableName
) {
    // 安全性检查：只允许 tdm. schema
    if (!tableName.matches("^[a-z0-9_]+$")) {
        return ResponseEntity.badRequest().build();
    }
    
    // 动态SQL（仅SELECT + LIMIT，安全）
    String sql = String.format("SELECT * FROM tdm.%s LIMIT 5", tableName);
    List<Map<String, Object>> rows = jdbcTemplate.queryForList(sql);
    
    // 返回数据
    CatalogPreviewDTO result = new CatalogPreviewDTO();
    result.setTableName(tableName);
    result.setRows(rows);
    result.setRowCount(rows.size());
    
    return ResponseEntity.ok(result);
}
```

---

## ❓ 常见问题 & 故障排查

### Q1: P1和P2同时修改DashboardController，merge冲突

**症状**：
```
CONFLICT (content conflict): DashboardController.java
```

**解决**：
```bash
# 手动编辑DashboardController.java
# 找到 <<<<<<<< 和 >>>>>>> 之间的冲突
# 原则：
#   - 保留P1的 /congestion/* 和 /kpi/* 方法
#   - 保留P2的 /hotspot/* 和 /driver/* 方法
#   - 保留P3的 /catalog/* 和 /lineage/* 方法
#   - 共享的imports和基类保留一份

# 例如冲突片段：
<<<<<<< HEAD (P1)
    @GetMapping("/kpi")
    public ResponseEntity<KPIResponse> getKPI(...) { }
=======
    @GetMapping("/hotspot/grid")
    public ResponseEntity<HotspotGridResponse> getHotspot(...) { }
>>>>>>> feature/p2

# 应该保留为：
    @GetMapping("/kpi")
    public ResponseEntity<KPIResponse> getKPI(...) { }
    
    @GetMapping("/hotspot/grid")
    public ResponseEntity<HotspotGridResponse> getHotspot(...) { }
```

### Q2: dashboardStore.js 状态冲突

**症状**：某个页面的状态没有在store中定义

**原因**：P1/P2各自添加了不同的state，没有通知P3

**解决**：
```javascript
// ❌ 不要这样做（各自添加）
// P1: const selectedHour = ref(12)
// P2: const selectedHour = ref(0)  // 冲突！

// ✅ 应该在步骤 1 时，由 P3 统一设计所有需要的 state
// dashboardStore.js
const selectedDate = ref('2015-01-05')
const selectedHour = ref(12)
const selectedDayType = ref('workday')
const selectedGridType = ref('pickup')  // P2需要
const catalogKeyword = ref('')  // P3需要
// ... 所有state在这里统一定义
```

### Q3: 前端时间轴联动不生效

**症状**：改变 /congestion 的时间，其他页面的时间没有跟着变

**原因**：其他页面没有监听dashboardStore的时间变化

**解决**：
```vue
<!-- 每个页面都应该这样做 -->
<template>
  <div>
    <CongestionChart :date="selectedDate" :hour="selectedHour" />
  </div>
</template>

<script setup>
import { useDashboardStore } from '@/stores/dashboardStore'
import { computed } from 'vue'

const store = useDashboardStore()
const selectedDate = computed(() => store.selectedDate)
const selectedHour = computed(() => store.selectedHour)
</script>
```

### Q4: 冒烟测试时，后端启动报错

**症状**：
```
ERROR: Column 'road_name' not found in table ads.congestion_by_segment_hour
```

**原因**：P1的填充脚本有问题，表数据不完整

**解决**：
1. 告诉P1检查填充脚本的JOIN逻辑
2. 验证表字段：
   ```sql
   \d ads.congestion_by_segment_hour
   ```
3. 等待P1修复后，再进行集成测试

---

## 📞 与P1/P2的沟通机制

### 代码审查 + PR流程

**P1/P2 提交PR时**：
```
标题：[P1] Congestion APIs (或 [P2] Hotspot & Driver APIs)

要求：
1. 清晰的commit消息
2. 没有重大编译错误
3. 自己已Postman测试
4. 标注"可能冲突"的文件

P3的审查清单：
- [ ] 代码是否符合框架设计
- [ ] 所有权是否清晰（不侵犯P2的代码）
- [ ] 新增的异常处理是否统一
- [ ] SQL是否安全（无SQL注入）
- [ ] API响应格式是否与合同一致
```

### 冒烟测试反馈机制

**中期冒烟时，P3生成反馈表**：
```markdown
# 冒烟测试反馈 - 2026-05-06

## P1 的问题
- [ ] 高优先级：/kpi 返回500，需要修复异常处理
- [ ] 中优先级：热力图缺少坐标校验
- [ ] 低优先级：API文档需要补充示例

## P2 的问题
- [ ] 高优先级：Julia ETL脚本性能问题，需要优化
- [ ] 中优先级：热点地图偶现NULL坐标
- [ ] 低优先级：司机页面样式

## 行动计划
- P1：尽快修复高优先级问题
- P2：尽快修复高优先级问题
- P3：随后重新集成测试
```

---

## 🎯 Definition of Done

任务完成标准：

```
✅ T0   框架设计完成，P1/P2确认
✅ T16  4个取数API实现，Postman全通过
✅ T17  /catalog 页面完成，可访问
✅ T18  3个血缘质量API实现
✅ T19  /lineage 页面完成，DAG显示正常
✅ T20  冒烟测试完成，问题清单已生成，P1/P2已反馈
✅ T21  集成协调完成，主要问题已修复
✅ T22  回归测试通过，所有API + 页面就绪

最终交付物：
- 6个数据大屏页面，完全可用 ✅
- 12个后端API端点，全部200 ✅
- 全局时间轴联动功能 ✅
- 所有异常情况处理 ✅
- 项目文档完整 ✅

项目状态：✅ READY FOR PRODUCTION
```

---

## 参考资源

- API合同：[api-contract-reference.md](api-contract-reference.md)
- 项目总规划：[README-NEW-PLAN.md](README-NEW-PLAN.md)
- P1 执行计划：[p1-execution-plan.md](p1-execution-plan.md)
- P2 执行计划：[p2-execution-plan.md](p2-execution-plan.md)
- 数据库连接：
  ```
  地址: 101.35.234.65:5432
  数据库: postgres
  用户: osmuser
  密码: pass
  ```

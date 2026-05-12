# P3 实现文档

> 负责人：Person 3（项目整合人）
> 功能模块：数据资产目录 + 数据血缘质量 + 框架设计
> 完成日期：2026-05-10

---

## 目录

1. [文件结构](#文件结构)
2. [后端实现详解](#后端实现详解)
3. [前端实现详解](#前端实现详解)
4. [运行指令](#运行指令)
5. [API 测试](#api-测试)
6. [数据库依赖](#数据库依赖)
7. [代码所有权](#代码所有权)

---

## 文件结构

### 后端文件

```
backend/src/main/java/com/harbin/dataplatform/
├── controller/
│   └── DashboardController.java           ← API 端点定义
├── service/
│   ├── DashboardService.java              ← 服务接口
│   └── impl/
│       └── DashboardServiceImpl.java      ← 业务逻辑实现
└── dto/                                 ← 数据传输对象
    ├── CatalogTableDTO.java               ← 表列表项
    ├── FieldDetailDTO.java               ← 字段详情
    ├── CatalogFieldsResponse.java         ← 字段响应
    ├── QueryRequest.java                 ← 查询请求
    ├── QueryResponse.java                ← 查询响应
    ├── HotFieldDTO.java                 ← 热度字段
    ├── LineageNodeDTO.java              ← 血缘节点
    ├── LineageEdgeDTO.java              ← 血缘边
    ├── LineageResponse.java              ← 血缘响应
    ├── DataQualityDTO.java               ← 质量表
    └── QualityResponse.java              ← 质量响应
```

### 前端文件

```
frontend/src/
├── stores/
│   └── dashboardStore.js                ← 共用状态管理
├── services/
│   └── dashboardApi.js                 ← API 服务封装
├── router/
│   └── index.js                        ← 路由配置
├── pages/
│   ├── CatalogPage.vue                  ← 数据目录页面
│   └── LineagePage.vue                ← 血缘质量页面
├── App.vue                             ← 应用入口 + 导航
└── main.js                             ← 应用初始化
```

---

## 后端实现详解

### API 端点 (DashboardController.java)

| 行号 | 端点 | 方法 | 功能 |
|------|------|------|------|
| 21-24 | `GET /dashboard/catalog/tables` | getCatalogTables | 获取 TDM 层所有表 |
| 26-29 | `GET /dashboard/catalog/fields/{tableName}` | getCatalogFields | 获取指定表字段详情 |
| 31-34 | `POST /dashboard/catalog/query` | queryCatalog | 执行数据查询 |
| 36-39 | `GET /dashboard/catalog/hot-fields` | getHotFields | 获取字段热度排行 |
| 41-44 | `GET /dashboard/lineage` | getLineage | 获取数据血缘图 |
| 46-49 | `GET /dashboard/quality` | getQuality | 获取数据质量指标 |

### 业务逻辑实现 (DashboardServiceImpl.java)

| 方法 | 行号 | 功能说明 |
|------|------|---------|
| `getCatalogTables()` | 22-62 | 查询 `information_schema.tables` 获取表元数据，统计每表的行数和字段数 |
| `getCatalogFields()` | 64-100 | 获取表字段信息 + 前5行数据预览，带表名安全校验（防止 SQL 注入） |
| `queryCatalog()` | 102-157 | 动态 SQL 构建查询，支持字段选择、过滤条件、行数限制（最多1000行） |
| `logQuery()` | 165-178 | 记录查询日志到 `ads.data_field_access_log` |
| `getHotFields()` | 180-197 | 查询日志表统计字段热度，失败时返回 mock 数据 |
| `getLineage()` | 199-263 | 返回硬编码的 DAG 节点和边（ODS→DW→TDM→ADS） |
| `getQuality()` | 265-283 | 返回各表的质量指标（行数、完整度、状态等） |

### DTO 类说明

| 类 | 用途 |
|-----|------|
| `CatalogTableDTO` | 表元数据：schema, tableName, rowCount, fieldCount |
| `FieldDetailDTO` | 字段详情：name, type, nullable, description |
| `QueryRequest` | 查询请求：tableName, fields, filters, limit |
| `QueryResponse` | 查询响应：columns, rows, totalRows, queryId |
| `HotFieldDTO` | 热度字段：fieldName, queryCount, rank |
| `LineageNodeDTO` | 血缘节点：id, label, layer, rowCount |
| `LineageEdgeDTO` | 血缘边：source, target, label |
| `DataQualityDTO` | 质量指标：tableName, schema, rowCount, fieldCount, lastUpdated, completenessPct, status |

---

## 前端实现详解

### 1. 共用状态管理 (dashboardStore.js)

```javascript
// 全局时间状态（所有页面共用）
selectedDate, selectedHour, selectedDayType

// Catalog 页面状态
catalogKeyword, selectedTable

// Lineage 页面状态
selectedLayer  // all/ODS/DW/TDM/ADS

// 计算属性
dateRange  // 自动计算5天范围
```

### 2. API 服务封装 (dashboardApi.js)

| 函数 | 端点 |
|------|------|
| `getCatalogTables()` | GET /catalog/tables |
| `getCatalogFields(tableName)` | GET /catalog/fields/{tableName} |
| `queryCatalog(queryData)` | POST /catalog/query |
| `getHotFields()` | GET /catalog/hot-fields |
| `getLineage()` | GET /lineage |
| `getQuality()` | GET /quality |

### 3. 页面功能

| 页面 | 文件 | 主要功能 |
|------|------|---------|
| **数据目录** | CatalogPage.vue | 表列表浏览、搜索、字段详情、数据预览、查询功能、热度排行 |
| **数据血缘** | LineagePage.vue | DAG 图展示、层级筛选、质量指标卡片 |

### 4. 路由配置 (router/index.js)

```javascript
路由映射:
  /catalog     → CatalogPage.vue   (数据目录)
  /lineage     → LineagePage.vue   (数据血缘)
  /            → redirect /catalog   (首页重定向)
```

---

## 运行指令

### 环境要求

| 组件 | 版本要求 | 检查命令 |
|------|---------|---------|
| Java | 17+ | `java -version` |
| Maven | 3.6+ | `mvn -version` |
| Node.js | 18+ | `node -v` |
| npm | 9+ | `npm -v` |

### 后端启动（端口 8082）

#### 方式 1：使用 Maven Wrapper（推荐）

```bash
cd /d/Study/quant/.vs/harbin_-data-platform/backend

# 首次运行会下载 Maven 依赖，需要 2-5 分钟
./mvnw.cmd spring-boot:run
```

#### 方式 2：使用 PowerShell 脚本

```bash
cd /d/Study/quant/.vs/harbin_-data-platform

# 启动后端（如端口被占用，使用 -KillPortOwner 强制停止）
powershell.exe -ExecutionPolicy Bypass -File ./scripts/start-backend.ps1

# 停止后端
powershell.exe -ExecutionPolicy Bypass -File ./scripts/stop-backend.ps1
```

#### 启动成功标志

```
[INFO] Tomcat started on port(s): 8082 (http)
[INFO] Started DataPlatformApplication in 15.234 seconds
```

**访问地址：**
- API 基础路径：`http://localhost:8082/api`
- 示例 API：`http://localhost:8082/api/dashboard/catalog/tables`

### 前端启动（端口 5173）

```bash
cd /d/Study/quant/.vs/harbin_-data-platform/frontend

# 首次运行安装依赖
npm install

# 启动开发服务器
npm run dev
```

#### 启动成功标志

```
VITE v5.1.6  ready in 250 ms

➜  Local:   http://localhost:5173/
➜  Network: use --host to expose
```

**访问地址：**
- 前端首页：`http://localhost:5173`
- 数据目录：`http://localhost:5173/#/catalog`
- 数据血缘：`http://localhost:5173/#/lineage`

### 同时启动后端和前端

**Windows PowerShell（推荐）：**

```powershell
# 打开两个 PowerShell 窗口

# 窗口 1 - 后端
cd backend
.\mvnw.cmd spring-boot:run

# 窗口 2 - 前端
cd frontend
npm run dev
```

**Git Bash：**

```bash
# 窗口 1 - 后端
cd /d/Study/quant/.vs/harbin_-data-platform/backend
./mvnw.cmd spring-boot:run

# 窗口 2 - 前端
cd /d/Study/quant/.vs/harbin_-data-platform/frontend
npm run dev
```

---

## API 测试

### 使用 curl 测试

```bash
# 1. 获取表列表
curl http://localhost:8082/api/dashboard/catalog/tables

# 2. 获取字段详情
curl http://localhost:8082/api/dashboard/catalog/fields/congestion_baseline_5day

# 3. 执行查询
curl -X POST http://localhost:8082/api/dashboard/catalog/query \
  -H "Content-Type: application/json" \
  -d '{"tableName":"congestion_baseline_5day","fields":["road_segment_id","hour_of_day"],"limit":10}'

# 4. 获取字段热度
curl http://localhost:8082/api/dashboard/catalog/hot-fields

# 5. 获取数据血缘
curl http://localhost:8082/api/dashboard/lineage

# 6. 获取数据质量
curl http://localhost:8082/api/dashboard/quality
```

### 使用 Postman 测试

1. 导入以下端点到 Postman Collection
2. 设置 Base URL: `http://localhost:8082/api/dashboard`
3. 依次测试各端点

| 端点 | Method | Body |
|------|--------|------|
| `/catalog/tables` | GET | - |
| `/catalog/fields/{tableName}` | GET | - |
| `/catalog/query` | POST | 见下文 |
| `/catalog/hot-fields` | GET | - |
| `/lineage` | GET | - |
| `/quality` | GET | - |

**查询示例 Body:**
```json
{
  "tableName": "congestion_baseline_5day",
  "fields": ["road_segment_id", "hour_of_day", "baseline_speed_kmh"],
  "filters": [
    {"field": "day_type", "operator": "=", "value": "workday"},
    {"field": "hour_of_day", "operator": ">=", "value": 8}
  ],
  "limit": 20
}
```

---

## 数据库依赖

### 已有表（直接使用）

| Schema | 表名 | 用途 |
|--------|------|------|
| tdm | congestion_baseline_5day | 拥堵基线 |
| tdm | driver_shift_pattern | 司机排班 |
| tdm | grid_hotspot_score | 网格热点 |

### 日志表（可选，如不存在则返回 mock 数据）

```sql
-- 创建访问日志表
CREATE TABLE IF NOT EXISTS ads.data_field_access_log (
    query_id TEXT,
    table_name TEXT,
    field_name TEXT,
    query_time TIMESTAMP,
    row_count INTEGER,
    user_id TEXT
);
```

**说明：** 日志表不存在时，`hot-fields` 端点会返回 mock 数据。

---

## 代码所有权

根据 P3 执行计划定义的框架所有权：

| 文件 | 责任人 | 说明 |
|------|--------|------|
| `DashboardController.java` | P3 | 负责 /catalog/*、/lineage/*、/quality/* 端点 |
| `DashboardService/Impl` | P3 | P3 业务逻辑 |
| `CatalogPage.vue` | P3 | P3 数据目录页面 |
| `LineagePage.vue` | P3 | P3 血缘质量页面 |
| `dashboardStore.js` | P3 维护 | 所有人使用，P3 负责扩展 |

### P1/P2 集成点

P1 和 P2 需要使用 `dashboardStore.js` 中的时间状态来实现时间轴联动：

```javascript
import { useDashboardStore } from '@/stores/dashboardStore'

const store = useDashboardStore()

// 使用全局时间状态
const selectedDate = computed(() => store.selectedDate)
const selectedHour = computed(() => store.selectedHour)
```

---

## 配置说明

### 后端配置 (application.yml)

```yaml
server:
  port: 8082                      # 后端端口
  servlet:
    context-path: /api               # API 基础路径

spring:
  datasource:
    url: jdbc:postgresql://101.35.234.65:5432/postgres
    username: osmuser
    password: pass
```

### 前端配置 (vite.config.js)

```javascript
server: {
  port: 5173,                      # 前端端口
  proxy: {
    '/api': {
      target: 'http://localhost:8082',  # 代理到后端
      changeOrigin: true,
    },
  },
}
```

---

## 常见问题

### Q1: 端口被占用怎么办？

**A:** 使用 `-KillPortOwner` 参数强制停止占用进程：

```bash
powershell.exe -ExecutionPolicy Bypass -File ./scripts/start-backend.ps1 -KillPortOwner
```

### Q2: Java 版本不兼容怎么办？

**A:** 下载并安装 Java 17+，然后设置环境变量：

```bash
export JAVA_HOME="/c/Program Files/Java/jdk-17"
export PATH="$JAVA_HOME/bin:$PATH"
java -version  # 确认版本
```

### Q3: 前端代理不生效怎么办？

**A:** 确保 `vite.config.js` 中的代理目标端口与后端一致（当前为 8082）。

### Q4: 数据库连接失败怎么办？

**A:** 检查数据库配置和连接状态：

```sql
-- 测试数据库连接
psql -h 101.35.234.65 -U osmuser -d postgres
```

---

## Git 提交记录

```
commit d25af0c: [P3-T0/T16/T17/T18/T19] Dashboard catalog and lineage implementation

22 files changed, 2011 insertions(+), 59 deletions(-)

- 后端：6 个 API 端点、11 个 DTO 类、Service 实现
- 前端：2 个页面、路由配置、状态管理、API 封装
- 分支：feature/p3
- 远程：已推送到 origin/feature/p3
```

---

## 附录

### P3 任务完成清单

- [x] T0: 框架设计 + 代码所有权规范
- [x] T16: 数据资产API实现 (4个端点)
- [x] T17: /catalog 页面开发
- [x] T18+T19: 数据血缘+质量API + /lineage 页面
- [x] 前端路由和共用状态配置
- [x] 代码提交到 feature/p3 分支

### 待办事项（集成阶段）

- [ ] T20: 提前集成冒烟测试
- [ ] T21: 集成协调 + 修复
- [ ] T22: 回归测试 + 文档

---

**文档版本：** v1.0
**最后更新：** 2026-05-10

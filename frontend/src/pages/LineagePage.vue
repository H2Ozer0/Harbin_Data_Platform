<template>
  <div class="lineage-page">
    <div class="page-header">
      <h2 class="page-title">数据血缘关系图</h2>
      <div class="layer-filter">
        <button
          v-for="layer in layers"
          :key="layer.value"
          class="layer-btn"
          :class="{ active: store.selectedLayer === layer.value }"
          @click="store.selectedLayer = layer.value"
        >
          {{ layer.label }}
        </button>
      </div>
    </div>

    <div class="lineage-layout">
      <!-- 数据血缘图 -->
      <div class="lineage-section">
        <div class="section-header">
          <h3>数据血缘关系</h3>
          <div class="actions">
            <button
              class="btn btn-sm"
              type="button"
              :disabled="trajectoryLoading"
              @click="() => loadTrajectory()"
            >
              {{ trajectoryLoading ? '加载中...' : '显示轨迹' }}
            </button>
            <button type="button" class="btn btn-sm" @click="resetGraphView">重置视图</button>
            <button v-if="selectedNodeId" type="button" class="btn btn-sm btn-warn" @click="clearSelection">清除选择</button>
          </div>
        </div>

        <!-- 血缘追踪信息栏 -->
        <div v-if="selectedNodeId" class="lineage-info-bar">
          <span class="info-tag info-tag-selected">选中: {{ selectedNodeId }}</span>
          <span class="info-tag info-tag-upstream">上游: {{ upstreamIds.size }} 个表</span>
          <span class="info-tag info-tag-downstream">下游: {{ downstreamIds.size }} 个表</span>
          <span class="info-hint">追溯影响范围与数据异常时可点击节点查看上下游</span>
        </div>

        <div class="lineage-graph" v-if="lineageData.nodes.length > 0"
             @wheel.prevent="onGraphWheel">
          <svg :width="graphLayout.width * zoom" :height="graphLayout.height * zoom" class="lineage-svg"
               :viewBox="`0 0 ${graphLayout.width} ${graphLayout.height}`" preserveAspectRatio="xMidYMid meet">
            <!-- 箭头标记 -->
            <defs>
              <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                <polygon points="0 0, 10 3.5, 0 7" fill="rgba(120, 200, 255, 0.6)" />
              </marker>
              <marker id="arrowhead-upstream" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                <polygon points="0 0, 10 3.5, 0 7" fill="#3B82F6" />
              </marker>
              <marker id="arrowhead-downstream" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                <polygon points="0 0, 10 3.5, 0 7" fill="#22C55E" />
              </marker>
            </defs>

            <!-- 层列标题 -->
            <g v-for="col in graphLayout.columns" :key="'col-' + col.layer">
              <text :x="col.x + nodeWidth / 2" :y="18" class="column-title">{{ col.layer }} 层</text>
              <line :x1="col.x + nodeWidth / 2" :y1="26" :x2="col.x + nodeWidth / 2" :y2="graphLayout.height - 10" class="column-guide" />
            </g>

            <!-- 曲线连线 -->
            <path
              v-for="edge in displayedEdges"
              :key="'edge-' + edge.source + '-' + edge.target"
              :d="getEdgePath(edge)"
              class="edge-line"
              :class="getEdgeClass(edge)"
              :marker-end="getEdgeMarker(edge)"
            />

            <!-- 连线标签 -->
            <g v-for="edge in displayedEdges" :key="'elabel-' + edge.source + '-' + edge.target"
               :class="{ 'edge-label-dimmed': selectedNodeId && !isEdgeHighlighted(edge) }">
              <rect
                class="edge-label-bg"
                :x="getEdgeLabelPos(edge).x - 36"
                :y="getEdgeLabelPos(edge).y - 9"
                :width="edge.label.length * 8 + 12"
                height="18"
                rx="4"
              />
              <text
                class="edge-label"
                :x="getEdgeLabelPos(edge).x"
                :y="getEdgeLabelPos(edge).y"
              >
                {{ edge.label }}
              </text>
            </g>

            <!-- 节点卡片 -->
            <g
              v-for="node in displayedNodes"
              :key="'node-' + node.id"
              class="node"
              :class="getNodeClasses(node)"
              @click="selectNode(node)"
            >
              <!-- 卡片背景 -->
              <rect
                class="node-body"
                :x="getNodePos(node.id).x"
                :y="getNodePos(node.id).y"
                :width="nodeWidth"
                :height="nodeHeightMap[node.id] || 60"
                rx="6"
              />
              <!-- 顶部色条 -->
              <rect
                class="node-header"
                :x="getNodePos(node.id).x"
                :y="getNodePos(node.id).y"
                :width="nodeWidth"
                :height="4"
                rx="2"
                :fill="getNodeColor(node)"
              />
              <!-- 层级徽章 -->
              <rect
                :x="getNodePos(node.id).x + 8"
                :y="getNodePos(node.id).y + 10"
                width="30"
                height="14"
                rx="3"
                :fill="getNodeColor(node)"
                opacity="0.85"
              />
              <text
                :x="getNodePos(node.id).x + 23"
                :y="getNodePos(node.id).y + 20"
                class="layer-badge-text"
              >{{ node.layer }}</text>
              <!-- 中文标签 -->
              <text
                :x="getNodePos(node.id).x + 44"
                :y="getNodePos(node.id).y + 20"
                class="node-label-text"
              >{{ node.label }}</text>
              <!-- 技术表名 -->
              <text
                :x="getNodePos(node.id).x + 8"
                :y="getNodePos(node.id).y + 35"
                class="node-id-text"
              >{{ node.id }}</text>

              <!-- 有列信息时显示列 -->
              <template v-if="node.columns && node.columns.length > 0">
                <text
                  v-for="(col, ci) in getDisplayColumns(node)"
                  :key="'col-' + col"
                  :x="getNodePos(node.id).x + 10"
                  :y="getNodePos(node.id).y + 50 + ci * columnRowHeight"
                  class="column-item"
                >{{ col }}</text>
                <text
                  v-if="node.columns.length > maxDisplayColumns"
                  :x="getNodePos(node.id).x + 10"
                  :y="getNodePos(node.id).y + 50 + maxDisplayColumns * columnRowHeight"
                  class="column-more"
                >+{{ node.columns.length - maxDisplayColumns }} more</text>
              </template>
              <!-- 无列信息时显示元数据 -->
              <template v-else>
                <text
                  :x="getNodePos(node.id).x + nodeWidth / 2"
                  :y="getNodePos(node.id).y + 54"
                  class="node-meta"
                >{{ formatNumber(node.rowCount) }} 行 · {{ node.fieldCount || 0 }} 字段</text>
              </template>
            </g>
          </svg>

          <!-- 图例 -->
          <div class="graph-legend">
            <span class="legend-item"><span class="legend-dot" style="background:#E040FB"></span>ODS 原始层</span>
            <span class="legend-item"><span class="legend-dot" style="background:#66BB6A"></span>DW 仓库层</span>
            <span class="legend-item"><span class="legend-dot" style="background:#FFD740"></span>TDM 模型层</span>
            <span class="legend-item"><span class="legend-dot" style="background:#26C6DA"></span>ADS 应用层</span>
            <span class="legend-item"><span class="legend-dot" style="background:#F59E0B"></span>当前选中</span>
            <span class="legend-sep">|</span>
            <span class="legend-hint">点击节点追溯上下游血缘</span>
          </div>

          <div class="graph-zoom-hint">滚轮缩放 · 缩放 {{ Math.round(zoom * 100) }}%</div>

          <div v-if="loading" class="loading-overlay">
            <div class="spinner"></div>
            <p>正在加载数据血缘...</p>
          </div>
        </div>
      </div>

      <!-- 数据质量指标 -->
      <div class="quality-section">
        <div class="section-header">
          <h3>数据质量指标</h3>
          <span class="table-count">{{ qualityTables.length }} 张表</span>
        </div>
        <div class="quality-cards">
          <div
            v-for="table in qualityTables"
            :key="table.tableName"
            class="quality-card"
            :class="table.status"
          >
            <div class="quality-header">
              <div class="quality-title-row">
                <span class="schema-badge" :class="table.schema">{{ table.schema?.toUpperCase() }}</span>
                <span class="table-name">{{ table.tableName }}</span>
              </div>
              <span class="status-badge" :class="table.status">
                {{ table.status === 'healthy' ? '正常' : table.status === 'warning' ? '告警' : '异常' }}
              </span>
            </div>
            <div class="quality-metrics">
              <div class="metric">
                <span class="metric-label">行数</span>
                <span class="metric-value">{{ formatNumber(table.rowCount) }}</span>
              </div>
              <div class="metric">
                <span class="metric-label">字段数</span>
                <span class="metric-value">{{ table.fieldCount }}</span>
              </div>
              <div class="metric">
                <span class="metric-label">完整度</span>
                <span class="metric-value">{{ table.completenessPct }}%</span>
              </div>
              <div class="metric">
                <span class="metric-label">更新时间</span>
                <span class="metric-value">{{ formatDate(table.lastUpdated) }}</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 轨迹查询结果（地图展示） -->
    <div v-if="showTrajectoryPanel" class="trajectory-section">
      <div class="section-header">
        <h3>轨迹查询{{ selectedTrajectoryNode?.label ? '：' + selectedTrajectoryNode.label : '' }}</h3>
        <div class="actions">
          <button type="button" class="btn btn-sm" :disabled="trajectoryLoading" @click="() => loadTrajectory()">
            {{ trajectoryLoading ? '加载中...' : '刷新轨迹' }}
          </button>
          <button type="button" class="btn btn-sm" @click="closeTrajectory">关闭</button>
        </div>
      </div>
      <div class="trajectory-body">
        <div class="trajectory-info">
          <div class="info-item">
            <span class="info-label">源节点</span>
            <span class="info-value">{{ selectedTrajectoryNode?.label || selectedTrajectoryNode?.id || '无' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">时间窗</span>
            <span class="info-value">{{ trajectoryTimeLabel }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">节点行数</span>
            <span class="info-value">{{ formatNumber(selectedTrajectoryNode?.rowCount) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">轨迹点数</span>
            <span class="info-value">{{ trajectoryPoints.length }}</span>
          </div>
        </div>
        <p v-if="trajectoryError" class="trajectory-error">{{ trajectoryError }}</p>
        <div v-if="trajectoryPoints.length" class="trajectory-map-wrapper">
          <TrajectoryMap :points="trajectoryPoints" />
        </div>
        <div v-else-if="!trajectoryLoading" class="trajectory-empty">
          <p>暂无轨迹数据。请确认数据库已连接，且时间范围（2015-01-03 至 2015-01-07）内有数据。</p>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useDashboardStore } from '@/stores/dashboardStore'
import { dashboardApi } from '@/services/dashboardApi'
import { fetchTrajectorySlice } from '@/services/api'
import TrajectoryMap from '@/components/TrajectoryMap.vue'

const store = useDashboardStore()

const layers = [
  { label: '全部', value: 'all', color: '#0cf' },
  { label: 'ODS', value: 'ODS', color: '#f0f' },
  { label: 'DW', value: 'DW', color: '#0f0' },
  { label: 'TDM', value: 'TDM', color: '#ff0' },
  { label: 'ADS', value: 'ADS', color: '#0ff' },
]

const layerOrder = ['ODS', 'DW', 'TDM', 'ADS']

// --- 节点尺寸常量 ---
const nodeWidth = 200
const nodeHeaderHeight = 30
const columnRowHeight = 16
const maxDisplayColumns = 6
const nodeBottomPadding = 8
const nodeGapY = 16
const columnGap = 100
const headerHeight = 30
const padX = 30
const padY = 15

// --- 响应式状态 ---
const lineageData = ref({ nodes: [], edges: [] })
const qualityTables = ref([])
const trajectoryPoints = ref([])
const loading = ref(false)
const trajectoryLoading = ref(false)
const trajectoryError = ref('')
const selectedTrajectoryNode = ref(null)
const showTrajectoryPanel = ref(false)

// --- 血缘追踪状态 ---
const selectedNodeId = ref(null)

// 图缩放
const zoom = ref(1)

onMounted(async () => {
  await loadLineage()
  await loadQuality()
})

watch(
  () => store.selectedLayer,
  async () => {
    if (store.selectedLayer === 'ODS' || store.selectedLayer === 'all') {
      await loadODSSchema()
    }
  },
)

async function loadLineage() {
  loading.value = true
  try {
    lineageData.value = await dashboardApi.getLineage()
  } catch (error) {
    console.error('Failed to load lineage:', error)
  } finally {
    loading.value = false
  }
}

async function loadQuality() {
  try {
    const data = await dashboardApi.getQuality()
    qualityTables.value = data.tables
  } catch (error) {
    console.error('Failed to load quality:', error)
  }
}

async function loadODSSchema() {
  try {
    const schemaResponse = await dashboardApi.getCatalogTables()
    if (schemaResponse?.length) {
      console.log('Catalog tables:', schemaResponse.length)
    }
  } catch (error) {
    console.error('Failed to load ODS schema:', error)
  }
}

/** 根据点击节点来获取轨迹；调用真实 taxi/trajectory 接口 */
async function loadTrajectory(tableId) {
  trajectoryError.value = ''
  trajectoryLoading.value = true
  try {
    const date = store.selectedDate || '2015-01-05'
    const startTime = `${date}T00:00:00`
    const endTime = `${date}T23:59:59`

    const points = await fetchTrajectorySlice(startTime, endTime, {
      minLon: 126.0,
      maxLon: 127.2,
      minLat: 45.4,
      maxLat: 46.2,
    })

    trajectoryPoints.value = Array.isArray(points) ? points : []

    if (tableId) {
      selectedTrajectoryNode.value = lineageData.value.nodes.find((n) => n.id === tableId) || selectedTrajectoryNode.value
    } else if (!selectedTrajectoryNode.value) {
      selectedTrajectoryNode.value =
        lineageData.value.nodes.find((n) => n.layer === 'ODS') ||
        lineageData.value.nodes[0] ||
        null
    }

    showTrajectoryPanel.value = true
    if (trajectoryPoints.value.length === 0) {
      trajectoryError.value =
        '当前所选时间窗口没有轨迹点。建议日期范围为 2015-01-03 至 2015-01-07，并确认数据库中有 ods_taxi_trips_raw 数据。'
    }
  } catch (error) {
    console.error('Failed to load trajectory:', error)
    trajectoryPoints.value = []
    trajectoryError.value =
      error?.response?.data?.message ||
      error?.message ||
      '轨迹接口请求失败，请确认后端服务及数据库可访问。'
    showTrajectoryPanel.value = true
  } finally {
    trajectoryLoading.value = false
  }
}

const trajectoryTimeLabel = computed(() => {
  const date = store.selectedDate || '2015-01-05'
  return `${date} 00:00:00 至 ${date} 23:59:59`
})

const displayedNodes = computed(() => {
  if (store.selectedLayer === 'all') {
    return lineageData.value.nodes
  }
  return lineageData.value.nodes.filter((n) => n.layer === store.selectedLayer)
})

const displayedEdges = computed(() => {
  const nodeIds = new Set(displayedNodes.value.map((n) => n.id))
  return lineageData.value.edges.filter((e) => nodeIds.has(e.source) && nodeIds.has(e.target))
})

// === 血缘追踪: 上下游计算 ===

function getUpstreamNodes(nodeId) {
  const visited = new Set()
  const queue = [nodeId]
  while (queue.length > 0) {
    const current = queue.shift()
    for (const edge of lineageData.value.edges) {
      if (edge.target === current && !visited.has(edge.source)) {
        visited.add(edge.source)
        queue.push(edge.source)
      }
    }
  }
  return visited
}

function getDownstreamNodes(nodeId) {
  const visited = new Set()
  const queue = [nodeId]
  while (queue.length > 0) {
    const current = queue.shift()
    for (const edge of lineageData.value.edges) {
      if (edge.source === current && !visited.has(edge.target)) {
        visited.add(edge.target)
        queue.push(edge.target)
      }
    }
  }
  return visited
}

const upstreamIds = computed(() => {
  if (!selectedNodeId.value) return new Set()
  return getUpstreamNodes(selectedNodeId.value)
})

const downstreamIds = computed(() => {
  if (!selectedNodeId.value) return new Set()
  return getDownstreamNodes(selectedNodeId.value)
})

function clearSelection() {
  selectedNodeId.value = null
}

// === 节点高度计算 ===

const nodeInfoHeight = 46 // badge + label + id area height

function getNodeHeight(node) {
  const cols = node.columns || []
  if (cols.length === 0) {
    return nodeInfoHeight + 16 + nodeBottomPadding
  }
  const displayCount = Math.min(cols.length, maxDisplayColumns)
  const hasMore = cols.length > maxDisplayColumns
  return nodeInfoHeight + 8 + displayCount * columnRowHeight + (hasMore ? columnRowHeight : 0) + nodeBottomPadding
}

const nodeHeightMap = computed(() => {
  const map = {}
  for (const node of displayedNodes.value) {
    map[node.id] = getNodeHeight(node)
  }
  return map
})

function getDisplayColumns(node) {
  if (!node.columns || node.columns.length === 0) return []
  return node.columns.slice(0, maxDisplayColumns)
}

// === 节点颜色 ===

const layerColors = {
  ODS: '#E040FB',
  DW: '#66BB6A',
  TDM: '#FFD740',
  ADS: '#26C6DA',
}

function getNodeColor(node) {
  if (!selectedNodeId.value) {
    return layerColors[node.layer] || '#3B82F6'
  }
  if (node.id === selectedNodeId.value) return '#F59E0B'
  if (upstreamIds.value.has(node.id)) return '#3B82F6'
  if (downstreamIds.value.has(node.id)) return '#22C55E'
  return layerColors[node.layer] || '#3B82F6'
}

function getNodeClasses(node) {
  if (!selectedNodeId.value) return {}
  return {
    'node-selected': node.id === selectedNodeId.value,
    'node-upstream': upstreamIds.value.has(node.id),
    'node-downstream': downstreamIds.value.has(node.id),
    'node-dimmed': node.id !== selectedNodeId.value &&
      !upstreamIds.value.has(node.id) && !downstreamIds.value.has(node.id),
  }
}

function getEdgeClass(edge) {
  if (!selectedNodeId.value) return {}
  const isUp = edge.target === selectedNodeId.value || upstreamIds.value.has(edge.target) && (edge.source === selectedNodeId.value || upstreamIds.value.has(edge.source))
  const isDown = edge.source === selectedNodeId.value || downstreamIds.value.has(edge.source) && (edge.target === selectedNodeId.value || downstreamIds.value.has(edge.target))
  return {
    'edge-upstream': isUp && !isDown,
    'edge-downstream': isDown && !isUp,
    'edge-highlighted': isUp || isDown,
    'edge-dimmed': !isUp && !isDown,
  }
}

function isEdgeHighlighted(edge) {
  if (!selectedNodeId.value) return true
  const allHighlighted = new Set([selectedNodeId.value, ...upstreamIds.value, ...downstreamIds.value])
  return allHighlighted.has(edge.source) && allHighlighted.has(edge.target)
}

function getEdgeMarker(edge) {
  if (!selectedNodeId.value) return 'url(#arrowhead)'
  if (isEdgeHighlighted(edge)) {
    const srcIsUp = upstreamIds.value.has(edge.source) || edge.source === selectedNodeId.value
    const tgtIsDown = downstreamIds.value.has(edge.target) || edge.target === selectedNodeId.value
    if (tgtIsDown && !srcIsUp) return 'url(#arrowhead-downstream)'
    if (srcIsUp && !tgtIsDown) return 'url(#arrowhead-upstream)'
  }
  return 'url(#arrowhead)'
}

// === 布局计算 ===

// 按层分组
const layerGroups = computed(() => {
  const groups = {}
  for (const layer of layerOrder) {
    groups[layer] = displayedNodes.value.filter((n) => n.layer === layer)
  }
  return groups
})

// 计算每列 x 坐标 + 总图高度（基于动态节点高度）
const graphLayout = computed(() => {
  const activeLayers = layerOrder.filter((l) => (layerGroups.value[l] || []).length > 0)
  const columns = []
  let maxHeight = 0

  for (let i = 0; i < activeLayers.length; i++) {
    const layer = activeLayers[i]
    const x = padX + i * (nodeWidth + columnGap)
    columns.push({ layer, x })

    const nodes = layerGroups.value[layer] || []
    const totalH = nodes.reduce((sum, n) => sum + (nodeHeightMap.value[n.id] || 60) + nodeGapY, -nodeGapY)
    if (totalH > maxHeight) maxHeight = totalH
  }

  const totalHeight = headerHeight + maxHeight + padY * 2
  const totalWidth = activeLayers.length * (nodeWidth + columnGap) - columnGap + padX * 2

  return {
    columns,
    height: Math.max(totalHeight, 200),
    width: Math.max(totalWidth, 600),
    viewBox: `0 0 ${Math.max(totalWidth, 600)} ${Math.max(totalHeight, 200)}`,
    activeLayers,
  }
})

function getNodePos(nodeId) {
  const node = displayedNodes.value.find((n) => n.id === nodeId)
  if (!node) return { x: 0, y: 0 }

  const col = graphLayout.value.columns.find((c) => c.layer === node.layer)
  if (!col) return { x: 0, y: 0 }

  const siblings = layerGroups.value[node.layer] || []
  const totalHeight = siblings.reduce((sum, n) => sum + (nodeHeightMap.value[n.id] || 60) + nodeGapY, -nodeGapY)
  const startY = headerHeight + padY + (graphLayout.value.height - headerHeight - padY * 2 - totalHeight) / 2

  let y = startY
  for (const n of siblings) {
    if (n.id === nodeId) return { x: col.x, y }
    y += (nodeHeightMap.value[n.id] || 60) + nodeGapY
  }

  return { x: col.x, y: startY }
}

// 贝塞尔曲线路径
function getEdgePath(edge) {
  const srcPos = getNodePos(edge.source)
  const tgtPos = getNodePos(edge.target)
  const srcH = nodeHeightMap.value[edge.source] || 60
  const tgtH = nodeHeightMap.value[edge.target] || 60
  const x1 = srcPos.x + nodeWidth
  const y1 = srcPos.y + srcH / 2
  const x2 = tgtPos.x
  const y2 = tgtPos.y + tgtH / 2
  const dx = (x2 - x1) * 0.5
  return `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`
}

function getEdgeLabelPos(edge) {
  const srcPos = getNodePos(edge.source)
  const tgtPos = getNodePos(edge.target)
  const srcH = nodeHeightMap.value[edge.source] || 60
  const tgtH = nodeHeightMap.value[edge.target] || 60
  return {
    x: (srcPos.x + nodeWidth + tgtPos.x) / 2,
    y: (srcPos.y + srcH / 2 + tgtPos.y + tgtH / 2) / 2,
  }
}

function selectNode(node) {
  if (selectedNodeId.value === node.id) {
    selectedNodeId.value = null
  } else {
    selectedNodeId.value = node.id
  }
  // 保留原有 ODS 轨迹加载行为
  if (node.layer === 'ODS') {
    loadTrajectory(node.id)
  }
}

function isTrajectorySource(nodeId) {
  const edge = lineageData.value.edges.find((e) => e.source === nodeId)
  return edge && trajectoryPoints.value.length > 0
}

function closeTrajectory() {
  showTrajectoryPanel.value = false
  trajectoryPoints.value = []
  trajectoryError.value = ''
}

function formatNumber(num) {
  if (!num) return '0'
  return num.toLocaleString()
}

function formatDate(dateStr) {
  if (!dateStr) return '-'
  return dateStr.split('T')[0]
}

// === 图缩放 ===
function onGraphWheel(e) {
  const delta = e.deltaY > 0 ? -0.1 : 0.1
  zoom.value = Math.max(0.5, Math.min(2, zoom.value + delta))
}
function resetGraphView() {
  zoom.value = 1
}
</script>

<style scoped>
.lineage-page {
  display: flex;
  flex-direction: column;
  min-height: 100%;
  padding: 16px;
  background: #0a0e1a;
}

.page-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 16px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
}

.page-title {
  font-size: 20px;
  font-weight: 600;
  color: #0cf;
  margin: 0;
}

.layer-filter {
  display: flex;
  gap: 8px;
}

.layer-btn {
  padding: 6px 14px;
  background: rgba(0, 204, 255, 0.05);
  border: 1px solid rgba(0, 204, 255, 0.2);
  border-radius: 4px;
  color: rgba(255, 255, 255, 0.7);
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.layer-btn:hover {
  background: rgba(0, 204, 255, 0.1);
}

.layer-btn.active {
  background: rgba(0, 204, 255, 0.2);
  border-color: #0cf;
  color: #0cf;
}

.lineage-layout {
  display: flex;
  flex-direction: column;
  gap: 16px;
  padding-top: 16px;
}

.lineage-section,
.quality-section {
  background: rgba(0, 204, 255, 0.03);
  border-radius: 8px;
  border: 1px solid rgba(0, 204, 255, 0.1);
}

.section-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 12px 16px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
}

.actions {
  display: flex;
  gap: 8px;
}

h3 {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

/* === 血缘信息栏 === */
.lineage-info-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 16px;
  background: rgba(245, 158, 11, 0.06);
  border-bottom: 1px solid rgba(245, 158, 11, 0.15);
}

.info-tag {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  font-weight: 500;
}

.info-tag-selected {
  background: rgba(245, 158, 11, 0.15);
  color: #F59E0B;
  border: 1px solid rgba(245, 158, 11, 0.3);
}

.info-tag-upstream {
  background: rgba(59, 130, 246, 0.15);
  color: #60A5FA;
  border: 1px solid rgba(59, 130, 246, 0.3);
}

.info-tag-downstream {
  background: rgba(34, 197, 94, 0.15);
  color: #4ADE80;
  border: 1px solid rgba(34, 197, 94, 0.3);
}

.info-hint {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  margin-left: auto;
}

/* === 图例 === */
.graph-legend {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 8px 16px;
  background: rgba(0, 0, 0, 0.25);
  border-top: 1px solid rgba(0, 204, 255, 0.06);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 5px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.55);
}

.legend-dot {
  display: inline-block;
  width: 10px;
  height: 10px;
  border-radius: 2px;
}

.legend-sep {
  color: rgba(255, 255, 255, 0.15);
}

.legend-hint {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.3);
}

/* === 图形区域 === */
.lineage-graph {
  position: relative;
  padding: 16px;
  background: #0f1520;
  border-radius: 8px;
  overflow: auto;
}

.lineage-svg {
  flex-shrink: 0;
  min-width: 100%;
}

.column-title {
  fill: rgba(255, 255, 255, 0.35);
  font-size: 11px;
  font-weight: 600;
  text-anchor: middle;
  letter-spacing: 2px;
}

.column-guide {
  stroke: rgba(0, 204, 255, 0.06);
  stroke-width: 1;
  stroke-dasharray: 4, 4;
}

.graph-zoom-hint {
  position: absolute;
  bottom: 8px;
  right: 12px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
  pointer-events: none;
}

/* === 边线 === */
.edge-line {
  stroke: rgba(120, 200, 255, 0.45);
  stroke-width: 1.5;
  fill: none;
  marker-end: url(#arrowhead);
  transition: stroke 0.2s, opacity 0.2s;
}

.edge-line:hover {
  stroke: rgba(120, 200, 255, 0.8);
}

.edge-upstream {
  stroke: #3B82F6;
  stroke-width: 2;
}

.edge-downstream {
  stroke: #22C55E;
  stroke-width: 2;
}

.edge-dimmed {
  opacity: 0.12;
}

.edge-label-dimmed {
  opacity: 0.12;
}

.edge-label-bg {
  fill: rgba(15, 21, 32, 0.9);
  stroke: rgba(0, 204, 255, 0.15);
  stroke-width: 0.5;
}

.edge-label {
  fill: rgba(255, 255, 255, 0.6);
  font-size: 9px;
  text-anchor: middle;
  dominant-baseline: middle;
}

/* === 节点卡片 === */
.node {
  cursor: pointer;
}

.node-body {
  fill: rgba(12, 18, 30, 0.95);
  stroke: rgba(255, 255, 255, 0.08);
  stroke-width: 1;
  transition: fill 0.2s, stroke 0.2s, opacity 0.3s;
}

.node:hover .node-body {
  fill: rgba(20, 28, 48, 0.95);
  stroke: rgba(255, 255, 255, 0.2);
  stroke-width: 1.5;
}

.node-header {
  opacity: 0.9;
  transition: fill 0.2s;
}

.node-header-text {
  fill: #fff;
  font-size: 11px;
  font-weight: 600;
  text-anchor: middle;
  dominant-baseline: middle;
  font-family: 'Courier New', monospace;
}

.layer-badge-text {
  fill: #fff;
  font-size: 9px;
  font-weight: 700;
  text-anchor: middle;
  dominant-baseline: middle;
  letter-spacing: 0.5px;
}

.node-label-text {
  fill: rgba(255, 255, 255, 0.9);
  font-size: 12px;
  font-weight: 600;
  dominant-baseline: middle;
}

.node-id-text {
  fill: rgba(255, 255, 255, 0.45);
  font-size: 9px;
  font-family: 'Courier New', monospace;
}

.column-item {
  fill: rgba(255, 255, 255, 0.6);
  font-size: 10px;
  font-family: 'Courier New', monospace;
}

.column-more {
  fill: rgba(255, 255, 255, 0.3);
  font-size: 10px;
  font-style: italic;
}

.node-meta {
  fill: rgba(255, 255, 255, 0.4);
  font-size: 10px;
  text-anchor: middle;
  dominant-baseline: middle;
}

/* === 选中/高亮状态 === */
.node-selected .node-body {
  stroke: #F59E0B;
  stroke-width: 2.5;
  filter: drop-shadow(0 0 8px rgba(245, 158, 11, 0.45));
}

.node-upstream .node-body {
  stroke: #3B82F6;
  stroke-width: 2;
  filter: drop-shadow(0 0 5px rgba(59, 130, 246, 0.35));
}

.node-downstream .node-body {
  stroke: #22C55E;
  stroke-width: 2;
  filter: drop-shadow(0 0 5px rgba(34, 197, 94, 0.35));
}

.node-dimmed {
  opacity: 0.18;
  transition: opacity 0.3s;
}

/* === 加载 === */
.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.7);
  z-index: 10;
}

.spinner {
  width: 40px;
  height: 40px;
  border: 3px solid rgba(0, 204, 255, 0.3);
  border-top-color: #0cf;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to {
    transform: rotate(360deg);
  }
}

.loading-overlay p {
  color: rgba(255, 255, 255, 0.8);
  margin-top: 16px;
}

/* === 数据质量 === */
.quality-cards {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(260px, 1fr));
  gap: 12px;
  padding: 16px;
}

.quality-card {
  background: rgba(0, 204, 255, 0.05);
  border: 1px solid rgba(0, 204, 255, 0.15);
  border-radius: 6px;
  padding: 12px;
}

.quality-card.warning {
  border-color: rgba(255, 170, 0, 0.3);
  background: rgba(255, 170, 0, 0.05);
}

.quality-card.error {
  border-color: rgba(255, 50, 50, 0.3);
  background: rgba(255, 50, 50, 0.05);
}

.quality-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
}

.quality-title-row {
  display: flex;
  align-items: center;
  gap: 8px;
}

.schema-badge {
  padding: 1px 6px;
  border-radius: 3px;
  font-size: 9px;
  font-weight: 700;
  color: #000;
}

.schema-badge.ods { background: #f0f; }
.schema-badge.dw { background: #0f0; }
.schema-badge.tdm { background: #ff0; }
.schema-badge.ads { background: #0cf; }

.table-name {
  color: #0cf;
  font-size: 13px;
  font-weight: 500;
}

.table-count {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
}

.status-badge {
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 10px;
  font-weight: 600;
}

.status-badge.healthy {
  background: rgba(0, 255, 0, 0.15);
  color: #0f0;
}

.status-badge.warning {
  background: rgba(255, 170, 0, 0.15);
  color: #fa0;
}

.status-badge.error {
  background: rgba(255, 50, 50, 0.15);
  color: #f33;
}

.quality-metrics {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}

.metric {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.metric-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.metric-value {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.8);
  font-family: 'Courier New', monospace;
}

/* === 按钮 === */
.btn {
  padding: 8px 16px;
  background: rgba(0, 204, 255, 0.2);
  border: 1px solid rgba(0, 204, 255, 0.3);
  border-radius: 4px;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.btn:hover {
  background: rgba(0, 204, 255, 0.3);
}

.btn-sm {
  padding: 6px 12px;
  font-size: 12px;
}

.btn:disabled {
  opacity: 0.55;
  cursor: not-allowed;
}

.btn-warn {
  background: rgba(245, 158, 11, 0.2);
  border-color: rgba(245, 158, 11, 0.4);
  color: #F59E0B;
}

.btn-warn:hover {
  background: rgba(245, 158, 11, 0.3);
}

/* === 轨迹面板 === */
.trajectory-section {
  background: rgba(0, 204, 255, 0.03);
  border-radius: 8px;
  border: 1px solid rgba(255, 170, 0, 0.15);
}

.trajectory-body {
  padding: 16px;
  overflow-y: auto;
}

.trajectory-info {
  display: grid;
  grid-template-columns: repeat(4, 1fr);
  gap: 12px;
  margin-bottom: 16px;
}

.info-item {
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 8px 12px;
  background: rgba(0, 204, 255, 0.04);
  border-radius: 6px;
}

.info-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.5);
}

.info-value {
  font-size: 13px;
  color: #0cf;
  font-weight: 500;
}

.trajectory-error {
  color: #f88;
  font-size: 13px;
  margin: 0 0 12px;
  padding: 8px 12px;
  background: rgba(255, 50, 50, 0.08);
  border-radius: 6px;
}

.trajectory-map-wrapper {
  margin-top: 12px;
}

.trajectory-empty {
  padding: 32px;
  text-align: center;
}

.trajectory-empty p {
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
  margin: 0;
}
</style>

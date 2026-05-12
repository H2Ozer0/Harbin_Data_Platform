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
          </div>
        </div>
        <div class="lineage-graph" v-if="lineageData.nodes.length > 0">
          <svg :viewBox="graphLayout.viewBox" class="lineage-svg">
            <!-- 箭头标记 -->
            <defs>
              <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                <polygon points="0 0, 10 3.5, 0 7" fill="rgba(0, 204, 255, 0.5)" />
              </marker>
            </defs>

            <!-- 层列标题 -->
            <g v-for="col in graphLayout.columns" :key="'col-' + col.layer">
              <text :x="col.x + nodeWidth / 2" :y="20" class="column-title">{{ col.layer }} 层</text>
              <line :x1="col.x + nodeWidth / 2" :y1="28" :x2="col.x + nodeWidth / 2" :y2="graphLayout.height - 10" class="column-guide" />
            </g>

            <!-- 曲线连线 -->
            <path
              v-for="edge in displayedEdges"
              :key="'edge-' + edge.source + '-' + edge.target"
              :d="getEdgePath(edge)"
              class="edge-line"
            />

            <!-- 连线标签 -->
            <g v-for="edge in displayedEdges" :key="'elabel-' + edge.source + '-' + edge.target">
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

            <!-- 节点 -->
            <g
              v-for="node in displayedNodes"
              :key="'node-' + node.id"
              class="node"
              :class="{
                'node-source': isTrajectorySource(node.id),
                'node-ods': node.layer === 'ODS',
                'node-dw': node.layer === 'DW',
                'node-tdm': node.layer === 'TDM',
                'node-ads': node.layer === 'ADS',
              }"
              @click="selectNode(node)"
            >
              <rect
                class="node-rect"
                :x="getNodePos(node.id).x"
                :y="getNodePos(node.id).y"
                :width="nodeWidth"
                :height="nodeHeight"
                :style="{ stroke: getLayerStrokeColor(node.layer) }"
              />
              <!-- 层标签 -->
              <rect
                class="node-layer-badge"
                :x="getNodePos(node.id).x + 4"
                :y="getNodePos(node.id).y + 3"
                :width="28"
                :height="12"
                rx="3"
                :style="{ fill: getLayerStrokeColor(node.layer) }"
              />
              <text
                :x="getNodePos(node.id).x + 18"
                :y="getNodePos(node.id).y + 11"
                class="badge-text"
              >{{ node.layer }}</text>
              <!-- 中文标签 -->
              <text
                :x="getNodePos(node.id).x + nodeWidth / 2"
                :y="getNodePos(node.id).y + 22"
                class="node-label"
              >{{ node.label }}</text>
              <!-- 表名 -->
              <text
                :x="getNodePos(node.id).x + nodeWidth / 2"
                :y="getNodePos(node.id).y + 34"
                class="node-table-name"
              >{{ node.id }}</text>
              <!-- 行数 -->
              <text
                :x="getNodePos(node.id).x + nodeWidth / 2"
                :y="getNodePos(node.id).y + 46"
                class="node-meta"
              >{{ formatNumber(node.rowCount) }} 行 · {{ node.fieldCount || 0 }} 字段</text>
            </g>
          </svg>

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

    <!-- 轨迹查询结果（直接展示） -->
    <div v-if="showTrajectoryPanel" class="trajectory-section">
      <div class="section-header">
        <h3>轨迹查询{{ selectedNode?.label ? '：' + selectedNode.label : '' }}</h3>
        <button type="button" class="btn btn-sm" @click="closeTrajectory">关闭</button>
      </div>
      <div class="trajectory-body">
        <div class="trajectory-info">
          <div class="info-item">
            <span class="info-label">源节点</span>
            <span class="info-value">{{ selectedNode?.label || selectedNode?.id || '无' }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">时间窗</span>
            <span class="info-value">{{ trajectoryTimeLabel }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">节点行数</span>
            <span class="info-value">{{ formatNumber(selectedNode?.rowCount) }}</span>
          </div>
          <div class="info-item">
            <span class="info-label">轨迹点数</span>
            <span class="info-value">{{ trajectoryPoints.length }}</span>
          </div>
        </div>
        <p v-if="trajectoryError" class="trajectory-error">{{ trajectoryError }}</p>
        <div v-if="trajectoryPoints.length" class="trajectory-preview">
          <div class="preview-caption">轨迹预览（绿=起，蓝=终）</div>
          <svg
            :viewBox="trajPreview.viewBox"
            preserveAspectRatio="xMidYMid meet"
            class="traj-svg"
          >
            <polyline
              :points="trajPreview.polyline"
              fill="none"
              stroke="rgba(255, 170, 0, 0.9)"
              stroke-width="2"
              stroke-linejoin="round"
            />
            <circle
              v-if="trajPreview.start"
              :cx="trajPreview.start.x"
              :cy="trajPreview.start.y"
              r="5"
              fill="#0f0"
            />
            <circle
              v-if="trajPreview.end"
              :cx="trajPreview.end.x"
              :cy="trajPreview.end.y"
              r="5"
              fill="#0cf"
            />
          </svg>
        </div>
        <div class="map-placeholder">
          <MapComponent
            :show-legend="true"
            legend-title="图例"
            :legend-items="trajectoryLegendItems"
            :show-controls="false"
          />
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch } from 'vue'
import { useDashboardStore } from '@/stores/dashboardStore'
import { dashboardApi } from '@/services/dashboardApi'
import MapComponent from '@/components/MapComponent.vue'

const store = useDashboardStore()

const layers = [
  { label: '全部', value: 'all', color: '#0cf' },
  { label: 'ODS', value: 'ODS', color: '#f0f' },
  { label: 'DW', value: 'DW', color: '#0f0' },
  { label: 'TDM', value: 'TDM', color: '#ff0' },
  { label: 'ADS', value: 'ADS', color: '#0ff' },
]

const layerOrder = ['ODS', 'DW', 'TDM', 'ADS']
const layerStrokeColors = { ODS: '#f0f', DW: '#0f0', TDM: '#ff0', ADS: '#0cf' }

const nodeWidth = 130
const nodeHeight = 52
const nodeGapY = 16
const columnGap = 40
const headerHeight = 30
const padX = 16
const padY = 8

const lineageData = ref({ nodes: [], edges: [] })
const qualityTables = ref([])
const trajectoryPoints = ref([])
const loading = ref(false)
const trajectoryLoading = ref(false)
const trajectoryError = ref('')
const selectedNode = ref(null)
const showTrajectoryPanel = ref(false)

const trajectoryLegendItems = [
  { label: '起', color: '#00ff00' },
  { label: '途径点', color: 'rgba(0, 204, 255, 0.6)' },
]

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

/** 根据点击节点来获取轨迹；不选 tableId（ODS 节点 id）则默认第一个 ODS 节点 */
async function loadTrajectory(tableId) {
  trajectoryError.value = ''
  trajectoryLoading.value = true
  try {
    const date = store.selectedDate || '2015-01-05'
    const startTime = `${date}T00:00:00`
    const endTime = `${date}T23:59:59`
    const body = {
      startTime,
      endTime,
      limit: 100,
      minLon: 126.0,
      maxLon: 127.2,
      minLat: 45.4,
      maxLat: 46.2,
    }
    if (tableId) {
      body.deviceId = String(tableId)
    }

    const data = await dashboardApi.getTrajectory(body)
    trajectoryPoints.value = Array.isArray(data?.points) ? data.points : []

    if (tableId) {
      selectedNode.value = lineageData.value.nodes.find((n) => n.id === tableId) || selectedNode.value
    } else if (!selectedNode.value) {
      selectedNode.value =
        lineageData.value.nodes.find((n) => n.layer === 'ODS') ||
        lineageData.value.nodes[0] ||
        null
    }

    showTrajectoryPanel.value = true
    if (trajectoryPoints.value.length === 0) {
      trajectoryError.value =
        '当前所选时间窗口没有轨迹点。建议日期范围为 2015-01-03至2015-01-07，并在第二层（ODS）节点点击后重试。'
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

const trajPreview = computed(() => {
  const pts = trajectoryPoints.value.filter((p) => p.lon != null && p.lat != null)
  if (!pts.length) {
    return { viewBox: '0 0 400 220', polyline: '', start: null, end: null }
  }
  const lons = pts.map((p) => p.lon)
  const lats = pts.map((p) => p.lat)
  const minLon = Math.min(...lons)
  const maxLon = Math.max(...lons)
  const minLat = Math.min(...lats)
  const maxLat = Math.max(...lats)
  const dLon = Math.max(maxLon - minLon, 0.008)
  const dLat = Math.max(maxLat - minLat, 0.008)
  const W = 400
  const H = 220
  const pad = 16
  const innerW = W - pad * 2
  const innerH = H - pad * 2
  const toX = (lon) => pad + ((lon - minLon) / dLon) * innerW
  const toY = (lat) => pad + innerH - ((lat - minLat) / dLat) * innerH
  const polyline = pts.map((p) => `${toX(p.lon)},${toY(p.lat)}`).join(' ')
  const first = pts[0]
  const last = pts[pts.length - 1]
  return {
    viewBox: `0 0 ${W} ${H}`,
    polyline,
    start: { x: toX(first.lon), y: toY(first.lat) },
    end: { x: toX(last.lon), y: toY(last.lat) },
  }
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

// === 布局计算 ===

// 按层分组
const layerGroups = computed(() => {
  const groups = {}
  for (const layer of layerOrder) {
    groups[layer] = displayedNodes.value.filter((n) => n.layer === layer)
  }
  return groups
})

// 计算每列 x 坐标
const graphLayout = computed(() => {
  const activeLayers = layerOrder.filter((l) => (layerGroups.value[l] || []).length > 0)
  const columns = []
  let maxNodesInColumn = 1

  for (let i = 0; i < activeLayers.length; i++) {
    const layer = activeLayers[i]
    const x = padX + i * (nodeWidth + columnGap)
    columns.push({ layer, x })
    const count = (layerGroups.value[layer] || []).length
    if (count > maxNodesInColumn) maxNodesInColumn = count
  }

  const contentHeight = maxNodesInColumn * (nodeHeight + nodeGapY) - nodeGapY
  const totalHeight = headerHeight + contentHeight + padY * 2
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
  const index = siblings.findIndex((n) => n.id === nodeId)
  const totalHeight = siblings.length * (nodeHeight + nodeGapY) - nodeGapY
  const startY = headerHeight + padY + (graphLayout.value.height - headerHeight - padY * 2 - totalHeight) / 2

  return {
    x: col.x,
    y: startY + index * (nodeHeight + nodeGapY),
  }
}

// 贝塞尔曲线路径
function getEdgePath(edge) {
  const srcPos = getNodePos(edge.source)
  const tgtPos = getNodePos(edge.target)
  const x1 = srcPos.x + nodeWidth
  const y1 = srcPos.y + nodeHeight / 2
  const x2 = tgtPos.x
  const y2 = tgtPos.y + nodeHeight / 2
  const dx = (x2 - x1) * 0.5
  return `M ${x1} ${y1} C ${x1 + dx} ${y1}, ${x2 - dx} ${y2}, ${x2} ${y2}`
}

function getEdgeLabelPos(edge) {
  const srcPos = getNodePos(edge.source)
  const tgtPos = getNodePos(edge.target)
  return {
    x: (srcPos.x + nodeWidth + tgtPos.x) / 2,
    y: (srcPos.y + nodeHeight / 2 + tgtPos.y + nodeHeight / 2) / 2,
  }
}

function getLayerStrokeColor(layer) {
  return layerStrokeColors[layer] || '#0cf'
}

function selectNode(node) {
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

.lineage-graph {
  position: relative;
  padding: 16px;
  display: flex;
  justify-content: center;
  align-items: center;
  background: #0f1520;
  border-radius: 8px;
}

.lineage-svg {
  width: 100%;
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

.edge-line {
  stroke: rgba(0, 204, 255, 0.35);
  stroke-width: 1.5;
  fill: none;
  marker-end: url(#arrowhead);
  transition: stroke 0.2s;
}

.edge-line:hover {
  stroke: rgba(0, 204, 255, 0.7);
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

.node {
  cursor: pointer;
}

.node-rect {
  rx: 6;
  fill: rgba(0, 204, 255, 0.06);
  stroke-width: 1.5;
  transition: fill 0.2s, stroke-width 0.2s;
}

.node:hover .node-rect {
  fill: rgba(0, 204, 255, 0.12);
  stroke-width: 2;
}

.node-source .node-rect {
  fill: rgba(0, 255, 0, 0.08);
  stroke: #0f0;
  stroke-width: 2;
}

.node-layer-badge {
  opacity: 0.85;
}

.badge-text {
  fill: #000;
  font-size: 7px;
  font-weight: 700;
  text-anchor: middle;
  dominant-baseline: middle;
}

.node-label {
  fill: #fff;
  font-size: 10px;
  font-weight: 600;
  text-anchor: middle;
  dominant-baseline: middle;
}

.node-table-name {
  fill: rgba(255, 255, 255, 0.45);
  font-size: 7px;
  text-anchor: middle;
  dominant-baseline: middle;
  font-family: 'Courier New', monospace;
}

.node-meta {
  fill: rgba(255, 255, 255, 0.4);
  font-size: 7px;
  text-anchor: middle;
  dominant-baseline: middle;
}

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

.trajectory-preview {
  margin-bottom: 16px;
}

.preview-caption {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.55);
  margin-bottom: 8px;
}

.traj-svg {
  width: 100%;
  height: 200px;
  display: block;
  background: rgba(0, 0, 0, 0.35);
  border-radius: 8px;
  border: 1px solid rgba(0, 204, 255, 0.15);
}

.map-placeholder {
  min-height: 140px;
}
</style>

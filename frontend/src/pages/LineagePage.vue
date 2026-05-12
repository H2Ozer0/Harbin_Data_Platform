<template>
  <div class="lineage-page">
    <div class="page-header">
      <h2 class="page-title">数据血缘与质量</h2>
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
            <button class="btn btn-sm" @click="loadTrajectory">显示轨迹</button>
          </div>
        </div>
        <div class="lineage-graph" v-if="lineageData.nodes.length > 0">
          <svg :viewBox="svgViewBox" class="lineage-svg">
            <!-- 箭头定义 -->
            <defs>
              <marker id="arrowhead" markerWidth="10" markerHeight="7" refX="9" refY="3.5" orient="auto">
                <polygon points="0 0, 10 3.5, 0 7" fill="rgba(0, 204, 255, 0.4)" />
              </marker>
            </defs>
            <!-- 连接线 -->
            <line
              v-for="edge in displayedEdges"
              :key="edge.source + '-' + edge.target"
              :x1="getNodePosition(edge.source).x"
              :y1="getNodePosition(edge.source).y"
              :x2="getNodePosition(edge.target).x"
              :y2="getNodePosition(edge.target).y"
              class="edge-line"
            />

            <!-- 标签背景 -->
            <rect
              v-for="edge in displayedEdges"
              :key="'label-bg-' + edge.source + '-' + edge.target"
              class="edge-label-bg"
              :x="getLabelPosition(edge).x - 30"
              :y="getLabelPosition(edge).y - 10"
              width="60"
              height="20"
            />

            <!-- 连接标签 -->
            <text
              v-for="edge in displayedEdges"
              :key="'label-' + edge.source + '-' + edge.target"
              class="edge-label"
              :x="getLabelPosition(edge).x"
              :y="getLabelPosition(edge).y"
            >
              {{ edge.label }}
            </text>

            <!-- 轨迹（显示选中源的轨迹数据） -->
            <g v-if="trajectoryPoints.length > 0" class="trajectory-group">
              <path
                :d="trajectoryPath"
                class="trajectory-path"
                fill="none"
                stroke="rgba(255, 170, 0, 0.6)"
                stroke-width="2"
                stroke-dasharray="5,5"
              />
              <circle
                v-for="(point, idx) in trajectoryPoints.slice(0, 50)"
                :key="idx"
                :cx="scaleX(point.lon)"
                :cy="scaleY(point.lat)"
                r="2"
                class="trajectory-point"
              />
            </g>

            <!-- 节点 -->
            <g
              v-for="node in displayedNodes"
              :key="node.id"
              class="node"
              :class="{ 'node-source': isTrajectorySource(node.id) }"
              @click="selectNode(node)"
            >
              <rect
                  class="node-rect"
                  :width="140"
                  :height="50"
                  :style="{ fill: getLayerColor(node.layer) }"
              />
              <text x="70" y="18" class="node-label">{{ node.label }}</text>
              <text x="70" y="38" class="node-layer">{{ node.layer }}</text>
              <text x="70" y="48" class="node-meta">
                {{ formatNumber(node.rowCount) }} 行
              </text>
            </g>
          </svg>

          <div v-if="loading" class="loading-overlay">
            <div class="spinner"></div>
            <p>加载血缘数据...</p>
          </div>
        </div>
      </div>

      <!-- 数据质量指标 -->
      <div class="quality-section">
        <div class="section-header">
          <h3>数据质量指标</h3>
        </div>
        <div class="quality-cards">
          <div
            v-for="table in qualityTables"
            :key="table.tableName"
            class="quality-card"
            :class="table.status"
          >
            <div class="quality-header">
              <span class="table-name">{{ table.tableName }}</span>
              <span class="status-badge" :class="table.status">
                {{ table.status === 'healthy' ? '健康' : table.status === 'warning' ? '警告' : '异常' }}
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

    <!-- 轨迹详情弹窗 -->
    <div v-if="showTrajectoryModal" class="modal-overlay" @click="showTrajectoryModal = false">
      <div class="modal-content" @click.stop>
        <div class="modal-header">
          <h3>轨迹详情: {{ selectedNode?.label }}</h3>
          <button class="modal-close" @click="showTrajectoryModal = false">✕</button>
        </div>
        <div class="modal-body">
          <div class="trajectory-info">
            <div class="info-item">
              <span class="info-label">表名：</span>
              <span class="info-value">{{ selectedNode?.id }}</span>
            </div>
            <div class="info-item">
              <span class="info-label">数据量：</span>
              <span class="info-value">{{ formatNumber(selectedNode?.rowCount) }} 行</span>
            </div>
            <div class="info-item">
              <span class="info-label">轨迹点数：</span>
              <span class="info-value">{{ trajectoryPoints.length }} 点</span>
            </div>
          </div>
          <MapComponent
            :show-legend="true"
            :legend-title="'轨迹颜色'"
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
  { label: 'ADS', value: 'ADS', color: '#0ff' }
]

const lineageData = ref({ nodes: [], edges: [] })
const qualityTables = ref([])
const trajectoryPoints = ref([])
const loading = ref(false)
const selectedNode = ref(null)
const showTrajectoryModal = ref(false)

// SVG 坐标转换参数
const svgViewBox = ref('0 0 900 500')
const scaleX = (lon) => (lon - 125.8) / 2.2 * 800 + 50
const scaleY = (lat) => (lat - 45.4) / 1.8 * 400 + 200

// 轨迹图例
const trajectoryLegendItems = [
  { label: '起点', color: '#00ff00' },
  { label: '途经点', color: 'rgba(0, 204, 255, 0.6)' }
]

onMounted(async () => {
  await loadLineage()
  await loadQuality()
})

watch(() => store.selectedLayer, async () => {
  if (store.selectedLayer === 'ODS' || store.selectedLayer === 'all') {
    // ODS 层可以显示轨迹
    await loadODSSchema()
  } else {
    trajectoryPoints.value = []
  }
})

async function loadLineage() {
  loading.value = true
  try {
    lineageData.value = await dashboardApi.getLineage()
    calculateSvgViewBox()
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
    // 查询 ODS 层表结构
    const schemaResponse = await fetch('http://localhost:8082/api/dashboard/catalog/tables').then(r => r.json())
    if (schemaResponse && schemaResponse.length > 0) {
      console.log('ODS Tables:', schemaResponse)
    }
  } catch (error) {
    console.error('Failed to load ODS schema:', error)
  }
}

async function loadTrajectory(tableId) {
  try {
    const response = await fetch(`http://localhost:8082/api/dashboard/trajectory?startTime=2015-01-03T00:00:00&endTime=2015-01-07T23:59:59&limit=200&deviceId=${tableId}`).then(r => r.json())
    if (response) {
      trajectoryPoints.value = response.points || []
      selectedNode.value = lineageData.value.nodes.find(n => n.id === tableId)
      showTrajectoryModal.value = true
    }
  } catch (error) {
    console.error('Failed to load trajectory:', error)
  }
}

const displayedNodes = computed(() => {
  if (store.selectedLayer === 'all') {
    return lineageData.value.nodes
  }
  return lineageData.value.nodes.filter(n => n.layer === store.selectedLayer)
})

const displayedEdges = computed(() => {
  const nodeIds = new Set(displayedNodes.value.map(n => n.id))
  return lineageData.value.edges.filter(e =>
    nodeIds.has(e.source) || nodeIds.has(e.target)
  )
})

function getNodePosition(nodeId) {
  // 动态计算节点位置
  const layerGroups = { 'ODS': [], 'DW': [], 'TDM': [], 'ADS': [] }
  displayedNodes.value.forEach(n => {
    if (layerGroups[n.layer]) {
      layerGroups[n.layer].push(n)
    }
  })

  const baseY = { 'ODS': 300, 'DW': 150, 'TDM': 250, 'ADS': 100 }
  const baseX = { 'ODS': 100, 'DW': 300, 'TDM': 500, 'ADS': 700 }

  const layer = displayedNodes.value.find(n => n.id === nodeId)?.layer || 'ODS'
  const index = (layerGroups[layer] || []).findIndex(n => n.id === nodeId)

  return {
    x: index !== -1 ? baseX[layer] + (index % 3) * 100 : 400,
    y: baseY[layer]
  }
}

function getLabelPosition(edge) {
  const start = getNodePosition(edge.source)
  const end = getNodePosition(edge.target)
  return {
    x: (start.x + end.x) / 2,
    y: (start.y + end.y) / 2
  }
}

function getLayerColor(layer) {
  const found = layers.find(l => l.value === layer)
  return found ? found.color + '33' : '#333'
}

function selectNode(node) {
  // 只为 ODS 层的节点加载轨迹
  if (node.layer === 'ODS') {
    loadTrajectory(node.id)
  }
}

function isTrajectorySource(nodeId) {
  const edge = lineageData.value.edges.find(e => e.source === nodeId)
  return edge && trajectoryPoints.value.length > 0
}

const trajectoryPath = computed(() => {
  if (trajectoryPoints.value.length === 0) return ''

  const points = trajectoryPoints.value.slice(0, 50).map(p => {
    const x = scaleX(p.lon)
    const y = scaleY(p.lat)
    return `${x},${y}`
  }).join(' L ')

  return `M ${points}`
})

function calculateSvgViewBox() {
  if (lineageData.value.nodes.length === 0) {
    svgViewBox.value = '0 0 900 500'
    return
  }

  // 计算包含所有节点的边界
  const xs = []
  const ys = []

  lineageData.value.nodes.forEach(node => {
    const pos = getNodePosition(node.id)
    xs.push(pos.x)
    ys.push(pos.y)
  })

  const minX = Math.min(...xs) - 20
  const maxX = Math.max(...xs) + 170
  const minY = Math.min(...ys) - 30
  const maxY = Math.max(...ys) + 80

  svgViewBox.value = `${minX} ${minY} ${maxX - minX} ${maxY - minY}`
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
  height: 100%;
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
  flex: 1;
  min-height: 0;
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
  padding: 20px;
  display: flex;
  justify-content: center;
  background: #0f1520;
  border-radius: 8px;
  min-height: 400px;
}

.lineage-svg {
  width: 100%;
  max-width: 100%;
  flex: 1;
}

.edge-line {
  stroke: rgba(0, 204, 255, 0.4);
  stroke-width: 2;
  marker-end: url(#arrowhead);
}

.edge-label-bg {
  fill: #0f1520;
}

.edge-label {
  fill: rgba(255, 255, 255, 0.7);
  font-size: 10px;
  text-anchor: middle;
  dominant-baseline: middle;
}

.trajectory-group {
  pointer-events: none;
}

.trajectory-path {
  stroke-dasharray: 8,4;
  animation: dash 1s linear infinite;
}

@keyframes dash {
  to {
    stroke-dashoffset: -12;
  }
}

.trajectory-point {
  fill: #00ff00;
  opacity: 0.6;
}

.node {
  cursor: pointer;
  transition: all 0.2s;
}

.node:hover {
  transform: scale(1.05);
}

.node-source {
  stroke: #00ff00;
  stroke-width: 2;
}

.node-rect {
  rx: 8;
  stroke: rgba(0, 204, 255, 0.5);
  stroke-width: 1;
}

.node-label {
  fill: #fff;
  font-size: 11px;
  font-weight: 600;
  text-anchor: middle;
  dominant-baseline: middle;
}

.node-layer {
  fill: rgba(255, 255, 255, 0.6);
  font-size: 10px;
  text-anchor: middle;
  dominant-baseline: middle;
}

.node-meta {
  fill: rgba(255, 255, 255, 0.5);
  font-size: 9px;
  text-anchor: middle;
  dominant-baseline: middle;
}

.loading-overlay {
  position: absolute;
  inset: 0;
  display: flex;
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
  grid-template-columns: repeat(auto-fill, minmax(300px, 1fr));
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

.table-name {
  color: #0cf;
  font-size: 13px;
  font-weight: 500;
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

.modal-overlay {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.7);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.modal-content {
  background: #0f1520;
  border: 1px solid rgba(0, 204, 255, 0.2);
  border-radius: 8px;
  width: 700px;
  max-width: 90vw;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
}

.modal-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 16px 20px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.modal-close {
  background: none;
  border: none;
  color: rgba(255, 255, 255, 0.6);
  font-size: 18px;
  cursor: pointer;
}

.modal-close:hover {
  color: #fff;
}

.modal-body {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.trajectory-info {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding-bottom: 16px;
}

.info-item {
  display: flex;
  justify-content: space-between;
}

.info-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.7);
}

.info-value {
  font-size: 13px;
  color: #0cf;
  font-weight: 500;
}
</style>

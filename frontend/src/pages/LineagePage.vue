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
          <div class="legend">
            <span class="legend-item" v-for="layer in layers" :key="layer.value">
              <span class="legend-dot" :style="{ background: layer.color }"></span>
              {{ layer.label }}
            </span>
          </div>
        </div>
        <div class="lineage-graph">
          <svg viewBox="0 0 900 500" class="lineage-svg">
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

            <!-- 节点 -->
            <g
              v-for="node in displayedNodes"
              :key="node.id"
              class="node"
              :style="{ transform: `translate(${getNodePosition(node.id).x}, ${getNodePosition(node.id).y})` }"
            >
              <rect
                class="node-rect"
                :width="140"
                :height="50"
                :style="{ fill: getLayerColor(node.layer) }"
              />
              <text x="70" y="20" class="node-label">{{ node.label }}</text>
              <text x="70" y="38" class="node-layer">{{ node.layer }}</text>
            </g>
          </svg>
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
  </div>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboardStore'
import { dashboardApi } from '@/services/dashboardApi'

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

// 节点位置配置（与后端返回的 ID 匹配）
const nodePositions = {
  'ods_taxi_trips_raw': { x: 50, y: 200 },
  'fact_congestion_seg_hour': { x: 280, y: 100 },
  'driver_shift_pattern': { x: 280, y: 250 },
  'grid_hotspot_score': { x: 280, y: 350 },
  'congestion_baseline_5day': { x: 500, y: 100 },
  'ads_congestion_by_segment_hour': { x: 720, y: 100 }
}

onMounted(async () => {
  await loadLineage()
  await loadQuality()
})

async function loadLineage() {
  try {
    lineageData.value = await dashboardApi.getLineage()
  } catch (error) {
    console.error('Failed to load lineage:', error)
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
  return nodePositions[nodeId] || { x: 400, y: 200 }
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

.section-header h3 {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin: 0;
}

.legend {
  display: flex;
  gap: 16px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.legend-dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
}

.lineage-graph {
  padding: 20px;
  display: flex;
  justify-content: center;
}

.lineage-svg {
  width: 100%;
  height: 500px;
  max-width: 1000px;
}

.edge-line {
  stroke: rgba(0, 204, 255, 0.4);
  stroke-width: 2;
  marker-end: url(#arrowhead);
}

.edge-label-bg {
  fill: #0a0e1a;
}

.edge-label {
  fill: rgba(255, 255, 255, 0.6);
  font-size: 10px;
  text-anchor: middle;
  dominant-baseline: middle;
}

.node {
  cursor: pointer;
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
</style>

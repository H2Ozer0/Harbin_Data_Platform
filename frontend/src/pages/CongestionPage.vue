<template>
  <div class="congestion-page">
    <!-- Map Section (left ~65%) -->
    <section class="map-section">
      <div ref="mapRef" class="map-canvas"></div>
      <div class="map-legend">
        <div class="legend-item"><span class="legend-dot" style="background:#00e676"></span>≥ 40 km/h</div>
        <div class="legend-item"><span class="legend-dot" style="background:#ffd700"></span>25-40</div>
        <div class="legend-item"><span class="legend-dot" style="background:#ff8c00"></span>15-25</div>
        <div class="legend-item"><span class="legend-dot" style="background:#ff3d3d"></span>&lt; 15</div>
      </div>
      <div v-if="loading" class="map-loading">
        <div class="spinner"></div>
        <span>加载热力数据...</span>
      </div>
      <div v-if="error" class="map-error">
        <span class="error-icon">⚠</span>
        <span>{{ error }}</span>
      </div>
    </section>

    <!-- Ranking Panel (right ~35%) -->
    <aside class="ranking-panel">
      <div class="panel-header">
        <h3 class="panel-title">Top 10 {{ rankLabels[rankMode] }}</h3>
        <span class="panel-badge">{{ store.selectedDate }} {{ String(store.selectedHour).padStart(2, '0') }}:00</span>
      </div>
      <div class="rank-tabs">
        <button
          v-for="(label, key) in rankLabels"
          :key="key"
          :class="['rank-tab', { active: rankMode === key }]"
          @click="rankMode = key"
        >{{ label }}</button>
      </div>
      <div class="chart-wrap">
        <div v-if="loading" class="chart-loading">
          <div class="spinner small"></div>
          <span>加载中...</span>
        </div>
        <div v-else-if="error" class="chart-error">
          <span>数据加载失败</span>
        </div>
        <div v-else-if="top10.length === 0" class="chart-empty">
          <span>暂无数据</span>
        </div>
        <div ref="chartRef" class="ranking-chart"></div>
      </div>

      <div class="stats-grid">
        <div class="stat-card">
          <span class="stat-value">{{ totalSegments }}</span>
          <span class="stat-label">检测路段</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ avgCongestion }}</span>
          <span class="stat-label">平均拥堵指数</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ maxCongestion }}</span>
          <span class="stat-label">最高拥堵指数</span>
        </div>
      </div>
    </aside>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { Map, Popup } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import * as echarts from 'echarts'
import { useDashboardStore } from '@/stores/dashboardStore'
import { dashboardChartTheme, dashboardColors } from '@/utils/chartTheme'
import axios from 'axios'

const store = useDashboardStore()

// --- State ---
const mapRef = ref(null)
const chartRef = ref(null)
const loading = ref(false)
const error = ref(null)
const segments = ref([])
const rankMode = ref('deviation')
const rankLabels = {
  deviation: '速度降幅',
  congestion: '拥堵指数',
  slowest: '最慢路段',
}

let map = null
let chart = null

// --- Computed ---
// 过滤停车噪声：avg_speed < 5 km/h 是 GPS 漂移/停车，不是真实交通
const validSegments = computed(() =>
  segments.value.filter(s => s.avgSpeedKmh != null),
)

const top10 = computed(() => {
  const pool = validSegments.value.filter(s => s.congestionIndex != null && s.roadName)
  if (rankMode.value === 'deviation') {
    return pool.filter(s => s.deviationPct != null && s.deviationPct < -5)
      .sort((a, b) => (a.deviationPct || 0) - (b.deviationPct || 0))
      .slice(0, 10)
  }
  if (rankMode.value === 'congestion') {
    return pool.filter(s => s.congestionIndex != null)
      .sort((a, b) => (b.congestionIndex || 0) - (a.congestionIndex || 0))
      .slice(0, 10)
  }
  // slowest
  return pool.sort((a, b) => (a.avgSpeedKmh || 0) - (b.avgSpeedKmh || 0)).slice(0, 10)
})

const totalSegments = computed(() => validSegments.value.length)

const avgCongestion = computed(() => {
  const valid = validSegments.value.filter(s => s.congestionIndex != null)
  if (valid.length === 0) return '0.00'
  const sum = valid.reduce((acc, s) => acc + (s.congestionIndex || 0), 0)
  return (sum / valid.length).toFixed(2)
})

const maxCongestion = computed(() => {
  const valid = validSegments.value.filter(s => s.congestionIndex != null)
  if (valid.length === 0) return '0.00'
  const max = Math.max(...valid.map(s => s.congestionIndex || 0))
  return max.toFixed(2)
})

// --- API ---
async function fetchData() {
  loading.value = true
  error.value = null
  try {
    const resp = await axios.get('/api/dashboard/congestion/heatmap', {
      params: {
        dt: store.selectedDate,
        hour: store.selectedHour,
        day_type: 'workday',
      },
    })
    const data = resp.data
    segments.value = data.segments || []
  } catch (e) {
    console.error('[CongestionPage] Fetch error:', e)
    error.value = '数据加载失败，请稍后重试'
    segments.value = []
  } finally {
    loading.value = false
  }
}

// --- Map ---
function initMap() {
  if (!mapRef.value) return

  map = new Map({
    container: mapRef.value,
    style: {
      version: 8,
      sources: {
        'carto-dark': {
          type: 'raster',
          tiles: ['https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png'],
          tileSize: 256,
        },
      },
      glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf',
      layers: [
        { id: 'carto', type: 'raster', source: 'carto-dark', minzoom: 0, maxzoom: 19 },
      ],
    },
    center: [126.63, 45.75],
    zoom: 11,
    pitch: 0,
    bearing: 0,
    interactive: true,
    attributionControl: false,
  })
}

// 速度 → 颜色
function speedColor(speed) {
  if (speed >= 40) return '#00e676'  // 畅通
  if (speed >= 25) return '#ffd700'  // 正常
  if (speed >= 15) return '#ff8c00'  // 偏慢
  return '#ff3d3d'                   // 拥堵
}

function speedLabel(speed) {
  if (speed >= 40) return '畅通'
  if (speed >= 25) return '正常'
  if (speed >= 15) return '偏慢'
  return '拥堵'
}

function updateMap() {
  if (!map) return

  const data = validSegments.value.filter(s => s.geometry && s.congestionIndex != null)
  if (data.length === 0) {
    // 清空图层
    if (map.getSource('congestion-segments')) {
      map.getSource('congestion-segments').setData({ type: 'FeatureCollection', features: [] })
    }
    return
  }

  // 构建 GeoJSON FeatureCollection
  const geojson = {
    type: 'FeatureCollection',
    features: data.map(s => ({
      type: 'Feature',
      properties: {
        id: s.roadSegmentId,
        name: s.roadName,
        speed: s.avgSpeedKmh,
        ci: s.congestionIndex,
        dev: s.deviationPct,
      },
      geometry: typeof s.geometry === 'string' ? JSON.parse(s.geometry) : s.geometry,
    })),
  }

  // 首次加载：添加 source + layers；后续：更新数据
  if (!map.getSource('congestion-segments')) {
    map.addSource('congestion-segments', {
      type: 'geojson',
      data: geojson,
    })

    // 底层：所有路段蓝色发光骨架（宽线，被上层彩色窄线覆盖后露出两侧蓝色边框）
    map.addLayer({
      id: 'road-bg',
      type: 'line',
      source: 'congestion-segments',
      paint: {
        'line-color': 'rgba(60, 160, 255, 0.5)',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 3,
          12, 5,
          15, 8,
        ],
        'line-blur': 2,
      },
    })

    // 绿色层：畅通路段（≥ 40 km/h，亮绿）
    map.addLayer({
      id: 'road-green',
      type: 'line',
      source: 'congestion-segments',
      filter: ['>=', ['get', 'speed'], 40],
      paint: {
        'line-color': '#00e676',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 1.5,
          12, 2.5,
          15, 4,
        ],
        'line-opacity': 0.85,
      },
    })

    // 黄/橙层：正常/偏慢路段（15-40 km/h）
    map.addLayer({
      id: 'road-warm',
      type: 'line',
      source: 'congestion-segments',
      filter: ['all',
        ['>=', ['get', 'speed'], 15],
        ['<', ['get', 'speed'], 40],
      ],
      paint: {
        'line-color': ['case',
          ['<', ['get', 'speed'], 25], '#ff8c00',
          '#ffd700',
        ],
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 2,
          12, 3.5,
          15, 5.5,
        ],
        'line-opacity': 0.85,
      },
    })

    // 顶层：拥堵路段（红色，最粗+发光）
    map.addLayer({
      id: 'road-hot',
      type: 'line',
      source: 'congestion-segments',
      filter: ['<', ['get', 'speed'], 15],
      paint: {
        'line-color': '#ff3d3d',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 2.5,
          12, 5,
          15, 8,
        ],
        'line-opacity': 0.95,
      },
    })

    // 拥堵发光层
    map.addLayer({
      id: 'road-hot-glow',
      type: 'line',
      source: 'congestion-segments',
      filter: ['<', ['get', 'speed'], 15],
      paint: {
        'line-color': '#ff3d3d',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 6,
          12, 12,
          15, 20,
        ],
        'line-opacity': 0.15,
        'line-blur': 8,
      },
    })

    // Tooltip popup
    const popup = new Popup({
      closeButton: false,
      closeOnClick: false,
      maxWidth: '240px',
    })

    map.on('mousemove', 'road-warm', (e) => {
      if (e.features.length === 0) return
      const p = e.features[0].properties
      const spd = (p.speed || 0).toFixed(1)
      const color = speedColor(p.speed)
      const label = speedLabel(p.speed)
      popup
        .setLngLat(e.lngLat)
        .setHTML(`<b>${p.name || '未知道路'}</b><br/>
          速度: <b style="color:${color}">${spd} km/h</b>（${label}）`)
        .addTo(map)
    })

    map.on('mouseleave', 'road-warm', () => popup.remove())
    map.on('mousemove', 'road-hot', (e) => {
      if (e.features.length === 0) return
      const p = e.features[0].properties
      const spd = (p.speed || 0).toFixed(1)
      const color = speedColor(p.speed)
      const label = speedLabel(p.speed)
      popup
        .setLngLat(e.lngLat)
        .setHTML(`<b>${p.name || '未知道路'}</b><br/>
          速度: <b style="color:${color}">${spd} km/h</b>（${label}）`)
        .addTo(map)
    })
    map.on('mouseleave', 'road-hot', () => popup.remove())
  } else {
    map.getSource('congestion-segments').setData(geojson)
  }
}

// --- Chart ---
function initChart() {
  if (!chartRef.value) return
  chart = echarts.init(chartRef.value, null, { renderer: 'canvas' })
}

function updateChart() {
  if (!chart) return

  const data = top10.value
  if (data.length === 0) {
    chart.clear()
    return
  }

  // Top1（最慢）在最上面
  const roadNames = data.map(d => d.roadName).reverse()
  const speeds = data.map(d => d.avgSpeedKmh || 0).reverse()
  const ciValues = data.map(d => d.congestionIndex || 0).reverse()
  const devValues = data.map(d => d.deviationPct || 0).reverse()

  // Determine bar data and x-axis config based on mode
  let barValues, xMax, xName, labelFmt
  if (rankMode.value === 'congestion') {
    barValues = ciValues
    xMax = 1
    xName = '拥堵指数'
    labelFmt = (p) => p.value.toFixed(2)
  } else {
    barValues = speeds
    xMax = Math.ceil(Math.max(...speeds, 10) / 5) * 5 + 5
    xName = 'km/h'
    labelFmt = (p) => `${p.value.toFixed(1)} km/h`
  }

  // 速度 → 颜色（柱状图用）
  function barColor(speed) {
    if (speed < 15) return '#ff3d3d'
    if (speed < 25) return '#ff8c00'
    if (speed < 40) return '#ffd700'
    return '#00e676'
  }

  const option = {
    backgroundColor: 'transparent',
    grid: {
      left: 8,
      right: 16,
      top: 8,
      bottom: 8,
      containLabel: true,
    },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(10,14,26,0.9)',
      borderColor: 'rgba(0,204,255,0.3)',
      borderWidth: 1,
      textStyle: { color: '#fff', fontSize: 12 },
      formatter: (params) => {
        const idx = params[0].dataIndex
        const name = roadNames[idx]
        const spd = speeds[idx]
        const color = barColor(spd)
        const label = speedLabel(spd)
        return `<b>${name}</b><br/>` +
          `速度: <b style="color:${color}">${spd.toFixed(1)} km/h</b>（${label}）`
      },
    },
    xAxis: {
      type: 'value',
      name: xName,
      nameTextStyle: { color: 'rgba(255,255,255,0.4)', fontSize: 10 },
      max: xMax,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } },
    },
    yAxis: {
      type: 'category',
      data: roadNames,
      axisLine: { show: false },
      axisTick: { show: false },
      axisLabel: {
        color: 'rgba(255,255,255,0.7)',
        fontSize: 11,
        width: 120,
        overflow: 'truncate',
        ellipsis: '...',
      },
    },
    series: [
      {
        type: 'bar',
        data: barValues.map((val, i) => ({
          value: val,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: barColor(speeds[i]) + '44' },
              { offset: 1, color: barColor(speeds[i]) },
            ]),
            borderRadius: [0, 4, 4, 0],
          },
        })),
        barWidth: '60%',
        label: {
          show: true,
          position: 'right',
          color: 'rgba(255,255,255,0.6)',
          fontSize: 10,
          formatter: labelFmt,
        },
      },
    ],
  }

  chart.setOption(option, true)
}

function handleResize() {
  chart && chart.resize()
  map && map.resize()
}

// --- Lifecycle ---
onMounted(async () => {
  await nextTick()
  initMap()
  initChart()
  await fetchData()
  updateMap()
  updateChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (chart) {
    chart.dispose()
    chart = null
  }
  if (map) {
    map.remove()
    map = null
  }
})

// --- Reactivity ---
watch(
  [() => store.selectedDate, () => store.selectedHour],
  async () => {
    await fetchData()
    updateMap()
    updateChart()
  },
)

watch(rankMode, () => {
  updateChart()
})
</script>

<style scoped>
.congestion-page {
  display: flex;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #0a0e1a;
}

/* --- Map Section --- */
.map-section {
  position: relative;
  flex: 0 0 65%;
  height: 100%;
  overflow: hidden;
}

.map-canvas {
  width: 100%;
  height: 100%;
  background: #0a0e1a;
}

.map-legend {
  position: absolute;
  bottom: 16px;
  left: 16px;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 4px;
  padding: 10px 14px;
  background: rgba(6, 10, 22, 0.85);
  border: 1px solid rgba(0, 204, 255, 0.15);
  border-radius: 6px;
  backdrop-filter: blur(4px);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.6);
  font-family: 'Courier New', monospace;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}

.map-loading,
.map-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  z-index: 50;
  pointer-events: none;
  color: rgba(255, 255, 255, 0.5);
  font-size: 13px;
}

.map-error {
  color: #ff6b6b;
}

.error-icon {
  font-size: 28px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(0, 204, 255, 0.15);
  border-top-color: #0cf;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

.spinner.small {
  width: 24px;
  height: 24px;
  border-width: 2px;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

/* --- Ranking Panel --- */
.ranking-panel {
  flex: 0 0 35%;
  display: flex;
  flex-direction: column;
  height: 100%;
  border-left: 1px solid rgba(0, 204, 255, 0.12);
  background: rgba(6, 10, 22, 0.95);
  overflow: hidden;
}

.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 14px 18px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
  flex-shrink: 0;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #0cf;
  letter-spacing: 1px;
  margin: 0;
}

.panel-badge {
  font-size: 11px;
  font-family: 'Courier New', monospace;
  color: rgba(255, 255, 255, 0.4);
  padding: 3px 10px;
  border-radius: 10px;
  background: rgba(0, 204, 255, 0.06);
  border: 1px solid rgba(0, 204, 255, 0.1);
}

.chart-wrap {
  flex: 1;
  position: relative;
  min-height: 0;
  padding: 8px;
}

.ranking-chart {
  width: 100%;
  height: 100%;
}

.chart-loading,
.chart-error,
.chart-empty {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  z-index: 10;
  color: rgba(255, 255, 255, 0.35);
  font-size: 13px;
}

.chart-error {
  color: #ff6b6b;
}

/* --- Stats Grid --- */
.stats-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 1px;
  border-top: 1px solid rgba(0, 204, 255, 0.1);
  background: rgba(0, 204, 255, 0.04);
  flex-shrink: 0;
}

.stat-card {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
  padding: 12px 8px;
  background: rgba(6, 10, 22, 0.95);
}

.stat-value {
  font-size: 18px;
  font-weight: 700;
  font-family: 'Courier New', monospace;
  color: #0cf;
  letter-spacing: 1px;
}

.stat-label {
  font-size: 11px;
  color: rgba(255, 255, 255, 0.35);
}

.rank-tabs {
  display: flex;
  gap: 6px;
  padding: 0 18px 10px;
  flex-shrink: 0;
}
.rank-tab {
  flex: 1;
  padding: 5px 8px;
  font-size: 11px;
  color: rgba(255,255,255,0.4);
  background: rgba(0,204,255,0.04);
  border: 1px solid rgba(0,204,255,0.1);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}
.rank-tab:hover {
  color: rgba(255,255,255,0.7);
  border-color: rgba(0,204,255,0.3);
}
.rank-tab.active {
  color: #0cf;
  background: rgba(0,204,255,0.12);
  border-color: rgba(0,204,255,0.4);
}
</style>

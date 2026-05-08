<template>
  <div class="trend-page">
    <!-- 左列：速度曲线 + 排行 -->
    <div class="left-col">
      <!-- Panel 1: 道路类型 24h 速度曲线 -->
      <section class="panel speed-panel">
      <div class="panel-header">
        <h3 class="panel-title">道路类型 24h 速度对比</h3>
        <span class="panel-badge">{{ store.selectedDate }}</span>
      </div>
      <div class="panel-body">
        <div v-if="speedLoading" class="panel-loading">
          <div class="spinner"></div>
          <span>加载速度数据...</span>
        </div>
        <div v-if="speedError" class="panel-error">
          <span class="error-icon">⚠</span>
          <span>{{ speedError }}</span>
        </div>
        <div ref="speedChartRef" class="chart-canvas"></div>
      </div>
    </section>

    <!-- Panel 3: 拥堵持续时间 Top10 -->
    <section class="panel rank-panel">
      <div class="panel-header">
        <h3 class="panel-title">拥堵持续时间 Top10 排行</h3>
        <span class="panel-badge">{{ store.selectedDate }}</span>
      </div>
      <div class="panel-body">
        <div v-if="rankLoading" class="panel-loading">
          <div class="spinner"></div>
          <span>加载排行数据...</span>
        </div>
        <div v-if="rankError" class="panel-error">
          <span class="error-icon">⚠</span>
          <span>{{ rankError }}</span>
        </div>
        <div ref="rankChartRef" class="chart-canvas"></div>
      </div>
    </section>
    </div>

    <!-- 右列：地图 -->
    <section class="panel peak-panel">
      <div class="panel-header">
        <h3 class="panel-title">高峰拥堵空间对比</h3>
        <div class="peak-toggles">
          <button
            :class="['peak-btn', { active: peakMode === 'morning' }]"
            @click="peakMode = 'morning'"
          >早高峰 7-9</button>
          <button
            :class="['peak-btn', { active: peakMode === 'evening' }]"
            @click="peakMode = 'evening'"
          >晚高峰 17-19</button>
        </div>
      </div>
      <div class="panel-body map-body">
        <div v-if="peakLoading" class="panel-loading">
          <div class="spinner"></div>
          <span>加载高峰数据...</span>
        </div>
        <div v-if="peakError" class="panel-error">
          <span class="error-icon">⚠</span>
          <span>{{ peakError }}</span>
        </div>
        <div ref="peakMapRef" class="map-canvas"></div>
        <div class="map-legend">
          <div class="legend-item">
            <span class="legend-line legend-persistent"></span>持续拥堵（2h+）
          </div>
          <div class="legend-item">
            <span class="legend-line legend-transient"></span>短暂拥堵（1h）
          </div>
          <div class="legend-item">
            <span class="legend-line legend-normal"></span>正常路段
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { Map } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { useDashboardStore } from '@/stores/dashboardStore'
import { dashboardChartTheme, dashboardColors } from '@/utils/chartTheme'
import {
  getRoadTypeSpeed,
  getCongestionDurationRanking,
  getCongestionHeatmap,
} from '@/services/dashboardApi'

const store = useDashboardStore()

// --- DOM refs ---
const speedChartRef = ref(null)
const peakMapRef = ref(null)
const rankChartRef = ref(null)

// --- State ---
const speedLoading = ref(false)
const speedError = ref(null)
const speedData = ref([])

const peakLoading = ref(false)
const peakError = ref(null)
const peakMode = ref('morning')
const peakCache = ref({ morning: [], evening: [] })

const rankLoading = ref(false)
const rankError = ref(null)
const rankData = ref([])

// --- Chart / Map instances ---
let speedChart = null
let rankChart = null
let peakMap = null

// --- Road type color mapping ---
const roadTypeColors = {
  trunk: '#00e676',
  primary: '#ffd700',
  secondary: '#ff8c00',
  residential: '#ff3d3d',
}
const roadTypeColorsFaded = {
  trunk: 'rgba(0, 230, 118, 0.15)',
  primary: 'rgba(255, 215, 0, 0.15)',
  secondary: 'rgba(255, 140, 0, 0.15)',
  residential: 'rgba(255, 61, 61, 0.15)',
}
const roadTypeLabels = {
  trunk: '主干道',
  primary: '一级道路',
  secondary: '二级道路',
  residential: '居民区道路',
}

// =====================
// Panel 1: 速度曲线
// =====================
async function fetchSpeedData() {
  speedLoading.value = true
  speedError.value = null
  try {
    const resp = await getRoadTypeSpeed(store.selectedDate)
    speedData.value = Array.isArray(resp) ? resp : (resp.data || [])
  } catch (e) {
    console.error('[TrendPage] Speed fetch error:', e)
    speedError.value = '速度数据加载失败'
    speedData.value = []
  } finally {
    speedLoading.value = false
  }
}

function initSpeedChart() {
  if (!speedChartRef.value) return
  speedChart = echarts.init(speedChartRef.value, null, { renderer: 'canvas' })
}

function updateSpeedChart() {
  if (!speedChart) return
  const data = speedData.value
  if (!data.length) {
    speedChart.clear()
    return
  }

  // Group by road_type
  const grouped = {}
  data.forEach(d => {
    const rt = d.road_type
    if (!grouped[rt]) grouped[rt] = {}
    grouped[rt][d.hour_of_day] = d.avg_speed
  })

  const hours = Array.from({ length: 24 }, (_, i) => i)
  const typeOrder = ['trunk', 'primary', 'secondary', 'residential']

  const series = typeOrder
    .filter(rt => grouped[rt])
    .map(rt => ({
      name: roadTypeLabels[rt] || rt,
      type: 'line',
      smooth: true,
      symbol: 'circle',
      symbolSize: 5,
      lineStyle: {
        color: roadTypeColors[rt],
        width: 2.5,
      },
      itemStyle: {
        color: roadTypeColors[rt],
        borderWidth: 2,
      },
      areaStyle: {
        color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
          { offset: 0, color: roadTypeColorsFaded[rt] || 'rgba(0,204,255,0.15)' },
          { offset: 1, color: 'rgba(0,0,0,0)' },
        ]),
      },
      data: hours.map(h => grouped[rt][h] ?? null),
    }))

  const option = {
    backgroundColor: 'transparent',
    textStyle: { color: '#fff' },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10,14,26,0.92)',
      borderColor: 'rgba(0,204,255,0.3)',
      borderWidth: 1,
      textStyle: { color: '#fff', fontSize: 12 },
      axisPointer: { type: 'cross', crossStyle: { color: 'rgba(255,255,255,0.15)' } },
      formatter: (params) => {
        let html = `<b>${params[0].axisValue}:00</b><br/>`
        params.forEach(p => {
          if (p.value != null) {
            html += `${p.marker} ${p.seriesName}: <b>${p.value.toFixed(1)} km/h</b><br/>`
          }
        })
        return html
      },
    },
    legend: {
      data: series.map(s => s.name),
      textStyle: { color: 'rgba(255,255,255,0.7)', fontSize: 11 },
      top: 4,
      right: 16,
      itemWidth: 16,
      itemHeight: 10,
    },
    grid: {
      left: 12,
      right: 12,
      top: 38,
      bottom: 8,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: hours.map(h => `${h}`),
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.12)' } },
      axisLabel: {
        color: 'rgba(255,255,255,0.55)',
        fontSize: 10,
        fontFamily: 'Courier New, monospace',
        formatter: '{value}:00',
      },
      axisTick: { show: false },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      name: 'km/h',
      min: 15,
      max: 35,
      nameTextStyle: { color: 'rgba(255,255,255,0.5)', fontSize: 10 },
      axisLine: { show: false },
      axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
    },
    series,
    animation: true,
    animationDuration: 800,
    animationEasing: 'cubicOut',
  }

  speedChart.setOption(option, true)
}

// =====================
// Panel 2: 高峰拥堵空间对比
// =====================
const peakHours = {
  morning: [7, 8, 9],
  evening: [17, 18, 19],
}

async function fetchPeakData() {
  peakLoading.value = true
  peakError.value = null
  try {
    // Fetch both morning and evening peak data in parallel
    const [morningResults, eveningResults] = await Promise.all([
      Promise.all(peakHours.morning.map(h =>
        getCongestionHeatmap(store.selectedDate, h, 'workday')
          .then(r => r.segments || [])
          .catch(() => [])
      )),
      Promise.all(peakHours.evening.map(h =>
        getCongestionHeatmap(store.selectedDate, h, 'workday')
          .then(r => r.segments || [])
          .catch(() => [])
      )),
    ])

    peakCache.value.morning = mergePeakSegments(morningResults)
    peakCache.value.evening = mergePeakSegments(eveningResults)
  } catch (e) {
    console.error('[TrendPage] Peak fetch error:', e)
    peakError.value = '高峰数据加载失败'
  } finally {
    peakLoading.value = false
  }
}

/**
 * Merge segments across multiple hours.
 * A segment appearing in 2+ hours is "persistent" congestion (thicker red).
 * A segment appearing in exactly 1 hour is "transient" (thinner orange).
 */
function mergePeakSegments(hourSegmentsArrays) {
  const segMap = new globalThis.Map()

  hourSegmentsArrays.forEach(segments => {
    segments.forEach(seg => {
      const id = seg.roadSegmentId
      if (!segMap.has(id)) {
        segMap.set(id, {
          ...seg,
          _appearCount: 0,
          _minSpeed: Infinity,
          _maxDeviation: 0,
        })
      }
      const entry = segMap.get(id)
      entry._appearCount++
      entry._minSpeed = Math.min(entry._minSpeed, seg.avgSpeedKmh ?? Infinity)
      entry._maxDeviation = Math.min(entry._maxDeviation, seg.deviationPct ?? 0)
    })
  })

  return Array.from(segMap.values())
}

function initPeakMap() {
  if (!peakMapRef.value) return

  peakMap = new Map({
    container: peakMapRef.value,
    style: {
      version: 8,
      sources: {
        'carto-light': {
          type: 'raster',
          tiles: ['https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png'],
          tileSize: 256,
        },
      },
      glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf',
      layers: [
        { id: 'carto', type: 'raster', source: 'carto-light', minzoom: 0, maxzoom: 19 },
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

function updatePeakMap() {
  if (!peakMap) return

  const segments = peakCache.value[peakMode.value] || []
  if (!segments.length) {
    if (peakMap.getSource('peak-segments')) {
      peakMap.getSource('peak-segments').setData({
        type: 'FeatureCollection',
        features: [],
      })
    }
    return
  }

  const features = segments
    .filter(s => s.geometry)
    .map(s => {
      const isCongested = s._minSpeed < 20 && s._maxDeviation < -20
      let peakClass = 'normal'
      if (isCongested) {
        peakClass = s._appearCount >= 2 ? 'persistent' : 'transient'
      }
      return {
        type: 'Feature',
        properties: {
          id: s.roadSegmentId,
          name: s.roadName,
          speed: s._minSpeed,
          deviation: s._maxDeviation,
          hours: s._appearCount,
          _peakClass: peakClass,
        },
        geometry: typeof s.geometry === 'string' ? JSON.parse(s.geometry) : s.geometry,
      }
    })

  const geojson = { type: 'FeatureCollection', features }

  if (!peakMap.getSource('peak-segments')) {
    peakMap.addSource('peak-segments', {
      type: 'geojson',
      data: geojson,
    })

    // All segments - faint gray background
    peakMap.addLayer({
      id: 'road-bg',
      type: 'line',
      source: 'peak-segments',
      paint: {
        'line-color': 'rgba(180, 180, 180, 0.3)',
        'line-width': [
          'interpolate', ['linear'], ['zoom'],
          9, 1, 12, 2, 15, 3,
        ],
      },
    })

    // Persistent congestion - thick red
    peakMap.addLayer({
      id: 'road-persistent',
      type: 'line',
      source: 'peak-segments',
      filter: ['==', ['get', '_peakClass'], 'persistent'],
      paint: {
        'line-color': '#ff3d3d',
        'line-width': [
          'interpolate', ['linear'], ['zoom'],
          9, 3, 12, 5, 15, 8,
        ],
        'line-opacity': 0.9,
      },
    })

    // Persistent glow
    peakMap.addLayer({
      id: 'road-persistent-glow',
      type: 'line',
      source: 'peak-segments',
      filter: ['==', ['get', '_peakClass'], 'persistent'],
      paint: {
        'line-color': '#ff3d3d',
        'line-width': [
          'interpolate', ['linear'], ['zoom'],
          9, 8, 12, 14, 15, 22,
        ],
        'line-opacity': 0.15,
        'line-blur': 8,
      },
    })

    // Transient congestion - thinner orange
    peakMap.addLayer({
      id: 'road-transient',
      type: 'line',
      source: 'peak-segments',
      filter: ['==', ['get', '_peakClass'], 'transient'],
      paint: {
        'line-color': '#ff8c00',
        'line-width': [
          'interpolate', ['linear'], ['zoom'],
          9, 2, 12, 3.5, 15, 5.5,
        ],
        'line-opacity': 0.85,
      },
    })
  } else {
    peakMap.getSource('peak-segments').setData(geojson)
  }
}

// =====================
// Panel 3: 拥堵持续时间 Top10
// =====================
async function fetchRankData() {
  rankLoading.value = true
  rankError.value = null
  try {
    const resp = await getCongestionDurationRanking(store.selectedDate)
    rankData.value = Array.isArray(resp) ? resp : (resp.data || [])
  } catch (e) {
    console.error('[TrendPage] Rank fetch error:', e)
    rankError.value = '排行数据加载失败'
    rankData.value = []
  } finally {
    rankLoading.value = false
  }
}

function initRankChart() {
  if (!rankChartRef.value) return
  rankChart = echarts.init(rankChartRef.value, null, { renderer: 'canvas' })
}

function updateRankChart() {
  if (!rankChart) return
  const data = rankData.value
  if (!data.length) {
    rankChart.clear()
    return
  }

  const top10 = data.slice(0, 10)
  // Reversed so #1 at top
  const roadNames = top10.map(d => d.road_name || '未知道路').reverse()
  const hours = top10.map(d => d.congestion_hours || 0).reverse()
  const roadTypes = top10.map(d => d.road_type || 'secondary').reverse()
  const avgSpeeds = top10.map(d => d.avg_speed_when_congested || 0).reverse()
  const worstDevs = top10.map(d => d.worst_deviation || 0).reverse()

  function typeColor(rt) {
    return roadTypeColors[rt] || '#0cf'
  }

  const option = {
    backgroundColor: 'transparent',
    textStyle: { color: '#fff' },
    tooltip: {
      trigger: 'axis',
      axisPointer: { type: 'shadow' },
      backgroundColor: 'rgba(10,14,26,0.92)',
      borderColor: 'rgba(0,204,255,0.3)',
      borderWidth: 1,
      textStyle: { color: '#fff', fontSize: 12 },
      formatter: (params) => {
        const idx = params[0].dataIndex
        const name = roadNames[idx]
        const h = hours[idx]
        const spd = avgSpeeds[idx]
        const dev = worstDevs[idx]
        const rt = roadTypes[idx]
        return `<b>${name}</b><br/>` +
          `拥堵时长: <b>${h} 小时</b><br/>` +
          `拥堵时均速: <b style="color:${typeColor(rt)}">${spd.toFixed(1)} km/h</b><br/>` +
          `最大降幅: <b>${dev.toFixed(1)}%</b>`
      },
    },
    grid: {
      left: 8,
      right: 20,
      top: 6,
      bottom: 6,
      containLabel: true,
    },
    xAxis: {
      type: 'value',
      name: '拥堵时长 (h)',
      max: 24,
      nameTextStyle: { color: 'rgba(255,255,255,0.4)', fontSize: 10 },
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
        data: hours.map((val, i) => ({
          value: val,
          itemStyle: {
            color: new echarts.graphic.LinearGradient(0, 0, 1, 0, [
              { offset: 0, color: roadTypeColorsFaded[roadTypes[i]] || 'rgba(0,204,255,0.15)' },
              { offset: 1, color: typeColor(roadTypes[i]) },
            ]),
            borderRadius: [0, 4, 4, 0],
          },
        })),
        barWidth: '55%',
        label: {
          show: true,
          position: 'right',
          color: 'rgba(255,255,255,0.6)',
          fontSize: 10,
          formatter: (p) => `${p.value}h`,
        },
      },
    ],
    animation: true,
    animationDuration: 800,
    animationEasing: 'cubicOut',
  }

  rankChart.setOption(option, true)
}

// =====================
// Resize
// =====================
function handleResize() {
  speedChart && speedChart.resize()
  rankChart && rankChart.resize()
  peakMap && peakMap.resize()
}

// =====================
// Lifecycle
// =====================
onMounted(async () => {
  await nextTick()

  initSpeedChart()
  initPeakMap()
  initRankChart()

  await Promise.all([
    fetchSpeedData(),
    fetchPeakData(),
    fetchRankData(),
  ])

  updateSpeedChart()
  updateRankChart()

  // Map needs to be loaded before updating layers
  if (peakMap) {
    peakMap.on('load', () => {
      updatePeakMap()
    })
    // If map already loaded (race condition), update immediately
    if (peakMap.loaded()) {
      updatePeakMap()
    }
  }

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (speedChart) { speedChart.dispose(); speedChart = null }
  if (rankChart) { rankChart.dispose(); rankChart = null }
  if (peakMap) { peakMap.remove(); peakMap = null }
})

// =====================
// Reactivity
// =====================

// Refetch all when date changes
watch(
  () => store.selectedDate,
  async () => {
    await Promise.all([
      fetchSpeedData(),
      fetchPeakData(),
      fetchRankData(),
    ])
    updateSpeedChart()
    updateRankChart()
    if (peakMap && peakMap.loaded()) {
      updatePeakMap()
    }
  },
)

// Toggle morning/evening peak
watch(peakMode, () => {
  if (peakMap && peakMap.loaded()) {
    updatePeakMap()
  }
})
</script>

<style scoped>
.trend-page {
  display: flex;
  flex-direction: row;
  height: 100%;
  background: #0a0e1a;
  gap: 8px;
  padding: 8px;
  box-sizing: border-box;
}

.left-col {
  flex: 0 0 45%;
  display: flex;
  flex-direction: column;
  gap: 8px;
  min-height: 0;
}

/* --- Panel base --- */
.panel {
  display: flex;
  flex-direction: column;
  min-height: 0;
  overflow: hidden;
  background: rgba(6, 10, 22, 0.85);
  border: 1px solid rgba(0, 204, 255, 0.1);
  border-radius: 6px;
}

.speed-panel {
  flex: 1;
}

.peak-panel {
  flex: 1;
}

.rank-panel {
  flex: 1;
}

/* --- Panel header --- */
.panel-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 8px 16px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.08);
  flex-shrink: 0;
}

.panel-title {
  font-size: 13px;
  font-weight: 600;
  color: #0cf;
  letter-spacing: 0.8px;
  margin: 0;
}

.panel-badge {
  font-size: 10px;
  font-family: 'Courier New', monospace;
  color: rgba(255, 255, 255, 0.4);
  padding: 2px 8px;
  border-radius: 10px;
  background: rgba(0, 204, 255, 0.06);
  border: 1px solid rgba(0, 204, 255, 0.1);
}

/* --- Panel body --- */
.panel-body {
  flex: 1;
  position: relative;
  min-height: 0;
}

.map-body {
  position: relative;
}

.chart-canvas {
  width: 100%;
  height: 100%;
}

.map-canvas {
  width: 100%;
  height: 100%;
  background: #0a0e1a;
}

/* --- Peak toggles --- */
.peak-toggles {
  display: flex;
  gap: 4px;
}

.peak-btn {
  padding: 3px 10px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.4);
  background: rgba(0, 204, 255, 0.04);
  border: 1px solid rgba(0, 204, 255, 0.1);
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
  font-family: inherit;
}

.peak-btn:hover {
  color: rgba(255, 255, 255, 0.7);
  border-color: rgba(0, 204, 255, 0.3);
}

.peak-btn.active {
  color: #0cf;
  background: rgba(0, 204, 255, 0.12);
  border-color: rgba(0, 204, 255, 0.4);
}

/* --- Map legend --- */
.map-legend {
  position: absolute;
  bottom: 8px;
  left: 8px;
  z-index: 20;
  display: flex;
  flex-direction: column;
  gap: 3px;
  padding: 6px 10px;
  background: rgba(10, 14, 26, 0.88);
  border-radius: 4px;
  border: 1px solid rgba(0, 204, 255, 0.12);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 10px;
  color: rgba(255, 255, 255, 0.6);
  font-family: 'Courier New', monospace;
}

.legend-line {
  display: inline-block;
  width: 16px;
  height: 3px;
  border-radius: 1px;
  flex-shrink: 0;
}

.legend-persistent {
  background: #ff3d3d;
  height: 4px;
}

.legend-transient {
  background: #ff8c00;
}

.legend-normal {
  background: rgba(180, 180, 180, 0.5);
}

/* --- Loading / Error --- */
.panel-loading,
.panel-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  z-index: 10;
  pointer-events: none;
  font-size: 12px;
}

.panel-loading {
  color: rgba(255, 255, 255, 0.4);
}

.panel-error {
  color: #ff6b6b;
}

.error-icon {
  font-size: 24px;
}

.spinner {
  width: 28px;
  height: 28px;
  border: 3px solid rgba(0, 204, 255, 0.12);
  border-top-color: #0cf;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>

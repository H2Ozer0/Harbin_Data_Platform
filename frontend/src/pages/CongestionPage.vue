<template>
  <div class="congestion-page">
    <!-- Map Section (left ~65%) -->
    <section class="map-section">
      <div ref="mapRef" class="map-canvas"></div>
      <div class="map-legend">
        <div class="legend-item"><span class="legend-dot" style="background:#00e676"></span>≥ 40 畅通</div>
        <div class="legend-item"><span class="legend-dot" style="background:#66bb6a"></span>25-40 正常</div>
        <div class="legend-item"><span class="legend-dot" style="background:#ffd700"></span>偏慢 / 轻微拥堵</div>
        <div class="legend-item"><span class="legend-dot" style="background:#ff8c00"></span>异常降速 (降幅>20%)</div>
        <div class="legend-item"><span class="legend-dot" style="background:#ff3d3d"></span>真拥堵 (低速+降幅>20%)</div>
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
          <span class="stat-value">{{ avgSpeed }}</span>
          <span class="stat-label">平均速度 km/h</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ congestedCount }}</span>
          <span class="stat-label">拥堵路段</span>
        </div>
        <div class="stat-card">
          <span class="stat-value">{{ maxDeviation }}</span>
          <span class="stat-label">最大降幅</span>
        </div>
      </div>

      <!-- 24h 速度 sparkline（点击地图路段后显示） -->
      <div v-show="selectedSegment" class="sparkline-section">
        <div ref="sparkRef" class="sparkline-chart"></div>
        <button class="sparkline-close" @click="selectedSegment = null">✕</button>
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
const sparkRef = ref(null)
const loading = ref(false)
const error = ref(null)
const segments = ref([])
const rankMode = ref('deviation')
const rankLabels = {
  deviation: '速度降幅',
  congestion: '拥堵指数',
  slowest: '最慢路段',
}
const selectedSegment = ref(null)  // { id, name }

let map = null
let chart = null
let sparkChart = null

// --- 24h 预加载缓存 ---
const hourCache = new globalThis.Map()        // hour (0-23) → segments[]
let prefetchedDate = null           // 已预加载的日期
let prefetchAbort = null            // AbortController，换日期时取消旧请求
const prefetchAllProgress = ref(0)  // 0-24，进度跟踪

// --- Computed ---
// 过滤：tripCount < 10 且速度 < 10 km/h 的视为短暂低速（停车/等灯），不标红
// 这类路段虽然速度低但采样车辆少，不是真正的交通拥堵
const validSegments = computed(() =>
  segments.value.filter(s => {
    if (s.avgSpeedKmh == null) return false
    // 采样少 + 速度低 = 短暂停车/空驶，不属于拥堵
    if (s.tripCount != null && s.tripCount < 10 && s.avgSpeedKmh < 10) return false
    return true
  }),
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

const avgSpeed = computed(() => {
  const segs = validSegments.value
  if (segs.length === 0) return '0.0'
  const sum = segs.reduce((acc, s) => acc + (s.avgSpeedKmh || 0), 0)
  return (sum / segs.length).toFixed(1)
})

const congestedCount = computed(() => {
  const total = validSegments.value.length
  if (total === 0) return '0 (0%)'
  const congested = validSegments.value.filter(s => {
    const dev = s.deviationPct
    return dev != null && dev < -20
  }).length
  const pct = ((congested / total) * 100).toFixed(1)
  return `${congested} (${pct}%)`
})

const maxDeviation = computed(() => {
  const withDev = validSegments.value.filter(s => s.deviationPct != null)
  if (withDev.length === 0) return '0%'
  const min = Math.min(...withDev.map(s => s.deviationPct || 0))
  return `${min.toFixed(1)}%`
})

// 24h 速度数据（从预加载缓存中提取选中路段）
const sparklineData = computed(() => {
  if (!selectedSegment.value) return null
  const speeds = []
  for (let h = 0; h < 24; h++) {
    const cached = hourCache.get(h)
    if (!cached) { speeds.push(null); continue }
    const seg = cached.find(s => s.roadSegmentId === selectedSegment.value.id)
    speeds.push(seg?.avgSpeedKmh ?? null)
  }
  return { name: selectedSegment.value.name, speeds }
})

// --- API ---
function getDayType(date) {
  const d = new Date(date)
  const day = d.getDay()
  // 0=Sun, 6=Sat → weekend; 1-5 → workday
  return (day === 0 || day === 6) ? 'weekend' : 'workday'
}

async function fetchSingleHour(date, hour, signal) {
  const resp = await axios.get('/api/dashboard/congestion/heatmap', {
    params: { dt: date, hour, day_type: getDayType(date) },
    signal,
  })
  return resp.data.segments || []
}

/** 加载指定日期的数据；先加载当前小时（立即显示），其余后台静默预加载 */
async function prefetchAllHours(date, currentHour) {
  // 取消上一次未完成的预加载
  if (prefetchAbort) prefetchAbort.abort()
  const ac = new AbortController()
  prefetchAbort = ac

  hourCache.clear()
  prefetchedDate = date
  prefetchAllProgress.value = 0

  // 1) 先加载当前小时（用户立刻看到）
  loading.value = true
  error.value = null
  try {
    const segs = await fetchSingleHour(date, currentHour, ac.signal)
    if (ac.signal.aborted) return
    hourCache.set(currentHour, segs)
    segments.value = segs
    prefetchAllProgress.value = 1
  } catch (e) {
    if (axios.isCancel(e) || e.name === 'CanceledError' || ac.signal.aborted) return
    console.error('[CongestionPage] Fetch current hour error:', e)
    error.value = '数据加载失败，请稍后重试'
    segments.value = []
    loading.value = false
    return
  }
  loading.value = false

  // 2) 后台静默加载其余小时（每次 2 个并发，不阻塞 UI）
  const remaining = Array.from({ length: 24 }, (_, i) => i).filter(h => h !== currentHour)
  const batchSize = 2
  for (let i = 0; i < remaining.length; i += batchSize) {
    if (ac.signal.aborted) return
    const batch = remaining.slice(i, i + batchSize)
    const results = await Promise.allSettled(
      batch.map(h => fetchSingleHour(date, h, ac.signal).catch(() => null)),
    )
    if (ac.signal.aborted) return
    results.forEach((r, idx) => {
      if (r.status === 'fulfilled' && r.value) {
        hourCache.set(batch[idx], r.value)
      }
    })
    // 每批之间让出主线程，避免卡顿
    await new Promise(r => setTimeout(r, 50))
  }
}

/** 从缓存切换小时（零延迟） */
function switchHour(hour) {
  const cached = hourCache.get(hour)
  if (cached) {
    segments.value = cached
    updateMap()
    updateChart()
    return true
  }
  return false
}

// --- Map ---
function getCongestionTileUrl() {
  return store.mapStyle === 'dark'
    ? 'https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png'
    : 'https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png'
}

function initMap() {
  if (!mapRef.value) return

  map = new Map({
    container: mapRef.value,
    style: {
      version: 8,
      sources: {
        'carto-tiles': {
          type: 'raster',
          tiles: [getCongestionTileUrl()],
          tileSize: 256,
        },
      },
      glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf',
      layers: [
        { id: 'carto', type: 'raster', source: 'carto-tiles', minzoom: 0, maxzoom: 19 },
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

// 颜色判断：速度 + deviation + tripCount 三维度
// deviation 阈值根据 tripCount 动态调整：
//   tripCount 高（>50）→ 统计可靠，deviation<-20% 就标拥堵
//   tripCount 低（~10）→ 几辆车采样偏差大，需要 deviation<-35% 才标拥堵
function congestionColor(speed, deviation, tripCount) {
  // 计算动态 deviation 阈值
  const devThreshold = tripCount != null
    ? Math.min(35, 20 + Math.max(0, (50 - tripCount) * 0.5))
    : 25  // 无 tripCount 数据时用保守值

  if (deviation != null && deviation < -devThreshold) {
    if (speed < 15) return '#ff3d3d'   // 真拥堵
    if (speed < 25) return '#ff8c00'   // 异常降速
    return '#ffd700'                    // 轻微影响
  }
  // 正常波动范围内，按绝对速度着色
  if (speed >= 40) return '#00e676'    // 畅通
  if (speed >= 25) return '#66bb6a'    // 正常
  if (speed >= 10) return '#ffd700'    // 偏慢
  return '#ff3d3d'                     // 极慢
}

function congestionLabel(speed, deviation, tripCount) {
  const devThreshold = tripCount != null
    ? Math.min(35, 20 + Math.max(0, (50 - tripCount) * 0.5))
    : 25

  if (deviation != null && deviation < -devThreshold) {
    if (speed < 15) return '拥堵'
    if (speed < 25) return '异常降速'
    return '轻微拥堵'
  }
  if (speed >= 40) return '畅通'
  if (speed >= 25) return '正常'
  if (speed >= 10) return '偏慢'
  return '极慢'
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

  // 构建 GeoJSON FeatureCollection，附带根据速度+deviation算好的颜色
  const geojson = {
    type: 'FeatureCollection',
    features: data.map(s => {
      const spd = s.avgSpeedKmh
      const dev = s.deviationPct
      return {
        type: 'Feature',
        properties: {
          id: s.roadSegmentId,
          name: s.roadName,
          speed: spd,
          ci: s.congestionIndex,
          dev,
          tripCount: s.tripCount,
          _color: congestionColor(spd, dev, s.tripCount),
          _label: congestionLabel(spd, dev, s.tripCount),
        },
        geometry: typeof s.geometry === 'string' ? JSON.parse(s.geometry) : s.geometry,
      }
    }),
  }

  // 首次加载：添加 source + layers；后续：更新数据
  if (!map.getSource('congestion-segments')) {
    map.addSource('congestion-segments', {
      type: 'geojson',
      data: geojson,
    })

    // 底层：蓝色骨架（所有路段）
    map.addLayer({
      id: 'road-bg',
      type: 'line',
      source: 'congestion-segments',
      paint: {
        'line-color': 'rgba(60, 160, 255, 0.5)',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 3, 12, 5, 15, 8,
        ],
        'line-blur': 2,
      },
    })

    // 浅绿层：正常（25-40 km/h）
    map.addLayer({
      id: 'road-green-dim',
      type: 'line',
      source: 'congestion-segments',
      filter: ['==', ['get', '_color'], '#66bb6a'],
      paint: {
        'line-color': '#66bb6a',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 1.5, 12, 2.5, 15, 4,
        ],
        'line-opacity': 0.8,
      },
    })

    // 绿色层：畅通（≥ 40 km/h）
    map.addLayer({
      id: 'road-green',
      type: 'line',
      source: 'congestion-segments',
      filter: ['==', ['get', '_color'], '#00e676'],
      paint: {
        'line-color': '#00e676',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 1.5, 12, 2.5, 15, 4,
        ],
        'line-opacity': 0.85,
      },
    })

    // 黄色层：偏慢 / 轻微拥堵
    map.addLayer({
      id: 'road-yellow',
      type: 'line',
      source: 'congestion-segments',
      filter: ['==', ['get', '_color'], '#ffd700'],
      paint: {
        'line-color': '#ffd700',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 2, 12, 3.5, 15, 5.5,
        ],
        'line-opacity': 0.85,
      },
    })

    // 橙色层：异常降速（deviation<-20% 且速度 15-25）
    map.addLayer({
      id: 'road-orange',
      type: 'line',
      source: 'congestion-segments',
      filter: ['==', ['get', '_color'], '#ff8c00'],
      paint: {
        'line-color': '#ff8c00',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 2.5, 12, 4, 15, 6,
        ],
        'line-opacity': 0.9,
      },
    })

    // 顶层：真拥堵路段（红色，速度<15 且 deviation<-20%）
    map.addLayer({
      id: 'road-hot',
      type: 'line',
      source: 'congestion-segments',
      filter: ['==', ['get', '_color'], '#ff3d3d'],
      paint: {
        'line-color': '#ff3d3d',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 2.5, 12, 5, 15, 8,
        ],
        'line-opacity': 0.95,
      },
    })

    // 拥堵发光层
    map.addLayer({
      id: 'road-hot-glow',
      type: 'line',
      source: 'congestion-segments',
      filter: ['==', ['get', '_color'], '#ff3d3d'],
      paint: {
        'line-color': '#ff3d3d',
        'line-width': ['interpolate', ['linear'], ['zoom'],
          9, 6, 12, 12, 15, 20,
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

    const hoverLayers = ['road-green', 'road-green-dim', 'road-yellow', 'road-orange', 'road-hot']
    hoverLayers.forEach(layerId => {
      map.on('mousemove', layerId, (e) => {
        if (e.features.length === 0) return
        const p = e.features[0].properties
        const spd = (p.speed || 0).toFixed(1)
        const label = p._label || ''
        const color = p._color || '#fff'
        popup
          .setLngLat(e.lngLat)
          .setHTML(`<b>${p.name || '未知道路'}</b><br/>
            速度: <b style="color:${color}">${spd} km/h</b>（${label}）`)
          .addTo(map)
      })
      map.on('mouseleave', layerId, () => popup.remove())
    })

    // 点击路段 → 显示 24h sparkline
    hoverLayers.forEach(layerId => {
      map.on('click', layerId, (e) => {
        if (e.features.length === 0) return
        const p = e.features[0].properties
        selectedSegment.value = { id: p.id, name: p.name || '未知道路' }
      })
    })

    // 点击空白处取消选中
    map.on('click', (e) => {
      const features = map.queryRenderedFeatures(e.point, { layers: hoverLayers })
      if (features.length === 0) selectedSegment.value = null
    })
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

  // 速度 → 文字标签（柱状图 tooltip 用）
  function speedLabel(speed) {
    if (speed < 15) return '拥堵'
    if (speed < 25) return '异常降速'
    if (speed < 40) return '偏慢'
    return '畅通'
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

function updateSparkline() {
  if (!sparkRef.value) return
  if (!sparkChart) {
    sparkChart = echarts.init(sparkRef.value, null, { renderer: 'canvas' })
  }
  sparkChart.resize()
  const data = sparklineData.value
  if (!data) { sparkChart.clear(); return }

  const hours = Array.from({ length: 24 }, (_, i) => `${i}:00`)
  const speeds = data.speeds

  sparkChart.setOption({
    backgroundColor: 'transparent',
    grid: { top: 28, right: 12, bottom: 24, left: 36 },
    title: {
      text: `${data.name} — 24h 速度变化`,
      textStyle: { color: 'rgba(255,255,255,0.8)', fontSize: 12, fontWeight: 500 },
      left: 8, top: 4,
    },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10,14,26,0.9)',
      borderColor: 'rgba(0,204,255,0.3)',
      textStyle: { color: '#fff', fontSize: 11 },
      formatter: (params) => {
        const p = params[0]
        const spd = p.value != null ? p.value.toFixed(1) : '无数据'
        return `${p.name}<br/>速度: <b>${spd} km/h</b>`
      },
    },
    xAxis: {
      type: 'category',
      data: hours,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.1)' } },
      axisLabel: {
        color: 'rgba(255,255,255,0.4)', fontSize: 9,
        interval: 3,
      },
    },
    yAxis: {
      type: 'value',
      min: (value) => Math.max(0, Math.floor(value.min - 5)),
      axisLine: { show: false },
      axisLabel: { color: 'rgba(255,255,255,0.4)', fontSize: 9 },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.04)' } },
    },
    series: [{
      type: 'line',
      data: speeds,
      smooth: true,
      symbol: 'circle',
      symbolSize: 4,
      lineStyle: { color: '#00e676', width: 2 },
      itemStyle: { color: '#00e676' },
      areaStyle: {
        color: { type: 'linear', x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(0, 230, 118, 0.25)' },
            { offset: 1, color: 'rgba(0, 230, 118, 0.02)' },
          ],
        },
      },
      markLine: {
        silent: true,
        data: [{ yAxis: store.selectedHour, label: { show: false } }],
        lineStyle: { color: '#ff3d3d', width: 1.5, type: 'dashed' },
      },
    }],
  }, true)
}

function handleResize() {
  chart && chart.resize()
  sparkChart && sparkChart.resize()
  map && map.resize()
}

// --- Lifecycle ---
onMounted(async () => {
  await nextTick()
  initMap()
  initChart()
  await prefetchAllHours(store.selectedDate, store.selectedHour)
  updateMap()
  updateChart()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (prefetchAbort) prefetchAbort.abort()
  hourCache.clear()
  prefetchedDate = null
  window.removeEventListener('resize', handleResize)
  if (sparkChart) {
    sparkChart.dispose()
    sparkChart = null
  }
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
  async ([date, hour], [oldDate]) => {
    if (date !== oldDate) {
      // 换日期 → 重新预加载 24 小时
      await prefetchAllHours(date, hour)
      updateMap()
      updateChart()
    } else {
      // 同日期换小时 → 缓存命中零延迟；未命中则单独请求
      if (!switchHour(hour)) {
        loading.value = true
        try {
          const segs = await fetchSingleHour(date, hour)
          hourCache.set(hour, segs)
          segments.value = segs
        } catch (e) {
          console.error('[CongestionPage] Fetch hour error:', e)
          segments.value = []
        }
        loading.value = false
        updateMap()
        updateChart()
      }
    }
  },
)

watch(rankMode, () => {
  updateChart()
})

watch(selectedSegment, () => {
  nextTick(() => updateSparkline())
})

// Switch tile source when global map style changes
watch(() => store.mapStyle, () => {
  if (!map) return
  map.setStyle({
    version: 8,
    sources: {
      'carto-tiles': {
        type: 'raster',
        tiles: [getCongestionTileUrl()],
        tileSize: 256,
      },
    },
    glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf',
    layers: [
      { id: 'carto', type: 'raster', source: 'carto-tiles', minzoom: 0, maxzoom: 19 },
    ],
  })
  map.once('style.load', () => updateMap())
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
  padding: 8px 12px;
  background: rgba(6, 10, 22, 0.88);
  border-radius: 6px;
  backdrop-filter: blur(6px);
  border: 1px solid rgba(0, 204, 255, 0.12);
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.7);
  font-family: 'Courier New', monospace;
}

.legend-dot {
  width: 10px;
  height: 10px;
  border-radius: 2px;
  flex-shrink: 0;
}

.map-loading {
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
  color: rgba(6, 10, 22, 0.7);
  font-size: 13px;
  background: rgba(255, 255, 255, 0.75);
  padding: 16px 24px;
  border-radius: 8px;
  backdrop-filter: blur(4px);
}

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
  color: #d50000;
  font-size: 13px;
  background: rgba(255, 255, 255, 0.85);
  padding: 16px 24px;
  border-radius: 8px;
  backdrop-filter: blur(4px);
}

.map-error {
  color: #d50000;
}

.error-icon {
  font-size: 28px;
}

.spinner {
  width: 36px;
  height: 36px;
  border: 3px solid rgba(6, 10, 22, 0.1);
  border-top-color: #00e676;
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

/* --- Sparkline Section --- */
.sparkline-section {
  position: relative;
  height: 150px;
  min-height: 150px;
  flex-shrink: 0;
  border-top: 1px solid rgba(0, 204, 255, 0.1);
  padding: 4px 8px;
  box-sizing: border-box;
}

.sparkline-chart {
  width: 100%;
  height: 100%;
}

.sparkline-close {
  position: absolute;
  top: 6px;
  right: 8px;
  background: none;
  border: 1px solid rgba(255,255,255,0.2);
  color: rgba(255,255,255,0.5);
  border-radius: 3px;
  cursor: pointer;
  font-size: 10px;
  padding: 1px 5px;
  z-index: 5;
}

.sparkline-close:hover {
  color: #fff;
  border-color: rgba(255,255,255,0.5);
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
  flex: 1 1 0;
  position: relative;
  min-height: 0;
  overflow: hidden;
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

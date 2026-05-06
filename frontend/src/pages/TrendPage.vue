<template>
  <div class="trend-page">
    <!-- Top: 5-Day Trend Line Chart -->
    <section class="chart-section top-section">
      <div class="section-header">
        <h3 class="section-title">5天速度趋势</h3>
        <span class="section-badge">{{ dateRange }}</span>
      </div>
      <div class="chart-container">
        <div v-if="trendLoading" class="chart-loading">
          <div class="spinner"></div>
          <span>加载趋势数据...</span>
        </div>
        <div v-if="trendError" class="chart-error">
          <span class="error-icon">⚠</span>
          <span>{{ trendError }}</span>
        </div>
        <div ref="trendChartRef" class="chart-canvas"></div>
      </div>
    </section>

    <!-- Bottom: Day-Type Comparison Bar Chart -->
    <section class="chart-section bottom-section">
      <div class="section-header">
        <h3 class="section-title">工作日 vs 节假日 速度对比</h3>
        <span class="section-badge">按小时 0-23</span>
      </div>
      <div class="chart-container">
        <div v-if="compLoading" class="chart-loading">
          <div class="spinner"></div>
          <span>加载对比数据...</span>
        </div>
        <div v-if="compError" class="chart-error">
          <span class="error-icon">⚠</span>
          <span>{{ compError }}</span>
        </div>
        <div ref="compChartRef" class="chart-canvas"></div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, watch, onMounted, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import { useDashboardStore } from '@/stores/dashboardStore'
import { dashboardChartTheme, dashboardColors } from '@/utils/chartTheme'
import { getCongestionTrend, getCongestionComparison } from '@/services/dashboardApi'

const store = useDashboardStore()

// --- DOM refs ---
const trendChartRef = ref(null)
const compChartRef = ref(null)

// --- State ---
const trendLoading = ref(false)
const trendError = ref(null)
const trendData = ref([])

const compLoading = ref(false)
const compError = ref(null)
const compData = ref([])

// --- Chart instances ---
let trendChart = null
let compChart = null

// --- Computed ---
const dateRange = computed(() => {
  const dates = store.availableDates
  if (dates.length === 0) return ''
  return `${dates[0]} ~ ${dates[dates.length - 1]}`
})

// --- API Fetch ---
async function fetchTrend() {
  trendLoading.value = true
  trendError.value = null
  try {
    const dates = store.availableDates
    const startDt = dates[0]
    const endDt = dates[dates.length - 1]
    const resp = await getCongestionTrend(startDt, endDt)
    trendData.value = resp.daily || []
  } catch (e) {
    console.error('[TrendPage] Trend fetch error:', e)
    trendError.value = '趋势数据加载失败'
    trendData.value = []
  } finally {
    trendLoading.value = false
  }
}

async function fetchComparison() {
  compLoading.value = true
  compError.value = null
  try {
    const resp = await getCongestionComparison()
    compData.value = resp.hourly || []
  } catch (e) {
    console.error('[TrendPage] Comparison fetch error:', e)
    compError.value = '对比数据加载失败'
    compData.value = []
  } finally {
    compLoading.value = false
  }
}

// --- Chart Init ---
function initTrendChart() {
  if (!trendChartRef.value) return
  trendChart = echarts.init(trendChartRef.value, null, { renderer: 'canvas' })
}

function initCompChart() {
  if (!compChartRef.value) return
  compChart = echarts.init(compChartRef.value, null, { renderer: 'canvas' })
}

// --- Chart Update ---
function updateTrendChart() {
  if (!trendChart) return
  const data = trendData.value
  if (data.length === 0) {
    trendChart.clear()
    return
  }

  const dates = data.map(d => d.dt)
  const speedValues = data.map(d => d.avgSpeedKmh ?? null)
  const ciValues = data.map(d => d.avgCongestionIndex ?? null)

  const selectedIdx = dates.indexOf(store.selectedDate)

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
    },
    legend: {
      data: ['平均速度', '拥堵指数'],
      textStyle: { color: 'rgba(255,255,255,0.7)', fontSize: 11 },
      top: 0,
      right: 20,
      itemWidth: 16,
      itemHeight: 10,
    },
    grid: {
      left: 12,
      right: 12,
      top: 42,
      bottom: 10,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: dates,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.12)' } },
      axisLabel: {
        color: dates.map((_, i) =>
          i === selectedIdx ? '#0cf' : 'rgba(255,255,255,0.6)'
        ),
        fontSize: 11,
        fontFamily: 'Courier New, monospace',
      },
      axisTick: { show: false },
      splitLine: { show: false },
    },
    yAxis: [
      {
        type: 'value',
        name: 'km/h',
        min: 22,
        max: 28,
        nameTextStyle: { color: 'rgba(255,255,255,0.5)', fontSize: 10 },
        axisLine: { show: false },
        axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 10 },
        splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
      },
      {
        type: 'value',
        name: '拥堵指数',
        nameTextStyle: { color: 'rgba(255,255,255,0.5)', fontSize: 10 },
        axisLine: { show: false },
        axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 10 },
        splitLine: { show: false },
      },
    ],
    series: [
      {
        name: '平均速度',
        type: 'line',
        yAxisIndex: 0,
        smooth: true,
        symbol: 'circle',
        symbolSize: speedValues.map((_, i) => (i === selectedIdx ? 12 : 6)),
        lineStyle: { color: '#0fc', width: 3, shadowColor: 'rgba(0,255,204,0.4)', shadowBlur: 8 },
        itemStyle: { color: '#0fc', borderWidth: 2 },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,255,204,0.15)' },
            { offset: 1, color: 'rgba(0,255,204,0.01)' },
          ]),
        },
        z: 2,
        data: speedValues,
        markPoint: selectedIdx >= 0 && speedValues[selectedIdx] != null ? {
          data: [
            {
              coord: [dates[selectedIdx], speedValues[selectedIdx]],
              symbol: 'pin',
              symbolSize: 40,
              itemStyle: { color: '#0fc', shadowColor: 'rgba(0,255,204,0.6)', shadowBlur: 12 },
              label: { show: true, color: '#fff', fontSize: 10, fontWeight: 600, formatter: () => speedValues[selectedIdx].toFixed(1) },
            },
          ],
          animation: true,
        } : undefined,
      },
      {
        name: '拥堵指数',
        type: 'line',
        yAxisIndex: 1,
        smooth: true,
        symbol: 'diamond',
        symbolSize: 7,
        lineStyle: { color: '#0cf', width: 2, type: 'dashed' },
        itemStyle: { color: '#0cf', borderWidth: 2 },
        z: 3,
        data: ciValues,
      },
    ],
    animation: true,
    animationDuration: 800,
    animationEasing: 'cubicOut',
  }

  trendChart.setOption(option, true)
}

function updateCompChart() {
  if (!compChart) return
  const data = compData.value
  if (data.length === 0) {
    compChart.clear()
    return
  }

  const hours = data.map(d => `${d.hour}:00`)
  const workdaySpeed = data.map(d => d.workdayAvgSpeed ?? 0)
  const holidaySpeed = data.map(d => d.holidayAvgSpeed ?? 0)
  const makeupSpeed = data.map(d => d.makeupWorkdayAvgSpeed ?? 0)

  // y 轴范围缩到数据区间，放大差异
  const allSpeeds = [...workdaySpeed, ...holidaySpeed, ...makeupSpeed].filter(v => v > 0)
  const minSpeed = Math.floor(Math.min(...allSpeeds) - 1)
  const maxSpeed = Math.ceil(Math.max(...allSpeeds) + 1)

  const option = {
    backgroundColor: 'transparent',
    textStyle: { color: '#fff' },
    tooltip: {
      trigger: 'axis',
      backgroundColor: 'rgba(10,14,26,0.92)',
      borderColor: 'rgba(0,204,255,0.3)',
      borderWidth: 1,
      textStyle: { color: '#fff', fontSize: 12 },
      axisPointer: { type: 'shadow', shadowStyle: { color: 'rgba(0,204,255,0.04)' } },
      formatter: (params) => {
        let html = `<b>${params[0].axisValue}</b><br/>`
        params.forEach(p => {
          html += `${p.marker} ${p.seriesName}: <b>${p.value.toFixed(1)} km/h</b><br/>`
        })
        return html
      },
    },
    legend: {
      data: ['工作日', '节假日', '补班日'],
      textStyle: { color: 'rgba(255,255,255,0.7)', fontSize: 11 },
      top: 0,
      right: 20,
      itemWidth: 16,
      itemHeight: 10,
    },
    grid: {
      left: 12,
      right: 12,
      top: 42,
      bottom: 10,
      containLabel: true,
    },
    xAxis: {
      type: 'category',
      data: hours,
      axisLine: { lineStyle: { color: 'rgba(255,255,255,0.12)' } },
      axisLabel: {
        color: 'rgba(255,255,255,0.55)',
        fontSize: 9,
        interval: 1,
        fontFamily: 'Courier New, monospace',
      },
      axisTick: { show: false },
      splitLine: { show: false },
    },
    yAxis: {
      type: 'value',
      name: 'km/h',
      min: 22,
      max: 28,
      nameTextStyle: { color: 'rgba(255,255,255,0.5)', fontSize: 10 },
      axisLine: { show: false },
      axisLabel: { color: 'rgba(255,255,255,0.5)', fontSize: 10 },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.05)' } },
    },
    series: [
      {
        name: '工作日',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: '#0cf', width: 2.5 },
        itemStyle: { color: '#0cf' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(0,204,255,0.15)' },
            { offset: 1, color: 'rgba(0,204,255,0.01)' },
          ]),
        },
        data: workdaySpeed,
      },
      {
        name: '节假日',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: '#f0c', width: 2.5 },
        itemStyle: { color: '#f0c' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,0,204,0.12)' },
            { offset: 1, color: 'rgba(255,0,204,0.01)' },
          ]),
        },
        data: holidaySpeed,
      },
      {
        name: '补班日',
        type: 'line',
        smooth: true,
        symbol: 'circle',
        symbolSize: 5,
        lineStyle: { color: '#fc0', width: 2.5 },
        itemStyle: { color: '#fc0' },
        areaStyle: {
          color: new echarts.graphic.LinearGradient(0, 0, 0, 1, [
            { offset: 0, color: 'rgba(255,204,0,0.12)' },
            { offset: 1, color: 'rgba(255,204,0,0.01)' },
          ]),
        },
        data: makeupSpeed,
      },
    ],
    animation: true,
    animationDuration: 800,
    animationEasing: 'cubicOut',
  }

  compChart.setOption(option, true)
}

// --- Resize ---
function handleResize() {
  trendChart && trendChart.resize()
  compChart && compChart.resize()
}

// --- Lifecycle ---
onMounted(async () => {
  await nextTick()
  initTrendChart()
  initCompChart()

  await Promise.all([fetchTrend(), fetchComparison()])

  updateTrendChart()
  updateCompChart()

  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  window.removeEventListener('resize', handleResize)
  if (trendChart) {
    trendChart.dispose()
    trendChart = null
  }
  if (compChart) {
    compChart.dispose()
    compChart = null
  }
})

// --- Reactivity: re-fetch trend when selectedDate changes ---
watch(
  () => store.selectedDate,
  async () => {
    await fetchTrend()
    updateTrendChart()
  },
)
</script>

<style scoped>
.trend-page {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100%;
  min-height: 0;
  background: #0a0e1a;
  gap: 1px;
}

/* --- Shared Section --- */
.chart-section {
  display: flex;
  flex-direction: column;
  flex: 1 1 50%;
  min-height: 0;
  overflow: hidden;
}

.section-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 20px;
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
  flex-shrink: 0;
}

.section-title {
  font-size: 14px;
  font-weight: 600;
  color: #0cf;
  letter-spacing: 1px;
  margin: 0;
}

.section-badge {
  font-size: 11px;
  font-family: 'Courier New', monospace;
  color: rgba(255, 255, 255, 0.4);
  padding: 3px 10px;
  border-radius: 10px;
  background: rgba(0, 204, 255, 0.06);
  border: 1px solid rgba(0, 204, 255, 0.1);
}

.chart-container {
  flex: 1;
  position: relative;
  min-height: 0;
  border: 1px solid rgba(0, 204, 255, 0.08);
  border-radius: 4px;
  margin: 6px 12px 10px;
  background: rgba(6, 10, 22, 0.6);
}

.chart-canvas {
  width: 100%;
  height: 100%;
}

/* --- Loading / Error States --- */
.chart-loading,
.chart-error {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 10px;
  z-index: 10;
  pointer-events: none;
  font-size: 13px;
}

.chart-loading {
  color: rgba(255, 255, 255, 0.4);
}

.chart-error {
  color: #ff6b6b;
}

.error-icon {
  font-size: 26px;
}

.spinner {
  width: 32px;
  height: 32px;
  border: 3px solid rgba(0, 204, 255, 0.12);
  border-top-color: #0cf;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>

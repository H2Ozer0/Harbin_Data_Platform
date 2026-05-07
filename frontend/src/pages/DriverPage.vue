<template>
  <div class="page">
    <section class="controls">
      <div class="control-group">
        <label>&#26085;&#26399;</label>
        <input v-model="dt" type="date" />
      </div>
      <div class="control-group">
        <button class="primary" @click="loadData" :disabled="loading">&#21047;&#26032;</button>
      </div>
      <div class="summary">
        <div class="summary-item">&#21496;&#26426;&#24635;&#25968; <strong>{{ totalDrivers }}</strong></div>
        <div class="summary-item">&#20241;&#24687;&#28857;&#25968; <strong>{{ totalRests }}</strong></div>
      </div>
    </section>

    <section class="content">
      <div class="panel">
        <div class="panel-header">
          <h3>&#29677;&#27425;&#20998;&#24067;</h3>
        </div>
        <div class="panel-body">
          <div ref="pieRef" class="chart"></div>
        </div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h3>&#24179;&#22343;&#27963;&#36291;&#26102;&#38271;</h3>
        </div>
        <div class="panel-body">
          <div ref="barRef" class="chart"></div>
        </div>
      </div>

      <div class="panel map-panel">
        <div class="panel-header">
          <h3>&#20241;&#24687;&#28909;&#21147;&#20998;&#24067;</h3>
        </div>
        <div class="panel-body map-body">
          <DashboardMap
            :heatmap-points="restHeatmapPoints"
            heatmap-weight-key="weight"
            :scatter-points="restHeatmapPoints.slice(0, 300)"
            :scatter-radius="5"
            :tooltip-formatter="formatTooltip"
          />
          <div v-if="loading" class="overlay">&#21152;&#36733;&#20013;...</div>
          <div v-if="error" class="overlay error">{{ error }}</div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onUnmounted, watch, nextTick } from 'vue'
import * as echarts from 'echarts'
import DashboardMap from '@/components/DashboardMap.vue'
import { fetchDriverBehavior, fetchDriverRestHeatmap } from '@/services/dashboardApi'

const dt = ref('2015-01-05')
const loading = ref(false)
const error = ref('')

const shiftDistribution = ref([])
const avgActiveMinutes = ref([])
const totalDrivers = ref(0)

const restLocations = ref([])
const totalRests = ref(0)

const pieRef = ref(null)
const barRef = ref(null)
let pieChart = null
let barChart = null

const restHeatmapPoints = computed(() =>
  restLocations.value.map(r => ({
    ...r,
    weight: r.restMinutes ?? 1,
  }))
)

const PATTERN_LABELS = {
  full_day: '全天班',
  dual_peak: '双高峰',
  morning_peak: '早高峰',
  evening_peak: '晚高峰',
  night_owl: '夜猫子',
}

function translatePattern(p) {
  return PATTERN_LABELS[p] ?? p
}

function formatTooltip(item) {
  if (!item) return null
  const lines = [
    `\u5750\u6807: ${item.lon?.toFixed(4)}, ${item.lat?.toFixed(4)}`,
    `\u4f11\u606f\u65f6\u957f: ${item.restMinutes ?? '-'} \u5206\u949f`,
    `\u73ed\u6b21: ${translatePattern(item.shiftPattern)}`,
  ]
  return lines.join('\n')
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const [behavior, rest] = await Promise.all([
      fetchDriverBehavior(dt.value),
      fetchDriverRestHeatmap(dt.value),
    ])
    if (behavior.shiftDistribution && behavior.shiftDistribution.length > 0) {
      shiftDistribution.value = behavior.shiftDistribution
      avgActiveMinutes.value = behavior.avgActiveMinutesByPattern || []
      totalDrivers.value = behavior.totalDrivers ?? 0
    } else {
      error.value = '\u6682\u65e0\u53f8\u673a\u884c\u4e3a\u6570\u636e'
    }

    if (rest.restLocations && rest.restLocations.length > 0) {
      restLocations.value = rest.restLocations
      totalRests.value = rest.totalRests ?? restLocations.value.length
    } else {
      if (!error.value) error.value = '\u6682\u65e0\u4f11\u606f\u5730\u6bb5\u6570\u636e'
    }

    await nextTick()
    renderCharts()
  } catch (err) {
    console.error('Failed to load driver data:', err)
    error.value = `\u52a0\u8f7d\u5931\u8d25: ${err.message || '\u672a\u77e5\u9519\u8bef'}`
  } finally {
    loading.value = false
  }
}

function renderCharts() {
  if (pieRef.value && !pieChart) {
    pieChart = echarts.init(pieRef.value)
  }
  if (barRef.value && !barChart) {
    barChart = echarts.init(barRef.value)
  }

  const pieData = shiftDistribution.value.map(item => ({
    name: translatePattern(item.pattern),
    value: item.driverCount,
  }))

  pieChart?.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item' },
    legend: {
      bottom: 0,
      textStyle: { color: '#8aa0b8' },
    },
    series: [
      {
        type: 'pie',
        radius: ['40%', '70%'],
        data: pieData,
        label: { color: '#fff' },
        itemStyle: {
          borderColor: '#0a0e1a',
          borderWidth: 2,
        },
      },
    ],
  })

  const barData = avgActiveMinutes.value.map(item => ({
    name: translatePattern(item.pattern),
    value: Number(item.avgMinutes || 0),
  }))

  barChart?.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
    grid: { left: 20, right: 20, top: 20, bottom: 30, containLabel: true },
    xAxis: {
      type: 'category',
      data: barData.map(d => d.name),
      axisLabel: { color: '#8aa0b8' },
    },
    yAxis: {
      type: 'value',
      axisLabel: { color: '#8aa0b8' },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
    },
    series: [
      {
        type: 'bar',
        data: barData.map(d => d.value),
        barWidth: 18,
        itemStyle: { color: '#4dd0ff' },
      },
    ],
  })
}

function handleResize() {
  pieChart?.resize()
  barChart?.resize()
}

watch(dt, () => loadData())

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  pieChart?.dispose()
  barChart?.dispose()
  window.removeEventListener('resize', handleResize)
})
</script>

<style scoped>
.page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  height: 100%;
  padding: 16px 20px;
}

.controls {
  display: flex;
  flex-wrap: wrap;
  gap: 16px;
  align-items: flex-end;
  background: rgba(10, 14, 26, 0.6);
  border: 1px solid rgba(0, 204, 255, 0.2);
  border-radius: 16px;
  padding: 16px;
}

.control-group {
  display: flex;
  flex-direction: column;
  gap: 6px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.7);
}

.control-group input[type="date"] {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  padding: 6px 10px;
  border-radius: 8px;
}

.primary {
  border: 1px solid rgba(0, 204, 255, 0.3);
  background: rgba(0, 204, 255, 0.1);
  color: #fff;
  padding: 6px 12px;
  border-radius: 14px;
  font-size: 12px;
  cursor: pointer;
}

.primary:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.summary {
  margin-left: auto;
  display: flex;
  gap: 16px;
  font-size: 12px;
  color: rgba(255, 255, 255, 0.6);
}

.summary-item strong {
  color: #0cf;
  margin-left: 6px;
}

.content {
  flex: 1;
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  grid-auto-rows: minmax(240px, auto);
  gap: 16px;
  min-height: 0;
}

.panel {
  display: flex;
  flex-direction: column;
  background: rgba(8, 12, 24, 0.9);
  border-radius: 18px;
  border: 1px solid rgba(0, 204, 255, 0.15);
  overflow: hidden;
  min-height: 0;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.08);
}

.panel-header h3 {
  font-size: 14px;
  color: #0cf;
}

.panel-body {
  flex: 1;
  padding: 12px 16px;
  display: flex;
  flex-direction: column;
  min-height: 0;
}

.chart {
  width: 100%;
  height: 220px;
}

.map-panel {
  grid-column: 1 / -1;
  min-height: 420px;
}

.map-panel .panel-body {
  padding: 0;
}

.map-body {
  position: relative;
  flex: 1;
  padding: 0;
  min-height: 0;
}

.map-body :deep(.map-canvas) {
  width: 100%;
  height: 100%;
  min-height: 320px;
}

.overlay {
  position: absolute;
  inset: 12px;
  background: rgba(0, 0, 0, 0.45);
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 16px;
  font-size: 14px;
  color: #fff;
}

.overlay.error {
  color: #ff7a7a;
}

@media (max-width: 1100px) {
  .content {
    grid-template-columns: 1fr;
  }
}
</style>

<template>
  <div class="page">
    <section class="controls">
      <div class="control-group">
        <label>&#26085;&#26399;</label>
        <input v-model="dt" type="date" />
      </div>
      <div class="control-group">
        <label>&#23567;&#26102;&#65306;{{ hour }}:00</label>
        <input v-model.number="hour" type="range" min="0" max="23" />
      </div>
      <div class="control-group">
        <label>&#31867;&#22411;</label>
        <div class="toggle">
          <button :class="{ active: eventType === 'pickup' }" @click="eventType = 'pickup'">&#19978;&#36710;&#28909;&#28857;</button>
          <button :class="{ active: eventType === 'dropoff' }" @click="eventType = 'dropoff'">&#19979;&#36710;&#28909;&#28857;</button>
        </div>
      </div>
      <div class="control-group">
        <button class="primary" @click="loadData" :disabled="loading">&#21047;&#26032;</button>
      </div>
      <div class="summary">
        <div class="summary-item">&#32593;&#26684;&#25968; <strong>{{ totalGrids }}</strong></div>
        <div class="summary-item">&#39640;&#28909;&#24230;&#32593;&#26684; <strong>{{ topGrids.length }}</strong></div>
      </div>
    </section>

    <section class="content">
      <div class="map-card">
        <DashboardMap
          :heatmap-points="heatmapPoints"
          heatmap-weight-key="weight"
          :scatter-points="heatmapPoints.slice(0, 200)"
          :scatter-radius="5"
          :tooltip-formatter="formatTooltip"
        />
        <div v-if="loading" class="overlay">&#21152;&#36733;&#20013;...</div>
        <div v-if="error" class="overlay error">{{ error }}</div>
      </div>

      <div class="panel">
        <div class="panel-header">
          <h3>&#28909;&#28857;&#25490;&#34892;&#65288;TOP 15&#65289;</h3>
        </div>
        <div class="panel-body">
          <div ref="chartRef" class="chart"></div>
          <div class="table-wrap">
            <table>
              <thead>
                <tr>
                  <th>&#25490;&#21517;</th>
                  <th>&#22352;&#26631;</th>
                  <th>&#20107;&#20214;&#25968;</th>
                  <th>&#28909;&#24230;</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(g, idx) in topGrids" :key="g.gridId || idx">
                  <td>#{{ g.rankInHour ?? idx + 1 }}</td>
                  <td>{{ formatCoord(g.lon, g.lat) }}</td>
                  <td>{{ g.eventCount ?? '-' }}</td>
                  <td>{{ formatNumber(g.hotspotScore) }}</td>
                </tr>
              </tbody>
            </table>
          </div>
        </div>
      </div>
    </section>
  </div>
</template>

<script setup>
import { ref, computed, onMounted, watch, onUnmounted, nextTick } from 'vue'
import * as echarts from 'echarts'
import DashboardMap from '@/components/DashboardMap.vue'
import { fetchHotspotMap } from '@/services/dashboardApi'

const dt = ref('2015-01-05')
const hour = ref(8)
const eventType = ref('pickup')
const loading = ref(false)
const error = ref('')
const grids = ref([])
const totalGrids = ref(0)

const chartRef = ref(null)
let chart = null

const heatmapPoints = computed(() =>
  grids.value.map(g => ({
    ...g,
    weight: g.eventCount ?? g.hotspotScore ?? 1,
  }))
)

const topGrids = computed(() => {
  const list = [...grids.value]
  return list
    .sort((a, b) => {
      const rankDiff = (a.rankInHour ?? 9999) - (b.rankInHour ?? 9999)
      if (rankDiff !== 0) return rankDiff
      return (b.eventCount ?? 0) - (a.eventCount ?? 0)
    })
    .slice(0, 15)
})

function formatCoord(lon, lat) {
  if (lon == null || lat == null) return '-'
  return `${lon.toFixed(4)}, ${lat.toFixed(4)}`
}

function formatNumber(val) {
  if (val == null || Number.isNaN(val)) return '-'
  return Number(val).toFixed(2)
}

function formatTooltip(item) {
  if (!item) return null
  const lines = [
    `\u5750\u6807: ${formatCoord(item.lon, item.lat)}`,
    `\u4e8b\u4ef6\u6570: ${item.eventCount ?? '-'}`,
    `\u70ed\u5ea6: ${formatNumber(item.hotspotScore)}`,
  ]
  return lines.join('\n')
}

async function loadData() {
  loading.value = true
  error.value = ''
  try {
    const data = await fetchHotspotMap(dt.value, hour.value, eventType.value)
    if (data.grids && data.grids.length > 0) {
      grids.value = data.grids
      totalGrids.value = data.totalGrids ?? grids.value.length
    } else {
      error.value = '\u6682\u65e0\u70ed\u70b9\u6570\u636e'
    }
    await nextTick()
    renderChart()
  } catch (err) {
    console.error('Failed to load hotspot data:', err)
    error.value = `\u52a0\u8f7d\u5931\u8d25: ${err.message || '\u672a\u77e5\u9519\u8bef'}`
  } finally {
    loading.value = false
  }
}

function renderChart() {
  if (!chartRef.value) return
  if (!chart) {
    chart = echarts.init(chartRef.value)
  }
  const data = topGrids.value.slice(0, 10).map(g => ({
    name: g.gridId || formatCoord(g.lon, g.lat),
    value: g.eventCount ?? 0,
  }))
  chart.setOption({
    backgroundColor: 'transparent',
    tooltip: { trigger: 'item' },
    grid: { left: 20, right: 20, top: 30, bottom: 20, containLabel: true },
    xAxis: {
      type: 'value',
      axisLabel: { color: '#8aa0b8' },
      splitLine: { lineStyle: { color: 'rgba(255,255,255,0.08)' } },
    },
    yAxis: {
      type: 'category',
      data: data.map(d => d.name).reverse(),
      axisLabel: { color: '#8aa0b8' },
    },
    series: [
      {
        type: 'bar',
        data: data.map(d => d.value).reverse(),
        barWidth: 10,
        itemStyle: {
          color: '#00d4ff',
        },
      },
    ],
  })
}

function handleResize() {
  if (chart) chart.resize()
}

watch([dt, hour, eventType], () => {
  loadData()
})

onMounted(() => {
  loadData()
  window.addEventListener('resize', handleResize)
})

onUnmounted(() => {
  if (chart) chart.dispose()
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

.control-group input[type="date"],
.control-group input[type="range"] {
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(255, 255, 255, 0.1);
  color: #fff;
  padding: 6px 10px;
  border-radius: 8px;
}

.toggle {
  display: flex;
  gap: 8px;
}

.toggle button,
.primary {
  border: 1px solid rgba(0, 204, 255, 0.3);
  background: rgba(0, 204, 255, 0.1);
  color: #fff;
  padding: 6px 12px;
  border-radius: 14px;
  font-size: 12px;
  cursor: pointer;
}

.toggle button.active {
  background: rgba(0, 204, 255, 0.3);
  color: #0cf;
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
  grid-template-columns: 2fr 1.2fr;
  grid-auto-rows: minmax(520px, 1fr);
  gap: 16px;
  min-height: 0;
}

.map-card {
  position: relative;
  background: rgba(8, 12, 24, 0.9);
  border-radius: 18px;
  padding: 12px;
  border: 1px solid rgba(0, 204, 255, 0.15);
  height: 100%;
  min-height: 520px;
  display: flex;
  flex-direction: column;
}

.map-card :deep(.map-canvas) {
  flex: 1;
  min-height: 0;
  width: 100%;
  height: 100%;
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

.panel {
  display: flex;
  flex-direction: column;
  background: rgba(8, 12, 24, 0.9);
  border-radius: 18px;
  border: 1px solid rgba(0, 204, 255, 0.15);
  overflow: hidden;
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
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 12px 16px 16px;
  overflow: hidden;
}

.chart {
  height: 180px;
  width: 100%;
}

.table-wrap {
  flex: 1;
  overflow: auto;
}

.table-wrap table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.table-wrap th,
.table-wrap td {
  padding: 8px 6px;
  border-bottom: 1px solid rgba(255, 255, 255, 0.05);
  color: rgba(255, 255, 255, 0.75);
  text-align: left;
}

.table-wrap th {
  color: rgba(255, 255, 255, 0.9);
  font-weight: 600;
}

@media (max-width: 1100px) {
  .content {
    grid-template-columns: 1fr;
  }
}
</style>

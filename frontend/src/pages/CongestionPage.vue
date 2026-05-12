<template>
  <div class="congestion-page">
    <div class="page-header">
      <h2 class="page-title">拥堵分析</h2>
      <div class="time-selector">
        <select v-model="store.selectedHour" class="hour-select">
          <option v-for="h in 24" :key="h" :value="h">
            {{ h }}:00
          </option>
        </select>
        <button class="btn" @click="refresh">刷新</button>
      </div>
    </div>

    <div class="congestion-content">
      <!-- 拥堵热力图 -->
      <div class="heatmap-section">
        <h3>拥堵热力图</h3>
        <div class="heatmap-container" id="heatmap"></div>
        <div class="heatmap-legend">
          <div class="legend-item">
            <span class="legend-color low"></span>
            <span>畅通</span>
          </div>
          <div class="legend-item">
            <span class="legend-color medium"></span>
            <span>缓行</span>
          </div>
          <div class="legend-item">
            <span class="legend-color high"></span>
            <span>拥堵</span>
          </div>
        </div>
      </div>

      <!-- Top 10 拥堵路段 -->
      <div class="top-section">
        <h3>Top 10 拥堵路段</h3>
        <div class="top-list">
          <div v-for="(item, idx) in topCongested" :key="idx" class="top-item">
            <span class="top-rank">{{ idx + 1 }}</span>
            <span class="top-name">{{ item.roadName }}</span>
            <span class="top-index" :class="item.level">
              {{ (item.congestionIndex * 100).toFixed(1) }}
            </span>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'
import { useDashboardStore } from '@/stores/dashboardStore'
import { dashboardApi } from '@/services/dashboardApi'

const store = useDashboardStore()

const topCongested = ref([])
const loading = ref(false)

let map = null
let heatmapLayer = null

onMounted(async () => {
  await loadCongestionData()
  initMap()
})

onUnmounted(() => {
  if (map) {
    map.remove()
    map = null
  }
})

async function loadCongestionData() {
  loading.value = true
  try {
    // 模拟数据 - 实际应从后端 API 获取
    topCongested.value = [
      { roadName: '裕虹路', congestionIndex: 0.92, level: 'high' },
      { roadName: '延平高架路', congestionIndex: 0.88, level: 'high' },
      { roadName: '学府路', congestionIndex: 0.85, level: 'high' },
      { roadName: '文昌街', congestionIndex: 0.82, level: 'high' },
      { roadName: '汉水路', congestionIndex: 0.78, level: 'medium' },
      { roadName: '黄河路', congestionIndex: 0.75, level: 'medium' },
      { roadName: '长江路', congestionIndex: 0.72, level: 'medium' },
      { roadName: '红旗大街', congestionIndex: 0.68, level: 'medium' },
      { roadName: '和兴路', congestionIndex: 0.65, level: 'low' },
      { roadName: '通达街', congestionIndex: 0.62, level: 'low' }
    ]
  } catch (error) {
    console.error('Failed to load congestion data:', error)
  } finally {
    loading.value = false
  }
}

function initMap() {
  // 使用 Mapbox GL 或类似地图库
  // 这里简化实现，实际需要加载 OSM 数据
  const mapDiv = document.getElementById('heatmap')
  if (mapDiv) {
    mapDiv.innerHTML = `
      <div style="width: 100%; height: 400px; background: #1a1a2e; border-radius: 8px; display: flex; align-items: center; justify-content: center;">
        <p style="color: rgba(255,255,255,0.5);">地图组件待实现</p>
        <p style="color: rgba(255,255,255,0.5); font-size: 12px;">需要加载 harbin.osm.pbf (1.9GB)</p>
        <p style="color: rgba(255,255,255,0.5); font-size: 12px;">建议使用 Mapbox GL 或 Deck.gl</p>
      </div>
    `
  }
}

function refresh() {
  loadCongestionData()
}
</script>

<style scoped>
.congestion-page {
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

.time-selector {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hour-select {
  padding: 8px 16px;
  background: rgba(0, 204, 255, 0.05);
  border: 1px solid rgba(0, 204, 255, 0.2);
  border-radius: 4px;
  color: #fff;
  font-size: 14px;
}

.congestion-content {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 16px;
  flex: 1;
  min-height: 0;
  padding-top: 16px;
}

.heatmap-section,
.top-section {
  background: rgba(0, 204, 255, 0.03);
  border-radius: 8px;
  border: 1px solid rgba(0, 204, 255, 0.1);
  padding: 16px;
}

h3 {
  font-size: 14px;
  font-weight: 600;
  color: #fff;
  margin: 0 0 12px;
}

.heatmap-container {
  width: 100%;
  height: 400px;
  background: #1a1a2e;
  border-radius: 8px;
}

.heatmap-legend {
  display: flex;
  gap: 24px;
  padding-top: 12px;
  font-size: 12px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  color: rgba(255, 255, 255, 0.7);
}

.legend-color {
  width: 24px;
  height: 12px;
  border-radius: 2px;
}

.legend-color.low { background: #00ff00; }
.legend-color.medium { background: #ffaa00; }
.legend-color.high { background: #ff0000; }

.top-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.top-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 8px 0;
}

.top-item:nth-child(-n+5):last-child {
  border-bottom: none;
}

.top-item {
  border-bottom: 1px solid rgba(0, 204, 255, 0.05);
}

.top-rank {
  width: 24px;
  height: 24px;
  background: rgba(0, 204, 255, 0.1);
  border-radius: 50%;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 600;
  color: #0cf;
}

.top-name {
  flex: 1;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.8);
}

.top-index {
  padding: 4px 12px;
  border-radius: 4px;
  font-weight: 600;
  min-width: 60px;
  text-align: center;
}

.top-index.low { background: rgba(0, 255, 0, 0.2); color: #0f0; }
.top-index.medium { background: rgba(255, 170, 0, 0.2); color: #fa0; }
.top-index.high { background: rgba(255, 0, 0, 0.2); color: #f33; }

.btn {
  padding: 8px 16px;
  background: rgba(0, 204, 255, 0.2);
  border: 1px solid rgba(0, 204, 255, 0.3);
  border-radius: 4px;
  color: #fff;
  cursor: pointer;
  font-size: 13px;
}

.btn:hover {
  background: rgba(0, 204, 255, 0.3);
}
</style>

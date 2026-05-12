<template>
  <div class="map-container" ref="mapContainer">
    <div class="map-legend" v-if="showLegend">
      <div class="legend-title">{{ legendTitle }}</div>
      <div class="legend-items">
        <div v-for="item in legendItems" :key="item.label" class="legend-item">
          <span class="legend-dot" :style="{ background: item.color }"></span>
          <span class="legend-label">{{ item.label }}</span>
        </div>
      </div>
    </div>
    <div class="map-controls" v-if="showControls">
      <button class="control-btn" @click="resetView">
        <span>⟲</span>
      </button>
      <button class="control-btn" @click="zoomIn">
        <span>+</span>
      </button>
      <button class="control-btn" @click="zoomOut">
        <span>−</span>
      </button>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted } from 'vue'

const props = defineProps({
  showLegend: { type: Boolean, default: true },
  showControls: { type: Boolean, default: true },
  legendTitle: { type: String, default: '图例' },
  legendItems: { type: Array, default: () => [] }
})

const mapContainer = ref(null)

let map = null

onMounted(() => {
  initMap()
})

onUnmounted(() => {
  if (map) {
    map.remove()
  }
})

function initMap() {
  // 简化版地图容器 - 实际项目可以使用 Mapbox GL
  const container = mapContainer.value
  if (!container) return

  // 初始化地图
  // 这里是简化实现，实际应使用 Mapbox GL 加载 OSM 数据
  console.log('Map initialized, OSM file: harbin.osm.pbf')
}

function resetView() {
  if (map && map.flyTo) {
    map.flyTo({
      center: [126.65, 45.75],
      zoom: 10
    })
  }
}

function zoomIn() {
  if (map && map.zoomIn) {
    map.zoomIn()
  }
}

function zoomOut() {
  if (map && map.zoomOut) {
    map.zoomOut()
  }
}

// 暴露方法供父组件调用
defineExpose({
  resetView,
  zoomIn,
  zoomOut,
  getMap: () => map
})
</script>

<style scoped>
.map-container {
  width: 100%;
  height: 100%;
  background: #0a0e1a;
  position: relative;
}

.map-legend {
  position: absolute;
  top: 16px;
  right: 16px;
  background: rgba(10, 14, 26, 0.9);
  border: 1px solid rgba(0, 204, 255, 0.2);
  border-radius: 8px;
  padding: 12px;
  z-index: 100;
  backdrop-filter: blur(10px);
}

.legend-title {
  font-size: 12px;
  font-weight: 600;
  color: #fff;
  margin-bottom: 8px;
}

.legend-items {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.legend-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 11px;
  color: rgba(255, 255, 255, 0.8);
}

.legend-dot {
  width: 12px;
  height: 12px;
  border-radius: 50%;
}

.legend-label {
  color: #fff;
}

.map-controls {
  position: absolute;
  bottom: 16px;
  right: 16px;
  display: flex;
  flex-direction: column;
  gap: 8px;
  z-index: 100;
}

.control-btn {
  width: 36px;
  height: 36px;
  background: rgba(10, 14, 26, 0.9);
  border: 1px solid rgba(0, 204, 255, 0.3);
  border-radius: 4px;
  color: #fff;
  font-size: 18px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  transition: all 0.2s;
}

.control-btn:hover {
  background: rgba(0, 204, 255, 0.2);
}
</style>

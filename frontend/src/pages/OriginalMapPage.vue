<template>
  <div class="app-layout">
    <header class="app-header">
      <h1 class="title">哈尔滨交通数据中台</h1>
      <div class="header-info">
        <span class="badge" :class="{ live: store.pointCount > 0 }">
          {{ store.pointCount > 0 ? '●' : '○' }}
          {{ store.pointCount.toLocaleString() }} 个轨迹点
        </span>
      </div>
    </header>

    <main class="map-view">
      <MapContainer />
      <TrajectoryLayer />
      <div v-if="store.loading && store.pointCount === 0 && !store.hasBoundaries" class="center-loading">
        <div class="spinner"></div>
        <p>正在初始化地图数据...</p>
      </div>
    </main>
  </div>
</template>

<script setup>
import MapContainer from '@/components/MapContainer.vue'
import TrajectoryLayer from '@/components/TrajectoryLayer.vue'
import { useMapStore } from '@/stores/mapStore'

const store = useMapStore()
</script>

<style scoped>
.app-layout {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
}

.app-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 24px;
  background: rgba(10, 14, 26, 0.95);
  border-bottom: 1px solid rgba(0, 204, 255, 0.2);
  z-index: 100;
  flex-shrink: 0;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #0cf;
  letter-spacing: 2px;
}

.badge {
  font-size: 12px;
  font-family: 'Courier New', monospace;
  color: #666;
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
}

.badge.live {
  color: #0f0;
  background: rgba(0, 255, 0, 0.08);
}

.map-view {
  flex: 1;
  position: relative;
  overflow: hidden;
  min-height: 0;
}

.center-loading {
  position: absolute;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  text-align: center;
  z-index: 50;
  pointer-events: none;
}

.spinner {
  width: 40px;
  height: 40px;
  margin: 0 auto 12px;
  border: 3px solid rgba(0, 204, 255, 0.15);
  border-top-color: #0cf;
  border-radius: 50%;
  animation: spin 0.8s linear infinite;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}

.center-loading p {
  color: rgba(255, 255, 255, 0.5);
  font-size: 14px;
}
</style>

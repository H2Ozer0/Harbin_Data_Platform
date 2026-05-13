<template>
  <div class="dash-layout">
    <header class="dash-header">
      <h1 class="title">哈尔滨交通数据中台</h1>
      <nav class="dash-nav">
        <RouterLink
          v-for="tab in tabs"
          :key="tab.path"
          :to="tab.path"
          class="nav-tab"
          active-class="active"
        >{{ tab.label }}</RouterLink>
      </nav>
      <div class="header-info">
        <button class="map-style-toggle" @click="store.toggleMapStyle()" :title="store.mapStyle === 'dark' ? '切换浅色底图' : '切换深色底图'">
          <span v-if="store.mapStyle === 'dark'" class="toggle-icon">☀</span>
          <span v-else class="toggle-icon">🌙</span>
        </button>
        <span class="badge live">● 数据大屏</span>
      </div>
    </header>

    <section v-if="showTimeline" class="timeline-bar">
      <label class="timeline-label">日期</label>
      <select v-model="store.selectedDate" class="date-select">
        <option v-for="d in store.availableDates" :key="d" :value="d">{{ d }}</option>
      </select>

      <label class="timeline-label">小时</label>
      <input type="range" v-model.number="store.selectedHour" min="0" max="23" class="hour-slider" />
      <span class="hour-value">{{ store.selectedHour }}:00</span>
    </section>

    <main class="dash-main">
      <RouterView v-slot="{ Component }">
        <KeepAlive>
          <component :is="Component" />
        </KeepAlive>
      </RouterView>
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { RouterLink, RouterView } from 'vue-router'
import { useRoute } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboardStore'

const store = useDashboardStore()
const route = useRoute()
const showTimeline = computed(() => route.path !== '/trend')

const tabs = [
  { path: '/congestion', label: '拥堵热力' },
  { path: '/trend', label: '趋势对比' },
  { path: '/hotspot', label: '热点地图' },
  { path: '/driver', label: '司机画像' },
  { path: '/catalog', label: '数据资产' },
  { path: '/lineage', label: '数据血缘' },
]
</script>

<style scoped>
.dash-layout {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
}

.dash-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 10px 24px;
  background: rgba(10, 14, 26, 0.95);
  border-bottom: 1px solid rgba(0, 204, 255, 0.2);
  z-index: 100;
  flex-shrink: 0;
  gap: 16px;
}

.title {
  font-size: 18px;
  font-weight: 600;
  color: #0cf;
  letter-spacing: 2px;
  white-space: nowrap;
}

.dash-nav {
  display: flex;
  gap: 4px;
  flex: 1;
  justify-content: center;
}

.nav-tab {
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  font-size: 13px;
  padding: 6px 14px;
  border-radius: 16px;
  border: 1px solid transparent;
  background: rgba(255, 255, 255, 0.04);
  transition: all 0.2s;
  white-space: nowrap;
}

.nav-tab:hover {
  color: rgba(255, 255, 255, 0.85);
  background: rgba(255, 255, 255, 0.08);
}

.nav-tab.active {
  color: #0cf;
  border-color: rgba(0, 204, 255, 0.35);
  background: rgba(0, 204, 255, 0.1);
}

.header-info {
  display: flex;
  align-items: center;
  gap: 8px;
  flex-shrink: 0;
}

.map-style-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 14px;
  border: 1px solid rgba(0, 204, 255, 0.2);
  background: rgba(0, 204, 255, 0.06);
  cursor: pointer;
  transition: all 0.2s;
  padding: 0;
}

.map-style-toggle:hover {
  background: rgba(0, 204, 255, 0.15);
  border-color: rgba(0, 204, 255, 0.4);
}

.toggle-icon {
  font-size: 14px;
  line-height: 1;
}

.badge {
  font-size: 12px;
  color: rgba(255, 255, 255, 0.5);
  padding: 4px 10px;
  border-radius: 12px;
  background: rgba(255, 255, 255, 0.05);
}

.badge.live {
  color: #0f0;
  background: rgba(0, 255, 0, 0.08);
}

.timeline-bar {
  display: flex;
  align-items: center;
  gap: 16px;
  padding: 8px 24px;
  background: rgba(10, 14, 26, 0.8);
  border-bottom: 1px solid rgba(0, 204, 255, 0.1);
  flex-shrink: 0;
}

.timeline-label {
  font-size: 13px;
  color: rgba(255, 255, 255, 0.5);
}

.date-select {
  padding: 4px 12px;
  font-size: 13px;
  font-family: 'Courier New', monospace;
  color: #fff;
  background: #1a1f2e;
  border: 1px solid rgba(0, 204, 255, 0.2);
  border-radius: 6px;
  outline: none;
  cursor: pointer;
}

.date-select:focus {
  border-color: #0cf;
}

.date-select option {
  background: #1a1f2e;
  color: #fff;
}

.hour-slider {
  flex: 1;
  max-width: 300px;
  accent-color: #0cf;
  cursor: pointer;
}

.hour-value {
  font-size: 13px;
  font-family: 'Courier New', monospace;
  color: #0cf;
  min-width: 40px;
}

.dash-main {
  flex: 1;
  position: relative;
  overflow-y: auto;
  min-height: 0;
}
</style>

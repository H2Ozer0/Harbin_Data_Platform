<template>
  <div class="dashboard-layout">
    <header class="top-nav">
      <div class="nav-brand">哈尔滨交通数据大屏</div>
      <nav class="nav-tabs">
        <router-link to="/congestion" class="nav-tab" active-class="active">拥堵</router-link>
        <router-link to="/trend" class="nav-tab" active-class="active">趋势</router-link>
        <router-link to="/hotspot" class="nav-tab" active-class="active">热点</router-link>
        <router-link to="/driver" class="nav-tab" active-class="active">司机</router-link>
        <router-link to="/catalog" class="nav-tab" active-class="active">数据资产</router-link>
        <router-link to="/lineage" class="nav-tab" active-class="active">血缘</router-link>
        <router-link to="/map" class="nav-tab" active-class="active">原地图</router-link>
      </nav>
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

    <main class="page-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute } from 'vue-router'
import { useDashboardStore } from '@/stores/dashboardStore'
const store = useDashboardStore()
const route = useRoute()
const showTimeline = computed(() => route.path !== '/trend')
</script>

<style scoped>
.dashboard-layout {
  display: flex;
  flex-direction: column;
  width: 100%;
  height: 100vh;
  background: #0a0e1a;
  color: #fff;
}

.top-nav {
  display: flex;
  align-items: center;
  gap: 24px;
  padding: 10px 24px;
  background: rgba(10, 14, 26, 0.95);
  border-bottom: 1px solid rgba(0, 204, 255, 0.2);
  flex-shrink: 0;
  z-index: 100;
}

.nav-brand {
  font-size: 18px;
  font-weight: 600;
  color: #0cf;
  letter-spacing: 2px;
  white-space: nowrap;
}

.nav-tabs {
  display: flex;
  gap: 4px;
}

.nav-tab {
  padding: 6px 16px;
  font-size: 14px;
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  border-radius: 6px;
  transition: all 0.2s;
}

.nav-tab:hover {
  color: #fff;
  background: rgba(0, 204, 255, 0.1);
}

.nav-tab.active {
  color: #0cf;
  background: rgba(0, 204, 255, 0.15);
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
  background: rgba(255, 255, 255, 0.08);
  border: 1px solid rgba(0, 204, 255, 0.2);
  border-radius: 6px;
  outline: none;
  cursor: pointer;
}

.date-select:focus {
  border-color: #0cf;
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

.page-content {
  flex: 1;
  overflow: hidden;
  min-height: 0;
}
</style>

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
        <span class="badge live">● 数据大屏</span>
      </div>
    </header>

    <main class="dash-main">
      <RouterView />
    </main>
  </div>
</template>

<script setup>
import { RouterLink, RouterView } from 'vue-router'

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
  flex-shrink: 0;
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

.dash-main {
  flex: 1;
  position: relative;
  overflow: hidden;
  min-height: 0;
}
</style>
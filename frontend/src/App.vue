<template>
  <div class="app-layout">
    <header class="app-header">
      <h1 class="title">哈尔滨交通数据中台</h1>
      <nav class="nav-menu">
        <router-link
          v-for="nav in navItems"
          :key="nav.path"
          :to="nav.path"
          class="nav-item"
          active-class="active"
        >
          {{ nav.label }}
        </router-link>
      </nav>
    </header>

    <main class="main-content">
      <router-view />
    </main>
  </div>
</template>

<script setup>
const navItems = [
  { label: '拥堵分析', path: '/congestion' },
  { label: '数据目录', path: '/catalog' },
  { label: '数据血缘', path: '/lineage' }
]
</script>

<style>
* {
  margin: 0;
  padding: 0;
  box-sizing: border-box;
}

html, body, #app {
  width: 100%;
  height: 100%;
  overflow: hidden;
  font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif;
  background: #0a0e1a;
  color: #fff;
}
</style>

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
  padding: 12px 24px;
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

.nav-menu {
  display: flex;
  gap: 8px;
}

.nav-item {
  padding: 8px 16px;
  color: rgba(255, 255, 255, 0.6);
  text-decoration: none;
  font-size: 14px;
  border-radius: 4px;
  transition: all 0.2s;
}

.nav-item:hover {
  background: rgba(0, 204, 255, 0.1);
  color: rgba(255, 255, 255, 0.9);
}

.nav-item.active {
  background: rgba(0, 204, 255, 0.2);
  color: #0cf;
}

.main-content {
  flex: 1;
  overflow-y: auto;
}
</style>

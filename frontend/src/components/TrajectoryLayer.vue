<template>
  <div class="trajectory-hud">
    <div v-if="store.loading" class="hud-loading">
      &#21152;&#36733;&#20013;...
    </div>
    <div v-if="store.error" class="hud-error">
      {{ store.error }}
    </div>
    <div v-if="store.pointCount > 0" class="hud-stats">
      <div class="stat-row">
        <span class="stat-label">&#21487;&#35265;&#28857;&#20301;</span>
        <span class="stat-value">{{ store.pointCount.toLocaleString() }}</span>
      </div>
      <div v-if="store.stats" class="stat-row">
        <span class="stat-label">&#36712;&#36857;&#25968;&#37327;</span>
        <span class="stat-value">{{ store.stats.tripCount }}</span>
      </div>
    </div>
  </div>
</template>

<script setup>
import { useMapStore } from '@/stores/mapStore'

const store = useMapStore()
</script>

<style scoped>
.trajectory-hud {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  pointer-events: none;
  z-index: 10;
}

.hud-loading,
.hud-error {
  position: absolute;
  top: 12px;
  left: 50%;
  transform: translateX(-50%);
  padding: 8px 20px;
  border-radius: 20px;
  font-size: 13px;
  z-index: 20;
}

.hud-loading {
  background: rgba(0, 0, 0, 0.75);
  color: #0cf;
}

.hud-error {
  background: rgba(200, 0, 0, 0.8);
  color: #fff;
  max-width: 400px;
  text-align: center;
}

.hud-stats {
  position: absolute;
  top: 12px;
  right: 16px;
  background: rgba(0, 0, 0, 0.7);
  border: 1px solid rgba(0, 204, 255, 0.15);
  border-radius: 8px;
  padding: 10px 16px;
  font-family: 'Courier New', monospace;
  z-index: 20;
}

.stat-row {
  display: flex;
  justify-content: space-between;
  gap: 20px;
  font-size: 12px;
  line-height: 1.8;
}

.stat-label {
  color: rgba(255, 255, 255, 0.5);
}

.stat-value {
  color: #0f0;
}
</style>

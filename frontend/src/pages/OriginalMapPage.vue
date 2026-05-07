<template>
  <div class="map-page">
    <MapContainer />
    <TrajectoryLayer />
  </div>
</template>

<script setup>
import MapContainer from '@/components/MapContainer.vue'
import TrajectoryLayer from '@/components/TrajectoryLayer.vue'
</script>

<style scoped>
.map-page {
  width: 100%;
  height: 100vh;
  position: relative;
  overflow: hidden;
}
</style>
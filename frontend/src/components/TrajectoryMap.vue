<template>
  <div ref="mapRef" class="trajectory-map"></div>
</template>

<script setup>
import { ref, watch, onMounted, onUnmounted, nextTick } from 'vue'
import { Map } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { PathLayer, ScatterplotLayer } from '@deck.gl/layers'
import { HeatmapLayer } from '@deck.gl/aggregation-layers'
import { MapboxOverlay } from '@deck.gl/mapbox'

const props = defineProps({
  points: { type: Array, default: () => [] },
  center: { type: Array, default: () => [126.63, 45.75] },
  zoom: { type: Number, default: 11 },
})

const mapRef = ref(null)
let map = null
let overlay = null

function buildLayers() {
  const layers = []
  if (!props.points.length) return layers

  // Heatmap layer
  layers.push(
    new HeatmapLayer({
      id: 'traj-heatmap',
      data: props.points,
      getPosition: d => [d.lon, d.lat],
      getWeight: () => 1,
      radiusPixels: 20,
      opacity: 0.6,
      threshold: 0.05,
    })
  )

  // Scatter points (limit for performance)
  const displayPoints = props.points.slice(0, 10000)
  layers.push(
    new ScatterplotLayer({
      id: 'traj-points',
      data: displayPoints,
      getPosition: d => [d.lon, d.lat],
      getFillColor: d => {
        const sec = (d.timestamp % 60000) / 1000
        if (sec < 20) return [255, 80, 80, 200]
        if (sec < 40) return [255, 200, 50, 200]
        return [80, 200, 255, 200]
      },
      getRadius: 3,
      radiusMinPixels: 1,
      radiusMaxPixels: 5,
      opacity: 0.8,
    })
  )

  // Path lines - group by devid
  const groups = {}
  for (const pt of props.points) {
    if (!groups[pt.devid]) groups[pt.devid] = []
    groups[pt.devid].push(pt)
  }

  const paths = Object.entries(groups)
    .filter(([, pts]) => pts.length >= 2)
    .map(([, pts]) => {
      pts.sort((a, b) => a.timestamp - b.timestamp)
      return { path: pts.map(p => [p.lon, p.lat]) }
    })
    .slice(0, 300)

  if (paths.length > 0) {
    layers.push(
      new PathLayer({
        id: 'traj-paths',
        data: paths,
        getPath: d => d.path,
        getColor: [255, 140, 0, 160],
        getWidth: 2,
        widthMinPixels: 1,
        jointRounded: true,
        capRounded: true,
      })
    )
  }

  return layers
}

function initMap() {
  if (!mapRef.value) return

  map = new Map({
    container: mapRef.value,
    style: {
      version: 8,
      sources: {
        'carto-tiles': {
          type: 'raster',
          tiles: ['https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png'],
          tileSize: 256,
        },
      },
      glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf',
      layers: [
        { id: 'carto', type: 'raster', source: 'carto-tiles', minzoom: 0, maxzoom: 19 },
      ],
    },
    center: props.center,
    zoom: props.zoom,
    attributionControl: false,
  })

  overlay = new MapboxOverlay({
    interleaved: true,
    layers: buildLayers(),
    getTooltip: ({ object }) => {
      if (object && object.lon) return `${object.lon.toFixed(4)}, ${object.lat.toFixed(4)}`
      return null
    },
  })

  map.on('load', () => {
    map.addControl(overlay)
  })
}

function refreshLayers() {
  if (overlay) {
    overlay.setProps({ layers: buildLayers() })
  }
}

function fitBoundsToData() {
  if (!map || !props.points.length) return
  const lons = props.points.map(p => p.lon).filter(Boolean)
  const lats = props.points.map(p => p.lat).filter(Boolean)
  if (!lons.length) return
  map.fitBounds(
    [[Math.min(...lons), Math.min(...lats)], [Math.max(...lons), Math.max(...lats)]],
    { padding: 50, maxZoom: 14, duration: 1000 }
  )
}

watch(() => props.points, (newPts) => {
  refreshLayers()
  if (newPts.length > 0) {
    fitBoundsToData()
  }
}, { deep: true })

onMounted(async () => {
  await nextTick()
  initMap()
})

onUnmounted(() => {
  if (overlay && map) map.removeControl(overlay)
  if (map) map.remove()
  map = null
  overlay = null
})
</script>

<style scoped>
.trajectory-map {
  width: 100%;
  height: 450px;
  border-radius: 8px;
  overflow: hidden;
  background: #0a0e1a;
  border: 1px solid rgba(0, 204, 255, 0.15);
}
</style>

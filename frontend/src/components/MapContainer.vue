<template>
  <div id="map-canvas" ref="containerRef" class="map-canvas"></div>
</template>

<script setup>
import { ref, onMounted, nextTick, onUnmounted, watch } from 'vue'
import { useMapStore } from '@/stores/mapStore'
import { Map } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { ScatterplotLayer, LineLayer, PathLayer } from '@deck.gl/layers'
import { HeatmapLayer } from '@deck.gl/aggregation-layers'
import { MapboxOverlay } from '@deck.gl/mapbox'

const emit = defineEmits(['viewport-change'])

const store = useMapStore()
const containerRef = ref(null)
let map = null
let overlay = null

function buildDeckLayers() {
  const layers = []

  if (store.boundaries.length > 0) {
    const boundaryData = store.boundaries
      .filter(b => b.geojson)
      .map(b => {
        try {
          const geo = JSON.parse(b.geojson)
          if (geo && geo.coordinates) {
            return {
              path: geo.coordinates,
              color: b.priority >= 5 ? [100, 200, 255, 200]
                    : b.priority >= 3 ? [80, 160, 200, 160]
                    : b.priority >= 1.5 ? [60, 120, 160, 120]
                    : [40, 80, 120, 80],
              width: b.priority >= 5 ? 4
                    : b.priority >= 3 ? 3
                    : b.priority >= 1.5 ? 2
                    : 1,
            }
          }
        } catch (e) { /* skip */ }
        return null
      })
      .filter(Boolean)

    if (boundaryData.length > 0) {
      layers.push(
        new LineLayer({
          id: 'boundary-lines',
          data: boundaryData,
          getSourcePosition: f => f.path[0],
          getTargetPosition: f => f.path[f.path.length - 1],
          getColor: f => f.color,
          getWidth: f => f.width,
          opacity: 0.6,
          lineWidthMinPixels: 1,
        })
      )
    }
  }

  if (store.trajectoryPoints.length > 0) {
    layers.push(
      new HeatmapLayer({
        id: 'taxi-heatmap',
        data: store.trajectoryPoints,
        getPosition: d => [d.lon, d.lat],
        getWeight: () => 1,
        radiusPixels: 20,
        opacity: 0.7,
        threshold: 0.05,
      })
    )

    const sliceSize = Math.min(store.trajectoryPoints.length, 10000)
    layers.push(
      new ScatterplotLayer({
        id: 'taxi-points',
        data: store.trajectoryPoints.slice(0, sliceSize),
        getPosition: d => [d.lon, d.lat],
        getFillColor: d => {
          const sec = (d.timestamp % 60000) / 1000
          if (sec < 20) return [255, 80, 80]
          if (sec < 40) return [255, 200, 50]
          return [80, 200, 255]
        },
        getRadius: 2,
        radiusMinPixels: 1,
        radiusMaxPixels: 4,
        opacity: 0.75,
      })
    )

    const groups = {}
    for (const pt of store.trajectoryPoints) {
      if (!groups[pt.devid]) groups[pt.devid] = []
      groups[pt.devid].push(pt)
    }

    const paths = Object.entries(groups)
      .filter(([, pts]) => pts.length >= 3)
      .map(([, pts]) => {
        pts.sort((a, b) => a.timestamp - b.timestamp)
        return { path: pts.map(p => [p.lon, p.lat]) }
      })
      .slice(0, 300)

    if (paths.length > 0) {
      layers.push(
        new PathLayer({
          id: 'taxi-paths',
          data: paths,
          getPath: d => d.path,
          getColor: [255, 140, 0, 140],
          getWidth: 2,
          widthMinPixels: 1,
          jointRounded: true,
          capRounded: true,
        })
      )
    }
  }

  return layers
}

function initMap() {
  map = new Map({
    container: containerRef.value,
    style: {
      version: 8,
      name: 'Dark',
      sources: {
        'osm-tiles': {
          type: 'raster',
          tiles: ['https://tile.openstreetmap.org/{z}/{x}/{y}.png'],
          tileSize: 256,
          attribution: '? OpenStreetMap contributors',
        },
      },
      glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf',
      layers: [
        {
          id: 'background',
          type: 'background',
          paint: { 'background-color': '#0a0e1a' },
        },
        {
          id: 'osm-raster',
          type: 'raster',
          source: 'osm-tiles',
          minzoom: 0,
          maxzoom: 19,
        },
      ],
    },
    center: [store.HARBIN_CENTER[0], store.HARBIN_CENTER[1]],
    zoom: 10,
    pitch: 0,
    bearing: 0,
    interactive: true,
    attributionControl: false,
  })

  map.on('move', () => {
    const c = map.getCenter()
    emit('viewport-change', {
      longitude: c.lng,
      latitude: c.lat,
      zoom: map.getZoom(),
      pitch: map.getPitch(),
      bearing: map.getBearing(),
      width: map.getContainer().clientWidth,
      height: map.getContainer().clientHeight,
    })
  })

  overlay = new MapboxOverlay({
    interleaved: true,
    layers: buildDeckLayers(),
    getTooltip: ({ object }) => {
      if (object && object.lon) return `${object.lon.toFixed(4)}, ${object.lat.toFixed(4)}`
      return null
    },
  })

  map.on('load', () => {
    console.log('[MapContainer] Map loaded, adding Deck overlay')
    map.addControl(overlay)
  })
}

function refresh() {
  if (overlay) {
    overlay.setProps({ layers: buildDeckLayers() })
  }
}

watch([() => store.boundaries, () => store.trajectoryPoints], () => {
  refresh()
}, { deep: true })

onMounted(async () => {
  await nextTick()
  store.loadBoundaries()
  store.loadTrajectorySlice(
    store.DATA_START,
    store.DATA_END,
    { minLon: 126.0, maxLon: 127.2, minLat: 45.4, maxLat: 46.2 }
  )
  initMap()
})

onUnmounted(() => {
  if (overlay && map) map.removeControl(overlay)
  if (map) map.remove()
})

defineExpose({ refresh })
</script>

<style scoped>
.map-canvas {
  width: 100%;
  height: 100%;
  background: #0a0e1a;
}
</style>

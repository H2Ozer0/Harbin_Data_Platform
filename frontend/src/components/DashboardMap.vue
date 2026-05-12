<template>
  <div ref="containerRef" class="map-canvas"></div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, watch, nextTick, computed } from 'vue'
import { Map } from 'maplibre-gl'
import 'maplibre-gl/dist/maplibre-gl.css'
import { HeatmapLayer } from '@deck.gl/aggregation-layers'
import { ScatterplotLayer } from '@deck.gl/layers'
import { MapboxOverlay } from '@deck.gl/mapbox'

const props = defineProps({
  heatmapPoints: {
    type: Array,
    default: () => [],
  },
  heatmapWeightKey: {
    type: String,
    default: 'weight',
  },
  scatterPoints: {
    type: Array,
    default: () => [],
  },
  scatterColor: {
    type: Array,
    default: () => [0, 204, 255, 180],
  },
  scatterRadius: {
    type: Number,
    default: 4,
  },
  center: {
    type: Array,
    default: () => [126.63, 45.75],
  },
  zoom: {
    type: Number,
    default: 10,
  },
  tooltipFormatter: {
    type: Function,
    default: null,
  },
  heatmapRadiusPixels: {
    type: Number,
    default: 30,
  },
  heatmapIntensity: {
    type: Number,
    default: 2,
  },
  heatmapThreshold: {
    type: Number,
    default: 0.01,
  },
})

const containerRef = ref(null)
let map = null
let overlay = null
let resizeObserver = null

const weightRange = computed(() => {
  const points = props.heatmapPoints
  if (!points.length) return [1, 1]
  let min = Infinity
  let max = -Infinity
  for (const p of points) {
    const v = p[props.heatmapWeightKey]
    if (Number.isFinite(v)) {
      if (v < min) min = v
      if (v > max) max = v
    }
  }
  if (!Number.isFinite(min)) return [1, 1]
  return [min, max > min ? max : min + 1]
})

function buildLayers() {
  const layers = []

  if (props.heatmapPoints.length > 0) {
    const [wMin, wMax] = weightRange.value
    const wSpan = wMax - wMin

    layers.push(
      new HeatmapLayer({
        id: 'heatmap-layer',
        data: props.heatmapPoints,
        getPosition: d => [d.lon, d.lat],
        getWeight: d => {
          const v = d[props.heatmapWeightKey]
          const raw = Number.isFinite(v) ? v : 0
          return wSpan > 0 ? (raw - wMin) / wSpan : 0.5
        },
        radiusPixels: props.heatmapRadiusPixels,
        intensity: props.heatmapIntensity,
        opacity: 0.75,
        threshold: props.heatmapThreshold,
        colorRange: [
          [0, 0, 255, 0],
          [0, 128, 255, 80],
          [0, 255, 255, 140],
          [0, 255, 128, 200],
          [255, 255, 0, 230],
          [255, 128, 0, 250],
          [255, 0, 0, 255],
        ],
      })
    )
  }

  if (props.scatterPoints.length > 0) {
    layers.push(
      new ScatterplotLayer({
        id: 'scatter-layer',
        data: props.scatterPoints,
        getPosition: d => [d.lon, d.lat],
        getFillColor: props.scatterColor,
        getRadius: props.scatterRadius,
        opacity: 0.8,
        radiusMinPixels: 1,
        radiusMaxPixels: 8,
      })
    )
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
          tiles: [
            'https://webrd01.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
            'https://webrd02.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
            'https://webrd03.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
            'https://webrd04.is.autonavi.com/appmaptile?lang=zh_cn&size=1&scale=1&style=8&x={x}&y={y}&z={z}',
          ],
          tileSize: 256,
          attribution: '? Amap',
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
    center: props.center,
    zoom: props.zoom,
    pitch: 0,
    bearing: 0,
    interactive: true,
    attributionControl: false,
  })

  overlay = new MapboxOverlay({
    interleaved: true,
    layers: buildLayers(),
    getTooltip: ({ object }) => {
      if (!object) return null
      if (props.tooltipFormatter) return props.tooltipFormatter(object)
      return object.lon ? `${object.lon.toFixed(4)}, ${object.lat.toFixed(4)}` : null
    },
  })

  map.on('load', () => {
    map.addControl(overlay)
    map.resize()
    requestAnimationFrame(() => map && map.resize())
    setTimeout(() => map && map.resize(), 300)
  })
}

function refreshLayers() {
  if (overlay) {
    overlay.setProps({ layers: buildLayers() })
  }
}

watch(
  () => [props.heatmapPoints, props.scatterPoints, props.heatmapWeightKey],
  () => {
    refreshLayers()
    if (map) {
      requestAnimationFrame(() => map && map.resize())
    }
  },
  { deep: true }
)

onMounted(async () => {
  await nextTick()
  initMap()
  resizeObserver = new ResizeObserver(() => {
    if (map) {
      map.resize()
    }
  })
  if (containerRef.value) {
    resizeObserver.observe(containerRef.value)
  }
})

onUnmounted(() => {
  if (resizeObserver && containerRef.value) {
    resizeObserver.unobserve(containerRef.value)
  }
  resizeObserver = null
  if (overlay && map) map.removeControl(overlay)
  if (map) map.remove()
})
</script>

<style scoped>
.map-canvas {
  width: 100%;
  height: 100%;
  background: #0a0e1a;
  border-radius: 16px;
  overflow: hidden;
}
</style>

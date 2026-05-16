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
import { useDashboardStore } from '@/stores/dashboardStore'

const store = useDashboardStore()

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
  /** 传入 [min,max] 时按固定区间映射；不传/undefined 时用当前点集 min–max */
  heatmapWeightDomain: {
    default: undefined,
    validator: (v) =>
      v === undefined ||
      v === null ||
      (Array.isArray(v) && v.length === 2 && Number.isFinite(Number(v[0])) && Number.isFinite(Number(v[1]))),
  },
  /**
   * 传入 [min,max] 时启用 HeatmapLayer 固定色域（deck.gl colorDomain），与 aggregation: MEAN 配合，
   * 用于「固定标尺」可比色带；不传则走默认（视口内自动拉伸，渐变柔和）。
   */
  heatmapColorDomain: {
    default: undefined,
    validator: (v) =>
      v === undefined ||
      v === null ||
      (Array.isArray(v) && v.length === 2 && Number.isFinite(Number(v[0])) && Number.isFinite(Number(v[1]))),
  },
  /** 为 true 时 getWeight 直接用 heatmapWeightKey 原始值（如事件数），不在层内做 min–max（热点「相对本屏」） */
  heatmapUseRawWeight: {
    type: Boolean,
    default: false,
  },
  /** 随口径/数据变化，用于 deck 图层 id 与 updateTriggers，避免聚合层不刷新 */
  heatmapRenderKey: {
    type: String,
    default: '',
  },
})

const containerRef = ref(null)
let map = null
let overlay = null
let resizeObserver = null

const weightRange = computed(() => {
  const domain = props.heatmapWeightDomain
  if (domain != null && Array.isArray(domain) && domain.length === 2) {
    const d0 = Number(domain[0])
    const d1 = Number(domain[1])
    if (Number.isFinite(d0) && Number.isFinite(d1) && d1 > d0) {
      return [d0, d1]
    }
  }

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

const sharedColorRange = [
  [0, 0, 255, 0],
  [0, 128, 255, 90],
  [0, 255, 255, 150],
  [0, 255, 128, 210],
  [255, 255, 0, 235],
  [255, 128, 0, 250],
  [255, 0, 0, 255],
]

function buildLayers() {
  const layers = []

  if (props.heatmapPoints.length > 0) {
    const rk = props.heatmapRenderKey || 'default'
    const cd = props.heatmapColorDomain
    const hasColorDomain =
      cd != null &&
      Array.isArray(cd) &&
      cd.length === 2 &&
      Number.isFinite(Number(cd[0])) &&
      Number.isFinite(Number(cd[1])) &&
      Number(cd[1]) > Number(cd[0])
    const c0 = hasColorDomain ? Number(cd[0]) : 0
    const c1 = hasColorDomain ? Number(cd[1]) : 1

    if (hasColorDomain) {
      layers.push(
        new HeatmapLayer({
          id: `heatmap-cd-${c0}-${c1}-${rk}-${props.heatmapPoints.length}`,
          data: props.heatmapPoints,
          getPosition: d => [d.lon, d.lat],
          getWeight: d => {
            const v = d[props.heatmapWeightKey]
            const raw = Number.isFinite(v) ? v : 0
            return Math.min(c1, Math.max(c0, raw))
          },
          aggregation: 'MEAN',
          colorDomain: [c0, c1],
          updateTriggers: {
            getWeight: [c0, c1, rk, props.heatmapWeightKey],
          },
          radiusPixels: props.heatmapRadiusPixels,
          intensity: props.heatmapIntensity,
          opacity: 0.78,
          threshold: props.heatmapThreshold,
          colorRange: sharedColorRange,
        }),
      )
    } else if (props.heatmapUseRawWeight) {
      layers.push(
        new HeatmapLayer({
          id: `heatmap-raw-${rk}-${props.heatmapPoints.length}`,
          data: props.heatmapPoints,
          getPosition: d => [d.lon, d.lat],
          getWeight: d => {
            const v = d[props.heatmapWeightKey]
            return Number.isFinite(v) ? Math.max(0, v) : 0
          },
          updateTriggers: {
            getWeight: [rk, props.heatmapWeightKey, props.heatmapPoints.length],
          },
          radiusPixels: props.heatmapRadiusPixels,
          intensity: props.heatmapIntensity,
          opacity: 0.75,
          threshold: props.heatmapThreshold,
          colorRange: sharedColorRange,
        }),
      )
    } else {
      const [wMin, wMax] = weightRange.value
      const wSpan = wMax - wMin

      const getWeightFromRange = d => {
        const v = d[props.heatmapWeightKey]
        const raw = Number.isFinite(v) ? v : 0
        if (wSpan <= 0) return 0.5
        const t = (raw - wMin) / wSpan
        return Math.min(1, Math.max(0, t))
      }

      layers.push(
        new HeatmapLayer({
          id: `heatmap-mm-${rk}-${props.heatmapPoints.length}`,
          data: props.heatmapPoints,
          getPosition: d => [d.lon, d.lat],
          getWeight: getWeightFromRange,
          updateTriggers: {
            getWeight: [wMin, wMax, props.heatmapWeightKey, rk],
          },
          radiusPixels: props.heatmapRadiusPixels,
          intensity: props.heatmapIntensity,
          opacity: 0.75,
          threshold: props.heatmapThreshold,
          colorRange: sharedColorRange,
        }),
      )
    }
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

function getTileUrl() {
  return store.mapStyle === 'dark'
    ? 'https://basemaps.cartocdn.com/dark_all/{z}/{x}/{y}@2x.png'
    : 'https://basemaps.cartocdn.com/light_all/{z}/{x}/{y}@2x.png'
}

function buildStyle() {
  return {
    version: 8,
    sources: {
      'carto-tiles': {
        type: 'raster',
        tiles: [getTileUrl()],
        tileSize: 256,
      },
    },
    glyphs: 'https://demotiles.maplibre.org/font/{fontstack}/{range}.pbf',
    layers: [
      { id: 'carto', type: 'raster', source: 'carto-tiles', minzoom: 0, maxzoom: 19 },
    ],
  }
}

function initMap() {
  map = new Map({
    container: containerRef.value,
    style: buildStyle(),
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
  () => [
    props.heatmapPoints,
    props.scatterPoints,
    props.heatmapWeightKey,
    props.heatmapWeightDomain,
    props.heatmapColorDomain,
    props.heatmapUseRawWeight,
    props.heatmapRenderKey,
  ],
  () => {
    refreshLayers()
    if (map) {
      requestAnimationFrame(() => map && map.resize())
    }
  },
  { deep: true }
)

watch(() => store.mapStyle, () => {
  if (map) map.setStyle(buildStyle())
})

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

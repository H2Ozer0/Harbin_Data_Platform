import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { fetchMapBoundaries, fetchTrajectorySlice, fetchTripStats } from '@/services/api'

const HARBIN_CENTER = [126.63, 45.75]
const DATA_START = '2015-01-03T00:00:00'
const DATA_END = '2015-01-07T23:59:59'

export const useMapStore = defineStore('map', () => {
  const boundaries = ref([])
  const trajectoryPoints = ref([])
  const loading = ref(false)
  const error = ref(null)
  const stats = ref(null)
  const currentPage = ref(0)

  const hasBoundaries = computed(() => boundaries.value.length > 0)
  const pointCount = computed(() => trajectoryPoints.value.length)

  async function loadBoundaries(params = {}) {
    loading.value = true
    error.value = null
    try {
      const data = await fetchMapBoundaries({
        minLon: 126.0,
        minLat: 45.4,
        maxLon: 127.2,
        maxLat: 46.2,
        page: 0,
        size: 500,
        ...params,
      })
      boundaries.value = data.content || []
    } catch (e) {
      error.value = e.message
      console.error('Failed to load boundaries:', e)
    } finally {
      loading.value = false
    }
  }

  async function loadTrajectorySlice(startTime, endTime, bbox = {}) {
    loading.value = true
    error.value = null
    try {
      const [points, st] = await Promise.all([
        fetchTrajectorySlice(startTime, endTime, bbox),
        fetchTripStats(startTime, endTime).catch(() => null),
      ])
      trajectoryPoints.value = points || []
      stats.value = st
    } catch (e) {
      error.value = e.message
      console.error('Failed to load trajectory:', e)
    } finally {
      loading.value = false
    }
  }

  return {
    boundaries,
    trajectoryPoints,
    loading,
    error,
    stats,
    currentPage,
    hasBoundaries,
    pointCount,
    HARBIN_CENTER,
    DATA_START,
    DATA_END,
    loadBoundaries,
    loadTrajectorySlice,
  }
})

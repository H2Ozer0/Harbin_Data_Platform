import axios from 'axios'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export async function fetchMapBoundaries(params = {}) {
  const {
    minLon = 126.0,
    minLat = 45.4,
    maxLon = 127.2,
    maxLat = 46.2,
    page = 0,
    size = 200,
  } = params
  const res = await api.get('/map/boundary', { params: { minLon, minLat, maxLon, maxLat, page, size } })
  return res.data
}

export async function fetchFullMap() {
  const res = await api.get('/map/full')
  return res.data
}

export async function fetchRoadsByType(highwayType, page = 0, size = 100) {
  const res = await api.get('/map/roads', { params: { highwayType, page, size } })
  return res.data
}

export async function fetchTrips(startTime, endTime, page = 0, size = 50) {
  const res = await api.get('/taxi/trips', {
    params: { startTime, endTime, page, size },
  })
  return res.data
}

export async function fetchTrajectorySlice(startTime, endTime, bbox = {}) {
  const params = { startTime, endTime }
  if (bbox.minLon != null) params.minLon = bbox.minLon
  if (bbox.maxLon != null) params.maxLon = bbox.maxLon
  if (bbox.minLat != null) params.minLat = bbox.minLat
  if (bbox.maxLat != null) params.maxLat = bbox.maxLat
  const res = await api.get('/taxi/trajectory', { params })
  return res.data
}

export async function fetchTripStats(startTime, endTime) {
  const res = await api.get('/taxi/stats', { params: { startTime, endTime } })
  return res.data
}

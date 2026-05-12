import axios from 'axios'

const api = axios.create({
  baseURL: '/api/dashboard',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

export async function fetchHotspotMap(dt, hour, eventType) {
  const res = await api.get('/hotspot/map', {
    params: { dt, hour, event_type: eventType },
  })
  return res.data
}

export async function fetchDriverBehavior(dt) {
  const res = await api.get('/driver/behavior', { params: { dt } })
  return res.data
}

export async function fetchDriverRestHeatmap(dt, limit = 1000) {
  const res = await api.get('/driver/rest-heatmap', { params: { dt, limit } })
  return res.data
}
import axios from 'axios'

const api = axios.create({
  baseURL: '/api/dashboard',
  timeout: 30000,
  headers: {
    'Content-Type': 'application/json',
  },
})

// P1: Congestion APIs
export async function getCongestionHeatmap(dt, hour, dayType) {
  const resp = await api.get('/congestion/heatmap', { params: { dt, hour, day_type: dayType || 'workday' } });
  return resp.data;
}
export async function getKPI(dt) {
  const resp = await api.get('/kpi', { params: { dt } });
  return resp.data;
}
export async function getCongestionTrend(startDt, endDt) {
  const resp = await api.get('/congestion/trend', { params: { start_dt: startDt, end_dt: endDt } });
  return resp.data;
}
export async function getCongestionComparison() {
  const resp = await api.get('/congestion/comparison');
  return resp.data;
}
export async function getRoadTypeSpeed(dt) {
  const resp = await api.get('/congestion/road-type-speed', { params: { dt } });
  return resp.data;
}
export async function getCongestionDurationRanking(dt) {
  const resp = await api.get('/congestion/duration-ranking', { params: { dt } });
  return resp.data;
}

// P2: Hotspot & Driver APIs
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

// P3: Catalog & Lineage stubs
export async function getCatalogTables() { return { tables: [] }; }
export async function getCatalogFields(table) { return { table: '', fields: [], preview: [] }; }
export async function queryCatalog(request) { return { columns: [], rows: [], total_rows: 0, query_id: '' }; }
export async function getHotFields() { return { hot_fields: [] }; }
export async function getLineage() { return { nodes: [], edges: [] }; }
export async function getQuality() { return { tables: [] }; }

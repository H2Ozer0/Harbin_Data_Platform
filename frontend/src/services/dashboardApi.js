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

// P3: Catalog & Lineage APIs
export async function getCatalogTables() {
  const resp = await api.get('/catalog/tables');
  return resp.data;
}
export async function getCatalogFields(schema, table) {
  const resp = await api.get(`/catalog/fields/${schema}/${table}`);
  return resp.data;
}
export async function queryCatalog(request) {
  const resp = await api.post('/catalog/query', request);
  return resp.data;
}
export async function getHotFields() {
  const resp = await api.get('/catalog/hot-fields');
  return resp.data;
}
export async function getLineage() {
  const resp = await api.get('/lineage');
  return resp.data;
}
export async function getQuality() {
  const resp = await api.get('/quality');
  return resp.data;
}
export async function getTrajectory(request) {
  const resp = await api.get('/trajectory', { params: request });
  return resp.data;
}

// P3 pages import as: import { dashboardApi } from '@/services/dashboardApi'
export const dashboardApi = {
  getCatalogTables,
  getCatalogFields,
  queryCatalog,
  getHotFields,
  getLineage,
  getQuality,
  getTrajectory,
}

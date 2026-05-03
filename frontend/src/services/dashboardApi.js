import axios from 'axios';

const api = axios.create({ baseURL: '/api/dashboard' });

export async function getCongestionHeatmap(dt, hour) { return { segments: [], total: 0 }; }
export async function getKPI(dt) { return { total_vehicles: 0, total_trips: 0, avg_speed_kmh: 0, top5_congested: [], most_active_hour: 0 }; }
export async function getCongestionTrend(startDt, endDt) { return { daily: [] }; }
export async function getCongestionComparison() { return { hourly: [] }; }
export async function getHotspotMap(dt, hour, eventType) { return { grids: [], total_grids: 0 }; }
export async function getDriverBehavior(dt) { return { shift_distribution: [], avg_active_minutes_by_pattern: [], total_drivers: 0 }; }
export async function getRestHeatmap(dt) { return { rest_locations: [], total_rests: 0 }; }
export async function getCatalogTables() { return { tables: [] }; }
export async function getCatalogFields(table) { return { table: '', fields: [], preview: [] }; }
export async function queryCatalog(request) { return { columns: [], rows: [], total_rows: 0, query_id: '' }; }
export async function getHotFields() { return { hot_fields: [] }; }
export async function getLineage() { return { nodes: [], edges: [] }; }
export async function getQuality() { return { tables: [] }; }

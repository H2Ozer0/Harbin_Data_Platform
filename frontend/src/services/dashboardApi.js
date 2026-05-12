import axios from 'axios'

const API_BASE = '/api/dashboard'

export const dashboardApi = {
  // Catalog APIs
  async getCatalogTables() {
    const response = await axios.get(`${API_BASE}/catalog/tables`)
    return response.data
  },

  async getCatalogFields(tableName) {
    const response = await axios.get(`${API_BASE}/catalog/fields/${tableName}`)
    return response.data
  },

  async queryCatalog(queryData) {
    const response = await axios.post(`${API_BASE}/catalog/query`, queryData)
    return response.data
  },

  async getHotFields() {
    const response = await axios.get(`${API_BASE}/catalog/hot-fields`)
    return response.data
  },

  // Lineage APIs
  async getLineage() {
    const response = await axios.get(`${API_BASE}/lineage`)
    return response.data
  },

  async getQuality() {
    const response = await axios.get(`${API_BASE}/quality`)
    return response.data
  },

  /**
   * 数据血缘页面按时间窗查询轨迹点（委托 Taxi 数据层）
   * 使用 GET 避免浏览器 CORS 预检
   */
  async getTrajectory(body) {
    const params = new URLSearchParams()
    params.append('startTime', body.startTime)
    params.append('endTime', body.endTime)
    if (body.deviceId) params.append('deviceId', body.deviceId)
    if (body.limit) params.append('limit', String(body.limit))
    if (body.minLon != null) params.append('minLon', String(body.minLon))
    if (body.maxLon != null) params.append('maxLon', String(body.maxLon))
    if (body.minLat != null) params.append('minLat', String(body.minLat))
    if (body.maxLat != null) params.append('maxLat', String(body.maxLat))
    const response = await axios.get(`${API_BASE}/trajectory`, {
      params,
      timeout: 120000,
    })
    return response.data
  },
}

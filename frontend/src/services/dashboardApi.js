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
  }
}

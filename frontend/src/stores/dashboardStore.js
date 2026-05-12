import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useDashboardStore = defineStore('dashboard', () => {
  /** 全局时间状态（跨页面共享） */
  const selectedDate = ref('2015-01-05')
  const selectedHour = ref(12)
  const selectedDayType = ref('workday') // workday / weekend / holiday

  /** Catalog 页面状态 */
  const catalogKeyword = ref('')
  const selectedTable = ref(null)

  /** Lineage 页面状态 */
  const selectedLayer = ref('all') // all / ODS / DW / TDM / ADS

  const loading = ref(false)

  /** 日期范围（前后两天，用于过滤） */
  const dateRange = computed(() => {
    const start = new Date(selectedDate.value)
    start.setDate(start.getDate() - 2)
    const end = new Date(selectedDate.value)
    end.setDate(end.getDate() + 2)

    return {
      start: start.toISOString().split('T')[0],
      end: end.toISOString().split('T')[0],
    }
  })

  function setDate(date) {
    selectedDate.value = date
  }

  function setHour(hour) {
    selectedHour.value = hour
  }

  function setDayType(type) {
    selectedDayType.value = type
  }

  function setCatalogKeyword(keyword) {
    catalogKeyword.value = keyword
  }

  function setSelectedTable(table) {
    selectedTable.value = table
  }

  function setLoading(isLoading) {
    loading.value = isLoading
  }

  return {
    selectedDate,
    selectedHour,
    selectedDayType,
    catalogKeyword,
    selectedTable,
    selectedLayer,
    loading,
    dateRange,
    setDate,
    setHour,
    setDayType,
    setCatalogKeyword,
    setSelectedTable,
    setLoading,
  }
})

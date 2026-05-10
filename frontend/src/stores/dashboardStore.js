import { defineStore } from 'pinia'
import { ref, computed } from 'vue'

export const useDashboardStore = defineStore('dashboard', () => {
  // 全局时间状态（所有页面共用）
  const selectedDate = ref('2015-01-05')
  const selectedHour = ref(12)
  const selectedDayType = ref('workday')  // workday/weekend/holiday

  // Catalog 页面状态
  const catalogKeyword = ref('')
  const selectedTable = ref(null)

  // Lineage 页面状态
  const selectedLayer = ref('all')  // all/ODS/DW/TDM/ADS

  // 加载状态
  const loading = ref(false)

  // 计算属性：日期范围（用于趋势图表）
  const dateRange = computed(() => {
    const start = new Date(selectedDate.value)
    start.setDate(start.getDate() - 2)
    const end = new Date(selectedDate.value)
    end.setDate(end.getDate() + 2)

    return {
      start: start.toISOString().split('T')[0],
      end: end.toISOString().split('T')[0]
    }
  })

  // Actions
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
    // State
    selectedDate,
    selectedHour,
    selectedDayType,
    catalogKeyword,
    selectedTable,
    selectedLayer,
    loading,

    // Computed
    dateRange,

    // Actions
    setDate,
    setHour,
    setDayType,
    setCatalogKeyword,
    setSelectedTable,
    setLoading
  }
})

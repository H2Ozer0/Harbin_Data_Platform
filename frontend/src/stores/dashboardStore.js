import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDashboardStore = defineStore('dashboard', () => {
  // P1: timeline state
  const selectedDate = ref('2015-01-05')
  const selectedHour = ref(12)
  const availableDates = ['2015-01-03', '2015-01-04', '2015-01-05', '2015-01-06', '2015-01-07']

  // P2: quick-access aliases used by HotspotPage/DriverPage
  const dt = ref('2015-01-05')
  const hour = ref(8)

  function setDt(val) {
    dt.value = val
  }

  function setHour(val) {
    hour.value = val
  }

  function setTime(newDt, newHour) {
    dt.value = newDt
    hour.value = newHour
  }

  // Map tile style: 'dark' | 'light'
  const mapStyle = ref('dark')
  function toggleMapStyle() {
    mapStyle.value = mapStyle.value === 'dark' ? 'light' : 'dark'
  }

  async function loadCongestion() { /* mock */ }
  async function loadHotspot() { /* mock */ }
  async function loadDriver() { /* mock */ }

  return {
    selectedDate, selectedHour, availableDates,
    dt, hour, setDt, setHour, setTime,
    mapStyle, toggleMapStyle,
    loadCongestion, loadHotspot, loadDriver,
  }
})

import { defineStore } from 'pinia'
import { ref } from 'vue'

export const useDashboardStore = defineStore('dashboard', () => {
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

  return {
    dt,
    hour,
    setDt,
    setHour,
    setTime,
  }
})
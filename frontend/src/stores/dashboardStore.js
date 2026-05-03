import { defineStore } from 'pinia';
import { ref } from 'vue';

export const useDashboardStore = defineStore('dashboard', () => {
  const selectedDate = ref('2015-01-05');
  const selectedHour = ref(12);
  const availableDates = ['2015-01-03', '2015-01-04', '2015-01-05', '2015-01-06', '2015-01-07'];

  async function loadCongestion() { /* mock */ }
  async function loadHotspot() { /* mock */ }
  async function loadDriver() { /* mock */ }

  return { selectedDate, selectedHour, availableDates, loadCongestion, loadHotspot, loadDriver };
});

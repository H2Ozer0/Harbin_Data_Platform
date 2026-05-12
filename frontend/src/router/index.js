import { createRouter, createWebHistory } from 'vue-router'
import DashboardLayout from '@/layouts/DashboardLayout.vue'
import HotspotPage from '@/pages/HotspotPage.vue'
import DriverPage from '@/pages/DriverPage.vue'
import OriginalMapPage from '@/pages/OriginalMapPage.vue'

const PlaceholderPage = { template: '<div class="placeholder"><h2>数据加载中...</h2></div>' }

const routes = [
  {
    path: '/',
    component: DashboardLayout,
    redirect: '/hotspot',
    children: [
      { path: 'congestion', component: PlaceholderPage },
      { path: 'trend', component: PlaceholderPage },
      { path: 'hotspot', component: HotspotPage },
      { path: 'driver', component: DriverPage },
      { path: 'catalog', component: PlaceholderPage },
      { path: 'lineage', component: PlaceholderPage },
    ],
  },
  {
    path: '/map',
    name: 'map',
    component: OriginalMapPage,
  },
]

const router = createRouter({
  history: createWebHistory(),
  routes,
  scrollBehavior() {
    return { left: 0, top: 0 }
  },
})

export default router
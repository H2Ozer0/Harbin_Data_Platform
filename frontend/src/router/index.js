import { createRouter, createWebHashHistory } from 'vue-router'

const routes = [
  {
    path: '/',
    redirect: '/congestion'
  },
  {
    path: '/congestion',
    name: 'Congestion',
    component: () => import('@/pages/CongestionPage.vue')
  },
  {
    path: '/catalog',
    name: 'Catalog',
    component: () => import('@/pages/CatalogPage.vue')
  },
  {
    path: '/lineage',
    name: 'Lineage',
    component: () => import('@/pages/LineagePage.vue')
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

export default router

import { createRouter, createWebHistory } from 'vue-router';

const routes = [
  {
    path: '/',
    component: () => import('@/layouts/DashboardLayout.vue'),
    redirect: '/congestion',
    children: [
      { path: 'congestion', name: 'Congestion', component: () => import('@/pages/CongestionPage.vue') },
      { path: 'trend', name: 'Trend', component: () => import('@/pages/TrendPage.vue') },
      { path: 'hotspot', name: 'Hotspot', component: () => import('@/pages/HotspotPage.vue') },
      { path: 'driver', name: 'Driver', component: () => import('@/pages/DriverPage.vue') },
      { path: 'catalog', name: 'Catalog', component: () => import('@/pages/CatalogPage.vue') },
      { path: 'lineage', name: 'Lineage', component: () => import('@/pages/LineagePage.vue') },
    ]
  },
  { path: '/map', name: 'OriginalMap', component: () => import('@/pages/OriginalMapPage.vue') }
];

export default createRouter({ history: createWebHistory(), routes });

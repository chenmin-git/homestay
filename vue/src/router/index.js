import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import HomestayDetailView from '../views/HomestayDetailView.vue'
import UserCenterView from '../views/UserCenterView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/homestays/:id', component: HomestayDetailView },
    { path: '/user', component: UserCenterView },
    { path: '/admin', component: AdminDashboardView }
  ]
})

export default router

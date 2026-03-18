import { createRouter, createWebHistory } from 'vue-router'
import HomeView from '../views/HomeView.vue'
import LoginView from '../views/LoginView.vue'
import HomestayDetailView from '../views/HomestayDetailView.vue'
import UserCenterView from '../views/UserCenterView.vue'
import AdminDashboardView from '../views/AdminDashboardView.vue'

const getStoredUser = () => {
  try {
    return JSON.parse(localStorage.getItem('homestay-user') || 'null')
  } catch {
    return null
  }
}

const router = createRouter({
  history: createWebHistory(),
  routes: [
    { path: '/', component: HomeView },
    { path: '/login', component: LoginView, meta: { guestOnly: true } },
    { path: '/homestays/:id', component: HomestayDetailView },
    { path: '/user', component: UserCenterView, meta: { requiresAuth: true } },
    { path: '/admin', component: AdminDashboardView, meta: { requiresAuth: true, requiresManager: true } }
  ]
})

router.beforeEach((to) => {
  const token = localStorage.getItem('homestay-token')
  const user = getStoredUser()
  const requiresAuth = to.matched.some((record) => record.meta.requiresAuth)
  const requiresManager = to.matched.some((record) => record.meta.requiresManager)
  const guestOnly = to.matched.some((record) => record.meta.guestOnly)

  if (requiresAuth && !token) {
    return {
      path: '/login',
      query: { redirect: to.fullPath }
    }
  }

  if (requiresManager && (!user || user.role === 'USER')) {
    return token ? '/' : '/login'
  }

  if (guestOnly && token) {
    return user && user.role !== 'USER' ? '/admin' : '/user'
  }
})

export default router

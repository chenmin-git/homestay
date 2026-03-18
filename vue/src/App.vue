<script setup>
import { computed, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useAuthStore } from './stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()

const isAdminRoute = computed(() => route.path.startsWith('/admin'))
const isLoginRoute = computed(() => route.path.startsWith('/login'))

onMounted(() => {
  authStore.restore()
})

const handleCommand = async (command) => {
  if (command === 'logout') {
    authStore.logout()
    router.push('/')
  }
  if (command === 'user') {
    router.push('/user')
  }
  if (command === 'profile') {
    router.push({ path: '/user', query: { profile: '1' } })
  }
  if (command === 'admin') {
    router.push('/admin')
  }
}
</script>

<template>
  <div class="app-shell">
    <header class="topbar">
      <div class="brand" @click="$router.push('/')">
        <span class="brand-mark">HS</span>
        <div>
          <strong>栖间民宿管理系统</strong>
          <p>Spring Boot + Vue 全栈课程设计</p>
        </div>
      </div>
      <nav class="topnav">
        <button :class="{ active: route.path === '/' }" @click="$router.push('/')">首页</button>
        <button :class="{ active: route.path.startsWith('/user') }" @click="$router.push('/user')">我的中心</button>
        <button
          v-if="authStore.user?.role && authStore.user.role !== 'USER'"
          :class="{ active: route.path.startsWith('/admin') }"
          @click="$router.push('/admin')"
        >后台管理</button>
      </nav>
      <div class="top-actions">
        <el-tag v-if="authStore.user?.role" type="success">{{ authStore.user.role }}</el-tag>
        <el-dropdown v-if="authStore.isLoggedIn" @command="handleCommand">
          <span class="user-pill">
            <img v-if="authStore.user?.avatar" :src="authStore.user.avatar" class="user-avatar" alt="avatar" />
            <span>{{ authStore.user?.nickname || authStore.user?.username }}</span>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="user">用户中心</el-dropdown-item>
              <el-dropdown-item command="profile">个人资料</el-dropdown-item>
              <el-dropdown-item v-if="authStore.user?.role !== 'USER'" command="admin">后台管理</el-dropdown-item>
              <el-dropdown-item command="logout">退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
        <button v-else class="outline-btn" @click="$router.push(isLoginRoute ? '/' : '/login')">
          {{ isLoginRoute ? '返回首页' : '去登录' }}
        </button>
      </div>
    </header>

    <main :class="['page-shell', { compact: isAdminRoute }]">
      <router-view />
    </main>
  </div>
</template>

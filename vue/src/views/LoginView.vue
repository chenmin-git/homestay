<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeTab = ref('login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '', phone: '' })
const demoAccounts = [
  { label: '管理员', username: 'admin', password: 'admin123' },
  { label: '房东', username: 'host', password: 'host123' },
  { label: '游客', username: 'user', password: 'user123' }
]

const resolveFallbackPath = (role) => {
  return role && role !== 'USER' ? '/admin' : '/user'
}

const resolveTargetPath = () => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  return redirect.startsWith('/') ? redirect : resolveFallbackPath(authStore.user?.role)
}

const fillDemo = (account) => {
  loginForm.username = account.username
  loginForm.password = account.password
  activeTab.value = 'login'
}

const doLogin = async () => {
  loginLoading.value = true
  try {
    await authStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push(resolveTargetPath())
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    loginLoading.value = false
  }
}

const doRegister = async () => {
  registerLoading.value = true
  try {
    await authStore.register(registerForm)
    ElMessage.success('注册成功')
    activeTab.value = 'login'
    loginForm.username = registerForm.username
    loginForm.password = ''
    router.push('/login')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    registerLoading.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <section class="panel login-hero">
      <span class="login-eyebrow">独立登录入口</span>
      <h1>首页专注展示房源，登录单独放到一页里。</h1>
      <p>
        这样浏览、搜索和账号操作就分开了，页面更清爽，课程设计演示时也更直观。
        登录后可以进入用户中心下单、收藏、评价，房东和管理员还能继续进入后台管理。
      </p>

      <div class="login-demo-list">
        <button
          v-for="account in demoAccounts"
          :key="account.username"
          class="demo-account"
          @click="fillDemo(account)"
        >
          <span>{{ account.label }}</span>
          <strong>{{ account.username }}</strong>
          <small>{{ account.password }}</small>
        </button>
      </div>
    </section>

    <section class="panel login-panel">
      <div class="section-title login-title">
        <div>
          <h2 style="margin: 0;">账号入口</h2>
          <p class="muted">使用演示账号快速登录，或注册一个新的游客账号</p>
        </div>
      </div>

      <el-tabs v-model="activeTab" stretch>
        <el-tab-pane label="登录" name="login">
          <el-form label-position="top" autocomplete="off" @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" autocomplete="off" @keyup.enter="doLogin" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="loginForm.password"
                type="password"
                show-password
                autocomplete="off"
                @keyup.enter="doLogin"
              />
            </el-form-item>
            <el-button
              type="primary"
              color="#b5653b"
              class="full-width"
              :loading="loginLoading"
              @click="doLogin"
            >
              立即登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form label-position="top" autocomplete="off" @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="registerForm.username" autocomplete="off" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="registerForm.nickname" autocomplete="off" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="registerForm.phone" autocomplete="off" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="registerForm.password"
                type="password"
                show-password
                autocomplete="new-password"
                @keyup.enter="doRegister"
              />
            </el-form-item>
            <el-button
              type="success"
              color="#5b8870"
              class="full-width"
              :loading="registerLoading"
              @click="doRegister"
            >
              注册并登录
            </el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

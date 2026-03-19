<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeTab = ref('login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const hostApplyLoading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '', phone: '' })
const hostApplyForm = reactive({ username: '', password: '', nickname: '', phone: '' })

const phonePattern = /^1[3-9]\d{9}$/
const isValidPhone = (value) => phonePattern.test(String(value || '').trim())

const resolveFallbackPath = (role) => {
  return role && role !== 'USER' ? '/admin' : '/user'
}

const resolveTargetPath = () => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  return redirect.startsWith('/') ? redirect : resolveFallbackPath(authStore.user?.role)
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
  if (!isValidPhone(registerForm.phone)) {
    ElMessage.error('请输入有效的手机号')
    return
  }
  registerLoading.value = true
  try {
    await authStore.register(registerForm)
    ElMessage.success('注册成功，已自动登录')
    router.push('/user')
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    registerLoading.value = false
  }
}

const doHostApply = async () => {
  if (!isValidPhone(hostApplyForm.phone)) {
    ElMessage.error('请输入有效的手机号')
    return
  }
  hostApplyLoading.value = true
  try {
    if (typeof authStore.applyHost === 'function') {
      await authStore.applyHost(hostApplyForm)
    } else {
      await http.post('/auth/host-apply', hostApplyForm)
    }
    ElMessage.success('申请已提交，请等待管理员审核')
    hostApplyForm.username = ''
    hostApplyForm.password = ''
    hostApplyForm.nickname = ''
    hostApplyForm.phone = ''
    activeTab.value = 'login'
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    hostApplyLoading.value = false
  }
}
</script>

<template>
  <div class="login-shell">
    <section class="panel login-panel">
      <div class="section-title login-title">
        <div>
          <h2 style="margin: 0;">账号入口</h2>
          <p class="muted">请输入账号登录，或注册一个新的账号</p>
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

        <el-tab-pane label="申请房东" name="host-apply">
          <el-form label-position="top" autocomplete="off" @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="hostApplyForm.username" autocomplete="off" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="hostApplyForm.nickname" autocomplete="off" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="hostApplyForm.phone" autocomplete="off" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input
                v-model="hostApplyForm.password"
                type="password"
                show-password
                autocomplete="new-password"
                @keyup.enter="doHostApply"
              />
            </el-form-item>
            <el-button
              type="primary"
              color="#b5653b"
              class="full-width"
              :loading="hostApplyLoading"
              @click="doHostApply"
            >
              提交申请
            </el-button>
            <p class="muted" style="margin-top: 10px;">管理员审核通过后即可使用房东账号登录后台。</p>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </section>
  </div>
</template>

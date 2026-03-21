<script setup>
import { reactive, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()
const authStore = useAuthStore()

const activeTab = ref('login')
const loginLoading = ref(false)
const registerLoading = ref(false)
const hostApplyLoading = ref(false)
const showForgotDialog = ref(false)
const forgotTab = ref('user')
const userResetLoading = ref(false)
const hostResetLoading = ref(false)
const loginForm = reactive({ username: '', password: '' })
const registerForm = reactive({ username: '', password: '', nickname: '', phone: '' })
const hostApplyForm = reactive({ username: '', password: '', nickname: '', phone: '' })
const userResetForm = reactive({ username: '', phone: '', newPassword: '', confirmPassword: '' })
const hostResetForm = reactive({ username: '', nickname: '', phone: '', newPassword: '', confirmPassword: '' })

const phonePattern = /^1[3-9]\d{9}$/
const isValidPhone = (value) => phonePattern.test(String(value || '').trim())

const resolveFallbackPath = (role) => {
  return role && role !== 'USER' ? '/admin' : '/user'
}

const resolveTargetPath = () => {
  const redirect = typeof route.query.redirect === 'string' ? route.query.redirect : ''
  return redirect.startsWith('/') ? redirect : resolveFallbackPath(authStore.user?.role)
}

const showFriendlyLoginFeedback = async (message) => {
  if (message.includes('待管理员审核')) {
    await ElMessageBox.alert(
      '你的房东申请已经提交成功，目前正在等待管理员审核。审核通过后，就可以使用当前账号登录后台了。',
      '房东申请审核中',
      {
        confirmButtonText: '我知道了',
        type: 'info'
      }
    )
    return true
  }

  if (message.includes('未通过')) {
    await ElMessageBox.alert(
      '你的房东申请暂未通过。你可以切换到“申请房东”标签页，补充或修改信息后重新提交。',
      '房东申请未通过',
      {
        confirmButtonText: '去重新申请',
        type: 'warning'
      }
    )
    activeTab.value = 'host-apply'
    return true
  }

  return false
}

const doLogin = async () => {
  loginLoading.value = true
  try {
    await authStore.login(loginForm)
    ElMessage.success('登录成功')
    router.push(resolveTargetPath())
  } catch (error) {
    const handled = await showFriendlyLoginFeedback(error.message || '')
    if (!handled) {
      ElMessage.error(error.message)
    }
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

const openForgotDialog = (type = 'user') => {
  forgotTab.value = type
  showForgotDialog.value = true
}

const resetUserPassword = async () => {
  if (!isValidPhone(userResetForm.phone)) {
    ElMessage.error('请输入有效的手机号')
    return
  }
  if (userResetForm.newPassword !== userResetForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  userResetLoading.value = true
  try {
    await authStore.resetUserPassword({
      username: userResetForm.username,
      phone: userResetForm.phone,
      newPassword: userResetForm.newPassword
    })
    ElMessage.success('密码已重置，已自动登录')
    showForgotDialog.value = false
    router.push(resolveTargetPath())
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    userResetLoading.value = false
  }
}

const submitHostPasswordReset = async () => {
  if (!isValidPhone(hostResetForm.phone)) {
    ElMessage.error('请输入有效的手机号')
    return
  }
  if (hostResetForm.newPassword !== hostResetForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  hostResetLoading.value = true
  try {
    if (typeof authStore.submitHostPasswordReset === 'function') {
      await authStore.submitHostPasswordReset({
        username: hostResetForm.username,
        nickname: hostResetForm.nickname,
        phone: hostResetForm.phone,
        newPassword: hostResetForm.newPassword
      })
    } else {
      await http.post('/auth/host/reset-password', {
        username: hostResetForm.username,
        nickname: hostResetForm.nickname,
        phone: hostResetForm.phone,
        newPassword: hostResetForm.newPassword
      })
    }
    ElMessage.success('改密申请已提交，请等待管理员审核')
    showForgotDialog.value = false
    forgotTab.value = 'user'
    hostResetForm.username = ''
    hostResetForm.nickname = ''
    hostResetForm.phone = ''
    hostResetForm.newPassword = ''
    hostResetForm.confirmPassword = ''
  } catch (error) {
    ElMessage.error(error.message)
  } finally {
    hostResetLoading.value = false
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
            <div class="forgot-entry-row">
              <el-button type="primary" link @click="openForgotDialog('user')">用户忘记密码</el-button>
              <el-button type="primary" link @click="openForgotDialog('host')">房东忘记密码</el-button>
            </div>
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

    <el-dialog v-model="showForgotDialog" title="找回密码" width="520px">
      <el-tabs v-model="forgotTab" stretch>
        <el-tab-pane label="用户找回" name="user">
          <el-form label-position="top" autocomplete="off" @submit.prevent>
            <el-form-item label="用户名">
              <el-input v-model="userResetForm.username" autocomplete="off" />
            </el-form-item>
            <el-form-item label="注册手机号">
              <el-input v-model="userResetForm.phone" autocomplete="off" />
            </el-form-item>
            <el-form-item label="新密码">
              <el-input
                v-model="userResetForm.newPassword"
                type="password"
                show-password
                autocomplete="new-password"
                @keyup.enter="resetUserPassword"
              />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input
                v-model="userResetForm.confirmPassword"
                type="password"
                show-password
                autocomplete="new-password"
                @keyup.enter="resetUserPassword"
              />
            </el-form-item>
            <el-button
              type="primary"
              color="#5b8870"
              class="full-width"
              :loading="userResetLoading"
              @click="resetUserPassword"
            >
              重置并登录
            </el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="房东找回" name="host">
          <el-form label-position="top" autocomplete="off" @submit.prevent>
            <el-form-item label="房东账号">
              <el-input v-model="hostResetForm.username" autocomplete="off" />
            </el-form-item>
            <el-form-item label="房东昵称">
              <el-input v-model="hostResetForm.nickname" autocomplete="off" />
            </el-form-item>
            <el-form-item label="绑定手机号">
              <el-input v-model="hostResetForm.phone" autocomplete="off" />
            </el-form-item>
            <el-form-item label="申请的新密码">
              <el-input
                v-model="hostResetForm.newPassword"
                type="password"
                show-password
                autocomplete="new-password"
                @keyup.enter="submitHostPasswordReset"
              />
            </el-form-item>
            <el-form-item label="确认新密码">
              <el-input
                v-model="hostResetForm.confirmPassword"
                type="password"
                show-password
                autocomplete="new-password"
                @keyup.enter="submitHostPasswordReset"
              />
            </el-form-item>
            <el-button
              type="primary"
              color="#b5653b"
              class="full-width"
              :loading="hostResetLoading"
              @click="submitHostPasswordReset"
            >
              提交审核申请
            </el-button>
            <p class="muted forgot-tip">提交后需管理员审核通过，房东新密码才会生效。</p>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </el-dialog>
  </div>
</template>

<style scoped>
.forgot-entry-row {
  display: flex;
  justify-content: space-between;
  margin-top: 10px;
}

.forgot-tip {
  margin-top: 10px;
  line-height: 1.6;
}
</style>

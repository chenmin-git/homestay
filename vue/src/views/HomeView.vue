<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const home = ref({ banners: [], hotHomestays: [], latestHomestays: [], notices: [] })
const homestays = ref([])
const loading = ref(false)
const query = reactive({
  city: '',
  keyword: '',
  minPrice: '',
  maxPrice: '',
  houseType: ''
})
const loginForm = reactive({ username: 'user', password: 'user123' })
const registerForm = reactive({ username: '', password: '', nickname: '', phone: '' })

const fetchHome = async () => {
  const [homeRes, listRes] = await Promise.all([
    http.get('/public/home'),
    http.get('/public/homestays')
  ])
  home.value = homeRes.data
  homestays.value = listRes.data.content
}

const search = async () => {
  loading.value = true
  try {
    const result = await http.get('/public/homestays', { params: query })
    homestays.value = result.data.content
  } finally {
    loading.value = false
  }
}

const doLogin = async () => {
  try {
    await authStore.login(loginForm)
    ElMessage.success('登录成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const doRegister = async () => {
  try {
    await authStore.register(registerForm)
    ElMessage.success('注册成功')
  } catch (error) {
    ElMessage.error(error.message)
  }
}

const toggleFavorite = async (id) => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再收藏')
    return
  }
  await http.post(`/user/favorites/${id}`)
  await search()
}

onMounted(fetchHome)
</script>

<template>
  <div class="content-grid">
    <section class="hero-grid">
      <div class="hero-card panel">
        <el-tag type="warning">支持按房号预订</el-tag>
        <h1>民宿不再是一套房只能住一单，而是按房东真实房间库存管理。</h1>
        <p>
          系统面向游客、房东与管理员，围绕“浏览、预订、评价、房态管理、数据统计”展开。
          你特别强调的多房间库存模式，已经放在整个设计核心里。
        </p>
        <div class="search-bar">
          <el-input v-model="query.city" placeholder="地点 / 商圈" />
          <el-input v-model="query.keyword" placeholder="关键词" />
          <el-input v-model="query.minPrice" placeholder="最低价" />
          <el-input v-model="query.maxPrice" placeholder="最高价" />
          <el-select v-model="query.houseType" clearable placeholder="房型">
            <el-option v-for="item in home.houseTypes || []" :key="item" :label="item" :value="item" />
          </el-select>
        </div>
        <div class="split" style="margin-top: 14px;">
          <div class="chip-list">
            <el-tag v-for="notice in home.notices" :key="notice.id" effect="plain">{{ notice.title }}</el-tag>
          </div>
          <el-button type="primary" color="#b5653b" @click="search">搜索房源</el-button>
        </div>
      </div>

      <div class="stats-board panel">
        <div class="metric">
          <span class="muted">热门房源</span>
          <strong>{{ home.hotHomestays?.length || 0 }}</strong>
        </div>
        <div class="metric">
          <span class="muted">最新上架</span>
          <strong>{{ home.latestHomestays?.length || 0 }}</strong>
        </div>
        <div class="metric">
          <span class="muted">演示账号</span>
          <strong>admin / host / user</strong>
          <p class="muted">密码分别为 admin123、host123、user123</p>
        </div>
      </div>
    </section>

    <section class="content-grid" style="grid-template-columns: 1.4fr 0.8fr;">
      <div class="panel" style="padding: 24px;">
        <div class="section-title">
          <div>
            <h2 style="margin: 0;">房源列表</h2>
            <p class="muted">支持地点、价格、房型组合筛选</p>
          </div>
        </div>
        <div v-loading="loading" class="card-grid">
          <article v-for="item in homestays" :key="item.id" class="stay-card panel">
            <img :src="item.coverImage" class="stay-cover" :alt="item.name" />
            <div class="stay-body">
              <div class="spaced">
                <h3 style="margin: 0;">{{ item.name }}</h3>
                <el-button text type="warning" @click="toggleFavorite(item.id)">
                  {{ item.favorite ? '已收藏' : '收藏' }}
                </el-button>
              </div>
              <p class="muted" style="margin: 8px 0 12px;">{{ item.city }} · {{ item.district }}</p>
              <div class="chip-list">
                <el-tag v-for="tag in item.tags" :key="tag" effect="plain">{{ tag }}</el-tag>
              </div>
              <div class="split" style="margin-top: 14px;">
                <strong>￥{{ item.basePrice }} / 晚</strong>
                <el-button text type="primary" @click="router.push(`/homestays/${item.id}`)">查看详情</el-button>
              </div>
            </div>
          </article>
        </div>
      </div>

      <div class="content-grid">
        <div class="panel form-panel">
          <div class="section-title">
            <div>
              <h3 style="margin: 0;">游客登录</h3>
              <p class="muted">登录后可下单、收藏、评价</p>
            </div>
          </div>
          <el-form label-position="top">
            <el-form-item label="用户名">
              <el-input v-model="loginForm.username" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="loginForm.password" type="password" show-password />
            </el-form-item>
            <el-button type="primary" color="#b5653b" class="full-width" @click="doLogin">立即登录</el-button>
          </el-form>
        </div>

        <div class="panel form-panel">
          <div class="section-title">
            <div>
              <h3 style="margin: 0;">快速注册</h3>
              <p class="muted">答辩演示时可以新增游客账号</p>
            </div>
          </div>
          <el-form label-position="top">
            <el-form-item label="用户名">
              <el-input v-model="registerForm.username" />
            </el-form-item>
            <el-form-item label="昵称">
              <el-input v-model="registerForm.nickname" />
            </el-form-item>
            <el-form-item label="手机号">
              <el-input v-model="registerForm.phone" />
            </el-form-item>
            <el-form-item label="密码">
              <el-input v-model="registerForm.password" type="password" show-password />
            </el-form-item>
            <el-button type="success" color="#5b8870" class="full-width" @click="doRegister">注册并登录</el-button>
          </el-form>
        </div>
      </div>
    </section>
  </div>
</template>

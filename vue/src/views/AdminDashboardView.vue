<script setup>
import * as echarts from 'echarts'
import { nextTick, onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const dashboard = ref({ orderTrend: [], typePie: [], todos: {} })
const orders = ref([])
const users = ref([])
const reviews = ref([])
const homestays = ref([])
const chartRef = ref()
const pieRef = ref()
const homestayForm = reactive({
  name: '新院样板房',
  city: '杭州',
  district: '西湖区',
  address: '龙井路 88 号',
  basePrice: 468,
  houseType: '大床房',
  tags: '茶园,庭院,拍照',
  facilities: '空调,投影,停车位',
  latitude: 30.2401,
  longitude: 120.1503,
  coverImage: 'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80',
  summary: '适合课程设计演示的新房源样例',
  description: '房东可以在后台维护多张图片与多个房间，并按真实房号管理库存。',
  images: [
    'https://images.unsplash.com/photo-1505693416388-ac5ce068fe85?auto=format&fit=crop&w=1200&q=80'
  ],
  rooms: [
    { roomNo: 'D101', roomType: '大床房', floorNo: 1, price: 468, bedCount: 1, capacity: 2 },
    { roomNo: 'D102', roomType: '双床房', floorNo: 1, price: 498, bedCount: 2, capacity: 2 }
  ]
})

const loadAll = async () => {
  if (!authStore.isLoggedIn) return
  const requests = [
    http.get('/admin/dashboard'),
    http.get('/admin/homestays'),
    http.get('/admin/orders'),
    http.get('/admin/reviews')
  ]
  if (authStore.user?.role === 'ADMIN') {
    requests.push(http.get('/admin/users'))
  }
  const [dashboardRes, homestayRes, orderRes, reviewRes, userRes] = await Promise.all(requests)
  dashboard.value = dashboardRes.data
  homestays.value = homestayRes.data
  orders.value = orderRes.data
  reviews.value = reviewRes.data
  users.value = userRes?.data || []
  await nextTick()
  renderCharts()
}

const renderCharts = () => {
  if (chartRef.value) {
    const chart = echarts.init(chartRef.value)
    chart.setOption({
      color: ['#b5653b'],
      tooltip: { trigger: 'axis' },
      xAxis: {
        type: 'category',
        data: dashboard.value.orderTrend.map((item) => item.date)
      },
      yAxis: { type: 'value' },
      series: [{ type: 'line', smooth: true, data: dashboard.value.orderTrend.map((item) => item.count) }]
    })
  }
  if (pieRef.value) {
    const chart = echarts.init(pieRef.value)
    chart.setOption({
      tooltip: { trigger: 'item' },
      series: [{
        type: 'pie',
        radius: ['38%', '72%'],
        data: dashboard.value.typePie,
        color: ['#b5653b', '#5b8870', '#d2a679', '#7f3f20', '#95b3a1']
      }]
    })
  }
}

const publishHomestay = async () => {
  await http.post('/admin/homestays', homestayForm)
  ElMessage.success('房源发布成功')
  await loadAll()
}

const confirmOrder = async (id) => {
  await http.post(`/admin/orders/${id}/confirm`)
  ElMessage.success('已确认入住')
  await loadAll()
}

const refundOrder = async (id) => {
  await http.post(`/admin/orders/${id}/refund`)
  ElMessage.success('退款完成')
  await loadAll()
}

const toggleUser = async (id, type) => {
  await http.post(`/admin/users/${id}/${type}`)
  ElMessage.success('用户状态已更新')
  await loadAll()
}

const replyReview = async (review) => {
  await http.post(`/admin/reviews/${review.id}/reply`, { replyContent: '感谢反馈，我们会继续优化入住体验。' })
  ElMessage.success('回复成功')
  await loadAll()
}

onMounted(loadAll)
</script>

<template>
  <div v-if="authStore.isLoggedIn && authStore.user?.role !== 'USER'" class="content-grid">
    <section class="panel chart-panel">
      <div class="section-title">
        <div>
          <h2 style="margin: 0;">后台看板</h2>
          <p class="muted">今日订单、总销售额、新增用户数、近 7 日趋势</p>
        </div>
      </div>
      <div class="admin-metrics">
        <div class="metric panel">
          <span class="muted">今日订单数</span>
          <strong>{{ dashboard.todayOrders || 0 }}</strong>
        </div>
        <div class="metric panel">
          <span class="muted">总销售额</span>
          <strong>￥{{ dashboard.totalSales || 0 }}</strong>
        </div>
        <div class="metric panel">
          <span class="muted">新增用户数</span>
          <strong>{{ dashboard.newUsers || 0 }}</strong>
        </div>
        <div class="metric panel">
          <span class="muted">待办提醒</span>
          <strong>{{ dashboard.todos?.pendingOrders || 0 }} / {{ dashboard.todos?.newComments || 0 }}</strong>
        </div>
      </div>
      <div class="dashboard-grid" style="margin-top: 20px;">
        <div class="panel chart-panel">
          <h3 style="margin-top: 0;">近 7 日订单趋势</h3>
          <div ref="chartRef" style="height: 300px;"></div>
        </div>
        <div class="panel chart-panel">
          <h3 style="margin-top: 0;">房源类型占比</h3>
          <div ref="pieRef" style="height: 300px;"></div>
        </div>
      </div>
    </section>

    <section class="dashboard-grid">
      <div class="panel form-panel">
        <div class="section-title">
          <div>
            <h3 style="margin: 0;">发布新民宿</h3>
            <p class="muted">房东可维护多图与多个房间库存</p>
          </div>
        </div>
        <el-form label-position="top">
          <div class="two-cols">
            <el-form-item label="名称"><el-input v-model="homestayForm.name" /></el-form-item>
            <el-form-item label="城市"><el-input v-model="homestayForm.city" /></el-form-item>
            <el-form-item label="区县"><el-input v-model="homestayForm.district" /></el-form-item>
            <el-form-item label="地址"><el-input v-model="homestayForm.address" /></el-form-item>
            <el-form-item label="基础价格"><el-input-number v-model="homestayForm.basePrice" class="full-width" /></el-form-item>
            <el-form-item label="房型"><el-input v-model="homestayForm.houseType" /></el-form-item>
          </div>
          <el-form-item label="标签"><el-input v-model="homestayForm.tags" /></el-form-item>
          <el-form-item label="设施"><el-input v-model="homestayForm.facilities" /></el-form-item>
          <el-form-item label="封面图"><el-input v-model="homestayForm.coverImage" /></el-form-item>
          <el-form-item label="摘要"><el-input v-model="homestayForm.summary" /></el-form-item>
          <el-form-item label="详情"><el-input v-model="homestayForm.description" type="textarea" :rows="4" /></el-form-item>
          <el-button type="primary" color="#b5653b" @click="publishHomestay">发布房源</el-button>
        </el-form>
      </div>

      <div class="content-grid">
        <div class="panel table-panel">
          <div class="section-title">
            <div>
              <h3 style="margin: 0;">房源管理</h3>
              <p class="muted">展示房东名下房源与房间数</p>
            </div>
          </div>
          <el-table :data="homestays" max-height="280">
            <el-table-column prop="name" label="房源" min-width="130" />
            <el-table-column prop="totalRooms" label="房间数" width="90" />
            <el-table-column prop="status" label="状态" width="110" />
          </el-table>
        </div>

        <div class="panel table-panel">
          <div class="section-title">
            <div>
              <h3 style="margin: 0;">评论管理</h3>
              <p class="muted">可回复或隐藏恶意评论</p>
            </div>
          </div>
          <div class="content-grid">
            <div v-for="review in reviews.slice(0, 3)" :key="review.id" class="panel detail-panel">
              <div class="spaced">
                <strong>{{ review.nickname }}</strong>
                <el-rate :model-value="review.score" disabled />
              </div>
              <p style="margin: 8px 0 14px;">{{ review.content }}</p>
              <el-button size="small" type="primary" @click="replyReview(review)">回复</el-button>
            </div>
          </div>
        </div>
      </div>
    </section>

    <section class="panel table-panel">
      <div class="section-title">
        <div>
          <h3 style="margin: 0;">订单管理</h3>
          <p class="muted">支持确认入住、退款处理、导出订单</p>
        </div>
        <a class="outline-btn" href="http://localhost:8080/api/admin/orders/export" target="_blank">导出 CSV</a>
      </div>
      <el-table :data="orders" stripe>
        <el-table-column prop="orderNo" label="订单号" min-width="180" />
        <el-table-column prop="username" label="用户" width="100" />
        <el-table-column prop="homestayName" label="房源" min-width="120" />
        <el-table-column prop="totalAmount" label="金额" width="100" />
        <el-table-column prop="orderStatus" label="订单状态" width="120" />
        <el-table-column label="操作" width="180">
          <template #default="{ row }">
            <div class="chip-list">
              <el-button size="small" type="primary" @click="confirmOrder(row.id)">确认</el-button>
              <el-button size="small" @click="refundOrder(row.id)">退款</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>

    <section v-if="authStore.user?.role === 'ADMIN'" class="panel table-panel">
      <div class="section-title">
        <div>
          <h3 style="margin: 0;">用户管理</h3>
          <p class="muted">支持禁用/启用与黑名单管理</p>
        </div>
      </div>
      <el-table :data="users" stripe>
        <el-table-column prop="username" label="账号" width="120" />
        <el-table-column prop="nickname" label="昵称" width="120" />
        <el-table-column prop="role" label="角色" width="100" />
        <el-table-column prop="phone" label="手机号" width="140" />
        <el-table-column label="状态" width="220">
          <template #default="{ row }">
            <div class="chip-list">
              <el-tag :type="row.enabled ? 'success' : 'danger'">{{ row.enabled ? '启用' : '禁用' }}</el-tag>
              <el-tag :type="row.blacklisted ? 'danger' : 'info'">{{ row.blacklisted ? '黑名单' : '正常' }}</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="操作" min-width="220">
          <template #default="{ row }">
            <div class="chip-list">
              <el-button size="small" @click="toggleUser(row.id, 'toggle-enabled')">切换启用</el-button>
              <el-button size="small" type="danger" plain @click="toggleUser(row.id, 'toggle-blacklist')">切换黑名单</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>
    </section>
  </div>

  <div v-else class="panel empty-state">
    只有房东或管理员可以进入后台，请先使用 `host` 或 `admin` 账号登录。
  </div>
</template>

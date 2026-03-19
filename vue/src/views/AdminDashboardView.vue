<script setup>
import * as echarts from 'echarts'
import { computed, nextTick, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Location, Plus } from '@element-plus/icons-vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const homestayStatusTextMap = {
  ONLINE: '上架中',
  OFFLINE: '已下架',
  DRAFT: '待审核'
}

const orderStatusTextMap = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  CONFIRMED: '待入住',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUND_REQUESTED: '退款中',
  REFUNDED: '已退款'
}

const roleTextMap = {
  ADMIN: '管理员',
  HOST: '房东',
  USER: '用户'
}

const hostApplyStatusTextMap = {
  PENDING: '待审核',
  APPROVED: '已通过',
  REJECTED: '已拒绝'
}

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const dashboard = ref({ orderTrend: [], typePie: [], todos: {} })
const orders = ref([])
const users = ref([])
const reviews = ref([])
const homestays = ref([])
const hostApplications = ref([])
const chartRef = ref()
const pieRef = ref()
const editingHomestayId = ref(null)
const showHomestayEditor = ref(false)
const calendar = ref({ homestayName: '', dates: [], rooms: [] })
const today = new Date().toISOString().slice(0, 10)
const activeSection = ref('dashboard')

const adminSections = computed(() => {
  const sections = [
    { key: 'dashboard', label: '看板', desc: '数据总览与趋势' },
    { key: 'homestays', label: '房源管理', desc: '发布、编辑与上下架' },
    { key: 'calendar', label: '房态日历', desc: '按房号查看占用' },
    { key: 'orders', label: '订单管理', desc: '确认入住与退款' },
    { key: 'reviews', label: '评论管理', desc: '回复与隐藏评论' }
  ]
  if (authStore.user?.role === 'ADMIN') {
    sections.push({ key: 'host-approvals', label: '房东审核', desc: '审核房东入驻申请' })
    sections.push({ key: 'settings', label: '系统设置', desc: '轮播图、公告与密码' })
    sections.push({ key: 'users', label: '用户管理', desc: '启用、禁用与黑名单' })
  }
  return sections
})

const normalizeSection = (section) => {
  if (typeof section !== 'string') return 'dashboard'
  return adminSections.value.some((item) => item.key === section) ? section : 'dashboard'
}

const createRoomItem = (room = {}) => ({
  id: room.id ?? null,
  roomNo: room.roomNo ?? '',
  roomType: room.roomType ?? '',
  floorNo: room.floorNo ?? 1,
  price: room.price ?? 368,
  bedCount: room.bedCount ?? 1,
  capacity: room.capacity ?? 2
})

const createHomestayDraft = () => ({
  name: '新院样板房',
  city: '杭州',
  district: '西湖区',
  address: '龙井路 88 号',
  basePrice: 468,
  houseType: '庭院民宿',
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
    createRoomItem({ roomNo: 'D101', roomType: '大床房', floorNo: 1, price: 468, bedCount: 1, capacity: 2 }),
    createRoomItem({ roomNo: 'D102', roomType: '双床房', floorNo: 1, price: 498, bedCount: 2, capacity: 2 })
  ]
})

const homestayForm = reactive(createHomestayDraft())
const calendarQuery = reactive({
  homestayId: null,
  startDate: today,
  days: 7
})

const applyHomestayDraft = (draft) => {
  const source = draft || createHomestayDraft()
  homestayForm.name = source.name
  homestayForm.city = source.city
  homestayForm.district = source.district || ''
  homestayForm.address = source.address
  homestayForm.basePrice = Number(source.basePrice)
  homestayForm.houseType = source.houseType
  homestayForm.tags = source.tags
  homestayForm.facilities = source.facilities
  homestayForm.latitude = Number(source.latitude)
  homestayForm.longitude = Number(source.longitude)
  homestayForm.coverImage = source.coverImage
  homestayForm.summary = source.summary
  homestayForm.description = source.description
  homestayForm.images.splice(0, homestayForm.images.length, ...(source.images?.length ? source.images : ['']))
  homestayForm.rooms.splice(
    0,
    homestayForm.rooms.length,
    ...((source.rooms?.length ? source.rooms : [createRoomItem()]).map((room) => createRoomItem(room)))
  )
}

const handleUploadSuccess = (response, type, index = null) => {
  const isOk = response?.success === true || response?.code === 200
  const url = response?.data

  if (isOk && url) {
    if (type === 'cover') {
      homestayForm.coverImage = url
    } else if (type === 'images' && index !== null) {
      homestayForm.images[index] = url
    } else if (type === 'banner' && index !== null) {
      banners.value[index].imageUrl = url
    }
    ElMessage.success('图片上传成功')
    return
  }

  if (url) {
    if (type === 'cover') {
      homestayForm.coverImage = url
    } else if (type === 'images' && index !== null) {
      homestayForm.images[index] = url
    } else if (type === 'banner' && index !== null) {
      banners.value[index].imageUrl = url
    }
    ElMessage.success('图片上传成功')
    return
  }

  ElMessage.error(response?.message || '上传失败')
}

// 地图拾取相关
const showMapPicker = ref(false)
let mapInstance = null
let markerInstance = null

const openMapPicker = () => {
  showMapPicker.value = true
  initMap()
}

const initMap = () => {
  // 设置高德地图安全密钥 (使用用户提供的真实密钥)
  window._AMapSecurityConfig = {
    securityJsCode: 'bff902e5c7e910511640421729ec30a1' 
  }

  // 动态加载高德地图脚本
  if (window.AMap) {
    nextTick(() => renderMap())
    return
  }
  
  const script = document.createElement('script')
  script.src = 'https://webapi.amap.com/maps?v=2.0&key=1fe4fae05e875f7e4923b0258755942d' 
  script.onload = () => renderMap()
  document.head.appendChild(script)
}

const renderMap = () => {
  const center = homestayForm.longitude && homestayForm.latitude 
    ? [homestayForm.longitude, homestayForm.latitude] 
    : [116.397428, 39.90923] // 默认北京
    
  mapInstance = new window.AMap.Map('map-container', {
    zoom: 13,
    center: center
  })
  
  markerInstance = new window.AMap.Marker({
    position: center,
    map: mapInstance
  })
  
  mapInstance.on('click', (e) => {
    const lnglat = e.lnglat
    homestayForm.longitude = Number(lnglat.lng.toFixed(6))
    homestayForm.latitude = Number(lnglat.lat.toFixed(6))
    markerInstance.setPosition(lnglat)
    ElMessage.info(`已拾取坐标: ${homestayForm.longitude}, ${homestayForm.latitude}`)
  })
}

const normalizePayload = () => {
  const images = homestayForm.images.map((item) => item.trim()).filter(Boolean)
  const rooms = homestayForm.rooms
    .map((room) => ({
      id: room.id || null,
      roomNo: room.roomNo.trim(),
      roomType: room.roomType.trim(),
      floorNo: Number(room.floorNo),
      price: Number(room.price),
      bedCount: Number(room.bedCount),
      capacity: Number(room.capacity)
    }))
    .filter((room) => room.roomNo && room.roomType)

  if (!images.length) {
    ElMessage.warning('请至少填写一张房源图片')
    return null
  }
  if (!rooms.length) {
    ElMessage.warning('请至少配置一个房间')
    return null
  }

  return {
    name: homestayForm.name.trim(),
    city: homestayForm.city.trim(),
    district: homestayForm.district.trim(),
    address: homestayForm.address.trim(),
    basePrice: Number(homestayForm.basePrice),
    houseType: homestayForm.houseType.trim(),
    tags: homestayForm.tags.trim(),
    facilities: homestayForm.facilities.trim(),
    latitude: Number(homestayForm.latitude),
    longitude: Number(homestayForm.longitude),
    coverImage: homestayForm.coverImage.trim(),
    summary: homestayForm.summary.trim(),
    description: homestayForm.description.trim(),
    images,
    rooms
  }
}

const decorateCalendar = (data) => {
  return {
    ...data,
    rooms: (data.rooms || []).map((room) => ({
      ...room,
      slotMap: Object.fromEntries((room.slots || []).map((slot) => [slot.date, slot]))
    }))
  }
}

const renderCharts = () => {
  if (chartRef.value) {
    echarts.getInstanceByDom(chartRef.value)?.dispose()
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
    echarts.getInstanceByDom(pieRef.value)?.dispose()
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

const setActiveSection = async (section, replace = false) => {
  const normalized = normalizeSection(section)
  activeSection.value = normalized
  await router[replace ? 'replace' : 'push']({
    path: '/admin',
    query: { section: normalized }
  })
  if (normalized === 'dashboard') {
    await nextTick()
    renderCharts()
  }
}

const loadCalendar = async (forceHomestayId) => {
  const homestayId = forceHomestayId || calendarQuery.homestayId
  if (!homestayId) {
    calendar.value = { homestayName: '', dates: [], rooms: [] }
    return
  }
  calendarQuery.homestayId = homestayId
  const result = await http.get(`/admin/homestays/${homestayId}/calendar`, {
    params: {
      startDate: calendarQuery.startDate,
      days: calendarQuery.days
    }
  })
  calendar.value = decorateCalendar(result.data)
}

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

  if (!calendarQuery.homestayId && homestays.value.length) {
    calendarQuery.homestayId = homestays.value[0].id
  }

  if (activeSection.value === 'dashboard') {
    await nextTick()
    renderCharts()
  }
  if (calendarQuery.homestayId) {
    await loadCalendar(calendarQuery.homestayId)
  }
}

const submitHomestay = async () => {
  const payload = normalizePayload()
  if (!payload) return
  const isHost = authStore.user?.role === 'HOST'

  try {
    if (editingHomestayId.value) {
      await http.put(`/admin/homestays/${editingHomestayId.value}`, payload)
      ElMessage.success(isHost ? '修改已提交审核' : '房源已更新')
    } else {
      const result = await http.post('/admin/homestays', payload)
      ElMessage.success(isHost ? '已提交审核' : '房源发布成功')
      calendarQuery.homestayId = result.data.id
    }

    editingHomestayId.value = null
    showHomestayEditor.value = false
    applyHomestayDraft(createHomestayDraft())
    await loadAll()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const openCreateHomestay = () => {
  editingHomestayId.value = null
  showHomestayEditor.value = true
  applyHomestayDraft(createHomestayDraft())
}

const editHomestay = async (id) => {
  const result = await http.get(`/admin/homestays/${id}`)
  editingHomestayId.value = id
  showHomestayEditor.value = true
  applyHomestayDraft(result.data)
  await setActiveSection('homestays')
}

const cancelEdit = () => {
  editingHomestayId.value = null
  showHomestayEditor.value = false
  applyHomestayDraft(createHomestayDraft())
}

const toggleHomestayStatus = async (row) => {
  try {
    await http.post(`/admin/homestays/${row.id}/toggle-status`)
    if (row.status === 'DRAFT') {
      ElMessage.success('审核已通过，房源已上架')
    } else {
      ElMessage.success(row.status === 'ONLINE' ? '房源已下架' : '房源已上架')
    }
    await loadAll()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const deleteHomestay = async (row) => {
  try {
    await ElMessageBox.confirm(`确认删除房源“${row.name}”吗？`, '删除房源', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/admin/homestays/${row.id}`)
    ElMessage.success('房源已删除')
    if (editingHomestayId.value === row.id) {
      cancelEdit()
    }
    if (calendarQuery.homestayId === row.id) {
      calendarQuery.homestayId = homestays.value.find((item) => item.id !== row.id)?.id || null
    }
    await loadAll()
  } catch {
    // User canceled the confirmation dialog.
  }
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

const exportOrders = async () => {
  try {
    const blob = await http.get('/admin/orders/export', { responseType: 'blob' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    link.download = '订单列表.xls'
    document.body.appendChild(link)
    link.click()
    link.remove()
    window.URL.revokeObjectURL(url)
    ElMessage.success('订单已导出')
  } catch (error) {
    ElMessage.error(error?.message || '导出失败')
  }
}

const canConfirmOrder = (order) =>
  authStore.user?.role === 'HOST' && order?.orderStatus === 'PAID'
const canRefundOrder = (order) =>
  authStore.user?.role === 'HOST' && order?.orderStatus === 'REFUND_REQUESTED'

const toggleUser = async (id, type) => {
  await http.post(`/admin/users/${id}/${type}`)
  ElMessage.success('用户状态已更新')
  await loadAll()
}

const loadHostApplications = async () => {
  if (authStore.user?.role !== 'ADMIN') return
  const result = await http.get('/admin/host-applications')
  hostApplications.value = result.data
}

const approveHostApplication = async (row) => {
  try {
    await http.post(`/admin/host-applications/${row.id}/approve`)
    ElMessage.success('审核通过')
    await loadHostApplications()
    await loadAll()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const rejectHostApplication = async (row) => {
  try {
    await http.post(`/admin/host-applications/${row.id}/reject`)
    ElMessage.success('已拒绝')
    await loadHostApplications()
  } catch (error) {
    ElMessage.error(error?.message || '操作失败')
  }
}

const replyReview = async (review) => {
  try {
    const { value } = await ElMessageBox.prompt('输入房东回复内容', '回复评论', {
      inputValue: review.replyContent || '',
      confirmButtonText: '提交回复',
      cancelButtonText: '取消'
    })
    await http.post(`/admin/reviews/${review.id}/reply`, { replyContent: value })
    ElMessage.success('回复成功')
    await loadAll()
  } catch {
    // User canceled the prompt dialog.
  }
}

const hideReview = async (review) => {
  try {
    await ElMessageBox.confirm('确认隐藏这条评论吗？', '评论管理', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.post(`/admin/reviews/${review.id}/hide`)
    ElMessage.success('评论已隐藏')
    await loadAll()
  } catch {
    // User canceled the confirmation dialog.
  }
}

const addImage = () => {
  homestayForm.images.push('')
}

const removeImage = (index) => {
  if (homestayForm.images.length === 1) return
  homestayForm.images.splice(index, 1)
}

const addRoom = () => {
  homestayForm.rooms.push(createRoomItem())
}

const removeRoom = (index) => {
  if (homestayForm.rooms.length === 1) return
  homestayForm.rooms.splice(index, 1)
}

const openCalendar = async (id) => {
  calendarQuery.homestayId = id
  await setActiveSection('calendar')
  await loadCalendar(id)
}

const handleTodoClick = () => {
  if (dashboard.value.todos?.pendingOrders > 0) {
    setActiveSection('orders')
  } else if (dashboard.value.todos?.newComments > 0) {
    setActiveSection('reviews')
  } else {
    setActiveSection('orders')
  }
}

const shortOrderNo = (orderNo) => {
  if (!orderNo) return ''
  return orderNo.length > 8 ? orderNo.slice(-8) : orderNo
}

const formatDateTime = (value) => {
  if (!value) return ''
  const text = String(value)
  if (text.includes('T')) {
    return text.replace('T', ' ').slice(0, 19)
  }
  return text.length > 19 ? text.slice(0, 19) : text
}

const formatHomestayStatus = (status) => homestayStatusTextMap[status] || status
const formatOrderStatus = (status) => orderStatusTextMap[status] || status
const formatRole = (role) => roleTextMap[role] || role
const formatHostApplyStatus = (status) => hostApplyStatusTextMap[status] || status

const isPastDate = (date) => typeof date === 'string' && date < today

const calendarCellClass = (row, date) => {
  if (row.slotMap?.[date]?.occupied) {
    return 'occupied'
  }
  if (isPastDate(date)) {
    return 'unavailable'
  }
  return 'free'
}

const calendarCellTitle = (row, date) => {
  if (row.slotMap?.[date]?.occupied) {
    return '已订'
  }
  return isPastDate(date) ? '不可预订' : '空闲'
}

const calendarCellSubtitle = (row, date) => {
  if (row.slotMap?.[date]?.occupied) {
    return shortOrderNo(row.slotMap[date].orderNo)
  }
  return isPastDate(date) ? '已过期' : '可预订'
}

const homestayActionLabel = (row) => {
  if (row.status === 'DRAFT') {
    return authStore.user?.role === 'ADMIN' ? '审核通过' : '待审核'
  }
  return row.status === 'ONLINE' ? '下架' : '上架'
}

const canToggleHomestayStatus = (row) => {
  if (row.status === 'DRAFT') {
    return authStore.user?.role === 'ADMIN'
  }
  return true
}

// Settings Management
const banners = ref([])
const notices = ref([])
const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const loadSettings = async () => {
  const result = await http.get('/admin/settings')
  banners.value = result.data.banners || []
  notices.value = result.data.notices || []
}

const addBanner = () => {
  banners.value.push({ title: '', imageUrl: '', linkUrl: '', sortOrder: banners.value.length + 1, enabled: true })
}

const removeBanner = (index) => {
  banners.value.splice(index, 1)
}

const saveBanners = async () => {
  await http.put('/admin/banners', banners.value)
  ElMessage.success('轮播图配置已保存')
  await loadSettings()
}

const addNotice = () => {
  notices.value.push({ title: '', content: '', published: true })
}

const removeNotice = (index) => {
  notices.value.splice(index, 1)
}

const saveNotices = async () => {
  await http.put('/admin/notices', notices.value)
  ElMessage.success('公告发布已同步')
  await loadSettings()
}

const changePassword = async () => {
  if (passwordForm.newPassword !== passwordForm.confirmPassword) {
    ElMessage.error('两次输入的新密码不一致')
    return
  }
  await http.post('/admin/password', {
    oldPassword: passwordForm.oldPassword,
    newPassword: passwordForm.newPassword
  })
  ElMessage.success('密码修改成功，请妥善保管')
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
}

watch(
  () => route.query.section,
  async (section) => {
    const normalized = normalizeSection(section)
    activeSection.value = normalized
    if (normalized !== 'homestays') {
      showHomestayEditor.value = false
      editingHomestayId.value = null
    }
    if (normalized === 'dashboard') {
      await nextTick()
      renderCharts()
    }
    if (normalized === 'host-approvals') {
      await loadHostApplications()
    }
    if (normalized === 'settings') {
      await loadSettings()
    }
  },
  { immediate: true }
)

onMounted(loadAll)
</script>

<template>
  <div v-if="authStore.isLoggedIn && authStore.user?.role !== 'USER'" class="admin-shell">
    <aside class="panel admin-sidebar">
      <div class="admin-sidebar-header">
        <h3 style="margin: 0;">后台菜单</h3>
        <p class="muted">按功能分类切换，不再把所有内容堆在首页</p>
      </div>
      <button
        v-for="item in adminSections"
        :key="item.key"
        :class="['admin-nav-btn', { active: activeSection === item.key }]"
        @click="setActiveSection(item.key)"
      >
        <strong>{{ item.label }}</strong>
        <small>{{ item.desc }}</small>
      </button>
    </aside>

    <div class="content-grid admin-main">
      <section v-if="activeSection === 'dashboard'" class="panel admin-dashboard-panel admin-module">
        <div class="section-title admin-section-head">
          <div>
            <h2 style="margin: 0;">后台看板</h2>
            <p class="muted">今日订单、今日销售额、总销售额、新增用户数、近 7 日趋势</p>
          </div>
        </div>
        <div class="admin-metrics">
          <div class="metric admin-kpi-card clickable" @click="setActiveSection('orders')">
            <span class="muted">今日订单数</span>
            <strong>{{ dashboard.todayOrders || 0 }}</strong>
          </div>
          <div class="metric admin-kpi-card clickable" @click="setActiveSection('orders')">
            <span class="muted">今日销售额</span>
            <strong>￥{{ dashboard.todaySales || 0 }}</strong>
          </div>
          <div class="metric admin-kpi-card clickable" @click="setActiveSection('orders')">
            <span class="muted">总销售额</span>
            <strong>￥{{ dashboard.totalSales || 0 }}</strong>
          </div>
          <div class="metric admin-kpi-card clickable" @click="setActiveSection('users')">
            <span class="muted">新增用户数</span>
            <strong>{{ dashboard.newUsers || 0 }}</strong>
          </div>
          <div class="metric admin-kpi-card clickable" @click="handleTodoClick">
            <span class="muted">待办提醒</span>
            <strong>{{ dashboard.todos?.pendingOrders || 0 }} / {{ dashboard.todos?.newComments || 0 }}</strong>
          </div>
        </div>
        <div class="admin-chart-grid">
          <div class="panel chart-panel admin-chart-card">
            <h3 style="margin-top: 0;">近 7 日订单趋势</h3>
            <div ref="chartRef" class="admin-chart-canvas"></div>
          </div>
          <div class="panel chart-panel admin-chart-card">
            <h3 style="margin-top: 0;">房源类型占比</h3>
            <div ref="pieRef" class="admin-chart-canvas"></div>
          </div>
        </div>
      </section>

      <section v-if="activeSection === 'homestays'" class="content-grid">
        <div class="panel table-panel admin-table-card admin-module">
          <div class="section-title admin-section-head">
            <div>
              <h3 style="margin: 0;">房源管理</h3>
              <p class="muted">先查看列表，再通过新增按钮录入房源信息</p>
            </div>
            <div class="chip-list">
              <el-button type="primary" color="#b5653b" @click="openCreateHomestay">新增房源</el-button>
            </div>
          </div>
          <el-table :data="homestays" max-height="440" stripe>
            <el-table-column prop="name" label="房源" min-width="160" />
            <el-table-column prop="totalRooms" label="房间数" width="90" />
            <el-table-column prop="status" label="状态" width="110">
              <template #default="{ row }">
                <el-tag :type="row.status === 'ONLINE' ? 'success' : row.status === 'DRAFT' ? 'warning' : 'info'">
                  {{ formatHomestayStatus(row.status) }}
                </el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" min-width="280">
              <template #default="{ row }">
                <div class="chip-list">
                  <el-button size="small" @click="editHomestay(row.id)">编辑</el-button>
                  <el-button size="small" @click="openCalendar(row.id)">房态</el-button>
                  <el-button
                    size="small"
                    type="warning"
                    :disabled="!canToggleHomestayStatus(row)"
                    @click="toggleHomestayStatus(row)"
                  >
                    {{ homestayActionLabel(row) }}
                  </el-button>
                  <el-button size="small" type="danger" plain @click="deleteHomestay(row)">删除</el-button>
                </div>
              </template>
            </el-table-column>
          </el-table>
        </div>

        <el-dialog
          v-model="showHomestayEditor"
          :title="editingHomestayId ? '编辑房源' : '新增房源'"
          width="900px"
          destroy-on-close
          top="5vh"
        >
          <div class="admin-form-container">
            <p class="muted" style="margin-bottom: 20px;">
              房东可维护房源的基本信息、多张展示图片以及具体房间库存。
            </p>

            <el-form label-position="top">
              <div class="two-cols">
                <el-form-item label="名称"><el-input v-model="homestayForm.name" /></el-form-item>
                <el-form-item label="城市"><el-input v-model="homestayForm.city" /></el-form-item>
                <el-form-item label="区县"><el-input v-model="homestayForm.district" /></el-form-item>
                <el-form-item label="地址"><el-input v-model="homestayForm.address" /></el-form-item>
                <el-form-item label="基础价格"><el-input-number v-model="homestayForm.basePrice" class="full-width" /></el-form-item>
                <el-form-item label="房型"><el-input v-model="homestayForm.houseType" /></el-form-item>
                <el-form-item label="位置坐标" class="full-row">
                  <div class="latlng-picker-group">
                    <div class="latlng-inputs">
                      <el-input-number v-model="homestayForm.latitude" :precision="6" :step="0.0001" placeholder="纬度" />
                      <el-input-number v-model="homestayForm.longitude" :precision="6" :step="0.0001" placeholder="经度" />
                    </div>
                    <el-button type="primary" plain @click="openMapPicker">
                      <el-icon style="margin-right: 4px;"><Location /></el-icon>
                      地图拾取坐标
                    </el-button>
                  </div>
                </el-form-item>
              </div>
              <el-form-item label="标签"><el-input v-model="homestayForm.tags" placeholder="用逗号分隔" /></el-form-item>
              <el-form-item label="设施"><el-input v-model="homestayForm.facilities" placeholder="用逗号分隔" /></el-form-item>
              <el-form-item label="封面图">
                <el-upload
                  class="avatar-uploader"
                  action="/api/public/upload"
                  :show-file-list="false"
                  :on-success="(res) => handleUploadSuccess(res, 'cover')"
                  name="file"
                >
                  <img v-if="homestayForm.coverImage" :src="homestayForm.coverImage" class="avatar" />
                  <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
                </el-upload>
                <div v-if="homestayForm.coverImage" class="image-url-tag">
                  <el-tag size="small" closable @close="homestayForm.coverImage = ''">已上传</el-tag>
                </div>
              </el-form-item>
              <el-form-item label="摘要"><el-input v-model="homestayForm.summary" /></el-form-item>
              <el-form-item label="详情"><el-input v-model="homestayForm.description" type="textarea" :rows="3" /></el-form-item>

              <div class="section-title" style="margin-top: 8px;">
                <h4 style="margin: 0;">房源图片</h4>
                <el-button size="small" plain @click="addImage">新增图片</el-button>
              </div>
              <div class="admin-inline-grid" style="margin-top: 10px;">
                <div v-for="(image, index) in homestayForm.images" :key="`image-${index}`" class="editable-row-image">
                  <el-upload
                    class="image-uploader-small"
                    action="/api/public/upload"
                    :show-file-list="false"
                    :on-success="(res) => handleUploadSuccess(res, 'images', index)"
                    name="file"
                  >
                    <img v-if="homestayForm.images[index]" :src="homestayForm.images[index]" class="uploader-img-small" />
                    <el-icon v-else class="uploader-icon-small"><Plus /></el-icon>
                  </el-upload>
                  <div class="image-action-group">
                    <el-button circle type="danger" plain :disabled="homestayForm.images.length === 1" @click="removeImage(index)">-</el-button>
                  </div>
                </div>
              </div>

              <div class="section-title" style="margin-top: 18px;">
                <h4 style="margin: 0;">房间库存</h4>
                <el-button size="small" plain @click="addRoom">新增房间</el-button>
              </div>
              <div class="room-editor-grid">
                <div v-for="(room, index) in homestayForm.rooms" :key="room.id || `room-${index}`" class="panel room-card">
                  <div class="spaced" style="margin-bottom: 10px;">
                    <strong>房间 {{ index + 1 }}</strong>
                    <el-button size="small" type="danger" plain :disabled="homestayForm.rooms.length === 1" @click="removeRoom(index)">删除</el-button>
                  </div>
                  <div class="two-cols">
                    <el-form-item label="房号"><el-input v-model="room.roomNo" /></el-form-item>
                    <el-form-item label="房型"><el-input v-model="room.roomType" /></el-form-item>
                    <el-form-item label="楼层"><el-input-number v-model="room.floorNo" class="full-width" /></el-form-item>
                    <el-form-item label="价格"><el-input-number v-model="room.price" class="full-width" /></el-form-item>
                    <el-form-item label="床位数"><el-input-number v-model="room.bedCount" class="full-width" /></el-form-item>
                    <el-form-item label="可住人数"><el-input-number v-model="room.capacity" class="full-width" /></el-form-item>
                  </div>
                </div>
              </div>
            </el-form>
          </div>
          <template #footer>
            <div class="dialog-footer">
              <el-button @click="cancelEdit">取消</el-button>
              <el-button type="primary" color="#b5653b" @click="submitHomestay">
                {{ editingHomestayId ? '保存更新' : '确认发布' }}
              </el-button>
            </div>
          </template>
        </el-dialog>
      </section>

      <section v-if="activeSection === 'calendar'" class="panel table-panel admin-module">
        <div class="section-title admin-section-head">
          <div>
            <h3 style="margin: 0;">房态面板</h3>
            <p class="muted">按房间号查看某时间段的占用情况，避免超售</p>
          </div>
        </div>

        <div class="calendar-toolbar">
          <el-select v-model="calendarQuery.homestayId" placeholder="选择房源" style="width: 240px;">
            <el-option v-for="item in homestays" :key="item.id" :label="item.name" :value="item.id" />
          </el-select>
          <el-date-picker v-model="calendarQuery.startDate" value-format="YYYY-MM-DD" type="date" />
          <el-select v-model="calendarQuery.days" style="width: 140px;">
            <el-option :value="7" label="7 天" />
            <el-option :value="10" label="10 天" />
            <el-option :value="14" label="14 天" />
          </el-select>
          <el-button type="primary" color="#5b8870" @click="loadCalendar()">查询房态</el-button>
        </div>

        <div class="calendar-legend">
          <span class="legend-pill free">空闲</span>
          <span class="legend-pill occupied">已订</span>
          <span class="legend-pill unavailable">不可预订</span>
          <span class="muted" v-if="calendar.homestayName">当前房源：{{ calendar.homestayName }}</span>
        </div>

        <el-table v-if="calendar.rooms.length" :data="calendar.rooms" border max-height="420">
          <el-table-column fixed prop="roomNo" label="房号" width="96" />
          <el-table-column fixed prop="roomType" label="房型" width="120" />
          <el-table-column label="近期订单" min-width="220">
            <template #default="{ row }">
              <div class="calendar-bookings">
                <span v-if="!row.bookings.length" class="muted">当前区间暂无占用</span>
                <span v-for="item in row.bookings" :key="item.orderNo" class="booking-pill">
                  {{ item.checkInDate }} - {{ item.checkOutDate }} {{ item.guestName }}
                </span>
              </div>
            </template>
          </el-table-column>
          <el-table-column v-for="date in calendar.dates" :key="date" :label="date" min-width="108">
            <template #default="{ row }">
              <div :class="['calendar-cell', calendarCellClass(row, date)]">
                <strong>{{ calendarCellTitle(row, date) }}</strong>
                <small>{{ calendarCellSubtitle(row, date) }}</small>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state">选择房源后可以查看每个房号在指定日期段内的占用状态。</div>
      </section>

      <section v-if="activeSection === 'orders'" class="panel table-panel admin-module">
        <div class="section-title admin-section-head">
          <div>
            <h3 style="margin: 0;">订单管理</h3>
            <p class="muted">支持确认入住、退款处理、导出订单</p>
          </div>
          <button class="outline-btn" type="button" @click="exportOrders">导出订单</button>
        </div>
        <el-table :data="orders" stripe>
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column label="下单时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column prop="username" label="用户" width="100" />
          <el-table-column prop="homestayName" label="房源" min-width="140" />
          <el-table-column prop="totalAmount" label="金额" width="100" />
          <el-table-column label="订单状态" width="120">
            <template #default="{ row }">{{ formatOrderStatus(row.orderStatus) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <div class="chip-list">
                <el-button v-if="canConfirmOrder(row)" size="small" type="primary" @click="confirmOrder(row.id)">确认</el-button>
                <el-button v-if="canRefundOrder(row)" size="small" @click="refundOrder(row.id)">同意退款</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="activeSection === 'reviews'" class="panel table-panel admin-module">
        <div class="section-title admin-section-head">
          <div>
            <h3 style="margin: 0;">评论管理</h3>
            <p class="muted">集中查看评论、回复内容和隐藏操作</p>
          </div>
        </div>
        <el-table :data="reviews" stripe>
          <el-table-column prop="nickname" label="用户" width="120" />
          <el-table-column prop="homestayName" label="房源" min-width="160" />
          <el-table-column prop="score" label="评分" width="90" />
          <el-table-column prop="content" label="评论内容" min-width="220" />
          <el-table-column prop="replyContent" label="回复内容" min-width="220">
            <template #default="{ row }">{{ row.replyContent || '暂未回复' }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="180">
            <template #default="{ row }">
              <div class="chip-list">
                <el-button size="small" type="primary" @click="replyReview(row)">回复</el-button>
                <el-button size="small" type="danger" plain @click="hideReview(row)">隐藏</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </section>

      <section v-if="activeSection === 'host-approvals' && authStore.user?.role === 'ADMIN'" class="panel table-panel admin-module">
        <div class="section-title admin-section-head">
          <div>
            <h3 style="margin: 0;">房东审核</h3>
            <p class="muted">审核房东入驻申请，审核通过后自动生成房东账号</p>
          </div>
        </div>
        <el-table v-if="hostApplications.length" :data="hostApplications" stripe>
          <el-table-column prop="username" label="账号" width="140" />
          <el-table-column prop="nickname" label="昵称" width="140" />
          <el-table-column prop="phone" label="手机号" width="140" />
          <el-table-column label="状态" width="120">
            <template #default="{ row }">
              <el-tag :type="row.status === 'PENDING' ? 'warning' : row.status === 'APPROVED' ? 'success' : 'danger'">
                {{ formatHostApplyStatus(row.status) }}
              </el-tag>
            </template>
          </el-table-column>
          <el-table-column label="申请时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="审核时间" min-width="170">
            <template #default="{ row }">{{ formatDateTime(row.reviewedAt) || '-' }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="180">
            <template #default="{ row }">
              <div class="chip-list">
                <el-button size="small" type="primary" :disabled="row.status !== 'PENDING'" @click="approveHostApplication(row)">
                  通过
                </el-button>
                <el-button size="small" type="danger" plain :disabled="row.status !== 'PENDING'" @click="rejectHostApplication(row)">
                  拒绝
                </el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
        <div v-else class="empty-state">暂无房东入驻申请</div>
      </section>

      <section v-if="activeSection === 'users' && authStore.user?.role === 'ADMIN'" class="panel table-panel admin-module">
        <div class="section-title admin-section-head">
          <div>
            <h3 style="margin: 0;">用户管理</h3>
            <p class="muted">支持禁用/启用与黑名单管理</p>
          </div>
        </div>
        <el-table :data="users" stripe>
          <el-table-column prop="username" label="账号" width="120" />
          <el-table-column prop="nickname" label="昵称" width="120" />
          <el-table-column label="角色" width="100">
            <template #default="{ row }">{{ formatRole(row.role) }}</template>
          </el-table-column>
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
                  <el-button size="small" @click="toggleUser(row.id, 'toggle-enabled')">
                    {{ row.enabled ? '切换禁用' : '切换启用' }}
                  </el-button>
                  <el-button size="small" type="danger" plain @click="toggleUser(row.id, 'toggle-blacklist')">
                    {{ row.blacklisted ? '移除黑名单' : '切换黑名单' }}
                  </el-button>
                </div>
              </template>
            </el-table-column>
        </el-table>
      </section>

      <section v-if="activeSection === 'settings'" class="content-grid">
        <div class="panel admin-module settings-panel">
          <div class="admin-section-head">
            <h2 style="margin: 0;">系统设置</h2>
            <p class="muted">配置首页轮播图、系统公告及修改管理员密码</p>
          </div>

          <el-tabs type="border-card" style="margin-top: 20px;">
            <el-tab-pane label="轮播图配置">
              <div class="settings-tab-header">
                <h4>首页轮播图</h4>
                <el-button type="primary" color="#b5653b" plain size="small" @click="addBanner">新增轮播图</el-button>
              </div>
              <div class="banner-list admin-inline-grid">
                <div v-for="(banner, index) in banners" :key="index" class="panel setting-item">
                  <div class="spaced">
                    <strong>轮播项 {{ index + 1 }}</strong>
                    <el-button type="danger" link @click="removeBanner(index)">删除</el-button>
                  </div>
                  <div class="two-cols" style="margin-top: 10px;">
                    <el-form-item label="标题"><el-input v-model="banner.title" /></el-form-item>
                    <el-form-item label="排序"><el-input-number v-model="banner.sortOrder" :min="1" /></el-form-item>
                    <el-form-item label="轮播图片" class="full-row">
                      <div class="setting-item-body" style="display: flex; gap: 16px; align-items: center;">
                        <el-upload
                          class="banner-uploader"
                          action="/api/public/upload"
                          :show-file-list="false"
                          :on-success="(res) => handleUploadSuccess(res, 'banner', index)"
                          name="file"
                        >
                          <img v-if="banner.imageUrl" :src="banner.imageUrl" class="banner-preview" />
                          <el-icon v-else class="banner-uploader-icon"><Plus /></el-icon>
                        </el-upload>
                        <el-tag v-if="banner.imageUrl" type="success" size="small">已就绪</el-tag>
                      </div>
                    </el-form-item>
                    <el-form-item label="跳转链接" class="full-row"><el-input v-model="banner.linkUrl" /></el-form-item>
                  </div>
                </div>
              </div>
              <div class="settings-actions">
                <el-button type="primary" color="#b5653b" @click="saveBanners">同步轮播图配置</el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="公告发布">
              <div class="settings-tab-header">
                <h4>系统公告</h4>
                <el-button type="primary" color="#b5653b" plain size="small" @click="addNotice">新增公告</el-button>
              </div>
              <div class="notice-list admin-inline-grid">
                <div v-for="(notice, index) in notices" :key="index" class="panel setting-item">
                  <div class="spaced">
                    <strong>公告 {{ index + 1 }}</strong>
                    <el-button type="danger" link @click="removeNotice(index)">删除</el-button>
                  </div>
                  <div class="two-cols" style="margin-top: 10px;">
                    <el-form-item label="标题" class="full-row"><el-input v-model="notice.title" /></el-form-item>
                    <el-form-item label="内容" class="full-row">
                      <el-input v-model="notice.content" type="textarea" :rows="3" />
                    </el-form-item>
                    <el-form-item label="已发布">
                      <el-switch v-model="notice.published" />
                    </el-form-item>
                  </div>
                </div>
              </div>
              <div class="settings-actions">
                <el-button type="primary" color="#b5653b" @click="saveNotices">同步公告发布</el-button>
              </div>
            </el-tab-pane>

            <el-tab-pane label="管理员密码修改">
              <div class="settings-tab-header">
                <h4>安全设置</h4>
              </div>
              <div class="password-form-container" style="max-width: 400px; margin: 20px auto;">
                <el-form label-position="top">
                  <el-form-item label="原密码">
                    <el-input v-model="passwordForm.oldPassword" type="password" show-password />
                  </el-form-item>
                  <el-form-item label="新密码">
                    <el-input v-model="passwordForm.newPassword" type="password" show-password />
                  </el-form-item>
                  <el-form-item label="确认新密码">
                    <el-input v-model="passwordForm.confirmPassword" type="password" show-password />
                  </el-form-item>
                  <div style="margin-top: 30px; text-align: center;">
                    <el-button type="primary" color="#b5653b" style="width: 100%;" @click="changePassword">确认修改密码</el-button>
                  </div>
                </el-form>
              </div>
            </el-tab-pane>
          </el-tabs>
        </div>
      </section>
    </div>
  </div>

  <div v-else class="panel empty-state">
    只有房东或管理员可以进入后台，请先使用 `host` 或 `admin` 账号登录。
  </div>

  <!-- 地图坐标拾取器 -->
  <el-dialog
    v-model="showMapPicker"
    title="地图坐标拾取 (点击地图选择位置)"
    width="800px"
    append-to-body
  >
    <div id="map-container" class="picker-map-box"></div>
    <template #footer>
      <div class="dialog-footer" style="padding: 10px 0;">
        <div class="picked-values" v-if="homestayForm.latitude">
          已选坐标: <strong>{{ homestayForm.longitude }}, {{ homestayForm.latitude }}</strong>
        </div>
        <el-button @click="showMapPicker = false">取消</el-button>
        <el-button type="primary" @click="showMapPicker = false">确定使用此坐标</el-button>
      </div>
    </template>
  </el-dialog>
</template>

<style scoped>
.admin-kpi-card.clickable {
  cursor: pointer;
  transition: all 0.2s ease;
}

.admin-kpi-card.clickable:hover {
  transform: translateY(-4px);
  box-shadow: 0 4px 12px rgba(181, 101, 59, 0.15);
  border-color: #b5653b;
  background-color: #fefaf6;
}

.room-editor-grid {
  display: flex;
  flex-direction: column;
  gap: 16px;
  margin-top: 12px;
}

.room-card {
  padding: 16px;
  background: #fcfcfc;
  border: 1px solid #eee;
  border-radius: 8px;
}

.dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding-top: 20px;
  border-top: 1px solid #f0f0f0;
}

.settings-tab-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 20px;
}

.settings-tab-header h4 {
  margin: 0;
  color: #b5653b;
}

.setting-item {
  padding: 16px;
  background: #fdfdfd;
  border: 1px solid #eee;
}

.settings-actions {
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px dashed #eee;
  text-align: right;
}

.password-form-container {
  padding: 30px;
  background: #fdfaf6;
  border-radius: 12px;
  border: 1px solid #f2e6db;
}

/* 上传组件样式 */
.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 8px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 178px;
  height: 178px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.3s;
}
.avatar-uploader:hover {
  border-color: #b5653b;
}
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  text-align: center;
}
.avatar {
  width: 178px;
  height: 178px;
  display: block;
  object-fit: cover;
}

.editable-row-image {
  display: grid;
  grid-template-columns: auto 1fr auto;
  gap: 12px;
  align-items: center;
  background: #f9f9f9;
  padding: 10px;
  border-radius: 8px;
  border: 1px solid #eee;
}
.image-uploader-small {
  border: 1px dashed #ccc;
  border-radius: 4px;
  width: 60px;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
}
.uploader-img-small {
  width: 60px;
  height: 60px;
  object-fit: cover;
}
.uploader-icon-small {
  font-size: 20px;
  color: #999;
}

.banner-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  width: 240px;
  height: 120px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}
.banner-preview {
  width: 240px;
  height: 120px;
  object-fit: cover;
}
.banner-uploader-icon {
  font-size: 24px;
  color: #8c939d;
}

.image-url-tag {
  margin-top: 8px;
}

.image-action-group {
  margin-left: auto;
}

.latlng-picker-group {
  display: flex;
  gap: 12px;
  align-items: center;
}
.latlng-inputs {
  display: flex;
  gap: 8px;
  flex: 1;
}
.latlng-inputs :deep(.el-input-number) {
  flex: 1;
}

.picker-map-box {
  width: 100%;
  height: 450px;
  border-radius: 8px;
  border: 1px solid #ddd;
}
.picked-values {
  margin-right: auto;
  color: #666;
}

.settings-panel {
  padding: 24px;
}

.settings-panel .admin-section-head h2 {
  margin: 0;
}

.settings-panel .admin-section-head p {
  margin: 6px 0 0;
  line-height: 1.6;
}
</style>

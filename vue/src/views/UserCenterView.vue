<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const authStore = useAuthStore()
const profile = reactive({ nickname: '', avatar: '', phone: '' })
const orders = ref([])
const favorites = ref([])
const reviewForm = reactive({ orderId: null, score: 5, content: '', imageUrls: [] })

const loadAll = async () => {
  if (!authStore.isLoggedIn) return
  const [profileRes, orderRes, favoriteRes] = await Promise.all([
    http.get('/user/profile'),
    http.get('/user/orders'),
    http.get('/user/favorites')
  ])
  Object.assign(profile, profileRes.data)
  orders.value = orderRes.data
  favorites.value = favoriteRes.data.content
}

const saveProfile = async () => {
  await http.patch('/user/profile', profile)
  ElMessage.success('个人资料已更新')
}

const payOrder = async (id) => {
  await http.post(`/user/orders/${id}/pay`)
  ElMessage.success('模拟支付成功')
  await loadAll()
}

const cancelOrder = async (id) => {
  await http.post(`/user/orders/${id}/cancel`)
  ElMessage.success('订单已取消')
  await loadAll()
}

const completeOrder = async (id) => {
  await http.post(`/user/orders/${id}/complete`)
  ElMessage.success('订单已完成')
  reviewForm.orderId = id
  await loadAll()
}

const submitReview = async () => {
  if (!reviewForm.orderId) {
    ElMessage.warning('请选择已完成订单')
    return
  }
  await http.post('/user/reviews', reviewForm)
  ElMessage.success('评价成功')
  reviewForm.orderId = null
  reviewForm.content = ''
  reviewForm.score = 5
  await loadAll()
}

onMounted(loadAll)
</script>

<template>
  <div v-if="authStore.isLoggedIn" class="user-layout">
    <section class="content-grid">
      <div class="panel form-panel">
        <div class="section-title">
          <div>
            <h2 style="margin: 0;">我的订单</h2>
            <p class="muted">支持待支付、已支付、已完成、已取消状态流转</p>
          </div>
        </div>
        <el-table :data="orders" stripe>
          <el-table-column prop="orderNo" label="订单号" min-width="180" />
          <el-table-column prop="homestayName" label="民宿" min-width="140" />
          <el-table-column prop="roomNos" label="房号" min-width="150">
            <template #default="{ row }">{{ row.roomNos.join('、') }}</template>
          </el-table-column>
          <el-table-column prop="totalAmount" label="金额" width="100" />
          <el-table-column prop="orderStatus" label="状态" width="120" />
          <el-table-column label="操作" min-width="230">
            <template #default="{ row }">
              <div class="chip-list">
                <el-button v-if="row.orderStatus === 'PENDING_PAYMENT'" size="small" type="primary" @click="payOrder(row.id)">支付</el-button>
                <el-button v-if="row.orderStatus === 'PENDING_PAYMENT' || row.orderStatus === 'PAID'" size="small" @click="cancelOrder(row.id)">取消</el-button>
                <el-button v-if="row.orderStatus === 'PAID' || row.orderStatus === 'CONFIRMED'" size="small" type="success" @click="completeOrder(row.id)">完成</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>

      <div class="panel form-panel">
        <div class="section-title">
          <div>
            <h2 style="margin: 0;">我的收藏</h2>
            <p class="muted">展示用户收藏的民宿列表</p>
          </div>
        </div>
        <div v-if="favorites.length" class="card-grid">
          <div v-for="item in favorites" :key="item.id" class="panel detail-panel">
            <strong>{{ item.name }}</strong>
            <p class="muted" style="margin: 8px 0;">{{ item.city }} · {{ item.district }}</p>
            <p>￥{{ item.basePrice }} / 晚</p>
          </div>
        </div>
        <div v-else class="empty-state">你还没有收藏房源</div>
      </div>
    </section>

    <aside class="content-grid">
      <div class="panel form-panel">
        <h3 style="margin-top: 0;">个人资料</h3>
        <el-form label-position="top">
          <el-form-item label="昵称">
            <el-input v-model="profile.nickname" />
          </el-form-item>
          <el-form-item label="头像地址">
            <el-input v-model="profile.avatar" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="profile.phone" />
          </el-form-item>
          <el-button type="primary" color="#b5653b" class="full-width" @click="saveProfile">保存资料</el-button>
        </el-form>
      </div>

      <div class="panel form-panel">
        <h3 style="margin-top: 0;">订单评价</h3>
        <p class="muted">选择已完成订单后可提交评分与文字评论</p>
        <el-form label-position="top">
          <el-form-item label="订单 ID">
            <el-input-number v-model="reviewForm.orderId" class="full-width" />
          </el-form-item>
          <el-form-item label="评分">
            <el-rate v-model="reviewForm.score" />
          </el-form-item>
          <el-form-item label="评论内容">
            <el-input v-model="reviewForm.content" type="textarea" :rows="4" />
          </el-form-item>
          <el-button type="success" color="#5b8870" class="full-width" @click="submitReview">提交评价</el-button>
        </el-form>
      </div>
    </aside>
  </div>

  <div v-else class="panel empty-state">
    请先在首页登录，再进入用户中心查看订单与收藏。
  </div>
</template>

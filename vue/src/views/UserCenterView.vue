<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus, StarFilled } from '@element-plus/icons-vue'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const route = useRoute()

const orderStatusTextMap = {
  PENDING_PAYMENT: '待支付',
  PAID: '已支付',
  CONFIRMED: '待入住',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
  REFUND_REQUESTED: '退款中',
  REFUNDED: '已退款'
}

const authStore = useAuthStore()
const profile = reactive({ nickname: '', avatar: '', phone: '' })
const orders = ref([])
const favorites = ref([])
const showReviewDialog = ref(false)
const reviewForm = reactive({ orderId: null, score: 5, content: '', imageUrls: [] })
const fileList = ref([])
const showProfileDialog = ref(false)

const formatOrderStatus = (status) => orderStatusTextMap[status] || status

const loadAll = async () => {
  if (!authStore.isLoggedIn) return
  const [profileRes, orderRes, favoriteRes] = await Promise.all([
    http.get('/user/profile'),
    http.get('/user/orders'),
    http.get('/user/favorites')
  ])
  Object.assign(profile, profileRes.data)
  authStore.updateUser({
    nickname: profileRes.data?.nickname,
    avatar: profileRes.data?.avatar,
    phone: profileRes.data?.phone
  })
  orders.value = orderRes.data
  favorites.value = favoriteRes.data.content
}

const saveProfile = async () => {
  await http.patch('/user/profile', profile)
  ElMessage.success('个人资料已更新')
  authStore.updateUser({
    nickname: profile.nickname,
    avatar: profile.avatar,
    phone: profile.phone
  })
}

const payOrder = async (id) => {
  await http.post(`/user/orders/${id}/pay`)
  ElMessage.success('支付成功')
  await loadAll()
}

const cancelOrder = async (id) => {
  try {
    await ElMessageBox.confirm('确定要取消该订单吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.post(`/user/orders/${id}/cancel`)
    ElMessage.success('订单已取消')
    await loadAll()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

const refundOrder = async (id) => {
  try {
    await ElMessageBox.confirm('确定要发起退款吗？需要管理员审核后退款。', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.post(`/user/orders/${id}/refund`)
    ElMessage.success('退款申请已提交')
    await loadAll()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

const deleteOrder = async (id) => {
  try {
    await ElMessageBox.confirm('确定要删除该订单吗？删除后将不可恢复。', '提示', {
      confirmButtonText: '删除',
      cancelButtonText: '取消',
      type: 'warning'
    })
    await http.delete(`/user/orders/${id}`)
    ElMessage.success('订单已删除')
    await loadAll()
  } catch (e) {
    if (e !== 'cancel') {
      ElMessage.error(e.message || '操作失败')
    }
  }
}

const openReviewDialog = (order) => {
  reviewForm.orderId = order.id
  reviewForm.score = 5
  reviewForm.content = ''
  reviewForm.imageUrls = []
  fileList.value = []
  showReviewDialog.value = true
}

const handleUploadSuccess = (response) => {
  reviewForm.imageUrls.push(response.data)
}

const handleAvatarUploadSuccess = (response) => {
  if (response?.code === 200 && response?.data) {
    profile.avatar = response.data
    ElMessage.success('头像上传成功')
    authStore.updateUser({ avatar: profile.avatar })
  } else if (response?.data) {
    profile.avatar = response.data
    ElMessage.success('头像上传成功')
    authStore.updateUser({ avatar: profile.avatar })
  } else {
    ElMessage.error(response?.message || '头像上传失败')
  }
}

const handleRemove = (file) => {
  const url = file.response ? file.response.data : file.url
  const index = reviewForm.imageUrls.indexOf(url)
  if (index !== -1) {
    reviewForm.imageUrls.splice(index, 1)
  }
}

const completeOrder = async (id) => {
  await http.post(`/user/orders/${id}/complete`)
  ElMessage.success('订单已完成')
  await loadAll()
}

const toggleFavorite = async (homestayId) => {
  await http.post(`/user/favorites/${homestayId}`)
  await loadAll()
}

const submitReview = async () => {
  if (!reviewForm.orderId) {
    ElMessage.warning('请选择已完成订单')
    return
  }
  await http.post('/user/reviews', reviewForm)
  ElMessage.success('评价成功')
  showReviewDialog.value = false
  await loadAll()
}

onMounted(loadAll)

watch(
  () => route.query.profile,
  (value) => {
    showProfileDialog.value = value === '1'
  },
  { immediate: true }
)

const closeProfileDialog = () => {
  showProfileDialog.value = false
  if (route.query.profile) {
    router.replace({ path: '/user', query: {} })
  }
}
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
          <el-table-column label="状态" width="120">
            <template #default="{ row }">{{ formatOrderStatus(row.orderStatus) }}</template>
          </el-table-column>
          <el-table-column label="操作" min-width="230">
            <template #default="{ row }">
              <div class="chip-list">
                <el-button v-if="row.orderStatus === 'PENDING_PAYMENT'" size="small" type="primary" @click="payOrder(row.id)">支付</el-button>
                <el-button v-if="row.orderStatus === 'PENDING_PAYMENT'" size="small" @click="cancelOrder(row.id)">取消</el-button>
                <el-button v-if="row.orderStatus === 'PAID' || row.orderStatus === 'CONFIRMED'" size="small" type="danger" @click="refundOrder(row.id)">退款</el-button>
                <el-tag v-if="row.orderStatus === 'REFUND_REQUESTED'" type="warning">退款中</el-tag>
                <el-button v-if="row.orderStatus === 'PAID' || row.orderStatus === 'CONFIRMED'" size="small" type="success" @click="completeOrder(row.id)">完成</el-button>
                <el-button v-if="row.orderStatus === 'COMPLETED' && !row.reviewed" size="small" type="warning" @click="openReviewDialog(row)">评价</el-button>
                <el-button
                  v-if="['COMPLETED', 'CANCELLED', 'REFUNDED'].includes(row.orderStatus)"
                  size="small"
                  type="danger"
                  plain
                  @click="deleteOrder(row.id)"
                >
                  删除
                </el-button>
                <el-tag v-if="row.reviewed" type="info">已评价</el-tag>
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
        <div v-if="favorites.length" class="favorite-grid">
          <div
            v-for="item in favorites"
            :key="item.id"
            class="panel favorite-card clickable-card"
            @click="router.push(`/homestays/${item.id}`)"
          >
            <img :src="item.coverImage" class="favorite-cover" alt="封面图" />
            <div class="favorite-body">
              <div class="spaced">
                <strong>{{ item.name }}</strong>
                <el-button
                  circle
                  type="danger"
                  text
                  :icon="StarFilled"
                  @click.stop="toggleFavorite(item.id)"
                />
              </div>
              <p class="muted" style="margin: 4px 0;">{{ item.city }} · {{ item.district }}</p>
              <p class="price-text">￥{{ item.basePrice }} / 晚</p>
            </div>
          </div>
        </div>
        <div v-else class="empty-state">你还没有收藏房源</div>
      </div>
    </section>

    <aside class="content-grid">
      <div class="panel form-panel">
        <h3 style="margin-top: 0;">系统提示</h3>
        <p class="muted">已完成的订单点击操作列的“评价”按钮即可进行评分与晒图。</p>
        <p class="muted">已支付订单可发起退款申请，最迟入住当天 12:00 前可提交，需管理员审核后完成。</p>
      </div>
    </aside>

    <el-dialog v-model="showProfileDialog" title="个人资料" width="520px" @close="closeProfileDialog">
      <el-form label-position="top">
        <el-form-item label="昵称">
          <el-input v-model="profile.nickname" />
        </el-form-item>
        <el-form-item label="头像">
          <div class="avatar-upload-row">
            <el-upload
              class="avatar-uploader"
              action="/api/public/upload"
              :show-file-list="false"
              :on-success="handleAvatarUploadSuccess"
              :headers="{ Authorization: 'Bearer ' + authStore.token }"
              name="file"
            >
              <img v-if="profile.avatar" :src="profile.avatar" class="avatar-preview" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
            <span class="avatar-hint muted">点击上传头像</span>
          </div>
        </el-form-item>
        <el-form-item label="联系电话">
          <el-input v-model="profile.phone" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="closeProfileDialog">取消</el-button>
        <el-button type="primary" color="#b5653b" @click="saveProfile">保存资料</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="showReviewDialog" title="订单评价" width="500px">
      <el-form label-position="top">
        <el-form-item label="评分">
          <el-rate v-model="reviewForm.score" />
        </el-form-item>
        <el-form-item label="评论内容">
          <el-input v-model="reviewForm.content" type="textarea" :rows="4" placeholder="说说你的居住体验吧..." />
        </el-form-item>
        <el-form-item label="上传图片">
          <el-upload
            action="/api/public/upload"
            :headers="{ Authorization: 'Bearer ' + authStore.token }"
            list-type="picture-card"
            :on-success="handleUploadSuccess"
            :on-remove="handleRemove"
            :file-list="fileList"
            multiple
          >
            <el-icon><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showReviewDialog = false">取消</el-button>
        <el-button type="primary" color="#5b8870" @click="submitReview">提交评价</el-button>
      </template>
    </el-dialog>
  </div>

  <div v-else class="panel empty-state">
    请先在首页登录，再进入用户中心查看订单与收藏。
  </div>
</template>

<style scoped>
.favorite-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 20px;
}

.favorite-card {
  max-width: 360px;
  overflow: hidden;
  transition: transform 0.2s ease, box-shadow 0.2s ease;
}

.favorite-cover {
  width: 100%;
  height: 140px;
  object-fit: cover;
  display: block;
}

.favorite-body {
  padding: 14px 18px 18px;
}

.price-text {
  margin: 10px 0 0;
  font-weight: bold;
  color: var(--accent-deep);
}

.clickable-card {
  cursor: pointer;
}

.clickable-card:hover {
  transform: translateY(-3px);
  box-shadow: var(--shadow);
}

.avatar-upload-row {
  display: flex;
  align-items: center;
  gap: 16px;
}

.avatar-uploader {
  border: 1px dashed #d9d9d9;
  border-radius: 10px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  width: 96px;
  height: 96px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: border-color 0.3s;
}

.avatar-uploader:hover {
  border-color: #b5653b;
}

.avatar-uploader-icon {
  font-size: 24px;
  color: #8c939d;
  text-align: center;
}

.avatar-preview {
  width: 96px;
  height: 96px;
  display: block;
  object-fit: cover;
}

.avatar-hint {
  font-size: 13px;
}
</style>

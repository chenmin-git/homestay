<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const authStore = useAuthStore()
const detail = ref(null)
const availableRooms = ref([])
const booking = reactive({
  checkInDate: '',
  checkOutDate: '',
  roomIds: [],
  contactName: '演示联系人',
  contactPhone: '13800138000',
  remark: ''
})

const nights = computed(() => {
  if (!booking.checkInDate || !booking.checkOutDate) return 0
  const start = new Date(booking.checkInDate).getTime()
  const end = new Date(booking.checkOutDate).getTime()
  return Math.max((end - start) / 86400000, 0)
})

const totalPrice = computed(() => {
  return availableRooms.value
    .filter((room) => booking.roomIds.includes(room.id))
    .reduce((sum, room) => sum + Number(room.price), 0) * nights.value
})

const fetchDetail = async () => {
  const result = await http.get(`/public/homestays/${route.params.id}`)
  detail.value = result.data
}

const queryAvailability = async () => {
  if (!booking.checkInDate || !booking.checkOutDate) {
    ElMessage.warning('请先选择入住日期')
    return
  }
  const result = await http.get(`/public/homestays/${route.params.id}/availability`, {
    params: {
      checkInDate: booking.checkInDate,
      checkOutDate: booking.checkOutDate
    }
  })
  availableRooms.value = result.data
}

const createOrder = async () => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  await http.post('/user/orders', {
    homestayId: Number(route.params.id),
    ...booking
  })
  ElMessage.success('已生成待支付订单')
}

const toggleFavorite = async () => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  await http.post(`/user/favorites/${route.params.id}`)
  await fetchDetail()
}

onMounted(fetchDetail)
</script>

<template>
  <div v-if="detail" class="detail-layout">
    <section class="content-grid">
      <div class="panel detail-panel">
        <div class="split">
          <div>
            <el-tag type="warning">{{ detail.houseType }}</el-tag>
            <h1 style="margin: 14px 0 8px; font-size: 36px;">{{ detail.name }}</h1>
            <p class="muted">{{ detail.city }} · {{ detail.district }} · {{ detail.address }}</p>
          </div>
          <div>
            <el-button text type="warning" @click="toggleFavorite">
              {{ detail.favorite ? '取消收藏' : '加入收藏' }}
            </el-button>
          </div>
        </div>

        <div class="detail-gallery" style="margin-top: 20px;">
          <img v-for="image in detail.images" :key="image" :src="image" :alt="detail.name" />
        </div>

        <div class="detail-meta" style="margin-top: 20px;">
          <el-tag v-for="tag in detail.tags" :key="tag" effect="plain">{{ tag }}</el-tag>
        </div>

        <div class="two-cols" style="margin-top: 20px;">
          <div class="panel detail-panel">
            <h3 style="margin-top: 0;">房东信息</h3>
            <p>{{ detail.host.nickname }}</p>
            <p class="muted">{{ detail.host.phone }}</p>
          </div>
          <div class="panel detail-panel">
            <h3 style="margin-top: 0;">配套设施</h3>
            <div class="chip-list">
              <el-tag v-for="item in detail.facilities" :key="item" type="success" effect="plain">{{ item }}</el-tag>
            </div>
          </div>
        </div>

        <div class="panel detail-panel" style="margin-top: 20px;">
          <h3 style="margin-top: 0;">房源介绍</h3>
          <p class="muted" style="line-height: 1.8;">{{ detail.description }}</p>
        </div>

        <div class="panel detail-panel" style="margin-top: 20px;">
          <div class="section-title">
            <div>
              <h3 style="margin: 0;">历史评价</h3>
              <p class="muted">已完成订单后才能评价</p>
            </div>
          </div>
          <div v-if="detail.reviews.length" class="content-grid">
            <div v-for="review in detail.reviews" :key="review.id" class="panel detail-panel">
              <div class="split">
                <strong>{{ review.nickname }}</strong>
                <el-rate :model-value="review.score" disabled />
              </div>
              <p style="margin-top: 8px;">{{ review.content }}</p>
              <p v-if="review.replyContent" class="muted" style="margin-top: 8px;">房东回复：{{ review.replyContent }}</p>
            </div>
          </div>
          <div v-else class="empty-state">当前暂无评价</div>
        </div>
      </div>
    </section>

    <aside class="content-grid">
      <div class="panel form-panel">
        <h3 style="margin-top: 0;">在线预订</h3>
        <p class="muted">系统会按日期判断可订房号，避免超售</p>
        <el-form label-position="top" style="margin-top: 12px;">
          <el-form-item label="入住日期">
            <el-date-picker v-model="booking.checkInDate" value-format="YYYY-MM-DD" type="date" class="full-width" />
          </el-form-item>
          <el-form-item label="退房日期">
            <el-date-picker v-model="booking.checkOutDate" value-format="YYYY-MM-DD" type="date" class="full-width" />
          </el-form-item>
          <el-button type="primary" color="#5b8870" class="full-width" @click="queryAvailability">查询可用房间</el-button>
          <el-form-item label="可选房号" style="margin-top: 16px;">
            <el-select v-model="booking.roomIds" multiple clearable placeholder="请选择房号" class="full-width">
              <el-option
                v-for="room in availableRooms"
                :key="room.id"
                :label="`${room.roomNo} · ${room.roomType} · ￥${room.price}/晚`"
                :value="room.id"
              />
            </el-select>
          </el-form-item>
          <el-form-item label="联系人">
            <el-input v-model="booking.contactName" />
          </el-form-item>
          <el-form-item label="联系电话">
            <el-input v-model="booking.contactPhone" />
          </el-form-item>
          <el-form-item label="备注">
            <el-input v-model="booking.remark" type="textarea" />
          </el-form-item>
        </el-form>

        <div class="panel detail-panel" style="margin-top: 18px; background: #fff;">
          <p class="muted">入住天数</p>
          <strong style="font-size: 28px;">{{ nights }} 晚</strong>
          <p class="muted" style="margin-top: 8px;">预计总价</p>
          <strong style="font-size: 28px; color: var(--accent-deep);">￥{{ totalPrice }}</strong>
        </div>

        <el-button type="primary" color="#b5653b" class="full-width" style="margin-top: 16px;" @click="createOrder">
          提交订单
        </el-button>
      </div>
    </aside>
  </div>
</template>

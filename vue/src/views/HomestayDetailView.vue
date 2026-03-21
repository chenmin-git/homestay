<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const route = useRoute()
const router = useRouter()
const authStore = useAuthStore()
const detail = ref(null)
const selectedImage = ref('')
const availableRooms = ref([])
const booking = reactive({
  checkInDate: '',
  checkOutDate: '',
  roomIds: [],
  contactName: '',
  contactPhone: '',
  remark: ''
})

const disablePastDate = (date) => {
  const today = new Date()
  today.setHours(0, 0, 0, 0)
  return date.getTime() < today.getTime()
}

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

const hasCoordinates = computed(() => {
  if (!detail.value) return false
  return Number.isFinite(Number(detail.value.latitude)) && Number.isFinite(Number(detail.value.longitude))
})

const mapEmbedUrl = computed(() => {
  if (!hasCoordinates.value) return ''
  const lat = Number(detail.value.latitude)
  const lng = Number(detail.value.longitude)
  const delta = 0.018
  return `https://www.openstreetmap.org/export/embed.html?bbox=${lng - delta}%2C${lat - delta}%2C${lng + delta}%2C${lat + delta}&layer=mapnik&marker=${lat}%2C${lng}`
})

const mapLink = computed(() => {
  if (!hasCoordinates.value) return '#'
  return `https://www.openstreetmap.org/?mlat=${detail.value.latitude}&mlon=${detail.value.longitude}#map=14/${detail.value.latitude}/${detail.value.longitude}`
})

const selectedRooms = computed(() => {
  return availableRooms.value.filter((room) => booking.roomIds.includes(room.id))
})

const isBlacklisted = computed(() => authStore.user?.blacklisted === true)

const applyRouteDates = () => {
  const checkInDate = typeof route.query.checkInDate === 'string' ? route.query.checkInDate : ''
  const checkOutDate = typeof route.query.checkOutDate === 'string' ? route.query.checkOutDate : ''
  if (checkInDate && checkOutDate) {
    booking.checkInDate = checkInDate
    booking.checkOutDate = checkOutDate
  }
}

const fetchDetail = async () => {
  const result = await http.get(`/public/homestays/${route.params.id}`)
  detail.value = result.data
  selectedImage.value = result.data.images?.[0] || result.data.coverImage || ''
}

const queryAvailability = async () => {
  if (!booking.checkInDate || !booking.checkOutDate) {
    ElMessage.warning('请先选择入住和退房日期')
    return
  }
  const result = await http.get(`/public/homestays/${route.params.id}/availability`, {
    params: {
      checkInDate: booking.checkInDate,
      checkOutDate: booking.checkOutDate
    }
  })
  availableRooms.value = result.data
  if (!availableRooms.value.length) {
    ElMessage.info('当前日期区间暂无可用房号')
  }
}

const createOrder = async () => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  if (isBlacklisted.value) {
    ElMessage.error('账号已被拉入黑名单，无法继续订房')
    return
  }
  await http.post('/user/orders', {
    homestayId: Number(route.params.id),
    ...booking
  })
  ElMessage.success('已生成待支付订单，正在前往我的订单')
  router.push('/user')
}

const toggleFavorite = async () => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录')
    return
  }
  await http.post(`/user/favorites/${route.params.id}`)
  await fetchDetail()
}

watch(
  () => [booking.checkInDate, booking.checkOutDate],
  () => {
    availableRooms.value = []
    booking.roomIds = []
  }
)

onMounted(async () => {
  applyRouteDates()
  await fetchDetail()
  if (booking.checkInDate && booking.checkOutDate) {
    await queryAvailability()
  }
})
</script>

<template>
  <div v-if="detail" class="detail-page">
    <section class="detail-layout">
      <section class="content-grid">
        <div class="panel detail-panel detail-main-card">
          <div class="split">
            <div>
              <el-tag type="warning">{{ detail.houseType }}</el-tag>
              <h1 style="margin: 14px 0 8px; font-size: 36px;">{{ detail.name }}</h1>
              <p class="muted">{{ detail.city }} · {{ detail.district }} · {{ detail.address }}</p>
            </div>
            <div class="chip-list">
              <button
                type="button"
                :class="['icon-favorite-btn', { active: detail.favorite }]"
                :aria-label="detail.favorite ? '取消收藏' : '加入收藏'"
                :title="detail.favorite ? '取消收藏' : '加入收藏'"
                @click="toggleFavorite"
              >
                <span class="favorite-heart">{{ detail.favorite ? '♥' : '♡' }}</span>
              </button>
            </div>
          </div>

          <div class="detail-hero-grid">
            <div class="detail-main-visual">
              <img :src="selectedImage" :alt="detail.name" class="detail-main-image" />
            </div>
            <div class="detail-thumb-grid">
              <button
                v-for="image in detail.images"
                :key="image"
                type="button"
                :class="['detail-thumb', { active: selectedImage === image }]"
                @click="selectedImage = image"
              >
                <img :src="image" :alt="detail.name" />
              </button>
            </div>
          </div>

          <div class="detail-summary-grid">
            <div class="summary-tile">
              <span class="muted">基础房价</span>
              <strong>￥{{ detail.basePrice }}</strong>
            </div>
            <div class="summary-tile">
              <span class="muted">房间总数</span>
              <strong>{{ detail.totalRooms }} 间</strong>
            </div>
            <div class="summary-tile">
              <span class="muted">累计预订</span>
              <strong>{{ detail.bookingCount }} 次</strong>
            </div>
            <div class="summary-tile">
              <span class="muted">评分</span>
              <strong>{{ detail.rating }}</strong>
            </div>
          </div>

          <div class="detail-meta" style="margin-top: 18px;">
            <el-tag v-for="tag in detail.tags" :key="tag" effect="plain">{{ tag }}</el-tag>
          </div>

          <div class="two-cols" style="margin-top: 20px;">
            <div class="panel detail-panel">
              <h3 style="margin-top: 0;">房东信息</h3>
              <p style="margin: 8px 0 0;">{{ detail.host.nickname }}</p>
              <p class="muted" style="margin: 6px 0 0;">联系电话：{{ detail.host.phone }}</p>
              <p class="muted" style="margin: 10px 0 0;">支持按真实房号维护库存，避免一套房只能接一单。</p>
            </div>
            <div class="panel detail-panel">
              <h3 style="margin-top: 0;">配套设施</h3>
              <div class="chip-list">
                <el-tag v-for="item in detail.facilities" :key="item" type="success" effect="plain">{{ item }}</el-tag>
              </div>
            </div>
          </div>

          <div class="panel detail-panel" style="margin-top: 20px;">
            <h3 style="margin-top: 0;">房型与房号</h3>
            <div class="room-showcase-grid">
              <article v-for="room in detail.rooms" :key="room.id" class="room-showcase-card">
                <div class="split" style="align-items: flex-start;">
                  <div>
                    <strong>{{ room.roomNo }}</strong>
                    <p class="muted" style="margin: 6px 0 0;">{{ room.roomType }}</p>
                  </div>
                  <span class="room-price">￥{{ room.price }}</span>
                </div>
                <p class="muted" style="margin: 12px 0 0;">
                  {{ room.floorNo }} 层 · {{ room.bedCount }} 床 · 可住 {{ room.capacity }} 人
                </p>
              </article>
            </div>
          </div>

          <div class="panel detail-panel" style="margin-top: 20px;">
            <h3 style="margin-top: 0;">房源介绍</h3>
            <p class="muted" style="line-height: 1.8;">{{ detail.description }}</p>
          </div>

          <div class="panel detail-panel" style="margin-top: 20px;">
            <div class="section-title">
              <div>
                <h3 style="margin: 0;">地理位置地图</h3>
                <p class="muted">展示房源地址、经纬度和地图定位</p>
              </div>
              <a :href="mapLink" class="outline-btn" target="_blank">打开地图</a>
            </div>
            <div v-if="hasCoordinates" class="map-block">
              <iframe
                class="detail-map-frame"
                :src="mapEmbedUrl"
                title="房源地图"
                loading="lazy"
              ></iframe>
              <div class="map-meta">
                <strong>{{ detail.address }}</strong>
                <p class="muted" style="margin: 6px 0 0;">
                  纬度 {{ detail.latitude }} · 经度 {{ detail.longitude }}
                </p>
              </div>
            </div>
            <div v-else class="empty-state">当前房源暂无地图坐标信息。</div>
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
                <div v-if="review.imageUrls?.length" class="review-image-grid" style="margin-top: 10px;">
                  <el-image
                    v-for="(url, index) in review.imageUrls"
                    :key="`${review.id}-${index}`"
                    :src="url"
                    :preview-src-list="review.imageUrls"
                    fit="cover"
                    class="review-image"
                  />
                </div>
                <p v-if="review.replyContent" class="muted" style="margin-top: 10px;">房东回复：{{ review.replyContent }}</p>
              </div>
            </div>
            <div v-else class="empty-state">当前暂无评价</div>
          </div>
        </div>
      </section>

      <aside class="content-grid">
        <div class="panel form-panel booking-sidebar">
          <h3 style="margin-top: 0;">在线预订</h3>
          <p class="muted">系统会按日期判断可订房号，避免超售</p>

          <el-form label-position="top" style="margin-top: 12px;">
            <el-form-item label="入住日期">
              <el-date-picker
                v-model="booking.checkInDate"
                value-format="YYYY-MM-DD"
                type="date"
                class="full-width"
                :disabled-date="disablePastDate"
              />
            </el-form-item>
            <el-form-item label="退房日期">
              <el-date-picker
                v-model="booking.checkOutDate"
                value-format="YYYY-MM-DD"
                type="date"
                class="full-width"
                :disabled-date="disablePastDate"
              />
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

          <div class="booking-summary-card">
            <p class="muted">入住天数</p>
            <strong>{{ nights }} 晚</strong>
            <p class="muted" style="margin-top: 12px;">已选房号</p>
            <div class="chip-list" style="margin-top: 8px;">
              <span v-if="!selectedRooms.length" class="toolbar-pill">未选择房号</span>
              <span v-for="room in selectedRooms" :key="room.id" class="toolbar-pill">{{ room.roomNo }}</span>
            </div>
            <p class="muted" style="margin-top: 12px;">预计总价</p>
            <strong class="booking-price">￥{{ totalPrice }}</strong>
          </div>

          <el-alert
            v-if="isBlacklisted"
            title="当前账号已被拉入黑名单，不能提交新的预订订单。"
            type="warning"
            :closable="false"
            style="margin-top: 16px;"
          />

          <el-button
            type="primary"
            color="#b5653b"
            class="full-width"
            style="margin-top: 16px;"
            :disabled="isBlacklisted"
            @click="createOrder"
          >
            提交订单
          </el-button>
        </div>
      </aside>
    </section>
  </div>
</template>

<style scoped>
.detail-page {
  display: grid;
  gap: 24px;
}

.detail-main-card {
  overflow: hidden;
}

.detail-hero-grid {
  margin-top: 22px;
  display: grid;
  grid-template-columns: minmax(0, 1.12fr) minmax(220px, 0.88fr);
  gap: 16px;
}

.detail-main-visual {
  min-height: 400px;
  border-radius: 22px;
  overflow: hidden;
  border: 1px solid rgba(75, 58, 45, 0.1);
}

.detail-main-image {
  width: 100%;
  height: 100%;
  min-height: 400px;
  object-fit: cover;
  display: block;
}

.detail-thumb-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px;
}

.detail-thumb {
  padding: 0;
  border: 2px solid transparent;
  border-radius: 18px;
  overflow: hidden;
  background: transparent;
  cursor: pointer;
}

.detail-thumb.active {
  border-color: rgba(181, 101, 59, 0.38);
}

.detail-thumb img {
  width: 100%;
  height: 190px;
  object-fit: cover;
  display: block;
}

.detail-summary-grid {
  margin-top: 18px;
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 14px;
}

.summary-tile {
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid rgba(75, 58, 45, 0.1);
  background: rgba(255, 255, 255, 0.76);
}

.summary-tile strong {
  display: block;
  margin-top: 10px;
  font-size: 28px;
}

.room-showcase-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px;
}

.room-showcase-card {
  padding: 18px;
  border-radius: 18px;
  border: 1px solid rgba(75, 58, 45, 0.1);
  background: rgba(255, 255, 255, 0.76);
}

.room-price {
  color: var(--accent-deep);
  font-weight: 700;
}

.toolbar-pill {
  display: inline-flex;
  align-items: center;
  padding: 8px 12px;
  border-radius: 999px;
  font-size: 13px;
  color: var(--accent-deep);
  background: rgba(181, 101, 59, 0.08);
  border: 1px solid rgba(181, 101, 59, 0.14);
}

.review-image-grid {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
}

.review-image {
  width: 80px;
  height: 80px;
  border-radius: 12px;
  border: 1px solid rgba(75, 58, 45, 0.1);
  cursor: zoom-in;
}

.map-block {
  display: grid;
  gap: 14px;
}

.detail-map-frame {
  width: 100%;
  height: 320px;
  border: 0;
  border-radius: 20px;
}

.booking-sidebar {
  position: sticky;
  top: 110px;
}

.booking-summary-card {
  margin-top: 18px;
  padding: 18px;
  border-radius: 18px;
  background: #fff;
  border: 1px solid rgba(75, 58, 45, 0.08);
}

.booking-summary-card strong {
  font-size: 28px;
}

.booking-price {
  color: var(--accent-deep);
}

@media (max-width: 1100px) {
  .detail-hero-grid,
  .detail-summary-grid,
  .room-showcase-grid {
    grid-template-columns: 1fr;
  }

  .detail-thumb-grid {
    grid-template-columns: repeat(4, minmax(0, 1fr));
  }

  .booking-sidebar {
    position: static;
  }
}

@media (max-width: 760px) {
  .detail-thumb-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }

  .detail-main-visual,
  .detail-main-image {
    min-height: 280px;
  }
}
</style>

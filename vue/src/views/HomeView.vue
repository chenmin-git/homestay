<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import http from '../api/http'
import { useAuthStore } from '../stores/auth'

const router = useRouter()
const authStore = useAuthStore()
const home = ref({ banners: [], hotHomestays: [], latestHomestays: [], notices: [], houseTypes: [] })
const homestays = ref([])
const total = ref(0)
const loading = ref(false)

const query = reactive({
  city: '',
  keyword: '',
  stayDates: [],
  minPrice: '',
  maxPrice: '',
  houseType: '',
  page: 1,
  size: 8
})

const hasDateFilter = computed(() => query.stayDates.length === 2)
const resultSummary = computed(() => {
  if (!hasDateFilter.value) {
    return '支持地点、价格、入住日期、房型组合筛选'
  }
  return `已按 ${query.stayDates[0]} 至 ${query.stayDates[1]} 筛选可订房源`
})

const tickerNotices = computed(() => home.value.notices || [])
const tickerDuration = computed(() => Math.max(12, tickerNotices.value.length * 4))

const buildSearchParams = () => ({
  city: query.city || undefined,
  keyword: query.keyword || undefined,
  minPrice: query.minPrice || undefined,
  maxPrice: query.maxPrice || undefined,
  houseType: query.houseType || undefined,
  checkInDate: hasDateFilter.value ? query.stayDates[0] : undefined,
  checkOutDate: hasDateFilter.value ? query.stayDates[1] : undefined,
  page: query.page - 1,
  size: query.size
})

const fetchHomeData = async () => {
  const result = await http.get('/public/home')
  home.value = result.data
}

const loadHomestays = async ({ resetPage = false } = {}) => {
  if (resetPage) {
    query.page = 1
  }
  loading.value = true
  try {
    const result = await http.get('/public/homestays', {
      params: buildSearchParams()
    })
    homestays.value = result.data.content
    total.value = result.data.total
    query.page = Number(result.data.page) + 1
  } finally {
    loading.value = false
  }
}

const search = async () => {
  await loadHomestays({ resetPage: true })
}

const resetSearch = async () => {
  query.city = ''
  query.keyword = ''
  query.stayDates = []
  query.minPrice = ''
  query.maxPrice = ''
  query.houseType = ''
  await loadHomestays({ resetPage: true })
}

const handlePageChange = async (page) => {
  query.page = page
  await loadHomestays()
}

const openDetail = (id) => {
  router.push({
    path: `/homestays/${id}`,
    query: hasDateFilter.value
      ? { checkInDate: query.stayDates[0], checkOutDate: query.stayDates[1] }
      : undefined
  })
}

const toggleFavorite = async (id) => {
  if (!authStore.isLoggedIn) {
    ElMessage.warning('请先登录后再收藏')
    return
  }
  await http.post(`/user/favorites/${id}`)
  await Promise.all([fetchHomeData(), loadHomestays()])
}

const formatDate = (value) => {
  if (!value) return ''
  const text = String(value)
  return text.length >= 10 ? text.slice(0, 10) : text
}

const showNoticeDialog = ref(false)
const activeNotice = ref(null)
const noticeSectionRef = ref(null)
const showNoticeSection = false

const openNotice = (notice) => {
  activeNotice.value = notice
  showNoticeDialog.value = true
}

const scrollToNotices = () => {
  noticeSectionRef.value?.scrollIntoView({ behavior: 'smooth', block: 'start' })
}

onMounted(async () => {
  await Promise.all([fetchHomeData(), loadHomestays()])
})
</script>

<template>
  <div class="home-page content-grid">
    <section class="panel notice-ticker">
      <div class="ticker-label">公告</div>
      <div class="ticker-track">
        <div v-if="tickerNotices.length" class="ticker-row" :style="{ '--ticker-duration': `${tickerDuration}s` }">
          <span
            v-for="notice in tickerNotices"
            :key="notice.id"
            class="ticker-item"
            role="button"
            tabindex="0"
            @click="openNotice(notice)"
            @keydown.enter="openNotice(notice)"
          >
            {{ notice.title }}
          </span>
        </div>
        <div v-else class="ticker-empty">暂无公告</div>
      </div>
      <button class="ticker-more" type="button" @click="scrollToNotices">查看全部</button>
    </section>

    <section class="home-top-grid">
      <div class="panel banner-stage">
        <el-carousel height="420px" trigger="click" indicator-position="outside">
          <el-carousel-item v-for="banner in home.banners" :key="banner.id">
            <div class="banner-slide">
              <img :src="banner.imageUrl" :alt="banner.title" class="banner-image" />
              <div class="banner-overlay">
                <span class="banner-eyebrow">热门民宿推荐</span>
                <h1>{{ banner.title }}</h1>
                <p>按真实房号库存管理，支持按日期查询可订房间，适合作为课程设计首页展示。</p>
                <div class="chip-list">
                  <el-tag v-for="notice in home.notices.slice(0, 3)" :key="notice.id" effect="plain">{{ notice.title }}</el-tag>
                </div>
                <div class="hero-actions" style="margin-top: 18px;">
                  <el-button type="primary" color="#b5653b" @click="search">开始查找</el-button>
                  <el-button plain @click="router.push('/login')">用户登录</el-button>
                </div>
              </div>
            </div>
          </el-carousel-item>
        </el-carousel>
      </div>

      <div class="panel discovery-panel">
        <div class="section-title home-section-head">
          <div>
            <h2 style="margin: 0;">首页搜索</h2>
            <p class="muted">支持地点、价格、入住日期、房型联合筛选</p>
          </div>
        </div>

        <div class="discovery-metrics">
          <div class="metric-card">
            <span class="muted">热门房源</span>
            <strong>{{ home.hotHomestays?.length || 0 }}</strong>
          </div>
          <div class="metric-card">
            <span class="muted">最新上架</span>
            <strong>{{ home.latestHomestays?.length || 0 }}</strong>
          </div>
        </div>

        <el-form label-position="top" class="home-search-form">
          <div class="home-search-grid">
            <el-form-item label="地点 / 商圈">
              <el-input v-model="query.city" placeholder="例如：三亚 / 静安区" />
            </el-form-item>
            <el-form-item label="关键词">
              <el-input v-model="query.keyword" placeholder="房源名称、摘要、特色标签" />
            </el-form-item>
            <el-form-item label="入住日期">
              <el-date-picker
                v-model="query.stayDates"
                type="daterange"
                value-format="YYYY-MM-DD"
                start-placeholder="入住日期"
                end-placeholder="退房日期"
                class="full-width"
              />
            </el-form-item>
            <el-form-item label="房型">
              <el-select v-model="query.houseType" clearable placeholder="全部房型" class="full-width">
                <el-option v-for="item in home.houseTypes || []" :key="item" :label="item" :value="item" />
              </el-select>
            </el-form-item>
            <el-form-item label="最低价">
              <el-input v-model="query.minPrice" placeholder="最低价" />
            </el-form-item>
            <el-form-item label="最高价">
              <el-input v-model="query.maxPrice" placeholder="最高价" />
            </el-form-item>
          </div>
        </el-form>

        <div class="search-toolbar">
          <div class="chip-list">
            <span v-for="notice in home.notices" :key="notice.id" class="toolbar-pill">{{ notice.title }}</span>
          </div>
          <div class="chip-list">
            <el-button plain @click="resetSearch">重置筛选</el-button>
            <el-button type="primary" color="#b5653b" @click="search">搜索房源</el-button>
          </div>
        </div>
      </div>
    </section>

    <section v-if="showNoticeSection" ref="noticeSectionRef" class="panel notice-panel">
      <div class="section-title home-section-head">
        <div>
          <h2 style="margin: 0;">系统公告</h2>
          <p class="muted">滚动查看，点击可查看完整内容</p>
        </div>
      </div>
      <div v-if="home.notices?.length" class="notice-list">
        <article
          v-for="notice in home.notices"
          :key="notice.id"
          class="notice-item"
          role="button"
          tabindex="0"
          @click="openNotice(notice)"
          @keydown.enter="openNotice(notice)"
        >
          <div class="notice-header">
            <strong>{{ notice.title }}</strong>
            <span v-if="notice.createdAt" class="notice-date">{{ formatDate(notice.createdAt) }}</span>
          </div>
          <p class="muted notice-preview">{{ notice.content }}</p>
        </article>
      </div>
      <div v-else class="empty-state">暂无公告</div>
    </section>

    <section class="home-highlight-grid">
      <div class="panel spotlight-panel">
        <div class="section-title home-section-head">
          <div>
            <h2 style="margin: 0;">热门民宿排行</h2>
            <p class="muted">按预订热度排序，适合首页重点展示</p>
          </div>
        </div>
        <div class="rank-list">
          <button
            v-for="(item, index) in home.hotHomestays"
            :key="item.id"
            class="rank-item"
            @click="openDetail(item.id)"
          >
            <span class="rank-badge">TOP {{ index + 1 }}</span>
            <div class="rank-text">
              <strong>{{ item.name }}</strong>
              <small>{{ item.city }} · {{ item.district }} · ￥{{ item.basePrice }}/晚</small>
            </div>
          </button>
        </div>
      </div>

      <div class="panel spotlight-panel">
        <div class="section-title home-section-head">
          <div>
            <h2 style="margin: 0;">最新上架民宿</h2>
            <p class="muted">优先展示新发布的房源，便于首页引流</p>
          </div>
        </div>
        <div class="latest-grid">
          <article
            v-for="item in home.latestHomestays"
            :key="item.id"
            class="latest-card"
            @click="openDetail(item.id)"
          >
            <img :src="item.coverImage" :alt="item.name" />
            <div class="latest-body">
              <strong>{{ item.name }}</strong>
              <span class="muted">{{ item.city }} · {{ item.houseType }}</span>
            </div>
          </article>
        </div>
      </div>
    </section>

    <section class="panel home-list-panel">
      <div class="section-title home-section-head">
        <div>
          <h2 style="margin: 0;">民宿列表</h2>
          <p class="muted">{{ resultSummary }}</p>
        </div>
        <span class="result-count">共 {{ total }} 套房源</span>
      </div>

      <div v-if="!loading && !homestays.length" class="empty-state">当前筛选条件下暂无可展示房源，请调整条件后再试。</div>

      <div v-loading="loading" class="home-list-grid">
        <article
          v-for="item in homestays"
          :key="item.id"
          class="stay-card panel clickable-card"
          @click="openDetail(item.id)"
        >
          <img :src="item.coverImage" class="stay-cover" :alt="item.name" />
          <div class="stay-body">
            <div class="spaced">
              <div>
                <h3 style="margin: 0;">{{ item.name }}</h3>
                <p class="muted" style="margin: 8px 0 0;">{{ item.city }} · {{ item.district }}</p>
              </div>
              <el-button text type="warning" @click.stop="toggleFavorite(item.id)">
                {{ item.favorite ? '已收藏' : '收藏' }}
              </el-button>
            </div>

            <div class="chip-list" style="margin-top: 14px;">
              <el-tag v-for="tag in item.tags" :key="tag" effect="plain">{{ tag }}</el-tag>
            </div>

            <p class="muted" style="margin: 14px 0 0; line-height: 1.7;">{{ item.summary }}</p>

            <div class="list-card-footer">
              <div>
                <strong>￥{{ item.basePrice }} / 晚</strong>
                <p class="muted" style="margin: 6px 0 0;">
                  {{ item.houseType }}
                  <span v-if="item.availableRoomCount !== null && item.availableRoomCount !== undefined">
                    · 可订 {{ item.availableRoomCount }} 间
                  </span>
                </p>
              </div>
              <el-button type="primary" plain @click.stop="openDetail(item.id)">查看详情</el-button>
            </div>
          </div>
        </article>
      </div>

      <div v-if="total > query.size" class="pagination-bar">
        <el-pagination
          background
          layout="prev, pager, next"
          :current-page="query.page"
          :page-size="query.size"
          :total="total"
          @current-change="handlePageChange"
        />
      </div>
    </section>

    <el-dialog v-model="showNoticeDialog" :title="activeNotice?.title || '公告详情'" width="520px">
      <div class="notice-dialog-body">
        <div class="notice-dialog-meta" v-if="activeNotice?.createdAt">
          发布日期：{{ formatDate(activeNotice.createdAt) }}
        </div>
        <p class="notice-dialog-content">{{ activeNotice?.content || '' }}</p>
      </div>
      <template #footer>
        <el-button type="primary" color="#b5653b" @click="showNoticeDialog = false">知道了</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<style scoped>
.home-page {
  gap: 28px;
}

.notice-ticker {
  display: flex;
  align-items: center;
  gap: 14px;
  padding: 12px 18px;
  border-radius: 18px;
  overflow: hidden;
}

.ticker-label {
  padding: 6px 12px;
  border-radius: 999px;
  font-weight: 700;
  color: var(--accent-deep);
  background: rgba(181, 101, 59, 0.12);
  border: 1px solid rgba(181, 101, 59, 0.2);
  white-space: nowrap;
}

.ticker-track {
  position: relative;
  flex: 1;
  overflow: hidden;
}

.ticker-row {
  display: inline-flex;
  align-items: center;
  gap: 16px;
  white-space: nowrap;
  animation: ticker-scroll var(--ticker-duration, 16s) linear infinite;
}

.ticker-row:hover {
  animation-play-state: paused;
}

.ticker-item {
  padding: 6px 12px;
  border-radius: 999px;
  border: 1px solid rgba(75, 58, 45, 0.16);
  background: rgba(255, 255, 255, 0.75);
  cursor: pointer;
  font-size: 14px;
}

.ticker-item:hover {
  border-color: rgba(181, 101, 59, 0.36);
  color: var(--accent-deep);
}

.ticker-empty {
  color: var(--muted);
  font-size: 14px;
}

.ticker-more {
  border: 1px solid var(--line);
  background: rgba(255, 255, 255, 0.72);
  color: var(--accent-deep);
  border-radius: 999px;
  padding: 8px 14px;
  cursor: pointer;
  white-space: nowrap;
  transition: all 0.2s ease;
}

.ticker-more:hover {
  border-color: rgba(181, 101, 59, 0.35);
  transform: translateY(-1px);
}

.ticker-more:focus-visible,
.ticker-item:focus-visible {
  outline: 2px solid rgba(181, 101, 59, 0.5);
  outline-offset: 2px;
}

.notice-ticker + .home-top-grid {
  margin-top: -6px;
}

.home-top-grid {
  display: grid;
  grid-template-columns: minmax(0, 1.18fr) minmax(360px, 0.82fr);
  gap: 24px;
}

.banner-stage,
.discovery-panel,
.spotlight-panel,
.home-list-panel,
.notice-panel {
  overflow: hidden;
}

.banner-stage {
  padding: 18px;
}

.banner-slide {
  position: relative;
  height: 100%;
  border-radius: 22px;
  overflow: hidden;
}

.banner-image {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.banner-overlay {
  position: absolute;
  inset: 0;
  display: flex;
  flex-direction: column;
  justify-content: flex-end;
  gap: 12px;
  padding: 34px;
  background:
    linear-gradient(180deg, rgba(27, 20, 14, 0.08), rgba(27, 20, 14, 0.62));
  color: #fffdf8;
}

.banner-overlay h1 {
  margin: 0;
  max-width: 520px;
  font-size: 42px;
  line-height: 1.1;
}

.banner-overlay p {
  margin: 0;
  max-width: 520px;
  line-height: 1.8;
  color: rgba(255, 250, 243, 0.88);
}

.banner-eyebrow {
  width: fit-content;
  padding: 8px 12px;
  border-radius: 999px;
  background: rgba(255, 255, 255, 0.14);
  border: 1px solid rgba(255, 255, 255, 0.22);
}

.discovery-panel,
.spotlight-panel,
.home-list-panel,
.notice-panel {
  padding: 28px;
}

.home-section-head {
  padding-bottom: 14px;
  margin-bottom: 20px;
  border-bottom: 1px solid rgba(75, 58, 45, 0.08);
}

.discovery-metrics {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px;
}

.metric-card {
  min-height: 114px;
  padding: 18px 20px;
  border-radius: 18px;
  border: 1px solid rgba(75, 58, 45, 0.1);
  background: linear-gradient(180deg, rgba(255, 255, 255, 0.98), rgba(247, 240, 231, 0.9));
  display: flex;
  flex-direction: column;
  justify-content: space-between;
}

.metric-card strong {
  font-size: 28px;
}

.home-search-form {
  margin-top: 20px;
}

.home-search-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 6px 16px;
}

.search-toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  justify-content: space-between;
  gap: 14px;
  margin-top: 12px;
}

.notice-list {
  display: grid;
  gap: 14px;
  max-height: 260px;
  overflow: auto;
  padding-right: 4px;
  scrollbar-width: none;
  -ms-overflow-style: none;
}

.notice-list::-webkit-scrollbar {
  width: 0;
  height: 0;
}

.notice-item {
  padding: 12px 16px;
  border-radius: 18px;
  border: 1px solid rgba(75, 58, 45, 0.1);
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.notice-item:hover {
  transform: translateY(-2px);
  border-color: rgba(181, 101, 59, 0.32);
  box-shadow: 0 12px 24px rgba(86, 57, 33, 0.08);
}

.notice-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 12px;
  flex-wrap: wrap;
}

.notice-date {
  padding: 4px 10px;
  border-radius: 999px;
  font-size: 12px;
  color: var(--muted);
  background: rgba(181, 101, 59, 0.08);
  border: 1px solid rgba(181, 101, 59, 0.14);
}

.notice-preview {
  margin: 8px 0 0;
  line-height: 1.6;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.notice-dialog-body {
  display: grid;
  gap: 10px;
}

.notice-dialog-meta {
  color: var(--muted);
  font-size: 13px;
}

.notice-dialog-content {
  margin: 0;
  line-height: 1.8;
}

@keyframes ticker-scroll {
  0% {
    transform: translateX(100%);
  }
  100% {
    transform: translateX(-100%);
  }
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

.home-highlight-grid {
  display: grid;
  grid-template-columns: minmax(0, 0.9fr) minmax(0, 1.1fr);
  gap: 24px;
}

.rank-list,
.latest-grid {
  display: grid;
  gap: 14px;
}

.rank-item {
  width: 100%;
  display: grid;
  grid-template-columns: auto 1fr;
  gap: 14px;
  align-items: center;
  padding: 16px 18px;
  border-radius: 18px;
  border: 1px solid rgba(75, 58, 45, 0.1);
  background: rgba(255, 255, 255, 0.72);
  text-align: left;
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.rank-item:hover,
.latest-card:hover {
  transform: translateY(-2px);
  border-color: rgba(181, 101, 59, 0.32);
}

.rank-badge {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 70px;
  padding: 10px 12px;
  border-radius: 14px;
  background: rgba(181, 101, 59, 0.12);
  color: var(--accent-deep);
  font-weight: 700;
}

.rank-text {
  display: grid;
  gap: 6px;
}

.rank-text small {
  color: var(--muted);
}

.latest-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.latest-card {
  border-radius: 20px;
  overflow: hidden;
  border: 1px solid rgba(75, 58, 45, 0.1);
  background: rgba(255, 255, 255, 0.72);
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease;
}

.latest-card img {
  width: 100%;
  height: 160px;
  object-fit: cover;
  display: block;
}

.latest-body {
  padding: 14px 16px 16px;
  display: grid;
  gap: 6px;
}

.result-count {
  color: var(--accent-deep);
  font-weight: 700;
}

.list-card-footer {
  margin-top: 18px;
  display: flex;
  justify-content: space-between;
  align-items: flex-end;
  gap: 12px;
}

.pagination-bar {
  margin-top: 22px;
  display: flex;
  justify-content: center;
}

.home-list-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(320px, 1fr));
  gap: 18px;
  align-items: start;
}

.home-list-grid .stay-card {
  max-width: 420px;
}

.clickable-card {
  cursor: pointer;
  transition: transform 0.2s ease, border-color 0.2s ease, box-shadow 0.2s ease;
}

.clickable-card:hover {
  transform: translateY(-3px);
  border-color: rgba(181, 101, 59, 0.26);
  box-shadow: 0 18px 36px rgba(86, 57, 33, 0.12);
}

.home-list-grid .stay-cover {
  height: 240px;
  object-position: center;
}

@media (max-width: 1100px) {
  .home-top-grid,
  .home-highlight-grid,
  .discovery-metrics,
  .home-search-grid {
    grid-template-columns: 1fr;
  }

  .latest-grid {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 760px) {
  .banner-overlay {
    padding: 22px;
  }

  .banner-overlay h1 {
    font-size: 32px;
  }

  .home-list-grid {
    grid-template-columns: 1fr;
  }

  .home-list-grid .stay-card {
    max-width: none;
  }

  .latest-grid,
  .list-card-footer {
    grid-template-columns: 1fr;
  }

  .list-card-footer {
    display: grid;
    align-items: stretch;
  }
}
</style>

<template>
  <main class="admin-home" :class="{ 'admin-home-ready': pageReady }">
    <section class="hero-panel" aria-labelledby="admin-home-title">
      <div class="hero-topline">
        <span>数据驱动 · 高效运营</span>
        <div class="hero-actions">
          <button class="icon-button" type="button" aria-label="查看通知" title="查看通知" @click="router.push('/notifications?role=admin')">
            <i class="fa fa-bell-o" aria-hidden="true"></i>
            <span v-if="pendingTotal" class="notification-dot"></span>
          </button>
          <button class="logout-button" type="button" title="退出登录" @click="logout">
            <i class="fa fa-sign-out" aria-hidden="true"></i><span>退出登录</span>
          </button>
        </div>
      </div>
      <h1 id="admin-home-title">外卖管理后台</h1>
      <p>让美食更好地抵达</p>
      <span class="hero-line" aria-hidden="true"></span>
    </section>

    <section class="profile-panel" aria-label="管理员信息">
      <div class="profile-avatar">
        <img :src="currentUser?.photo || DEFAULT_AVATAR_URL" alt="管理员头像" @error="handleAvatarError">
      </div>
      <div class="profile-copy">
        <div class="profile-name-row">
          <strong>{{ currentUser?.username || '管理员' }}</strong>
          <span class="role-badge"><i class="fa fa-crown" aria-hidden="true"></i> 管理员</span>
        </div>
        <p>ID: ADMIN{{ currentUser?.id || '00' }}</p>
        <p class="profile-meta">账号已通过身份认证</p>
      </div>
      <div class="profile-note">
        <i class="fa fa-motorcycle" aria-hidden="true"></i>
        <span>用心管理<br>让每一份美食准时抵达</span>
      </div>
    </section>

    <section class="dashboard-panel filter-panel" aria-labelledby="filter-title">
      <div class="date-row">
        <span id="filter-title" class="filter-label">统计区间</span>
        <label class="date-input">
          <span class="sr-only">开始日期</span>
          <input v-model="statsFrom" type="date" :max="todayString" @change="loadDetailedStats">
          <i class="fa fa-calendar-o" aria-hidden="true"></i>
        </label>
        <span class="date-separator">至</span>
        <label class="date-input">
          <span class="sr-only">结束日期</span>
          <input v-model="statsTo" type="date" :max="todayString" @change="loadDetailedStats">
          <i class="fa fa-calendar-o" aria-hidden="true"></i>
        </label>
      </div>
      <div class="filter-actions">
        <button class="primary-action" type="button" @click="loadDetailedStats">
          <i class="fa fa-search" aria-hidden="true"></i> 查询
        </button>
        <button class="outline-action" type="button" @click="downloadStats">
          <i class="fa fa-download" aria-hidden="true"></i> 导出CSV
        </button>
      </div>
      <p class="filter-summary">
        有效订单 <strong>{{ completedCount }}</strong>
        <span>·</span>
        有效营业额 <strong>¥{{ formatAmount(revenue) }}</strong>
      </p>
    </section>

    <section class="metric-grid" aria-label="核心指标">
      <article class="metric-card metric-users">
        <span class="metric-icon"><i class="fa fa-users" aria-hidden="true"></i></span>
        <div><h2>总用户人数</h2><strong aria-live="polite"><CountUp v-if="userCount !== null" :end-val="Number(userCount)" :duration="1.8" :decimal-places="0" /><span v-else>—</span></strong><p><i class="fa fa-arrow-up"></i> {{ userGrowthText }}</p><small>较上期变化</small></div>
      </article>
      <article class="metric-card metric-shops">
        <span class="metric-icon"><i class="fa fa-shopping-basket" aria-hidden="true"></i></span>
        <div><h2>总店铺数</h2><strong aria-live="polite"><CountUp v-if="shopCount !== null" :end-val="Number(shopCount)" :duration="1.8" :decimal-places="0" /><span v-else>—</span></strong><p><i class="fa fa-arrow-up"></i> {{ shopGrowthText }}</p><small>较上期变化</small></div>
      </article>
      <article class="metric-card metric-revenue">
        <span class="metric-icon"><i class="fa fa-bar-chart" aria-hidden="true"></i></span>
        <div><h2>总营业额</h2><strong aria-live="polite">¥<CountUp v-if="totalRevenue !== null" :end-val="Number(totalRevenue)" :duration="1.8" :decimal-places="1" /><span v-else>0.0</span></strong><p><i class="fa fa-arrow-up"></i> {{ revenueGrowthText }}</p><small>较上期变化</small></div>
      </article>
    </section>

    <section class="dashboard-panel chart-panel" aria-labelledby="trend-title">
      <div class="section-heading">
        <div><span class="section-mark"></span><h2 id="trend-title">营业额趋势</h2></div>
        <div class="range-switch" role="group" aria-label="趋势时间范围">
          <button v-for="range in trendRanges" :key="range.days" type="button" :class="{ active: selectedRange === range.days }" @click="selectTrendRange(range.days)">{{ range.label }}</button>
        </div>
      </div>
      <div v-if="trend.length" class="chart-wrap">
        <svg :key="chartAnimationKey" class="revenue-chart" viewBox="0 0 360 170" role="img" aria-label="营业额趋势图">
          <g class="chart-grid-lines">
            <line v-for="tick in chartTicks" :key="tick.value" x1="34" :y1="tick.y" x2="350" :y2="tick.y"></line>
            <text v-for="tick in chartTicks" :key="`label-${tick.value}`" x="0" :y="tick.y + 4">{{ tick.label }}</text>
          </g>
          <path class="chart-area" :d="chartAreaPath"></path>
          <polyline class="chart-line" :points="chartPolyline" pathLength="1"></polyline>
          <g class="chart-points">
            <circle v-for="(point, index) in chartPoints" :key="point.day" :style="{ '--point-delay': `${900 + index * 45}ms` }" :cx="point.x" :cy="point.y" r="3.7"></circle>
          </g>
          <g class="chart-x-labels">
            <text v-for="label in chartLabels" :key="label.text" :x="label.x" y="166" text-anchor="middle">{{ label.text }}</text>
          </g>
        </svg>
        <div v-if="chartPeak" class="chart-peak" :style="{ left: `${chartPeak.left}%` }">¥{{ formatAmount(chartPeak.revenue) }}</div>
      </div>
      <div v-else class="chart-empty"><i class="fa fa-line-chart" aria-hidden="true"></i><p>所选区间暂无已完成订单</p></div>
    </section>

    <section class="dashboard-panel order-overview" aria-labelledby="order-overview-title">
      <div class="section-heading compact-heading"><div><span class="section-mark"></span><h2 id="order-overview-title">订单概览</h2></div></div>
      <div class="overview-grid">
        <div><span>有效订单</span><strong>{{ completedCount }}</strong></div>
        <div><span>平均客单价</span><strong>¥{{ formatAmount(averageOrderValue) }}</strong></div>
        <div><span>今日订单</span><strong>{{ todayCompletedCount }}</strong></div>
        <div><span>今日营业额</span><strong>¥{{ formatAmount(todayRevenue) }}</strong></div>
      </div>
    </section>

    <section class="dashboard-panel review-panel" aria-labelledby="review-title">
      <div class="review-tabs" role="tablist" aria-labelledby="review-title">
        <button id="review-title" type="button" role="tab" :aria-selected="activeTab === 'user-review'" :class="{ active: activeTab === 'user-review' }" @click="activeTab = 'user-review'">用户审核<span v-if="merchantApplications.length > 0" class="review-badge review-badge-user">{{ merchantApplications.length }}</span></button>
        <button type="button" role="tab" :aria-selected="activeTab === 'shop-review'" :class="{ active: activeTab === 'shop-review' }" @click="activeTab = 'shop-review'">商铺审核<span v-if="shopApplications.length > 0" class="review-badge review-badge-shop">{{ shopApplications.length }}</span></button>
      </div>
      <div v-if="activeTab === 'user-review'" class="review-content">
        <div v-if="loadingMerchant" class="state-message">正在同步申请数据...</div>
        <div v-else-if="merchantApplications.length === 0" class="state-message"><Vue3Lottie :animation-data="emptyStateAnimation" :width="56" :height="56" :loop="true" :auto-play="true" aria-hidden="true" /><p>暂无待审核的商家申请</p><small>一切运行正常，辛苦了！</small></div>
        <div v-for="application in merchantApplications" :key="`merchant-${application.id}`" class="review-item">
          <div><strong>用户ID: {{ application.userId }}</strong><p>{{ application.username }}</p><small>{{ formatTime(application.createTime) }}</small></div>
          <button type="button" class="review-action" @click="handleMerchantReview(application)">审核</button>
        </div>
      </div>
      <div v-else class="review-content">
        <div v-if="loadingShop" class="state-message">正在同步申请数据...</div>
        <div v-else-if="shopApplications.length === 0" class="state-message"><Vue3Lottie :animation-data="emptyStateAnimation" :width="56" :height="56" :loop="true" :auto-play="true" aria-hidden="true" /><p>暂无待审核的店铺申请</p><small>一切运行正常，辛苦了！</small></div>
        <div v-for="application in shopApplications" :key="`shop-${application.id}`" class="review-item">
          <div><strong>{{ application.businessName }}</strong><p>{{ getShopTypeName(application.orderTypeId) }}</p><small>{{ formatTime(application.createTime) }}</small></div>
          <button type="button" class="review-action" @click="handleShopReview(application)">审核</button>
        </div>
      </div>
    </section>

    <div v-if="showReviewModal" class="modal-overlay" @click.self="closeModal">
      <section class="review-modal" role="dialog" aria-modal="true" :aria-labelledby="modalTitle">
        <header><h2 :id="modalTitle">{{ modalTitle }}</h2><button type="button" class="close-modal" aria-label="关闭" title="关闭" @click="closeModal">&times;</button></header>
        <div class="modal-body">
          <div v-if="modalType === 'merchant'">
            <div v-if="loadingPersonInfo" class="state-message">正在加载用户信息...</div>
            <div v-else class="detail-list">
              <p><span>用户名</span><strong>{{ currentMerchantApp?.username }}</strong></p>
              <p><span>用户ID</span><strong>{{ currentMerchantApp?.userId }}</strong></p>
              <p><span>手机号</span><strong>{{ currentPersonInfo?.phone || '未填写' }}</strong></p>
              <p><span>邮箱</span><strong>{{ currentPersonInfo?.email || '未填写' }}</strong></p>
              <p><span>申请时间</span><strong>{{ formatTime(currentMerchantApp?.createTime) }}</strong></p>
            </div>
          </div>
          <div v-else class="detail-list">
            <p><span>申请ID</span><strong>{{ currentShopApp?.id }}</strong></p>
            <p><span>店铺名称</span><strong>{{ currentShopApp?.businessName }}</strong></p>
            <p><span>店铺地址</span><strong>{{ currentShopApp?.businessAddress || '未填写' }}</strong></p>
            <p><span>店铺介绍</span><strong>{{ currentShopApp?.businessExplain || '未填写' }}</strong></p>
            <p><span>起送价</span><strong>¥{{ formatAmount(currentShopApp?.startPrice) }}</strong></p>
            <p><span>配送费</span><strong>¥{{ formatAmount(currentShopApp?.deliveryPrice) }}</strong></p>
            <p><span>店铺类型</span><strong>{{ getShopTypeName(currentShopApp?.orderTypeId) }}</strong></p>
            <img v-if="currentShopApp?.businessImg" :src="currentShopApp.businessImg" alt="店铺图片" class="shop-image" @error="handleImageError">
          </div>
        </div>
        <footer><button type="button" class="reject-button" @click="submitAudit(2)">拒绝</button><button type="button" class="approve-button" @click="submitAudit(1)">批准</button></footer>
      </section>
    </div>
  </main>
</template>

<script setup>
import { computed, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import CountUp from 'vue-countup-v3';
import { Vue3Lottie } from 'vue3-lottie';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { createRealtimeConnection } from '@/services/realtimeService';
import { clearAuth, getStoredUser, updateStoredUser } from '@/utils/auth';
import { DEFAULT_AVATAR_URL } from '@/utils/profileDefaults';
import emptyStateAnimation from '@/assets/animations/admin-empty-state.json';

const router = useRouter();
const currentUser = ref({});
const userCount = ref(null);
const shopCount = ref(null);
const totalRevenue = ref(null);
const statsFrom = ref('');
const statsTo = ref('');
const detailedStats = ref(null);
const selectedRange = ref(7);
const activeTab = ref('user-review');
const showReviewModal = ref(false);
const modalTitle = ref('');
const modalType = ref('');
const currentMerchantApp = ref(null);
const currentShopApp = ref(null);
const currentPersonInfo = ref(null);
const loadingMerchant = ref(false);
const loadingShop = ref(false);
const loadingPersonInfo = ref(false);
const merchantApplications = ref([]);
const shopApplications = ref([]);
const pageReady = ref(false);

const trendRanges = [{ days: 7, label: '近7天' }, { days: 30, label: '近30天' }, { days: 90, label: '近90天' }];
const shopTypeMap = { 1: '美食', 2: '早餐', 3: '跑腿代购', 4: '汉堡披萨', 5: '甜品饮品', 6: '速食简餐', 7: '地方小吃', 8: '米粉面馆', 9: '包子粥铺', 10: '炸鸡炸串' };
let realtimeConnection = null;
let pageEnterFrame = null;

const toDateInput = (date) => {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, '0');
  const day = String(date.getDate()).padStart(2, '0');
  return `${year}-${month}-${day}`;
};
const todayString = toDateInput(new Date());
const completedCount = computed(() => Number(detailedStats.value?.completedCount || 0));
const revenue = computed(() => Number(detailedStats.value?.revenue || 0));
const averageOrderValue = computed(() => Number(detailedStats.value?.averageOrderValue || 0));
const todayCompletedCount = computed(() => Number(detailedStats.value?.today?.completedCount || 0));
const todayRevenue = computed(() => Number(detailedStats.value?.today?.revenue || 0));
const trend = computed(() => Array.isArray(detailedStats.value?.trend) ? detailedStats.value.trend : []);
const pendingTotal = computed(() => merchantApplications.value.length + shopApplications.value.length);
const growth = computed(() => detailedStats.value?.growth || {});
const formatDelta = (value) => value === null || value === undefined ? '—' : `${Number(value) > 0 ? '+' : ''}${Number(value)}`;
const formatRate = (value) => value === null || value === undefined ? '—' : `${Number(value) > 0 ? '+' : ''}${Number(value).toFixed(1)}%`;
const userGrowthText = computed(() => formatDelta(growth.value.users));
const shopGrowthText = computed(() => formatDelta(growth.value.businesses));
const revenueGrowthText = computed(() => formatRate(growth.value.revenueRate));

const formatInteger = (value) => value === null || value === undefined ? '—' : Number(value).toLocaleString('zh-CN');
const formatAmount = (value) => Number(value || 0).toLocaleString('zh-CN', { minimumFractionDigits: 1, maximumFractionDigits: 1 });
const formatTime = (value) => value ? new Date(value).toLocaleString('zh-CN', { year: 'numeric', month: '2-digit', day: '2-digit', hour: '2-digit', minute: '2-digit' }) : '时间未知';
const getShopTypeName = (typeId) => shopTypeMap[typeId] || '未设置类型';

const chartValues = computed(() => trend.value.map(item => Number(item.revenue || 0)));
const chartMax = computed(() => Math.max(...chartValues.value, 1));
const chartTicks = computed(() => {
  const step = chartMax.value / 4;
  return Array.from({ length: 5 }, (_, index) => {
    const value = step * (4 - index);
    return { value, label: value >= 1000 ? `${(value / 1000).toFixed(1)}k` : Math.round(value), y: 18 + index * 32 };
  });
});
const chartPoints = computed(() => {
  const count = trend.value.length;
  return trend.value.map((item, index) => ({
    day: item.day,
    revenue: Number(item.revenue || 0),
    x: count === 1 ? 192 : 34 + (index * 316) / (count - 1),
    y: 146 - (Number(item.revenue || 0) / chartMax.value) * 128
  }));
});
const chartPolyline = computed(() => chartPoints.value.map(point => `${point.x},${point.y}`).join(' '));
const chartAreaPath = computed(() => {
  if (!chartPoints.value.length) return '';
  const first = chartPoints.value[0];
  const last = chartPoints.value[chartPoints.value.length - 1];
  return `M ${first.x} 146 L ${chartPoints.value.map(point => `${point.x} ${point.y}`).join(' L ')} L ${last.x} 146 Z`;
});
const chartLabels = computed(() => {
  const points = chartPoints.value;
  if (!points.length) return [];
  const indexes = [...new Set([0, Math.floor((points.length - 1) / 2), points.length - 1])];
  return indexes.map(index => ({ x: points[index].x, text: String(points[index].day || '').slice(5).replace('-', '/') }));
});
const chartPeak = computed(() => {
  const points = chartPoints.value;
  if (!points.length) return null;
  const peak = points.reduce((best, point) => point.revenue > best.revenue ? point : best, points[0]);
  return { revenue: peak.revenue, left: (peak.x / 360) * 100 };
});
const chartAnimationKey = computed(() => trend.value.map(item => `${item.day}:${item.revenue}`).join('|'));

onMounted(() => {
  pageEnterFrame = window.requestAnimationFrame(() => { pageReady.value = true; });
  const today = new Date();
  const from = new Date(today);
  from.setDate(today.getDate() - selectedRange.value + 1);
  statsFrom.value = toDateInput(from);
  statsTo.value = todayString;
  getCurrentUserInfo();
  getStatisticData();
  loadDetailedStats();
  getMerchantApplications();
  getShopApplications();
  realtimeConnection = createRealtimeConnection({ onMessage: handleWebSocketMessage });
  realtimeConnection.start();
});

onUnmounted(() => {
  if (pageEnterFrame !== null) window.cancelAnimationFrame(pageEnterFrame);
  realtimeConnection?.stop();
});

const getCurrentUserInfo = async () => {
  try {
    const savedUser = getStoredUser();
    if (savedUser) currentUser.value = savedUser;
    const response = await request.get('/api/user');
    if (response?.id) {
      currentUser.value = response;
      updateStoredUser(response);
    }
  } catch (error) {
    console.error('获取管理员信息失败:', error);
  }
};

const getStatisticData = async () => {
  try {
    const [userResponse, shopResponse, revenueResponse] = await Promise.all([
      request.get('/api/admin/countUser'), request.get('/api/admin/countBusiness'), request.get('/api/admin/countPrice')
    ]);
    if (userResponse?.success) userCount.value = userResponse.data;
    if (shopResponse?.success) shopCount.value = shopResponse.data;
    if (revenueResponse?.success) totalRevenue.value = revenueResponse.data;
  } catch (error) {
    console.error('获取首页统计失败:', error);
    toast.error('统计数据加载失败');
  }
};

const loadDetailedStats = async () => {
  if (!statsFrom.value || !statsTo.value) return;
  if (statsFrom.value > statsTo.value) {
    toast.warning('开始日期不能晚于结束日期');
    return;
  }
  try {
    const response = await request.get('/api/admin/statistics', { params: { from: statsFrom.value, to: statsTo.value } });
    if (response?.success) detailedStats.value = response.data;
  } catch (error) {
    console.error('统计数据加载失败:', error);
    toast.error('统计数据加载失败');
  }
};

const selectTrendRange = (days) => {
  selectedRange.value = days;
  const from = new Date();
  from.setDate(from.getDate() - days + 1);
  statsFrom.value = toDateInput(from);
  statsTo.value = todayString;
  loadDetailedStats();
};

const downloadStats = async () => {
  try {
    const blob = await request.get('/api/admin/statistics/export', { params: { from: statsFrom.value, to: statsTo.value }, responseType: 'blob' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.href = url;
    link.download = `statistics-${statsFrom.value}-${statsTo.value}.csv`;
    link.click();
    URL.revokeObjectURL(url);
  } catch (error) {
    console.error('统计导出失败:', error);
    toast.error('导出失败');
  }
};

const getMerchantApplications = async () => {
  loadingMerchant.value = true;
  try {
    const response = await request.get('/api/permission/merchant-applications');
    if (response?.success && Array.isArray(response.data)) merchantApplications.value = response.data;
  } catch (error) {
    console.error('获取商家申请失败:', error);
    toast.error('商家申请加载失败');
  } finally { loadingMerchant.value = false; }
};

const getShopApplications = async () => {
  loadingShop.value = true;
  try {
    const response = await request.get('/api/permission/shop-applications');
    if (response?.success && Array.isArray(response.data)) shopApplications.value = response.data;
  } catch (error) {
    console.error('获取店铺申请失败:', error);
    toast.error('店铺申请加载失败');
  } finally { loadingShop.value = false; }
};

const getPersonInfo = async (application) => {
  loadingPersonInfo.value = true;
  try {
    const response = await request.get('/api/admin/users', { params: { keyword: application.username } });
    currentPersonInfo.value = response?.success && response.data?.length ? response.data[0] : null;
  } catch (error) {
    console.error('获取用户信息失败:', error);
    currentPersonInfo.value = null;
  } finally { loadingPersonInfo.value = false; }
};

const handleMerchantReview = async (application) => {
  modalType.value = 'merchant';
  modalTitle.value = `商家申请审核 · ${application.username}`;
  currentMerchantApp.value = { ...application };
  showReviewModal.value = true;
  await getPersonInfo(application);
};

const handleShopReview = (application) => {
  modalType.value = 'shop';
  modalTitle.value = `店铺申请审核 · ${application.businessName}`;
  currentShopApp.value = application;
  showReviewModal.value = true;
};

const submitAudit = async (result) => {
  try {
    const response = modalType.value === 'merchant'
      ? await request.post('/api/permission/audit', { id: currentMerchantApp.value.id, auditResult: result })
      : await request.post('/api/permission/audit-shop', { id: currentShopApp.value.id, status: result });
    if (!response?.success) throw new Error(response?.message || '审核提交失败');
    toast.success(result === 1 ? '批准成功' : '拒绝成功');
    closeModal();
    await Promise.all([getMerchantApplications(), getShopApplications()]);
  } catch (error) {
    console.error('审核提交失败:', error);
    toast.error(error.message || '审核提交失败');
  }
};

const handleWebSocketMessage = (message) => {
  toast.info(message.notificationContent || message.content || '有新的审核任务');
  if (message.type === 0) getMerchantApplications();
  if (message.type === 1) getShopApplications();
};

const closeModal = () => {
  showReviewModal.value = false;
  currentMerchantApp.value = null;
  currentShopApp.value = null;
  currentPersonInfo.value = null;
  modalType.value = '';
  modalTitle.value = '';
};

const logout = () => {
  realtimeConnection?.stop();
  realtimeConnection = null;
  clearAuth();
  toast.success('已成功退出登录');
  router.replace({ path: '/login', query: { role: 'admin' } });
};

const handleAvatarError = (event) => {
  if (event.target.dataset.fallbackApplied === 'true') return;
  event.target.dataset.fallbackApplied = 'true';
  event.target.src = DEFAULT_AVATAR_URL;
};
const handleImageError = (event) => {
  event.target.style.display = 'none';
};
</script>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(body) { background: var(--skin-surface); }
.admin-home { min-height: 100%; padding: 0 0 86px; background: var(--skin-surface); color: var(--skin-ink); opacity: 0; transform: translateY(12px); will-change: opacity, transform; }
.admin-home.admin-home-ready { animation: admin-home-enter 500ms cubic-bezier(.22, .61, .36, 1) both; }
.admin-home .profile-panel { opacity: 0; transform: translateY(-24px); will-change: opacity, transform; }
.admin-home.admin-home-ready .profile-panel { animation: admin-profile-enter 500ms cubic-bezier(.22, .61, .36, 1) 100ms both; }
.admin-home .metric-card { opacity: 0; transform: translateY(18px); will-change: opacity, transform; }
.admin-home.admin-home-ready .metric-card { animation: admin-metric-enter 500ms cubic-bezier(.22, .61, .36, 1) both; }
.admin-home.admin-home-ready .metric-card:nth-child(1) { animation-delay: 200ms; }
.admin-home.admin-home-ready .metric-card:nth-child(2) { animation-delay: 300ms; }
.admin-home.admin-home-ready .metric-card:nth-child(3) { animation-delay: 400ms; }
@keyframes admin-home-enter { from { opacity: 0; transform: translateY(12px); } to { opacity: 1; transform: translateY(0); } }
@keyframes admin-profile-enter { from { opacity: 0; transform: translateY(-24px); } to { opacity: 1; transform: translateY(0); } }
@keyframes admin-metric-enter { from { opacity: 0; transform: translateY(18px); } to { opacity: 1; transform: translateY(0); } }
.hero-panel { position: relative; min-height: 194px; padding: 17px 28px 28px; overflow: hidden; color: #fff; background: linear-gradient(135deg, var(--skin-brand) 0%, var(--skin-brand-soft) 100%); border-radius: 0 0 20px 20px; }
.hero-panel::before,.hero-panel::after { content: ''; position: absolute; border-radius: 50%; background: rgba(255,255,255,.09); pointer-events: none; }
.hero-panel::before { width: 150px; height: 150px; top: -78px; right: 28%; }
.hero-panel::after { width: 114px; height: 114px; top: -46px; right: -9px; }
.hero-topline { position: relative; z-index: 1; display: flex; justify-content: space-between; align-items: center; font-size: 13px; opacity: .94; }
.hero-panel h1 { position: relative; z-index: 1; margin: 30px 0 3px; font-size: 30px; line-height: 1.15; letter-spacing: 0; }
.hero-panel p { position: relative; z-index: 1; margin: 0; font-size: 16px; }
.hero-line { position: absolute; right: 82px; bottom: 34px; width: 166px; height: 26px; border-top: 2px solid rgba(255,255,255,.82); border-radius: 50%; transform: rotate(-12deg); }
.hero-line::after { content: '让美食更好地抵达'; position: absolute; right: -3px; top: -37px; width: 180px; font-family: 'KaiTi','STKaiti',serif; font-size: 18px; transform: rotate(4deg); }
.hero-actions { display: flex; align-items: center; gap: 5px; }
.icon-button { position: relative; z-index: 1; width: 40px; height: 40px; border: 0; color: #fff; background: transparent; font-size: 24px; cursor: pointer; }
.logout-button { position: relative; z-index: 1; display: inline-flex; min-width: 78px; height: 34px; align-items: center; justify-content: center; gap: 5px; padding: 0 9px; border: 1px solid rgba(255,255,255,.52); border-radius: 8px; color: #fff; background: rgba(255,255,255,.14); font: inherit; font-size: 12px; font-weight: 600; white-space: nowrap; cursor: pointer; transition: background 180ms ease, transform 180ms ease; }
.logout-button:hover,.logout-button:focus-visible { background: rgba(255,255,255,.25); outline: none; }
.logout-button:active { transform: scale(.97); }
.logout-button i { font-size: 13px; }
.notification-dot { position: absolute; top: 4px; right: 2px; width: 9px; height: 9px; border-radius: 50%; background: #ff4d4f; }
.profile-panel { position: relative; z-index: 2; display: flex; align-items: center; gap: 16px; min-height: 168px; margin: -26px 14px 18px; padding: 23px 20px; overflow: hidden; background: rgba(255,255,255,.97); border-radius: 20px; box-shadow: 0 10px 28px rgba(var(--skin-brand-rgb),.10); }
.profile-avatar { flex: none; width: 98px; height: 98px; overflow: hidden; border: 4px solid #fff; border-radius: 50%; background: var(--skin-surface); box-shadow: 0 5px 16px rgba(var(--skin-brand-rgb),.20); }
.profile-avatar img { display: block; width: 100%; height: 100%; object-fit: cover; }
.profile-copy { min-width: 0; }
.profile-name-row { display: flex; align-items: center; gap: 9px; margin-bottom: 9px; }
.profile-name-row strong { max-width: 170px; overflow: hidden; font-size: 23px; color: var(--skin-ink); text-overflow: ellipsis; white-space: nowrap; }
.role-badge { display: inline-flex; align-items: center; gap: 5px; padding: 7px 11px; color: var(--skin-brand); background: color-mix(in srgb, var(--skin-brand) 12%, white); border-radius: 12px; font-size: 13px; white-space: nowrap; }
.profile-copy p { margin: 4px 0; color: var(--skin-muted); font-size: 15px; }
.profile-copy .profile-meta { color: var(--skin-subtle, var(--skin-muted)); font-size: 13px; }
.profile-note { position: absolute; right: 12px; bottom: 22px; display: flex; align-items: center; gap: 9px; color: color-mix(in srgb, var(--skin-brand) 42%, white); font-family: 'KaiTi','STKaiti',serif; font-size: 14px; line-height: 1.4; transform: rotate(-4deg); }
.profile-note i { font-size: 32px; opacity: .6; }
.dashboard-panel { margin: 0 14px 18px; padding: 20px 22px; background: rgba(255,255,255,.98); border-radius: 20px; box-shadow: 0 8px 24px rgba(var(--skin-brand-rgb),.07); }
.filter-panel { padding-top: 23px; }
.date-row { display: grid; grid-template-columns: auto minmax(0,1fr) auto minmax(0,1fr); align-items: center; gap: 12px; }
.filter-label { color: var(--skin-muted); font-size: 18px; font-weight: 600; white-space: nowrap; }
.date-separator { color: var(--skin-muted); font-size: 18px; }
.date-input { position: relative; display: block; min-width: 0; }
.date-input input { width: 100%; min-height: 48px; padding: 0 36px 0 14px; color: var(--skin-ink); font: inherit; font-size: 16px; border: 1px solid var(--skin-border); border-radius: 11px; background: #fff; }
.date-input i { position: absolute; top: 16px; right: 13px; color: var(--skin-ink); pointer-events: none; }
.filter-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 14px; margin-top: 18px; }
.filter-actions button { min-height: 48px; border-radius: 11px; font: inherit; font-size: 17px; cursor: pointer; }
.primary-action { border: 0; color: #fff; background: linear-gradient(90deg,var(--skin-brand),var(--skin-brand-soft)); box-shadow: 0 7px 14px rgba(var(--skin-brand-rgb),.18); }
.outline-action { border: 2px solid var(--skin-brand); color: var(--skin-brand); background: #fff; }
.filter-summary { margin: 17px 0 0; color: var(--skin-muted); font-size: 16px; }
.filter-summary strong { color: var(--skin-ink); font-weight: 500; }
.filter-summary span { margin: 0 7px; color: var(--skin-subtle, var(--skin-muted)); }
.metric-grid { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 14px; margin: 0 14px 18px; }
.metric-card { display: flex; align-items: flex-start; gap: 10px; min-height: 196px; padding: 22px 13px 17px; border-radius: 20px; background: #fff; box-shadow: 0 8px 24px rgba(var(--skin-brand-rgb),.07); }
.metric-users { background: color-mix(in srgb, var(--skin-brand) 4%, white); }.metric-shops { background: color-mix(in srgb, #ff962b 4%, white); }.metric-revenue { background: color-mix(in srgb, #10af7a 4%, white); }
.metric-icon { display: grid; flex: none; width: 42px; height: 42px; place-items: center; font-size: 29px; }.metric-users .metric-icon { color:var(--skin-brand); }.metric-shops .metric-icon { color:#ff962b; }.metric-revenue .metric-icon { color:#10af7a; }
.metric-card h2 { margin: 0 0 13px; color: var(--skin-ink); font-size: 16px; font-weight: 500; white-space: nowrap; }.metric-card strong { display: block; color: var(--skin-ink); font-size: 27px; line-height: 1; white-space: nowrap; }.metric-card p { margin: 16px 0 1px; color: #0aa966; font-size: 16px; font-weight: 600; white-space: nowrap; }.metric-card p i { margin-right: 4px; }.metric-card small { color: var(--skin-muted); font-size: 13px; white-space: nowrap; }
.section-heading { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 16px; }.section-heading > div:first-child { display: flex; align-items: center; gap: 12px; }.section-heading h2 { margin: 0; color: var(--skin-ink); font-size: 21px; }.section-mark { width: 7px; height: 31px; border-radius: 5px; background: var(--skin-brand); }.compact-heading { margin-bottom: 17px; }
.range-switch { display: flex; gap: 2px; padding: 3px; border-radius: 13px; background: var(--skin-surface); }.range-switch button { min-height: 38px; padding: 0 11px; border: 0; border-radius: 11px; color: var(--skin-muted); background: transparent; font: inherit; font-size: 14px; cursor: pointer; }.range-switch button.active { color: #fff; background: var(--skin-brand); box-shadow: 0 4px 10px rgba(var(--skin-brand-rgb),.2); }
.chart-panel { min-height: 292px; padding-bottom: 14px; }.chart-wrap { position: relative; height: 194px; }.revenue-chart { display: block; width: 100%; height: 100%; overflow: visible; }.chart-grid-lines line { stroke: var(--skin-border); stroke-width: 1; }.chart-grid-lines text,.chart-x-labels text { fill: var(--skin-muted); font-size: 10px; }.chart-area { fill: rgba(var(--skin-brand-rgb),.14); opacity: 0; animation: chart-area-reveal 700ms cubic-bezier(.33,1,.68,1) 500ms forwards; }.chart-line { fill: none; stroke: var(--skin-brand); stroke-width: 3.4; stroke-linecap: round; stroke-linejoin: round; stroke-dasharray: 1; stroke-dashoffset: 1; animation: chart-line-draw 1500ms cubic-bezier(.33,1,.68,1) forwards; will-change: stroke-dashoffset; }.chart-points circle { fill: #fff; stroke: var(--skin-brand); stroke-width: 3; opacity: 0; animation: chart-point-in 260ms ease-out var(--point-delay) forwards; }.chart-peak { position: absolute; top: 6px; padding: 6px 10px; color: #fff; background: var(--skin-brand); border-radius: 8px; font-size: 13px; font-weight: 600; transform: translateX(-50%); box-shadow: 0 5px 12px rgba(var(--skin-brand-rgb),.22); }.chart-empty,.state-message { display: grid; place-items: center; min-height: 170px; color: var(--skin-muted); text-align: center; }.chart-empty i,.state-message > i { margin-bottom: 9px; color: var(--skin-border); font-size: 38px; }.chart-empty p,.state-message p { margin: 0; font-size: 16px; }.state-message small { display: block; margin-top: 5px; color: var(--skin-subtle, var(--skin-muted)); font-size: 13px; }
.overview-grid { display: grid; grid-template-columns: repeat(4,1fr); padding: 14px 4px; border-radius: 14px; background: var(--skin-surface); }.overview-grid div { min-width: 0; padding: 0 8px; text-align: center; border-right: 1px solid var(--skin-border); }.overview-grid div:last-child { border-right: 0; }.overview-grid span,.overview-grid strong { display: block; white-space: nowrap; }.overview-grid span { color: var(--skin-muted); font-size: 14px; }.overview-grid strong { margin-top: 11px; color: var(--skin-ink); font-size: 23px; }
.review-panel { padding: 0; overflow: hidden; }.review-tabs { display: grid; grid-template-columns: 1fr 1fr; border-bottom: 1px solid var(--skin-border); }.review-tabs button { position: relative; display: inline-flex; align-items: center; justify-content: center; gap: 7px; min-height: 70px; border: 0; color: var(--skin-muted); background: transparent; font: inherit; font-size: 20px; font-weight: 600; cursor: pointer; }.review-tabs button.active { color: var(--skin-brand); }.review-tabs button.active::after { content: ''; position: absolute; right: 9%; bottom: -1px; left: 9%; height: 4px; background: var(--skin-brand); }.review-badge { display: inline-grid; min-width: 22px; height: 22px; padding: 0 6px; place-items: center; border-radius: 999px; color: #fff; font-size: 12px; font-weight: 700; line-height: 1; animation: review-badge-pulse 1600ms ease-in-out infinite; transform-origin: center; }.review-badge-user { background: var(--admin-review-user, #2196f3); }.review-badge-shop { background: var(--admin-review-shop, #ef5350); }.review-content { min-height: 174px; padding: 8px 22px 18px; }.review-item { display: flex; align-items: center; justify-content: space-between; gap: 12px; padding: 15px 0; border-bottom: 1px solid var(--skin-border); }.review-item:last-child { border-bottom: 0; }.review-item strong,.review-item p,.review-item small { display: block; }.review-item strong { color: var(--skin-ink); font-size: 16px; }.review-item p { margin: 5px 0 0; color: var(--skin-muted); font-size: 14px; }.review-item small { margin-top: 3px; color: var(--skin-subtle, var(--skin-muted)); font-size: 12px; }.review-action { flex: none; min-width: 64px; min-height: 36px; border: 0; border-radius: 9px; color: #fff; background: var(--skin-brand); font: inherit; cursor: pointer; }
.modal-overlay { position: fixed; z-index: 3000; inset: 0; display: grid; place-items: center; padding: 18px; background: rgba(var(--skin-brand-strong-rgb),.55); }.review-modal { width: min(100%,460px); max-height: calc(100dvh - 36px); overflow: auto; border-radius: 18px; background: #fff; box-shadow: 0 18px 50px rgba(var(--skin-brand-strong-rgb),.24); }.review-modal header { display: flex; align-items: center; justify-content: space-between; padding: 19px 20px; border-bottom: 1px solid var(--skin-border); }.review-modal h2 { margin: 0; color: var(--skin-ink); font-size: 18px; }.close-modal { border: 0; color: var(--skin-muted); background: transparent; font-size: 27px; line-height: 1; cursor: pointer; }.modal-body { padding: 18px 20px; }.detail-list { display: grid; gap: 9px; }.detail-list p { display: grid; grid-template-columns: 72px minmax(0,1fr); gap: 12px; margin: 0; padding: 10px 12px; border-radius: 9px; background: var(--skin-surface); font-size: 14px; }.detail-list span { color: var(--skin-muted); }.detail-list strong { overflow-wrap: anywhere; color: var(--skin-ink); font-weight: 500; }.shop-image { display: block; max-width: 200px; max-height: 140px; margin-top: 5px; border-radius: 10px; object-fit: cover; }.review-modal footer { display: flex; justify-content: flex-end; gap: 10px; padding: 14px 20px 20px; }.review-modal footer button { min-width: 86px; min-height: 42px; border-radius: 9px; font: inherit; cursor: pointer; }.reject-button { border: 1px solid var(--skin-border); color: var(--skin-muted); background: #fff; }.approve-button { border: 0; color: #fff; background: var(--skin-brand); }.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
@media (max-width: 560px) { .hero-panel { min-height: 178px; padding-inline: 20px; }.hero-panel h1 { margin-top: 28px; font-size: 27px; }.hero-line { right: 48px; bottom: 25px; }.hero-line::after { font-size: 16px; }.profile-panel { min-height: 151px; margin-inline: 12px; padding: 17px 13px; gap: 11px; }.profile-avatar { width: 76px; height: 76px; }.profile-name-row { gap: 6px; }.profile-name-row strong { max-width: 112px; font-size: 18px; }.role-badge { padding: 5px 7px; font-size: 11px; }.profile-copy p { font-size: 13px; }.profile-copy .profile-meta { font-size: 11px; }.profile-note { right: 9px; bottom: 15px; font-size: 11px; }.profile-note i { font-size: 25px; }.dashboard-panel { margin-inline: 12px; padding-inline: 15px; }.date-row { grid-template-columns: auto minmax(0,1fr) auto minmax(0,1fr); gap: 7px; }.filter-label,.date-separator { font-size: 15px; }.date-input input { min-height: 44px; padding-left: 8px; padding-right: 28px; font-size: 13px; }.date-input i { top: 14px; right: 8px; }.filter-actions { gap: 9px; }.filter-actions button { min-height: 44px; font-size: 15px; }.filter-summary { font-size: 14px; }.metric-grid { gap: 8px; margin-inline: 12px; }.metric-card { display: block; min-height: 178px; padding: 14px 8px; text-align: center; }.metric-icon { width: 100%; height: 34px; margin-bottom: 8px; font-size: 25px; }.metric-card h2 { margin-bottom: 12px; font-size: 13px; }.metric-card strong { font-size: 22px; }.metric-card p { margin-top: 14px; font-size: 13px; }.metric-card small { font-size: 11px; }.section-heading h2 { font-size: 19px; }.range-switch button { min-height: 34px; padding-inline: 8px; font-size: 12px; }.chart-panel { min-height: 260px; }.chart-wrap { height: 165px; }.overview-grid span { font-size: 11px; }.overview-grid strong { font-size: 18px; }.review-tabs button { min-height: 62px; font-size: 17px; }.review-content { padding-inline: 15px; } }
@media (max-width: 380px) { .profile-note { display: none; }.date-row { gap: 5px; }.filter-label,.date-separator { font-size: 13px; }.metric-card strong { font-size: 19px; }.metric-card h2 { font-size: 12px; }.overview-grid { padding-inline: 0; }.overview-grid div { padding-inline: 4px; } }
.state-message > .lottie-animation-container { margin-bottom: 9px; }
.metric-card strong .countup-wrap { display: inline; }
@keyframes chart-line-draw { to { stroke-dashoffset: 0; } }
@keyframes chart-area-reveal { from { opacity: 0; } to { opacity: 1; } }
@keyframes chart-point-in { from { opacity: 0; transform: scale(.7); } to { opacity: 1; transform: scale(1); } }
@keyframes review-badge-pulse { 0%, 100% { transform: scale(1); } 50% { transform: scale(1.15); } }
@media (prefers-reduced-motion: reduce) {
  .admin-home,
  .admin-home .profile-panel,
  .admin-home .metric-card {
    opacity: 1;
    transform: none;
    animation: none;
    will-change: auto;
  }
  .chart-line { stroke-dashoffset: 0; animation: none; will-change: auto; }
  .chart-area,
  .chart-points circle { opacity: 1; animation: none; }
  .review-badge { animation: none; transform: none; }
}
</style>

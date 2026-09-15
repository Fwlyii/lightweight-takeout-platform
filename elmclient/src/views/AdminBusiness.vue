<template>
  <main class="admin-business-page" aria-labelledby="business-management-title">
    <header class="business-hero">
      <button class="hero-back" type="button" aria-label="返回管理员首页" @click="router.push('/admin/home')">
        <i class="fa fa-chevron-left" aria-hidden="true"></i>
        <span>返回</span>
      </button>
      <div class="hero-copy">
        <h1 id="business-management-title">商铺管理</h1>
      </div>
      <div class="hero-decoration" aria-hidden="true">
        <i class="fa fa-shopping-bag"></i>
        <span>美好生活<br><small>从优质商家开始</small></span>
      </div>
    </header>

    <section class="business-content">
      <section class="business-stats" aria-label="商铺统计">
        <article class="stat-card stat-total">
          <span class="stat-icon"><i class="fa fa-store" aria-hidden="true"></i></span>
          <div><strong>{{ totalCount }}</strong><span>商铺总数</span></div>
        </article>
        <article class="stat-card stat-operating">
          <span class="stat-icon"><i class="fa fa-store" aria-hidden="true"></i></span>
          <div><strong>{{ operatingCount }}</strong><span>营业中</span></div>
        </article>
        <article class="stat-card stat-pending">
          <span class="stat-icon"><i class="fa fa-clock-o" aria-hidden="true"></i></span>
          <div><strong>{{ pendingCount }}</strong><span>待审核</span></div>
        </article>
        <article class="stat-card stat-closed">
          <span class="stat-icon"><i class="fa fa-minus" aria-hidden="true"></i></span>
          <div><strong>{{ closedCount }}</strong><span>已停业</span></div>
        </article>
      </section>

      <label class="business-search">
        <i class="fa fa-search" aria-hidden="true"></i>
        <span class="sr-only">搜索商铺名称、联系人或手机号</span>
        <input v-model.trim="keyword" type="search" placeholder="搜索商铺名称、联系人或手机号">
      </label>

      <div class="business-toolbar">
        <nav class="business-tabs" aria-label="商铺状态筛选" role="tablist">
          <button v-for="tab in tabs" :key="tab.key" type="button" role="tab"
            :aria-selected="activeTab === tab.key" :class="{ active: activeTab === tab.key }"
            @click="activeTab = tab.key">
            {{ tab.label }}
          </button>
        </nav>
        <label class="sort-control">
          <span class="sr-only">商铺排序</span>
          <select v-model="sortBy" aria-label="商铺排序">
            <option value="created">最新创建</option>
            <option value="rating">评分最高</option>
            <option value="orders">月订单最多</option>
          </select>
          <i class="fa fa-chevron-down" aria-hidden="true"></i>
        </label>
      </div>

      <p v-if="loading" class="list-state" role="status" aria-live="polite">正在加载商铺数据...</p>
      <p v-else-if="loadError" class="list-state error-state" role="alert">{{ loadError }}</p>
      <section v-else class="business-list" aria-label="商铺列表">
        <article v-for="business in filteredBusinesses" :key="business.id" class="business-card">
          <div class="business-main">
            <div class="business-logo-wrap">
              <img :src="business.businessImg || defaultImg" :alt="business.businessName || '商铺图片'"
                class="business-logo" @error="onImgError">
            </div>
            <div class="business-details">
              <h2>{{ business.businessName || '未命名商铺' }}</h2>
              <p class="phone-row"><i class="fa fa-phone" aria-hidden="true"></i>{{ ownerPhone(business) }}</p>
              <div class="business-tags">
                <span class="status-tag" :class="statusClass(business)">{{ statusLabel(business) }}</span>
                <span class="audit-tag">{{ auditLabel(business) }}</span>
              </div>
              <p class="business-metrics">
                <span class="rating"><i class="fa fa-star" aria-hidden="true"></i>{{ ratingLabel(business) }}</span>
                <span class="metric-divider" aria-hidden="true"></span>
                <span>月订单 {{ ordersLabel(business) }}</span>
              </p>
            </div>
          </div>
          <button class="view-business" type="button" @click="enterBusiness(business)">
            <span>查看商铺</span><i class="fa fa-angle-right" aria-hidden="true"></i>
          </button>
        </article>
      </section>

      <div v-if="!loading && !loadError && filteredBusinesses.length === 0" class="empty-state">
        <i class="fa fa-store" aria-hidden="true"></i>
        <p>{{ keyword ? '没有匹配的商铺' : '暂无商铺数据' }}</p>
        <small>{{ keyword ? '请尝试其他商铺名称或联系人' : '商家提交申请后会显示在这里' }}</small>
      </div>
    </section>
  </main>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { DEFAULT_AVATAR_URL } from '../utils/profileDefaults';

const router = useRouter();
const defaultImg = require('@/assets/business-default.png');
const businesses = ref([]);
const ownerDirectory = ref(new Map());
const activeSummaries = ref(new Map());
const keyword = ref('');
const activeTab = ref('all');
const sortBy = ref('created');
const loading = ref(false);
const loadError = ref('');

const tabs = [
  { key: 'all', label: '全部商铺' },
  { key: 'operating', label: '营业中' },
  { key: 'pending', label: '待审核' },
  { key: 'closed', label: '已停业' }
];

const toArray = value => Array.isArray(value) ? value : [];
const responseData = (response, fallback = []) => response?.success && Array.isArray(response.data) ? response.data : fallback;
const ownerIdOf = business => business?.userId || business?.businessOwner?.id || null;

const totalCount = computed(() => businesses.value.length);
const operatingCount = computed(() => businesses.value.filter(isOperating).length);
const pendingCount = computed(() => businesses.value.filter(business => Number(business.status) === 0).length);
const closedCount = computed(() => businesses.value.filter(isClosed).length);

const enrichedBusinesses = computed(() => businesses.value.map(business => ({
  ...business,
  _owner: ownerDirectory.value.get(ownerIdOf(business)) || business.businessOwner || null,
  _summary: activeSummaries.value.get(Number(business.id)) || null
})));

const filteredBusinesses = computed(() => {
  const query = keyword.value.toLowerCase();
  const list = enrichedBusinesses.value.filter(business => {
    const matchesTab = activeTab.value === 'all' ||
      (activeTab.value === 'operating' && isOperating(business)) ||
      (activeTab.value === 'pending' && Number(business.status) === 0) ||
      (activeTab.value === 'closed' && isClosed(business));
    const searchable = [business.businessName, business.businessAddress, business._owner?.username,
      business._owner?.phone].filter(Boolean).join(' ').toLowerCase();
    return matchesTab && (!query || searchable.includes(query));
  });
  return list.sort((left, right) => {
    if (sortBy.value === 'rating') return numeric(right._summary?.score) - numeric(left._summary?.score);
    if (sortBy.value === 'orders') return numeric(right._summary?.salesCount) - numeric(left._summary?.salesCount);
    return dateValue(right.createTime) - dateValue(left.createTime);
  });
});

const numeric = value => Number.isFinite(Number(value)) ? Number(value) : -1;
const dateValue = value => value ? new Date(value).getTime() || 0 : 0;
const isOperating = business => Number(business.status) === 1 && business.operatingStatus !== false;
const isClosed = business => Number(business.status) === 1 && business.operatingStatus === false;

const loadBusinesses = async () => {
  loading.value = true;
  loadError.value = '';
  try {
    const [businessResponse, ownerResponse, summaryResponse] = await Promise.all([
      request.get('/api/businesses'),
      request.get('/api/businesses/active'),
      request.get('/api/businesses/search')
    ]);
    businesses.value = responseData(businessResponse);
    ownerDirectory.value = new Map(toArray(ownerResponse).map(owner => [Number(owner.userId), owner]));
    activeSummaries.value = new Map(responseData(summaryResponse).map(summary => [Number(summary.id), summary]));
  } catch (error) {
    console.error('获取商铺列表失败:', error);
    businesses.value = [];
    loadError.value = error?.response?.data?.message || '商铺数据加载失败，请稍后重试';
  } finally {
    loading.value = false;
  }
};

const ownerPhone = business => business._owner?.phone || '手机号未填写';
const ratingLabel = business => business._summary?.score == null ? '暂无评分' : Number(business._summary.score).toFixed(1);
const ordersLabel = business => business._summary?.salesCount == null ? '--' : Number(business._summary.salesCount).toLocaleString('zh-CN');
const statusLabel = business => Number(business.status) === 0 ? '待审核' : Number(business.status) === 2 ? '已拒绝' : isClosed(business) ? '已停业' : '营业中';
const statusClass = business => Number(business.status) === 0 ? 'status-pending' : Number(business.status) === 2 ? 'status-rejected' : isClosed(business) ? 'status-closed' : 'status-operating';
const auditLabel = business => Number(business.status) === 1 ? '审核通过' : Number(business.status) === 0 ? '资料审核中' : '审核未通过';

const onImgError = event => { event.target.src = defaultImg || DEFAULT_AVATAR_URL; };
const enterBusiness = business => {
  const ownerId = ownerIdOf(business);
  if (!ownerId) return;
  router.push({
    path: '/admin/shop',
    query: {
      ownerId,
      merchantName: business._owner?.username ? `${business._owner.username}集团` : '商家店铺'
    }
  });
};

onMounted(loadBusinesses);
</script>

<style scoped>
:global(*) { box-sizing: border-box; }
:global(body) { background: var(--skin-surface, #f4f8fc); }
.admin-business-page { width: 100%; min-height: 100%; padding-bottom: calc(76px + env(safe-area-inset-bottom)); overflow-x: hidden; color: var(--skin-ink, #253b50); background: var(--skin-surface, #f4f8fc); }
.business-hero { position: relative; display: grid; grid-template-columns: 78px minmax(0, 1fr) 150px; align-items: center; min-height: 152px; padding: 18px 21px 22px; overflow: hidden; color: #fff; background: linear-gradient(135deg, var(--skin-brand-soft, #58a8ed) 0%, var(--skin-brand, #1882ed) 100%); }
.business-hero::after { content: ''; position: absolute; right: -74px; bottom: -124px; width: 240px; height: 240px; border: 1px solid rgba(255,255,255,.12); border-radius: 50%; }
.hero-back { position: relative; z-index: 1; display: inline-flex; align-items: center; gap: 9px; min-height: 44px; padding: 0; border: 0; color: #fff; background: transparent; font: inherit; font-size: 18px; cursor: pointer; }
.hero-back i { font-size: 23px; }
.hero-copy { position: relative; z-index: 1; text-align: center; }
.hero-copy h1 { margin: 0; font-size: 29px; line-height: 1.2; letter-spacing: 0; }
.hero-decoration { position: relative; z-index: 1; display: flex; align-items: center; justify-content: flex-end; gap: 10px; color: rgba(255,255,255,.28); text-align: right; }
.hero-decoration > i { font-size: 55px; }
.hero-decoration span { font-family: 'KaiTi', 'STKaiti', cursive; font-size: 17px; line-height: 1.75; white-space: nowrap; }
.hero-decoration small { font-size: 12px; }
.business-content { position: relative; margin-top: -1px; padding: 30px 20px 16px; border-radius: 21px 21px 0 0; background: var(--skin-surface, #f4f8fc); }
.business-stats { display: grid; grid-template-columns: repeat(4, minmax(0, 1fr)); gap: 15px; }
.stat-card { display: flex; min-width: 0; min-height: 134px; align-items: center; justify-content: center; gap: 11px; padding: 15px 10px; border: 1px solid rgba(var(--skin-border-rgb, 225,232,240), .9); border-radius: 13px; background: rgba(255,255,255,.9); box-shadow: 0 7px 20px rgba(var(--skin-brand-strong-rgb, 8,109,169), .07); }
.stat-icon { display: grid; width: 44px; height: 44px; flex: 0 0 44px; place-items: center; border-radius: 50%; font-size: 22px; }
.stat-card strong, .stat-card span:not(.stat-icon) { display: block; }
.stat-card strong { color: var(--skin-ink, #1d304b); font-size: 32px; line-height: 1; }
.stat-card span:not(.stat-icon) { margin-top: 10px; color: var(--skin-muted, #6e7b8d); font-size: 16px; white-space: nowrap; }
.stat-total .stat-icon { color: var(--skin-brand, #258be8); background: var(--skin-surface, #dceeff); }
.stat-operating .stat-icon { color: #25b38b; background: #d9f6ed; }
.stat-pending .stat-icon { color: #ff9d21; background: #fff0d8; }
.stat-closed .stat-icon { color: #f25865; background: #ffe2e4; }
.business-search { display: flex; align-items: center; min-height: 76px; margin: 29px 0 25px; padding: 0 28px; border: 2px solid var(--skin-border, #dbe8f4); border-radius: 40px; background: rgba(255,255,255,.28); }
.business-search i { margin-right: 19px; color: var(--skin-muted, #707c8d); font-size: 29px; }
.business-search input { width: 100%; min-width: 0; min-height: 56px; padding: 0; border: 0; outline: 0; color: var(--skin-ink, #253b50); background: transparent; font: inherit; font-size: 21px; }
.business-search input::placeholder { color: var(--skin-muted, #9ba7b7); }
.business-toolbar { display: flex; align-items: center; justify-content: space-between; gap: 10px; margin-bottom: 21px; }
.business-tabs { display: flex; min-width: 0; gap: 10px; overflow-x: auto; }
.business-tabs button { min-height: 54px; padding: 0 20px; border: 0; border-radius: 14px; color: var(--skin-muted, #687487); background: rgba(255,255,255,.45); font: inherit; font-size: 19px; white-space: nowrap; cursor: pointer; transition: color 180ms ease, background 180ms ease, box-shadow 180ms ease; }
.business-tabs button.active { color: #fff; background: var(--skin-brand, #278fea); box-shadow: 0 7px 16px rgba(var(--skin-brand-rgb, 22,139,209), .2); }
.sort-control { position: relative; display: inline-flex; flex: 0 0 auto; align-items: center; min-height: 54px; color: var(--skin-ink, #586679); }
.sort-control select { appearance: none; min-height: 54px; padding: 0 30px 0 9px; border: 0; outline: 0; color: inherit; background: transparent; font: inherit; font-size: 18px; cursor: pointer; }
.sort-control i { position: absolute; right: 7px; pointer-events: none; font-size: 15px; }
.business-list { display: grid; gap: 21px; }
.business-card { display: flex; min-height: 204px; align-items: center; justify-content: space-between; gap: 20px; padding: 27px 28px; border: 1px solid rgba(255,255,255,.95); border-radius: 17px; background: rgba(255,255,255,.95); box-shadow: 0 8px 22px rgba(var(--skin-brand-strong-rgb, 8,109,169), .07); }
.business-main { display: flex; min-width: 0; align-items: flex-start; gap: 27px; }
.business-logo-wrap { width: 112px; height: 112px; flex: 0 0 112px; overflow: hidden; border: 1px solid #f1cf46; border-radius: 50%; background: #fff; }
.business-logo { display: block; width: 100%; height: 100%; object-fit: cover; }
.business-details { min-width: 0; padding-top: 1px; }
.business-details h2 { overflow: hidden; margin: 0 0 9px; color: var(--skin-ink, #1e314d); font-size: 26px; line-height: 1.2; text-overflow: ellipsis; white-space: nowrap; }
.phone-row { display: flex; align-items: center; gap: 11px; margin-bottom: 10px; color: var(--skin-muted, #788698); font-size: 20px; white-space: nowrap; }
.phone-row i { font-size: 20px; transform: rotate(-18deg); }
.business-tags { display: flex; gap: 11px; margin-bottom: 9px; }
.business-tags span { display: inline-flex; align-items: center; min-height: 37px; padding: 0 13px; border-radius: 7px; font-size: 17px; white-space: nowrap; }
.status-operating { color: #239765; background: #daf3e5; }
.status-pending { color: #e58a18; background: #fff0d2; }
.status-closed, .status-rejected { color: #e05e65; background: #ffe4e3; }
.audit-tag { color: var(--skin-brand, #2187dd); background: var(--skin-surface, #e2f0ff); }
.business-metrics { display: flex; align-items: center; gap: 14px; color: var(--skin-muted, #778495); font-size: 19px; white-space: nowrap; }
.rating { color: var(--skin-ink, #445266); }
.rating i { margin-right: 8px; color: #ffaf13; }
.metric-divider { width: 1px; height: 22px; background: var(--skin-border, #aeb8c4); }
.view-business { display: inline-flex; flex: 0 0 auto; align-items: center; gap: 11px; min-height: 68px; padding: 0 27px; border: 2px solid var(--skin-border, #c6e3ff); border-radius: 36px; color: var(--skin-brand, #1889ec); background: var(--skin-surface, #f9fcff); font: inherit; font-size: 20px; cursor: pointer; }
.view-business i { font-size: 25px; }
.view-business:active { transform: scale(.98); }
.list-state, .empty-state { padding: 54px 15px; color: var(--skin-muted, #8592a2); text-align: center; font-size: 17px; }
.error-state { color: #d36b70; }
.empty-state i { display: block; margin-bottom: 15px; color: var(--skin-brand-soft, #9cc9ed); font-size: 42px; }
.empty-state p { color: var(--skin-muted, #708096); }
.empty-state small { display: block; margin-top: 8px; color: var(--skin-muted, #a2afbd); font-size: 13px; }
.sr-only { position: absolute; width: 1px; height: 1px; padding: 0; overflow: hidden; clip: rect(0,0,0,0); white-space: nowrap; border: 0; }
@media (max-width: 720px) {
  .business-hero { grid-template-columns: 66px minmax(0, 1fr) 92px; min-height: 132px; padding: 16px 15px 20px; }
  .hero-back { gap: 6px; font-size: 16px; }
  .hero-back i { font-size: 18px; }
  .hero-copy h1 { font-size: 24px; }
  .hero-decoration { gap: 5px; }
  .hero-decoration > i { font-size: 38px; }
  .hero-decoration span { font-size: 11px; line-height: 1.5; }
  .hero-decoration small { font-size: 8px; }
  .business-content { padding: 20px 12px 14px; border-radius: 17px 17px 0 0; }
  .business-stats { gap: 9px; }
  .stat-card { min-height: 94px; flex-direction: column; gap: 7px; padding: 10px 3px; }
  .stat-icon { width: 35px; height: 35px; flex-basis: 35px; font-size: 17px; }
  .stat-card strong { font-size: 22px; text-align: center; }
  .stat-card span:not(.stat-icon) { margin-top: 5px; font-size: 12px; }
  .business-search { min-height: 55px; margin: 20px 0 16px; padding: 0 17px; border-width: 1px; }
  .business-search i { margin-right: 12px; font-size: 22px; }
  .business-search input { min-height: 45px; font-size: 16px; }
  .business-toolbar { align-items: flex-start; margin-bottom: 15px; }
  .business-tabs { gap: 4px; }
  .business-tabs button, .sort-control, .sort-control select { min-height: 43px; font-size: 14px; }
  .business-tabs button { padding: 0 12px; border-radius: 10px; }
  .sort-control select { padding-left: 1px; padding-right: 22px; }
  .sort-control i { right: 3px; font-size: 11px; }
  .business-list { gap: 12px; }
  .business-card { min-height: 155px; gap: 10px; padding: 15px 13px; border-radius: 13px; }
  .business-main { gap: 12px; }
  .business-logo-wrap { width: 76px; height: 76px; flex-basis: 76px; }
  .business-details h2 { margin-bottom: 6px; font-size: 17px; }
  .phone-row { gap: 6px; margin-bottom: 7px; font-size: 13px; }
  .phone-row i { font-size: 14px; }
  .business-tags { gap: 5px; margin-bottom: 7px; }
  .business-tags span { min-height: 25px; padding: 0 7px; border-radius: 5px; font-size: 12px; }
  .business-metrics { gap: 7px; font-size: 13px; }
  .rating i { margin-right: 4px; }
  .metric-divider { height: 15px; }
  .view-business { min-height: 48px; padding: 0 11px; border-width: 1px; gap: 5px; font-size: 13px; }
  .view-business i { font-size: 17px; }
}
@media (max-width: 390px) {
  .business-hero { grid-template-columns: 57px minmax(0, 1fr) 73px; }
  .hero-copy h1 { font-size: 21px; }
  .hero-decoration > i { display: none; }
  .hero-decoration span { font-size: 10px; }
  .business-card { padding-left: 10px; padding-right: 10px; }
  .business-main { gap: 9px; }
  .business-logo-wrap { width: 66px; height: 66px; flex-basis: 66px; }
  .business-details h2 { font-size: 15px; }
  .phone-row { font-size: 11px; }
  .business-tags span { font-size: 10px; }
  .business-metrics { font-size: 11px; }
  .view-business { padding: 0 7px; font-size: 11px; }
}
@media (prefers-reduced-motion: reduce) { .business-tabs button, .view-business { transition: none; } }
</style>

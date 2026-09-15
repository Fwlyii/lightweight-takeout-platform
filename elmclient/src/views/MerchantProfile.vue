<template>
  <div class="container merchant-profile-page merchant-ui">
    <header class="top-background">
      <div><h1>商家信息</h1><p>用心经营 · 服务每一位顾客</p></div>
      <img src="/images/merchant/profile-shop.jpg" alt="" class="merchant-hero-art">
    </header>
    <section class="user-card">
      <div class="user-info-row">
        <div class="avatar"><img :src="merchant?.avatar || defaultAvatar" alt="商家头像" @error="handleImageError"></div>
        <div class="user-details">
          <h2 class="user-name">{{ merchant?.name || (loading ? '加载中…' : '未设置商家名称') }}</h2>
          <div class="user-phone"><i class="fas fa-phone"></i><span>{{ formattedPhone }}</span><span class="account-badge"><i class="fas fa-store"></i> 商家账号</span></div>
        </div>
      </div>
      <button class="logout-btn" @click="logout"><i class="fas fa-sign-out-alt"></i> 退出登录</button>
    </section>
    <section class="merchant-entry-card">
      <div class="entry-heading">
        <div><span class="entry-kicker"><i class="fas fa-store"></i> 经营管理</span><h2>店铺与菜单</h2></div>
        <button class="entry-button" @click="goToWorkbench">进入工作台 <i class="fas fa-arrow-right"></i></button>
      </div>
      <p class="entry-description">店铺资料、营业状态和商品统一在工作台维护。</p>
      <div class="entry-stats">
        <div><i class="fas fa-store stat-blue"></i><strong>{{ stores.length }}</strong><span>店铺总数</span></div>
        <div><i class="fas fa-store stat-green"></i><strong>{{ operatingStoreCount }}</strong><span>营业中</span></div>
        <div><i class="fas fa-clock stat-orange"></i><strong>{{ pendingStoreCount }}</strong><span>审核中</span></div>
      </div>
      <button class="reviews-link" @click="$router.push('/merchant/reviews')"><i class="fas fa-comment-alt"></i> 查看顾客评价并回复<i class="fas fa-chevron-right"></i></button>
    </section>
    <section class="merchant-tools">
      <div class="section-heading"><h2><i class="fas fa-th-large"></i> 常用工具</h2><span>高效经营 轻松管理</span></div>
      <div class="tool-grid">
        <button @click="goToWorkbench"><i class="fas fa-store stat-blue"></i><b>店铺与商品</b><small>维护店铺资料<br>管理店铺菜单</small></button>
        <button @click="$router.push('/merchant/orders')"><i class="fas fa-clipboard-list stat-orange"></i><b>订单管理</b><small>查看订单动态<br>处理接单与出餐</small></button>
        <button @click="$router.push('/merchant/reviews')"><i class="fas fa-comments stat-purple"></i><b>顾客评价</b><small>查看顾客反馈<br>回复店铺评价</small></button>
      </div>
    </section>
    <section v-if="stores.length === 0 && !loading" class="empty-store-tip"><i class="fas fa-store"></i><p>还没有店铺，去工作台申请第一家店吧。</p><button @click="goToWorkbench">申请新店</button></section>
  </div>
</template>

<script>
import { ref, computed, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { toast } from '../utils/toast';
import request from '../utils/request';
import { listMyBusinesses } from '../services/businessService';
import { DEFAULT_AVATAR_URL } from '../utils/profileDefaults';
import { clearAuth } from '../utils/auth';

export default {
  name: 'MerchantProfile',
  setup() {
    const router = useRouter();
    const defaultAvatar = DEFAULT_AVATAR_URL;

    const merchant = ref(null);
    const stores = ref([]); // 仅保留门店数量与状态摘要，明细统一在工作台维护

    const loading = ref(false);
    const approvedStoreCount = computed(() => stores.value.filter(store => store.status === 1).length);
    const operatingStoreCount = computed(() => stores.value.filter(store => store.status === 1 && store.operatingStatus !== false).length);
    const pendingStoreCount = computed(() => stores.value.filter(store => store.status === 0).length);

    // 格式化手机号显示
    const formattedPhone = computed(() => {
      if (!merchant.value?.phone) return '未绑定手机';
      return merchant.value.phone.replace(/(\d{3})\d{4}(\d{4})/, '$1****$2');
    });

    onMounted(async () => {
      await loadMerchantData();
      // 身份由后端登录态解析，页面只请求“我的店铺”。
      if (merchant.value?.id) {
        await loadMerchantStores();
      }
    });

    // 加载商家基本信息
    const loadMerchantData = async () => {
      loading.value = true;
      try {
        const data = await request.get('/api/user');

        if (data && data.id) {
          merchant.value = {
            id: data.id,
            name: data.username,
            phone: data.phone,
            avatar: data.photo,
          };
        } else {
          toast.error('获取商家信息失败：服务器返回数据为空或格式不正确！');
        }
      } catch (error) {
        console.error('获取商家信息失败:', error);
        toast.error('获取商家信息失败，请重试！');
      } finally {
        loading.value = false;
      }
    };

    // “我的”只显示门店摘要，门店明细统一由经营工作台负责
    const loadMerchantStores = async () => {
      try {
        stores.value = await listMyBusinesses();
      } catch (error) {
        console.error('获取商家店铺摘要失败:', error);
        toast.error('获取店铺信息失败，请重试！');
      }
    };

    const logout = () => {
      clearAuth();
      router.replace({ path: '/login', query: { role: 'merchant' } });
    };

    const goToWorkbench = () => {
      router.push('/merchant/business');
    };
    const handleImageError = (event) => {
      const image = event?.target;
      if (!image || image.dataset.fallbackApplied === 'true') return;
      image.dataset.fallbackApplied = 'true';
      image.src = defaultAvatar;
    };

    return {
      merchant,
      stores,
      formattedPhone,
      loading,
      approvedStoreCount,
      operatingStoreCount,
      pendingStoreCount,
      defaultAvatar,
      logout,
      goToWorkbench,
      handleImageError
    };
  }
};
</script>

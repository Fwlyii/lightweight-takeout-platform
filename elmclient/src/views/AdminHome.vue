<template>
    <div class="admin-container">
        <div class="container">
            <AdminPageHeader title="管理员管理平台" back-to="" />

            <div class="user-card">
                <div class="avatar">
                    <img :src="DEFAULT_AVATAR_URL" alt="用户头像" class="avatar-img" />
                </div>
                <div class="user-details">
                    <div class="user-name">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
                            <path fill="currentColor"
                                d="M224 256c70.7 0 128-57.31 128-128s-57.3-128-128-128S96 57.31 96 128s57.3 128 128 128zm-45.72 22.78c-55.8 17.5-91.1 72.8-91.1 133.3c0 6.64 5.36 12 12 12h264c6.64 0 12-5.36 12-12c0-60.5-35.3-115.8-91.1-133.3C306.6 304.8 266.3 320 224 320s-82.55-15.2-96.28-41.22z" />
                        </svg>
                        <span>{{ currentUser?.username || '管理员' }}</span>
                    </div>
                    <div class="user-full-name">
                        <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 576 512">
                            <path fill="currentColor"
                                d="M0 32C0 14.33 14.33 0 32 0h512c17.67 0 32 14.33 32 32v448c0 17.67-14.33 32-32 32h-512c-17.67 0-32-14.33-32-32V32zM320 128c0-17.67-14.33-32-32-32h-96c-17.67 0-32 14.33-32 32s14.33 32 32 32h96c17.67 0 32-14.33 32-32zm64 256c0 8.836-7.164 16-16 16h-288c-8.836 0-16-7.164-16-16s7.164-16 16-16h288c8.836 0 16 7.164 16 16zm0-96c0 8.836-7.164 16-16 16h-288c-8.836 0-16-7.164-16-16s7.164-16 16-16h288c8.836 0 16 7.164 16 16z" />
                        </svg>
                        <span>ID: ADMIN{{ currentUser?.id || '00' }}</span>
                    </div>
                </div>
            </div>

            <!-- 新增的退出按钮 -->
            <button class="logout-btn" @click="logout">
                <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512">
                    <path fill="currentColor"
                        d="M384 320c-17.67 0-32 14.33-32 32v96c0 17.67-14.33 32-32 32H64c-17.67 0-32-14.33-32-32V128c0-17.67 14.33-32 32-32h256c17.67 0 32-14.33 32-32S337.7 0 320 0H64C28.65 0 0 28.65 0 64v384c0 35.35 28.65 64 64 64h256c35.35 0 64-28.65 64-64v-96c0-17.67-14.33-32-32-32zm48.51-140.51l-104.5-104.5c-4.688-4.688-12.29-4.688-16.97 0s-4.688 12.29 0 16.97L404.7 192H192c-17.67 0-32 14.33-32 32s14.33 32 32 32h212.7l-43.5 43.5c-4.688 4.688-4.688 12.29 0 16.97s12.29 4.688 16.97 0l104.5-104.5c4.688-4.688 4.688-12.29 0-16.97z" />
                </svg>
                退出登录
            </button>

            <div class="stats-toolbar">
                <label>统计区间</label><input v-model="statsFrom" type="date"><span>至</span><input v-model="statsTo"
                    type="date">
                <button @click="loadDetailedStats">查询</button><button class="export-btn"
                    @click="downloadStats">导出CSV</button>
                <span v-if="detailedStats" class="stats-summary">有效订单 {{ detailedStats.completedCount || 0 }} · 有效营业额
                    ¥{{ detailedStats.revenue || 0 }}</span>
            </div>
            <div class="stats-container">
                <div class="stat-card users">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 640 512">
                        <path fill="currentColor"
                            d="M224 256c70.7 0 128-57.31 128-128s-57.3-128-128-128S96 57.31 96 128s57.3 128 128 128zm-45.72 22.78c-55.8 17.5-91.1 72.8-91.1 133.3c0 6.64 5.36 12 12 12h264c6.64 0 12-5.36 12-12c0-60.5-35.3-115.8-91.1-133.3C306.6 304.8 266.3 320 224 320s-82.55-15.2-96.28-41.22zM480 320c44.11 0 80-35.89 80-80s-35.89-80-80-80s-80 35.89-80 80s35.89 80 80 80zm-44.57 33.39c-29.42-10.32-52.92-26.6-70.2-46.12c-2.31 29.56-11.23 58.73-28.71 85.34c-13.88 20.82-35.39 37.81-60.85 47.78c1.375 1.5 2.768 2.977 4.225 4.434c32.74 32.74 76.5 50.88 122.9 50.88c53.07 0 102.6-21.41 139.6-58.42c37.01-37.01 58.42-86.54 58.42-139.6c0-46.4-18.14-90.16-50.88-122.9C611.3 331.4 567.5 349.6 521.1 349.6c-17.7 0-35.03-3.951-51.48-11.53z" />
                    </svg>
                    <h3>总用户人数</h3>
                    <div class="number">
                        {{ userCount !== null ? userCount.toLocaleString() : '加载中...' }}
                    </div>
                </div>

                <div class="stat-card shops">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 576 512">
                        <path fill="currentColor"
                            d="M576 432c0 26.51-21.49 48-48 48H48c-26.51 0-48-21.49-48-48V192c0-26.51 21.49-48 48-48h96v-16c0-52.93 43.07-96 96-96h96c52.93 0 96 43.07 96 96v16h96c26.51 0 48 21.49 48 48v240zM352 168c0-17.67-14.33-32-32-32h-96c-17.67 0-32 14.33-32 32v8h160v-8z" />
                    </svg>
                    <h3>总店铺数</h3>
                    <div class="number">
                        {{ shopCount !== null ? shopCount.toLocaleString() : '加载中...' }}
                    </div>
                </div>

                <div class="stat-card revenue">
                    <svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 448 512">
                        <path fill="currentColor"
                            d="M384 480c-26.51 0-48 21.49-48 48H48c-26.51 0-48-21.49-48-48V320c0-26.51 21.49-48 48-48h288c26.51 0 48 21.49 48 48v160zM416 160c-26.51 0-48 21.49-48 48v112c0 26.51 21.49 48 48 48s48-21.49 48-48V208c0-26.51-21.49-48-48-48zm-64-32c-26.51 0-48 21.49-48 48s21.49 48 48 48s48-21.49 48-48s-21.49-48-48-48z" />
                    </svg>
                    <h3>总营业额</h3>
                    <div class="number">
                        ¥{{ totalRevenue !== null ? totalRevenue.toLocaleString() : '0' }}
                    </div>
                </div>
            </div>

            <div class="review-section">
                <div class="review-tabs">
                    <div class="review-tab" :class="{ active: activeTab === 'user-review' }"
                        @click="activeTab = 'user-review'">
                        用户审核（{{ merchantApplications.length }}）
                    </div>
                    <div class="review-tab" :class="{ active: activeTab === 'shop-review' }"
                        @click="activeTab = 'shop-review'">
                        商铺审核（{{ shopApplications.length }}）
                    </div>
                </div>

                <div class="review-content" :class="{ active: activeTab === 'user-review' }">
                    <div v-if="merchantApplications.length === 0 && !loadingMerchant" class="empty-tip">
                        暂无待审核的商家申请
                    </div>
                    <div v-if="loadingMerchant" class="loading-tip">加载中...</div>
                    <div v-for="app in merchantApplications" :key="'merchant-' + app.id" class="review-item">
                        <div class="review-info">
                            <h3>用户ID: {{ app.userId }}</h3>
                            <p>用户名: {{ app.username }}</p>
                            <p>申请时间: {{ formatTime(app.createTime) }}</p>
                        </div>
                        <button class="review-btn" @click="handleMerchantReview(app)">审核</button>
                    </div>
                </div>

                <div class="review-content" :class="{ active: activeTab === 'shop-review' }">
                    <div v-if="shopApplications.length === 0 && !loadingShop" class="empty-tip">
                        暂无待审核的店铺申请
                    </div>
                    <div v-if="loadingShop" class="loading-tip">加载中...</div>
                    <div v-for="app in shopApplications" :key="'shop-' + app.id" class="review-item">
                        <div class="review-info">
                            <h3>{{ app.businessName }}</h3>
                            <p>店铺类型: {{ getShopTypeName(app.orderTypeId) }}</p>
                            <p>申请时间: {{ formatTime(app.createTime) }}</p>
                        </div>
                        <button class="review-btn" @click="handleShopReview(app)">审核</button>
                    </div>
                </div>
            </div>

            <div v-if="showReviewModal" class="modal-overlay" @click.self="closeModal">
                <div class="modal-content">
                    <div class="modal-header">
                        <h3>{{ modalTitle }}</h3>
                        <span class="close-btn" @click="closeModal">&times;</span>
                    </div>
                    <div class="modal-body">
                        <div v-if="modalType === 'merchant'">
                            <div v-if="loadingPersonInfo" class="loading-tip">加载用户信息中...</div>

                            <div v-else-if="currentPersonInfo" class="person-info-container">
                                <div class="modal-item">
                                    <label>用户名:</label>
                                    <span>{{ currentMerchantApp?.username }}</span>
                                </div>
                                <div class="modal-item">
                                    <label>姓名:</label>
                                    <span>{{ currentPersonInfo.firstName }} {{ currentPersonInfo.lastName }}</span>
                                </div>
                                <div class="modal-item">
                                    <label>手机号:</label>
                                    <span>{{ currentPersonInfo.phone || '未填写' }}</span>
                                </div>
                                <div class="modal-item">
                                    <label>邮箱:</label>
                                    <span>{{ currentPersonInfo.email || '未填写' }}</span>
                                </div>
                                <div class="modal-item">
                                    <label>性别:</label>
                                    <span>
                                        {{
                                            currentPersonInfo.gender === 'MALE'
                                                ? '男'
                                                : currentPersonInfo.gender === 'FEMALE'
                                                    ? '女'
                                        : '未设置'
                                        }}
                                    </span>
                                </div>
                                <div class="modal-item">
                                    <label>申请ID:</label>
                                    <span>{{ currentMerchantApp?.id }}</span>
                                </div>
                                <div class="modal-item">
                                    <label>申请时间:</label>
                                    <span>{{ formatTime(currentMerchantApp?.createTime) }}</span>
                                </div>
                            </div>

                            <div v-else class="empty-tip">无法获取用户详细信息</div>
                        </div>

                        <div v-if="modalType === 'shop'">
                            <div class="modal-item">
                                <label>申请ID:</label>
                                <span>{{ currentShopApp?.id }}</span>
                            </div>
                            <div class="modal-item">
                                <label>店铺名称:</label>
                                <span>{{ currentShopApp?.businessName }}</span>
                            </div>
                            <div class="modal-item">
                                <label>店铺地址:</label>
                                <span>{{ currentShopApp?.businessAddress }}</span>
                            </div>
                            <div class="modal-item">
                                <label>店铺介绍:</label>
                                <span>{{ currentShopApp?.businessExplain }}</span>
                            </div>
                            <div class="modal-item">
                                <label>起送价:</label>
                                <span>¥{{ currentShopApp?.startPrice.toFixed(2) }}</span>
                            </div>
                            <div class="modal-item">
                                <label>配送费:</label>
                                <span>¥{{ currentShopApp?.deliveryPrice.toFixed(2) }}</span>
                            </div>
                            <div class="modal-item">
                                <label>店铺类型:</label>
                                <span>{{ getShopTypeName(currentShopApp?.orderTypeId) }}</span>
                            </div>
                            <div class="modal-item">
                                <label>申请时间:</label>
                                <span>{{ formatTime(currentShopApp?.createTime) }}</span>
                            </div>
                            <div class="modal-item" v-if="currentShopApp?.businessImg">
                                <label>店铺图片:</label>
                                <img :src="currentShopApp?.businessImg || require('@/assets/business-default.png')"
                                    @error="handleImageError" alt="店铺图片" class="shop-img" />
                            </div>
                        </div>
                    </div>
                    <div class="modal-footer">
                        <button class="modal-btn approve-btn" @click="submitAudit(1)">批准</button>
                        <button class="modal-btn reject-btn" @click="submitAudit(2)">拒绝</button>
                    </div>
                </div>
            </div>
        </div>
    </div>
</template>

<script setup>
import { ref, onMounted, onUnmounted, computed, reactive } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { createRealtimeConnection } from '@/services/realtimeService';
import { clearAuth, getStoredUser, updateStoredUser } from '@/utils/auth';
import { DEFAULT_AVATAR_URL } from '@/utils/profileDefaults';
import AdminPageHeader from '../components/AdminPageHeader.vue';

// 路由实例
const router = useRouter();

// 状态管理
const currentUser = ref({}); // 当前管理员信息
const userCount = ref(null); // 总用户数
const shopCount = ref(null); // 总店铺数
const totalRevenue = ref(null); // 总营业额
const statsFrom = ref('');
const statsTo = ref('');
const detailedStats = ref(null);
const activeTab = ref('user-review'); // 活跃的审核标签
const showReviewModal = ref(false); // 审核弹窗显示状态
const modalTitle = ref(''); // 弹窗标题
const modalType = ref(''); // 弹窗类型（merchant/shop）
const currentMerchantApp = ref({}); // 当前待审核的商家申请
const currentShopApp = ref(null); // 当前待审核的店铺申请
const loadingMerchant = ref(false); // 商家申请加载状态
const loadingShop = ref(false); // 店铺申请加载状态

// 申请列表数据
const merchantApplications = ref([]); // 商家申请列表
const shopApplications = ref([]); // 店铺申请列表

let realtimeConnection = null;
const currentPersonInfo = ref(null); // 存储用户详细信息
const loadingPersonInfo = ref(false); // 加载状态

const handleImageError = (event) => {
    const image = event?.target;
    if (!image || image.dataset.fallbackApplied === 'true') return;
    image.dataset.fallbackApplied = 'true';
    image.src = require('@/assets/business-default.png');
};

// 店铺类型映射（根据实际业务补充）
const shopTypeMap = computed(() => ({
    1: '美食',
    2: '早餐',
    3: '跑腿代购',
    4: '汉堡披萨',
    5: '甜品饮品',
    6: '速食简餐',
    7: '地方小吃',
    8: '米粉面馆',
    9: '包子粥铺',
    10: '炸鸡炸串',
}));

// ====================== 初始化逻辑 ======================
onMounted(() => {
    // 1. 获取当前管理员信息
    getCurrentUserInfo();
    // 2. 获取统计数据
    getStatisticData();
    loadDetailedStats();
    // 3. 获取待审核列表
    getMerchantApplications();
    getShopApplications();
    // 4. 实时审核通知；用户标识和重连策略由统一服务负责。
    realtimeConnection = createRealtimeConnection({ onMessage: handleWebSocketMessage });
    realtimeConnection.start();
});

// 销毁WebSocket连接
onUnmounted(() => {
    realtimeConnection?.stop();
});

// ====================== 接口请求 ======================
/**
 * 获取当前管理员信息
 */
const getCurrentUserInfo = async () => {
    try {
        const savedUser = getStoredUser();
        if (savedUser) {
            currentUser.value = savedUser;
        } else {
            const res = await request.get('/api/user');
            const user = res?.success ? res.data : res;
            if (user?.id) {
                currentUser.value = user;
                updateStoredUser(user);
            }
        }
    } catch (error) {
        console.error('获取管理员信息失败:', error);
        toast.error('获取管理员信息失败，请刷新重试');
    }
};

const getPersonInfo = async (userId) => {
    if (!userId) return;

    loadingPersonInfo.value = true;
    try {
        const res = await request.get('/api/user');

        if (res && res.id) {
            currentPersonInfo.value = res;
        } else {
            toast.warning('获取用户信息失败');
            currentPersonInfo.value = null;
        }
    } catch (error) {
        console.error('获取用户详细信息失败:', error);
        toast.error('获取用户信息失败，请重试');
        currentPersonInfo.value = null;
    } finally {
        loadingPersonInfo.value = false;
    }
};

/**
 * 获取统计数据（用户数、店铺数、营业额）
 */
const getStatisticData = async () => {
    try {
        // 并行请求统计接口
        const [userRes, shopRes, revenueRes] = await Promise.all([
            request.get('/api/admin/countUser'),
            request.get('/api/admin/countBusiness'),
            request.get('/api/admin/countPrice'),
        ]);

        if (userRes.success) userCount.value = userRes.data;
        if (shopRes.success) shopCount.value = shopRes.data;
        if (revenueRes.success) totalRevenue.value = revenueRes.data;
    } catch (error) {
        console.error('获取统计数据失败:', error);
        toast.error('获取统计数据失败');
    }
};

const loadDetailedStats = async () => {
    try { const res = await request.get('/api/admin/statistics', { params: { from: statsFrom.value || undefined, to: statsTo.value || undefined } }); if (res.success) detailedStats.value = res.data; } catch (e) { toast.error('统计数据加载失败'); }
};
const downloadStats = async () => {
    try { const blob = await request.get('/api/admin/statistics/export', { params: { from: statsFrom.value || undefined, to: statsTo.value || undefined }, responseType: 'blob' }); const url = URL.createObjectURL(blob); const a = document.createElement('a'); a.href = url; a.download = 'statistics.csv'; a.click(); URL.revokeObjectURL(url); } catch (e) { toast.error('导出失败'); }
};

/**
 * 获取商家申请待审核列表
 */
const getMerchantApplications = async () => {
    loadingMerchant.value = true;
    try {
        const res = await request.get('/api/permission/merchant-applications');
        if (res.success && Array.isArray(res.data)) {
            merchantApplications.value = res.data;
        }
    } catch (error) {
        console.error('获取商家申请列表失败:', error);
        toast.error('获取商家申请列表失败');
    } finally {
        loadingMerchant.value = false;
    }
};

/**
 * 获取店铺申请待审核列表
 */
const getShopApplications = async () => {
    loadingShop.value = true;
    try {
        const res = await request.get('/api/permission/shop-applications');
        if (res.success && Array.isArray(res.data)) {
            shopApplications.value = res.data;
        }
    } catch (error) {
        console.error('获取店铺申请列表失败:', error);
        toast.error('获取店铺申请列表失败');
    } finally {
        loadingShop.value = false;
    }
};

/**
 * 提交商家审核结果
 */
const submitMerchantAudit = async (id, auditResult) => {
    try {
        const res = await request.post('/api/permission/audit', {
            id,
            auditResult, // 1-批准，2-拒绝
        });
        if (res.success) {
            toast.success(auditResult === 1 ? '批准成功' : '拒绝成功');
            // 重新获取申请列表
            getMerchantApplications();
            closeModal();
        } else {
            toast.error(res.message || '审核提交失败');
        }
    } catch (error) {
        console.error('提交商家审核失败:', error);
        toast.error('提交审核失败，请重试');
    }
};

/**
 * 提交店铺审核结果
 */
const submitShopAudit = async (id, auditResult) => {
    try {
        const res = await request.post('/api/permission/audit-shop', {
            id,
            status: auditResult, // 1-批准，2-拒绝（与接口字段匹配）
        });
        if (res.success) {
            toast.success(auditResult === 1 ? '批准成功' : '拒绝成功');
            // 重新获取申请列表
            getShopApplications();
            closeModal();
        } else {
            toast.error(res.message || '审核提交失败');
        }
    } catch (error) {
        console.error('提交店铺审核失败:', error);
        toast.error('提交审核失败，请重试');
    }
};

/**
 * 处理WebSocket消息
 */
const handleWebSocketMessage = (message) => {
    const { type } = message;
    const content = message.notificationContent || message.content || '有新的审核任务';
    // 提示消息
    toast.info(content);

    // 1. 商家申请通知（type=0）
    if (type === 0) {
        getMerchantApplications();
    }

    // 2. 店铺申请通知（根据后端实际type值调整，这里假设为1）
    if (type === 1) {
        // 触发重新获取列表或直接添加
        getShopApplications();
    }
};

// ====================== 事件处理 ======================
/**
 * 处理商家审核点击
 */
const handleMerchantReview = async (app) => {
    modalType.value = 'merchant';
    modalTitle.value = `商家申请审核 - ${app.username}`;
    currentMerchantApp.value = { ...app };
    // 打开弹窗前先获取用户详细信息
    await getPersonInfo(app.userId);
    showReviewModal.value = true;
};

/**
 * 处理店铺审核点击
 */
const handleShopReview = (app) => {
    modalType.value = 'shop';
    modalTitle.value = `店铺申请审核 - ${app.businessName}`;
    currentShopApp.value = app;
    showReviewModal.value = true;
};

/**
 * 提交审核结果
 */
const submitAudit = (result) => {
    if (modalType.value === 'merchant' && currentMerchantApp.value) {
        submitMerchantAudit(currentMerchantApp.value.id, result);
    }
    if (modalType.value === 'shop' && currentShopApp.value) {
        submitShopAudit(currentShopApp.value.id, result);
    }
};

/**
 * 关闭审核弹窗
 */
const closeModal = () => {
    showReviewModal.value = false;
    currentMerchantApp.value = null;
    currentShopApp.value = null;
    modalType.value = '';
    modalTitle.value = '';
};

/**
 * 退出登录
 */
const logout = () => {
    realtimeConnection?.stop();
    clearAuth();
    // 跳转到登录页
    router.push('/index');
    toast.success('已成功退出登录');
};

/**
 * 格式化时间
 */
const formatTime = (timeStr) => {
    if (!timeStr) return '';
    const date = new Date(timeStr);
    return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
    });
};

/**
 * 获取店铺类型名称
 */
const getShopTypeName = (typeId) => {
    return shopTypeMap.value[typeId] || `未知类型(${typeId})`;
};
</script>

<style scoped>
.admin-container { width: 100%; max-width: 960px; margin: 0 auto; background: #f5f8fb; color: #253f54; min-height: 100%; line-height: 1.5; }
.container { width: 100%; padding-bottom: calc(84px + env(safe-area-inset-bottom)); }
.user-card { margin: 20px 16px 12px; display: flex; align-items: center; gap: 18px; background: white; border: 1px solid #dfe8ef; border-radius: 12px; padding: 18px; }
.avatar-img { display: block; width: 64px; height: 64px; border-radius: 50%; object-fit: cover; }
.user-details { min-width: 0; overflow-wrap: anywhere; }
.user-name, .user-full-name { display: flex; gap: 8px; align-items: center; }
.user-name { font-weight: 650; font-size: 18px; }
.user-full-name { color: #758b9b; font-size: 13px; margin-top: 8px; }
svg { width: 20px; height: 20px; flex: 0 0 20px; color: #208bce; }
button { min-height: 40px; border: 1px solid #c8deec; border-radius: 8px; padding: 8px 14px; background: #fff; color: #087ecc; font: inherit; cursor: pointer; }
.logout-btn { display: flex; align-items: center; justify-content: center; gap: 8px; margin: 0 16px 20px auto; font-size: 14px; }
.stats-toolbar { margin: 16px; display: flex; flex-wrap: wrap; align-items: center; gap: 8px; font-size: 14px; }
.stats-toolbar input { min-width: 0; max-width: 160px; min-height: 40px; border: 1px solid #cbdde9; border-radius: 8px; padding: 6px 8px; font: inherit; background: #fff; color: inherit; }
.stats-summary { width: 100%; color: #6d8394; font-size: 13px; }
.stats-container { display: grid; grid-template-columns: repeat(3,minmax(0,1fr)); gap: 12px; margin: 16px; }
.stat-card { background: #fff; border: 1px solid #dfe8ef; border-radius: 12px; padding: 18px 10px; text-align: center; min-width: 0; }
.stat-card svg { width: 26px; height: 26px; }
.stat-card h3 { font-size: 13px; color: #74899a; font-weight: 500; margin: 8px 0; }
.number { font-size: clamp(18px,3vw,26px); font-weight: 700; overflow-wrap: anywhere; }
.review-section { margin: 20px 16px; border: 1px solid #dfe8ef; border-radius: 12px; background: #fff; overflow: hidden; }
.review-tabs { display: flex; border-bottom: 1px solid #e3ebf2; }
.review-tab { min-height: 48px; flex: 1; display: grid; place-items: center; padding: 12px 6px; cursor: pointer; color: #74899a; }
.review-tab.active { color: #087ecc; box-shadow: inset 0 -3px #087ecc; }
.review-content { display: none; padding: 16px; }
.review-content.active { display: block; }
.review-item { display: flex; align-items: center; gap: 14px; padding: 14px 0; border-bottom: 1px solid #edf2f6; }
.review-info { min-width: 0; flex: 1; overflow-wrap: anywhere; }
.review-info h3 { font-size: 16px; margin: 0 0 6px; }
.review-info p { font-size: 13px; color: #74899a; }
.empty-tip, .loading-tip { padding: 24px 10px; text-align: center; color: #74899a; }
.modal-overlay { position: fixed; inset: 0; z-index: 2000; background: #10283980; display: grid; place-items: center; padding: 16px; }
.modal-content { width: 100%; max-width: 540px; max-height: 85dvh; overflow-y: auto; background: #fff; border-radius: 14px; padding: 20px; }
.modal-header, .modal-footer { display: flex; justify-content: space-between; align-items: center; gap: 12px; }
.modal-header h3 { font-size: 18px; }
.close-btn { font-size: 26px; cursor: pointer; min-width: 36px; text-align: center; }
.modal-body { padding: 18px 0; }
.modal-item { display: flex; align-items: baseline; gap: 12px; margin-bottom: 12px; font-size: 14px; overflow-wrap: anywhere; }
.modal-item label { flex: 0 0 75px; color: #718799; }
.modal-item span { min-width: 0; }
.shop-img { max-width: 150px; max-height: 100px; object-fit: contain; }
.modal-footer { justify-content: flex-end; }
.approve-btn { background: #087ecc; color: white; }
.reject-btn { color: #b54050; border-color: #edc5ce; }
@media (max-width: 400px) { .stats-container { gap: 8px; } .stats-toolbar input { max-width: 128px; } .user-card { padding: 14px; } }
</style>

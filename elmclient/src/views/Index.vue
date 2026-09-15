<template>
    <!-- 登录、注册部分 -->
    <div class="wrapper home-page" :class="{ 'home-page-ready': pageReady }">
        <!-- 顶部插画区：位置、天气、问候、消息与搜索都是活元素，插画只作为背景 -->
        <div class="home-hero">
        <!-- header部分：动画只加在头部子块上，避免 header 成为 fixed 弹窗的包含块 -->
        <header class="home-header">
            <div class="location-text home-motion home-motion-header" @click="showLocationPicker">
                <i class="fas fa-map-marker-alt"></i>
                <span class="location-display">{{ displayLocation }}</span>
                <i class="fa fa-caret-down"></i>
            </div>

            <!-- 漂亮的位置选择弹窗 -->
            <Teleport to="body">
            <transition name="fade">
                <div v-if="showPicker" class="location-modal" @click.self="hideLocationPicker" @keydown.esc="hideLocationPicker" @keydown.tab="trapLocationFocus">
                    <div class="modal-container" role="dialog" aria-modal="true" aria-labelledby="location-dialog-title">
                        <div class="modal-header">
                            <h3 id="location-dialog-title">选择位置</h3>
                            <button type="button" class="close-btn" aria-label="关闭位置选择" @click="hideLocationPicker">
                                <i class="fa fa-times"></i>
                            </button>
                        </div>

                        <div class="modal-content">
                            <!-- 位置层级导航 -->
                            <div class="location-nav">
                                <div v-for="(level, index) in locationLevels" :key="index"
                                    :class="['nav-item', { active: currentLevel === index, disabled: index > currentLevel }]"
                                    @click="switchLevel(index)">
                                    {{ level }}
                                </div>
                            </div>

                            <!-- 位置列表 -->
                            <p v-if="locationError" class="location-error" role="status">{{ locationError }}</p>
                            <div v-else class="location-list-container">
                                <div v-if="loading" class="loading-state">
                                    <i class="fa fa-spinner fa-spin"></i>
                                    <span>加载中...</span>
                                </div>

                                <div v-else-if="locationData.length === 0" class="empty-state">
                                    <i class="fa fa-map-marker"></i>
                                    <span>暂无数据</span>
                                </div>

                                <div v-else class="location-items">
                                    <div v-for="item in locationData" :key="item.adcode || item.name"
                                        :class="['location-item', { selected: isSelected(item) }]"
                                        @click="selectLocation(item)">
                                        <span class="item-name">{{ item.name }}</span>
                                        <i v-if="isSelected(item)" class="fa fa-check selected-icon"></i>
                                    </div>
                                </div>
                            </div>

                            <!-- 当前选择显示 -->
                            <div v-if="pendingLocation.province" class="current-selection">
                                <span>已选择：</span>
                                <span class="selection-text">
                                    {{ getDisplayText(pendingLocation) }}
                                </span>
                            </div>
                        </div>

                        <div class="modal-footer">
                            <button class="btn-cancel" @click="hideLocationPicker">取消</button>
                            <button class="btn-confirm" :disabled="loading || !!locationError || !pendingLocation.district" @click="confirmLocation">确认</button>
                        </div>
                    </div>
                </div>
            </transition>
            </Teleport>

            <div class="hero-actions home-motion home-motion-header">
                <template v-if="!userInfo">
                    <button class="hero-pill" @click="goToLChoose">登录</button>
                    <button class="hero-pill" @click="goToRChoose">注册</button>
                </template>
                <span v-else class="hero-user">{{ userInfo.username }}，您好！</span>
                <button type="button" class="hero-bell" aria-label="消息通知" @click="goToNotifications">
                    <i class="fa fa-bell-o"></i>
                    <em class="hero-bell-dot" aria-hidden="true"></em>
                </button>
            </div>

            <div class="hero-bottom home-motion home-motion-header">
                <p class="hero-slogan">在校园<br>也能吃到好味道！</p>
            </div>
        </header>
        </div>
        <!-- search部分：不参与入场动画，避免 transform 影响滚动时吸顶定位 -->
        <div class="search">
            <div class="search-fixed-top" ref="fixedBox">
                <div class="search-box">
                    <i class="fa fa-search"></i>
                    <input v-model="searchKeyword" type="text" :placeholder="searchPlaceholder" @keyup.enter="performSearch"
                        @focus="searchFocused = true" @blur="searchFocused = false" />
                    <button @click="performSearch" class="search-btn">搜索</button>
                </div>
            </div>
        </div>
        <!-- 点餐分类部分 -->
        <ul class="foodtype home-motion home-motion-categories">
            <li @click="toBusinessList(1)">
                <span class="foodtype-icon"><img src="@/assets/dcfl01.png" alt="美食"></span>
                <p>美食</p>
            </li>
            <li @click="toBusinessList(2)">
                <span class="foodtype-icon"><img src="@/assets/dcfl02.png" alt="早餐"></span>
                <p>早餐</p>
            </li>
            <li @click="toBusinessList(3)">
                <span class="foodtype-icon"><img src="@/assets/dcfl03.png" alt="跑腿代购"></span>
                <p>跑腿代购</p>
            </li>
            <li @click="toBusinessList(4)">
                <span class="foodtype-icon"><img src="@/assets/dcfl04.png" alt="汉堡披萨"></span>
                <p>汉堡披萨</p>
            </li>
            <li @click="toBusinessList(5)">
                <span class="foodtype-icon"><img src="@/assets/dcfl05.png" alt="甜品饮品"></span>
                <p>甜品饮品</p>
            </li>
            <li @click="toBusinessList(6)">
                <span class="foodtype-icon"><img src="@/assets/dcfl06.png" alt="速食简餐"></span>
                <p>速食简餐</p>
            </li>
            <li @click="toBusinessList(7)">
                <span class="foodtype-icon"><img src="@/assets/dcfl07.png" alt="地方小吃"></span>
                <p>地方小吃</p>
            </li>
            <li @click="toBusinessList(8)">
                <span class="foodtype-icon"><img src="@/assets/dcfl08.png" alt="米粉面馆"></span>
                <p>米粉面馆</p>
            </li>
            <li @click="toBusinessList(9)">
                <span class="foodtype-icon"><img src="@/assets/dcfl09.png" alt="包子粥铺"></span>
                <p>包子粥铺</p>
            </li>
            <li @click="toBusinessList(10)">
                <span class="foodtype-icon"><img src="@/assets/dcfl10.png" alt="炸鸡炸串"></span>
                <p>炸鸡炸串</p>
            </li>
        </ul>

        <!-- 猜你想吃：保留为轻量横向推荐，不再突出销量冠军或排名 -->
        <section v-if="suggestedBusinesses.length" class="guess-section home-motion home-motion-offers" aria-label="猜你想吃">
            <div class="section-heading">
                <div>
                    <h2>猜你想吃 <span class="section-spark" aria-hidden="true">✦</span></h2>
                    <span>根据你的口味推荐</span>
                </div>
                <button type="button" @click="scrollToRecommendations">查看更多 <i class="fa fa-angle-right"></i></button>
            </div>
            <div class="guess-scroll">
                <button v-for="business in suggestedBusinesses" :key="business.id || business.businessId" type="button" class="guess-card" @click="toBusinessInfo(business.id || business.businessId)">
                    <span class="guess-card-media">
                        <img :src="business.businessImg || require('@/assets/business-default.png')" :alt="business.businessName" @error="handleImageError">
                        <em v-if="getGuessBadge(business)" :class="['guess-badge', `guess-badge-${getGuessBadge(business).tone}`]">{{ getGuessBadge(business).label }}</em>
                    </span>
                    <strong>{{ business.businessName || '附近好店' }}</strong>
                    <span class="guess-card-meta">
                        <b>{{ hasBusinessRating(business.score) ? `★ ${Number(business.score).toFixed(1)}` : '暂无评分' }}</b>
                        <span>月售 {{ business.salesCount || 0 }}</span>
                    </span>
                    <span class="guess-card-price">人均 ¥{{ formatMoney(getBusinessAveragePrice(business)) }}</span>
                </button>
            </div>
        </section>

        <!-- 推荐商家部分 -->
        <div id="recommendations" class="recommend home-motion home-motion-recommend">
            <div class="section-heading">
                <div>
                    <h2>推荐商家</h2>
                    <span>优质商家·美味送到你身边</span>
                </div>
            </div>
        </div>

        <!-- 推荐方式部分 -->
        <ul class="recommendtype home-motion home-motion-recommend">
            <li :class="{ active: sortBy === 'default' }" @click="setSortBy('default')">
                综合排序<i class="fa fa-angle-down"></i>
            </li>
            <li :class="{ active: sortBy === 'sales' }" @click="setSortBy('sales')">
                销量最高
            </li>
            <li :class="{ active: sortBy === 'distance' }" @click="setSortBy('distance')">
                距离最近
            </li>
            <li class="recommendtype-filter" :class="{ active: showFilter }" @click="toggleFilter">
                筛选<i class="fa fa-filter"></i>
            </li>
        </ul>

        <!-- 筛选面板：底部弹出的多维度筛选 -->
        <transition name="sheet">
            <div v-if="showFilter" class="filter-sheet-mask" @click.self="hideFilter">
                <div class="filter-sheet" role="dialog" aria-label="筛选">
                    <span class="sheet-handle" aria-hidden="true"></span>
                    <div class="filter-header">
                        <div>
                            <h3>筛选</h3>
                            <p>多维度筛选，快速找到心仪美食</p>
                        </div>
                        <button class="close-btn" @click="hideFilter" aria-label="关闭筛选">
                            <i class="fa fa-times"></i>
                        </button>
                    </div>

                    <div class="filter-content">
                        <!-- 排序方式 -->
                        <div class="filter-section">
                            <h4><i class="fa fa-sort" aria-hidden="true"></i>排序方式</h4>
                            <div class="filter-options">
                                <button v-for="option in sortOptions" :key="option.value" type="button"
                                    :class="['filter-chip', { active: sortBy === option.value }]"
                                    @click="setSortBy(option.value)">{{ option.label }}</button>
                            </div>
                        </div>

                        <!-- 配送与优惠 -->
                        <div class="filter-section">
                            <h4><i class="fa fa-truck" aria-hidden="true"></i>配送与优惠</h4>
                            <div class="filter-options">
                                <button v-for="option in deliveryOptions" :key="option.key" type="button"
                                    :class="['filter-chip', { active: filters[option.key] }]"
                                    @click="toggleSwitchFilter(option.key)">{{ option.label }}</button>
                            </div>
                        </div>

                        <!-- 起送价 -->
                        <div class="filter-section">
                            <h4><i class="fa fa-jpy" aria-hidden="true"></i>起送价</h4>
                            <div class="filter-options">
                                <button v-for="option in startPriceOptions" :key="option.value" type="button"
                                    :class="['filter-chip', { active: filters.startPrice === option.value }]"
                                    @click="setRadioFilter('startPrice', option.value)">{{ option.label }}</button>
                            </div>
                        </div>

                        <!-- 人均预算 -->
                        <div class="filter-section">
                            <h4><i class="fa fa-user-o" aria-hidden="true"></i>人均预算</h4>
                            <div class="filter-options">
                                <button v-for="option in budgetOptions" :key="option.value" type="button"
                                    :class="['filter-chip', { active: filters.budget === option.value }]"
                                    @click="setRadioFilter('budget', option.value)">{{ option.label }}</button>
                            </div>
                        </div>

                        <!-- 口味偏好：按商家的真实经营类目筛选 -->
                        <div class="filter-section">
                            <h4><i class="fa fa-cutlery" aria-hidden="true"></i>口味偏好</h4>
                            <div class="filter-options">
                                <button v-for="option in tasteOptions" :key="option.value" type="button"
                                    :class="['filter-chip', { active: filters.taste === option.value }]"
                                    @click="setRadioFilter('taste', option.value)">{{ option.label }}</button>
                            </div>
                        </div>
                    </div>

                    <div class="filter-footer">
                        <button class="btn-reset" @click="resetFilters">重置</button>
                        <button class="btn-confirm" @click="confirmFilters">查看 {{ businessList.length }} 家商家</button>
                    </div>
                </div>
            </div>
        </transition>

        <!-- 推荐商家列表部分 -->
        <div v-if="!businessList || businessList.length === 0" class="empty-business-list">
            <div class="empty-state">
                <i class="fa fa-store"></i>
                <p>暂无商家数据</p>
                <p class="empty-hint">请稍后再试或检查网络连接</p>
            </div>
        </div>

        <ul ref="businessListRef" class="business-list home-motion home-motion-businesses"
            :class="{ 'business-list-observe': observeBusinesses }" v-if="businessList && businessList.length > 0">
            <li v-for="(business, index) in visibleBusinessList" :key="business.id || business.businessId"
                :class="{ 'is-visible': revealedBusinessIds.has(businessKey(business)) }"
                :data-business-id="businessKey(business)"
                :style="{ '--stagger-index': index }"
                @click="toBusinessInfo(business.id || business.businessId)">
                <div class="business-info">
                    <img :src="business.businessImg || require('@/assets/business-default.png')"
                        @error="handleImageError" :alt="business.businessName"
                        :style="{ viewTransitionName: `restaurant-image-${business.id || business.businessId}` }">
                    <div class="business-info-detail">
                        <h3 :style="{ viewTransitionName: `restaurant-title-${business.id || business.businessId}` }">{{ business.businessName || '未命名商铺'}} <small v-if="business.operatingStatus === false" class="closed-shop-tag">休息中</small></h3>
                        <div class="business-info-rating">
                            <span class="rating-score">★ {{ formatBusinessScore(business.score) }}</span>
                            <span class="monthly-sales">月售 {{ business.salesCount || 0 }}</span>
                            <span class="average-price">人均 ¥{{ formatMoney(getBusinessAveragePrice(business)) }}</span>
                        </div>
                        <div class="business-info-delivery">
                            <span class="start-price">起送 ¥{{ (business.startPrice || 0).toFixed(2) }}</span>
                            <span class="delivery-fee" :class="{ 'free-delivery': (business.deliveryPrice || 0) === 0 }">
                                {{ (business.deliveryPrice || 0) === 0 ? '免配送费' : `配送费 ¥${(business.deliveryPrice || 0).toFixed(2)}` }}
                            </span>
                        </div>
                        <div class="business-tags">
                            <span v-for="tag in getBusinessTags(business)" :key="tag.label" :class="['business-tag', `tag-${tag.tone || 'neutral'}`]">{{ tag.label }}</span>
                        </div>
                    </div>
                    <div class="business-side">
                        <span class="business-distance">{{ getBusinessDistanceKm(business, index).toFixed(1) }}km</span>
                        <span class="business-eta">{{ getBusinessDeliveryMinutes(business, index) }}分钟送达</span>
                        <i class="fa fa-angle-right business-chevron" aria-hidden="true"></i>
                    </div>
                </div>
            </li>
        </ul>
        <button v-if="hasMoreBusinesses" type="button" class="load-more" @click="loadMoreBusinesses">加载更多商家</button>
<ai-chatbot class="ai-chat"/>
        <!-- 底部菜单部分 -->

    </div>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, computed, nextTick, watch } from 'vue';
import AiChatbot from '../components/AiChatbot.vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { formatMoney, formatRating } from '../utils/formatters';
import { clearAuth, getStoredUser, getToken, updateStoredUser } from '../utils/auth';
import { useLocationPicker } from '../composables/useLocationPicker';
import {
    getBusinessAveragePrice,
    getBusinessDeliveryMinutes,
    getBusinessDistanceKm,
    getBusinessTags,
    getGuessBadge,
    getRecommendationScore,
    hasConfiguredPromotion,
    isBusinessOpen,
    supportsDineIn
} from '../utils/businessPresentation';
import { pushWithViewTransition } from '../utils/navigationMotion';

/** 筛选面板的选项口径集中在这里，模板只负责渲染。 */
const SORT_OPTIONS = [
    { value: 'default', label: '综合排序' },
    { value: 'sales', label: '销量最高' },
    { value: 'distance', label: '距离最近' },
    { value: 'score', label: '评分优先' }
];
const DELIVERY_OPTIONS = [
    { key: 'freeDelivery', label: '免配送费' },
    { key: 'promotionOnly', label: '满减活动' },
    { key: 'dineIn', label: '支持堂食' },
    { key: 'openOnly', label: '营业中' }
];
const START_PRICE_OPTIONS = [
    { value: '0', label: '不限' },
    { value: '20', label: '20元以下' },
    { value: '30', label: '30元以下' },
    { value: '50', label: '50元以下' }
];
const BUDGET_OPTIONS = [
    { value: '0', label: '不限' },
    { value: '20', label: '20元以下' },
    { value: '20-30', label: '20-30元' },
    { value: '30-40', label: '30-40元' },
    { value: '40', label: '40元以上' }
];
/** 口味偏好按商家真实的经营类目（orderTypeId）筛选，而不是编造标签。 */
const TASTE_OPTIONS = [
    { value: '0', label: '不限' },
    { value: '1', label: '美食' },
    { value: '2', label: '早餐' },
    { value: '4', label: '汉堡披萨' },
    { value: '8', label: '米粉面馆' },
    { value: '5', label: '甜品饮品' }
];

export default {
    name: 'Index',
    setup() {
        const fixedBox = ref(null);
        const router = useRouter();
        const userInfo = ref(null);
        const businessList = ref([]);
        const originalBusinessList = ref([]); // 保存原始数据用于筛选和排序
        const currentPage = ref(1);
        const pageReady = ref(false);
        const businessListRef = ref(null);
        const observeBusinesses = ref(false);
        const revealedBusinessIds = ref(new Set());
        let businessObserver = null;
        const pageSize = 6;
        const suggestedBusinesses = computed(() => businessList.value.slice(0, 3));
        const visibleBusinessList = computed(() => businessList.value.slice(0, currentPage.value * pageSize));
        const hasMoreBusinesses = computed(() => visibleBusinessList.value.length < businessList.value.length);
        const businessKey = (business) => String(business?.id || business?.businessId || '');
        const revealBusiness = (element) => {
            const id = element?.dataset?.businessId;
            if (!id) return;
            const next = new Set(revealedBusinessIds.value);
            next.add(id);
            revealedBusinessIds.value = next;
            businessObserver?.unobserve(element);
        };
        const observeBusinessItems = () => {
            const elements = businessListRef.value?.querySelectorAll('[data-business-id]');
            if (!elements?.length) return;

            if (typeof window === 'undefined' || !('IntersectionObserver' in window)) {
                revealedBusinessIds.value = new Set(
                    [...elements].map(element => element.dataset.businessId).filter(Boolean)
                );
                observeBusinesses.value = false;
                return;
            }

            observeBusinesses.value = true;
            businessObserver ||= new IntersectionObserver((entries) => {
                entries.forEach((entry) => {
                    if (entry.isIntersecting) revealBusiness(entry.target);
                });
            }, { root: document.querySelector('.content'), rootMargin: '0px 0px 24px', threshold: 0.08 });
            elements.forEach(element => {
                if (!revealedBusinessIds.value.has(element.dataset.businessId)) businessObserver.observe(element);
            });
        };
        const {
            displayLocation,
            showPicker,
            loading,
            error: locationError,
            pendingLocation,
            locationData,
            currentLevel,
            locationLevels,
            selectedLocation,
            showLocationPicker,
            hideLocationPicker,
            switchLevel,
            selectLocation,
            isSelected,
            confirmLocation,
            getDisplayText,
            restoreSavedLocation
        } = useLocationPicker();

        let locationOpener = null;
        watch(showPicker, async open => {
            const shell = document.querySelector('.app-container');
            if (open) locationOpener = document.activeElement;
            if (shell) shell.inert = open;
            await nextTick();
            if (open) document.querySelector('.location-modal .close-btn')?.focus();
            else locationOpener?.focus();
        });
        const trapLocationFocus = event => {
            const buttons = [...event.currentTarget.querySelectorAll('button:not(:disabled), [tabindex="0"]')];
            const first = buttons[0], last = buttons[buttons.length - 1];
            if (event.shiftKey && document.activeElement === first) { event.preventDefault(); last?.focus(); }
            else if (!event.shiftKey && document.activeElement === last) { event.preventDefault(); first?.focus(); }
        };

        const scrollToRecommendations = () => {
            document.getElementById('recommendations')?.scrollIntoView({ behavior: 'smooth', block: 'start' });
        };

        const searchKeyword = ref('');
        const searchFocused = ref(false);
        /** 聚焦时提示可回车搜索，属于纯展示文案，不参与业务逻辑。 */
        const searchPlaceholder = computed(() => searchFocused.value
            ? '输入商家或菜品名，回车搜索'
            : '搜索商家、菜品');
        const sortBy = ref('default');
        const showFilter = ref(false);
        const filters = ref({
            freeDelivery: false,
            promotionOnly: false,
            dineIn: false,
            openOnly: false,
            startPrice: '0',
            budget: '0',
            taste: '0'
        });
        const fetchUserInfo = async () => {
            const storedUser = getStoredUser();
            if (storedUser?.id && storedUser?.username) {
                userInfo.value = storedUser;
                return;
            }
            if (!getToken()) return;

            try {
                const res = await request.get('/api/user');
                if (res && res.id && res.username) {
                    userInfo.value = res;
                    updateStoredUser(res);
                } else {
                    console.error('获取用户信息失败：接口返回数据不完整');
                    userInfo.value = null;
                }
            } catch (error) {
                console.error('获取用户信息异常:', error);
                userInfo.value = null;
                if (error.response?.status === 401) clearAuth();
            }
        };

        const hasBusinessRating = (score) => formatRating(score) !== null;
        const numericBusinessRating = (score) => Number(formatRating(score) || 0);
        const formatBusinessScore = (score) => (hasBusinessRating(score) ? formatRating(score) : '暂无');

        // 排序商家列表
        const sortBusinessList = (list, sortType) => {
            const sortedList = [...list];

            switch (sortType) {
                case 'default':
                    // 综合排序：按“个性化标签 > 评分 > 订单量 > 新鲜度”排序。
                    // 标签本身由 getBusinessTags 的阈值规则产生，避免首页只按销量粗暴排序。
                    sortedList.sort((a, b) => {
                        const recommendationDiff = getRecommendationScore(b) - getRecommendationScore(a);
                        if (Math.abs(recommendationDiff) > 0.01) return recommendationDiff;
                        const scoreA = numericBusinessRating(a.score);
                        const scoreB = numericBusinessRating(b.score);

                        // 评分比较（保留一位小数精度）
                        const scoreDiff = Math.round((scoreB - scoreA) * 10) / 10;
                        if (Math.abs(scoreDiff) >= 0.1) {
                            return scoreDiff; // 评分降序
                        }

                        // 评分相同，按ID排序（ID越大表示越新）
                        const idA = parseInt(a.id || a.businessId || 0);
                        const idB = parseInt(b.id || b.businessId || 0);
                        return idB - idA; // ID降序（新的在前）
                    });
                    break;

                case 'sales':
                    // 销量排序：销量优先，销量相同则按ID排序（ID大的在前）
                    sortedList.sort((a, b) => {
                        const salesA = parseInt(a.salesCount || 0);
                        const salesB = parseInt(b.salesCount || 0);
                        if (salesA !== salesB) return salesB - salesA;

                        // 销量相同，按ID排序（ID越大表示越新）
                        const idA = parseInt(a.id || a.businessId || 0);
                        const idB = parseInt(b.id || b.businessId || 0);
                        return idB - idA; // ID降序（新的在前）
                    });
                    break;

                case 'distance':
                    // 距离排序：与商家卡片使用同一距离口径，保证排序和展示一致。
                    sortedList.sort((a, b) => getBusinessDistanceKm(a) - getBusinessDistanceKm(b));
                    break;

                case 'score':
                    // 评分优先：评分降序，评分相同再比销量。
                    sortedList.sort((a, b) => {
                        const scoreDiff = numericBusinessRating(b.score) - numericBusinessRating(a.score);
                        if (Math.abs(scoreDiff) >= 0.01) return scoreDiff;
                        return parseInt(b.salesCount || 0) - parseInt(a.salesCount || 0);
                    });
                    break;

                default:
                    // 默认不排序，保持原有顺序
                    break;
            }

            return sortedList;
        };

        const navigateToOrders = () => {
            router.push({ path: '/orderList' });
        };
        const goToNotifications = () => {
            router.push({ path: '/notifications' });
        };
        onMounted(() => {
            restoreSavedLocation();
            // 加载用户信息
            fetchUserInfo();

            getBusinessList();
            requestAnimationFrame(() => { pageReady.value = true; });
            nextTick(observeBusinessItems);
        });

        onBeforeUnmount(() => {
            const shell = document.querySelector('.app-container');
            if (shell) shell.inert = false;
            businessObserver?.disconnect();
        });

        watch(visibleBusinessList, () => nextTick(observeBusinessItems), { flush: 'post' });

        const toBusinessList = (orderTypeId) => {
            router.push({ path: '/BusinessList', query: { orderTypeId } });
        };
        const goToLChoose = () => {
            // 跳转到登录页面
            router.push({ path: '/login', query: { role: 'user' } });
        };
        const goToRChoose = () => {
            router.push({ path: '/register' });
        }
        const navigateToSearch = () => {
            router.push({ path: '/search' });
        };

        // 执行搜索
        const performSearch = async () => {
            if (searchKeyword.value.trim() !== '') {
                try {
                    const params = {
                        keyword: searchKeyword.value.trim()
                    };

                    // 根据排序方式添加参数
                    if (sortBy.value === 'score') {
                        params.isScore = 1;
                        params.isSales = 0;
                    } else if (sortBy.value === 'sales') {
                        params.isScore = 0;
                        params.isSales = 1;
                    } else {
                        params.isScore = 0;
                        params.isSales = 0;
                    }

                    const response = await request.get('/api/businesses/search', { params });
                    const searchData = response?.success && Array.isArray(response.data)
                        ? response.data
                        : (Array.isArray(response) ? response : []);
                    originalBusinessList.value = searchData;
                    applyFiltersAndSort();
                } catch (error) {
                    console.error('搜索失败:', error);
                    getBusinessList();
                }
            } else {
                getBusinessList();
            }
        };

        // 设置排序方式
        const setSortBy = (type) => {
            sortBy.value = type;
            if (searchKeyword.value.trim() !== '') {
                performSearch();
            } else {
                applyFiltersAndSort();
            }
        };

        // 获取商家列表
        const getBusinessList = async () => {
            try {
                const response = await request.get('/api/businesses/search', {
                    params: { keyword: '', isScore: 0, isSales: 0 }
                });
                const businessData = response?.success && Array.isArray(response.data)
                    ? response.data
                    : (Array.isArray(response) ? response : []);
                originalBusinessList.value = businessData;
                applyFiltersAndSort();
            } catch (error) {
                console.error('获取商家列表失败:', error);
                originalBusinessList.value = [];
                businessList.value = [];
            }
        };

        // 处理图片加载失败
        const handleImageError = (e) => {
            e.target.src = require('@/assets/business-default.png');
        };

        // 跳转到商家详情页
        const toBusinessInfo = (businessId) => {
            pushWithViewTransition(router, {
                path: '/businessInfo',
                query: { businessId }
            });
        };

        // 筛选功能
        const toggleFilter = () => {
            showFilter.value = !showFilter.value;
        };

        const hideFilter = () => {
            showFilter.value = false;
        };

        // 应用筛选和排序的统一函数
        const applyFiltersAndSort = () => {
            let filteredList = [...originalBusinessList.value];

            // 免配送费筛选
            if (filters.value.freeDelivery) {
                filteredList = filteredList.filter(business =>
                    business.deliveryPrice === 0 || business.deliveryPrice === null
                );
            }

            if (filters.value.promotionOnly) {
                filteredList = filteredList.filter(hasConfiguredPromotion);
            }

            if (filters.value.dineIn) {
                filteredList = filteredList.filter(supportsDineIn);
            }

            if (filters.value.openOnly) {
                filteredList = filteredList.filter(isBusinessOpen);
            }

            // 起送价筛选
            if (filters.value.startPrice !== '0') {
                const maxPrice = parseInt(filters.value.startPrice);
                filteredList = filteredList.filter(business => {
                    const startPrice = business.startPrice || business.starPrice || 0;
                    return startPrice <= maxPrice;
                });
            }

            // 人均预算筛选
            filteredList = filteredList.filter(business => matchBudget(business, filters.value.budget));

            // 口味偏好筛选：仅使用商家真实的经营类目
            if (filters.value.taste !== '0') {
                const orderTypeId = Number(filters.value.taste);
                filteredList = filteredList.filter(business => Number(business.orderTypeId) === orderTypeId);
            }

            businessList.value = sortBusinessList(filteredList, sortBy.value);
            currentPage.value = 1;
        };

        /** 人均预算区间：'0' 不限，'20' 20元以下，'20-30' 区间，'40' 40元以上。 */
        const matchBudget = (business, budget) => {
            if (budget === '0') return true;
            const price = getBusinessAveragePrice(business);
            if (budget === '40') return price >= 40;
            if (budget.includes('-')) {
                const [min, max] = budget.split('-').map(Number);
                return price >= min && price <= max;
            }
            return price <= Number(budget);
        };

        const applyFilters = () => {
            applyFiltersAndSort();
        };

        /** 配送与优惠是开关型筛选，点一下即时生效。 */
        const toggleSwitchFilter = (key) => {
            filters.value = { ...filters.value, [key]: !filters.value[key] };
            applyFiltersAndSort();
        };

        /** 排序方式 / 起送价 / 人均预算 / 口味偏好都是单选。 */
        const setRadioFilter = (key, value) => {
            filters.value = { ...filters.value, [key]: value };
            applyFiltersAndSort();
        };

        const resetFilters = () => {
            filters.value = {
                freeDelivery: false,
                promotionOnly: false,
                dineIn: false,
                openOnly: false,
                startPrice: '0',
                budget: '0',
                taste: '0'
            };
            sortBy.value = 'default'; // 重置排序为默认
            applyFiltersAndSort();
        };

        const confirmFilters = () => {
            applyFiltersAndSort();
            hideFilter();
        };

        const loadMoreBusinesses = () => {
            if (hasMoreBusinesses.value) currentPage.value += 1;
        };

        return {
            fixedBox,
            pageReady,
            businessListRef,
            observeBusinesses,
            revealedBusinessIds,
            businessKey,
            toBusinessList,
            navigateToOrders,
            goToNotifications,
            goToLChoose,
            goToRChoose,
            userInfo,
            isuser: computed(() => !!userInfo.value),
            navigateToSearch,
            businessList,
            visibleBusinessList,
            hasMoreBusinesses,
            loadMoreBusinesses,
            toBusinessInfo,
            handleImageError,
            formatBusinessScore,
            hasBusinessRating,
            sortOptions: SORT_OPTIONS,
            deliveryOptions: DELIVERY_OPTIONS,
            startPriceOptions: START_PRICE_OPTIONS,
            budgetOptions: BUDGET_OPTIONS,
            tasteOptions: TASTE_OPTIONS,
            getGuessBadge,
            getBusinessAveragePrice,
            getBusinessDistanceKm,
            getBusinessDeliveryMinutes,
            toggleSwitchFilter,
            setRadioFilter,
            displayLocation,
            showPicker,
            loading,
            locationError,
            pendingLocation,
            trapLocationFocus,
            locationData,
            currentLevel,
            locationLevels,
            selectedLocation,
            showLocationPicker,
            hideLocationPicker,
            switchLevel,
            selectLocation,
            isSelected,
            confirmLocation,
            getDisplayText,
            searchKeyword,
            searchFocused,
            searchPlaceholder,
            sortBy,
            performSearch,
            setSortBy,
            showFilter,
            filters,
            toggleFilter,
            hideFilter,
            applyFilters,
            resetFilters,
            confirmFilters,
            applyFiltersAndSort,
            sortBusinessList,
            suggestedBusinesses,
            formatMoney,
            getBusinessTags,
            getRecommendationScore,
            scrollToRecommendations
        };
    },
    components: {
        AiChatbot
    }
}
</script>

<style scoped>
/****************** 总容器 ******************/
.wrapper {
    width: 100%;
    height: 100%;
}

/****************** header ******************/
.wrapper header {
    width: 100%;
    height: 12vw;
    background-color: var(--fwl-brand, #0097ff);
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 0 4vw;
    box-sizing: border-box;
}

/* 确保位置信息不会被挤压 */
.wrapper header .icon-location-box {
    width: 3.5vw;
    height: 3.5vw;
    margin-right: 1vw;
    flex-shrink: 0;
}

.wrapper header .location-text {
    font-size: 4.5vw;
    font-weight: 700;
    color: #fff;
    flex-shrink: 0;
    white-space: nowrap;
}

.wrapper header .icon-location-box i {
    font-size: 5vw;
    color: #fff;
}

.wrapper header .location-text .fa-caret-down {
    margin-left: 1vw;
}


/****************** search ******************/
.wrapper .search {
    width: 100%;
    height: 13vw;
}

.wrapper .search .search-fixed-top {
    width: 100%;
    height: 13vw;
    background-color: var(--fwl-brand, #0097FF);
    display: flex;
    justify-content: center;
    align-items: center;
    position: relative;
    z-index: 20;
    /* 确保搜索框在轮播图之上 */
}

.wrapper .search .search-fixed-top .search-box {
    width: 90%;
    height: 9vw;
    background-color: #fff;
    border-radius: 2px;

    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 0 2vw;

    font-size: 3.5vw;
    color: #AEAEAE;
    font-family: "宋体";
    /*此样式是让文本选中状态无效*/
    user-select: none;
}

.wrapper .search .search-fixed-top .search-box input {
    flex: 1;
    border: none;
    outline: none;
    background: transparent;
    font-size: 3.5vw;
    color: #333;
    margin: 0 1vw;
}

.wrapper .search .search-fixed-top .search-box input::placeholder {
    color: #AEAEAE;
}

.wrapper .search .search-fixed-top .search-box .search-btn {
    background: var(--fwl-brand, #0097ff);
    color: white;
    border: none;
    padding: 1.5vw 3vw;
    border-radius: 1vw;
    font-size: 3vw;
    cursor: pointer;
    transition: background-color 0.3s;
}

.wrapper .search .search-fixed-top .search-box .search-btn:hover {
    background: var(--fwl-brand, #0080e0);
}

.wrapper .search .search-fixed-top .search-box .fa-search {
    margin-right: 1vw;
}

/* 排序选项样式 */
.sort-options {
    width: 100%;
    padding: 3vw;
    background-color: var(--fwl-surface, #f8f9fa);
    border-bottom: 1px solid #e0e0e0;
}

.sort-buttons {
    display: flex;
    gap: 2vw;
    justify-content: center;
    flex-wrap: wrap;
}

.sort-buttons button {
    padding: 2vw 4vw;
    border: 1px solid #ddd;
    background-color: white;
    color: #666;
    border-radius: 2vw;
    cursor: pointer;
    transition: all 0.3s;
    font-size: 3.2vw;
    min-width: 20vw;
}

.sort-buttons button:hover {
    border-color: var(--fwl-brand, #0097ff);
    color: var(--fwl-brand, #0097ff);
}

.sort-buttons button.active {
    background-color: var(--fwl-brand, #0097ff);
    color: white;
    border-color: var(--fwl-brand, #0097ff);
}

/****************** 点餐分类部分 ******************/
.wrapper .foodtype {
    width: 100%;
    height: 48vw;
    background-color: white;
    display: flex;
    flex-wrap: wrap;
    justify-content: space-around;
    /*要使用align-content。10个子元素将自动换行为两行，而且两行作为一个整体垂直居中*/
    align-content: center;
}

.wrapper .foodtype li {
    /*一共10个子元素，通过计算，子元素宽度在16.7 ~ 20 之间，才能保证换两行*/
    width: 18vw;
    height: 20vw;

    display: flex;
    /*弹性盒子主轴方向设为column，然后仍然是垂直水平方向居中*/
    flex-direction: column;
    justify-content: center;
    align-items: center;

    user-select: none;
    cursor: pointer;
}

.wrapper .foodtype li img {
    width: 12vw;
    /*视频讲解时高度设置为12vw，实际上设置为10.3vw更佳*/
    height: 10.3vw;
}

.wrapper .foodtype li p {
    font-size: 3.2vw;
    color: #666;
}

/****************** 销量冠军3D轮播图部分 ******************/
.wrapper .top-businesses-carousel {
    width: 95%;
    margin: 1.5vw auto;
    /* 进一步减少上下外边距 */
    background: white;
    border-radius: 2vw;
    padding: 1.5vw 2vw;
    /* 进一步减少上下内边距 */
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.1);
    position: relative;
    z-index: 10;
    /* 确保在搜索框之下 */
}

.wrapper .top-businesses-carousel .carousel-header {
    text-align: center;
    margin-bottom: 1.5vw;
    /* 减少标题下方间距 */
    color: #333;
}

.wrapper .top-businesses-carousel .carousel-header h3 {
    font-size: 6vw;
    /* 增大字体大小 */
    margin: 0 0 1vw 0;
    font-weight: 700;
    text-shadow: none;
}

.wrapper .top-businesses-carousel .carousel-header p {
    font-size: 2.8vw;
    margin: 0;
    opacity: 0.7;
    color: #666;
}

.wrapper .top-businesses-carousel .carousel-3d-container {
    position: relative;
    height: 50vw;
    /* 减少整体高度，让占位更小 */
    min-height: 320px;
    /* 减少最小高度 */
    display: flex;
    justify-content: center;
    align-items: center;
    padding: 3vw 12vw;
    /* 减少内边距，让占位更小 */
    margin: -3vw -12vw;
    /* 调整负边距 */
}

.wrapper .top-businesses-carousel .carousel-3d-item {
    position: absolute;
    transition: all 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    cursor: pointer;
    transform-style: preserve-3d;
}

/* 中间激活状态 */
.wrapper .top-businesses-carousel .carousel-3d-item.active {
    z-index: 13;
    transform: translateX(0) scale(1);
    opacity: 1;
}

/* 左边状态 */
.wrapper .top-businesses-carousel .carousel-3d-item.left {
    z-index: 12;
    transform: translateX(-20vw) scale(0.75);
    opacity: 0.6;
}

/* 右边状态 */
.wrapper .top-businesses-carousel .carousel-3d-item.right {
    z-index: 12;
    transform: translateX(20vw) scale(0.75);
    opacity: 0.6;
}

.wrapper .top-businesses-carousel .business-card-3d {
    width: 38vw;
    /* 减少卡片宽度 */
    min-width: 240px;
    /* 减少最小宽度 */
    background: white;
    border-radius: 2vw;
    padding: 2.5vw;
    /* 减少内边距 */
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
    position: relative;
    overflow: hidden;
}

.wrapper .top-businesses-carousel .carousel-3d-item:hover .business-card-3d {
    transform: translateY(-0.5vw);
    box-shadow: 0 6px 24px rgba(0, 0, 0, 0.2);
}

.wrapper .top-businesses-carousel .rank-badge {
    position: absolute;
    top: 0;
    right: 0;
    padding: 1.5vw 3vw;
    border-radius: 0 2vw 0 2vw;
    color: white;
    font-weight: 700;
    font-size: 2.5vw;
    z-index: 15;
}

.wrapper .top-businesses-carousel .rank-badge.champion {
    background: var(--fwl-brand, #1d8bd1);
}

.wrapper .top-businesses-carousel .rank-badge.runner-up {
    background: var(--fwl-brand, #4d9fcf);
}

.wrapper .top-businesses-carousel .rank-badge.third {
    background: var(--fwl-brand-soft, #76b4d8);
}

.wrapper .top-businesses-carousel .business-image {
    width: 100%;
    height: 25vw;
    /* 调整图片高度，在更小的卡片中保持比例 */
    min-height: 160px;
    /* 调整最小高度 */
    border-radius: 1.5vw;
    overflow: hidden;
    margin-bottom: 1.5vw;
    /* 减少底部间距 */
}

.wrapper .top-businesses-carousel .business-image img {
    width: 100%;
    height: 100%;
    object-fit: cover;
    transition: transform 0.3s ease;
}

.wrapper .top-businesses-carousel .carousel-3d-item:hover .business-image img {
    transform: scale(1.05);
}

.wrapper .top-businesses-carousel .business-info {
    text-align: center;
}

.wrapper .top-businesses-carousel .business-info h4 {
    font-size: 3.2vw;
    /* 稍微减少字体大小 */
    font-weight: 700;
    color: #333;
    margin: 0 0 1vw 0;
    /* 减少底部间距 */
    line-height: 1.2;
}

.wrapper .top-businesses-carousel .stats {
    display: flex;
    justify-content: space-between;
    margin-bottom: 1.5vw;
    gap: 1.5vw;
}

.wrapper .top-businesses-carousel .stat-item {
    display: flex;
    align-items: center;
    justify-content: center;
    gap: 0.8vw;
    padding: 1.2vw 2vw;
    border-radius: 2vw;
    font-size: 2.5vw;
    font-weight: 600;
    flex: 1;
    color: white;
}

.wrapper .top-businesses-carousel .rating-stat {
    background: var(--fwl-surface, #eaf5ff);
    color: var(--fwl-brand-strong, #24577e);
}

.wrapper .top-businesses-carousel .rating-stat .fa-star {
    color: var(--fwl-brand, #2588c9);
}

.wrapper .top-businesses-carousel .sales-stat {
    background: var(--fwl-surface, #edf7fb);
    color: var(--fwl-brand-strong, #24577e);
}

.wrapper .top-businesses-carousel .sales-stat .fa-fire {
    color: var(--fwl-brand, #2588c9);
}

.wrapper .top-businesses-carousel .delivery-info {
    display: flex;
    justify-content: space-between;
    gap: 1vw;
}

.wrapper .top-businesses-carousel .delivery-tag {
    display: flex;
    align-items: center;
    gap: 0.5vw;
    background: var(--fwl-surface, #f8f9fa);
    padding: 1vw 1.5vw;
    border-radius: 1.5vw;
    border: 1px solid var(--fwl-border, #e9ecef);
    flex: 1;
    justify-content: center;
}

.wrapper .top-businesses-carousel .delivery-tag .tag-label {
    font-size: 2.2vw;
    color: #6c757d;
    font-weight: 500;
}

.wrapper .top-businesses-carousel .delivery-tag .tag-price {
    font-size: 2.4vw;
    color: var(--fwl-brand, #007bff);
    font-weight: 600;
}

/* 轮播箭头按钮 */
.wrapper .top-businesses-carousel .carousel-arrow {
    position: absolute;
    top: 50%;
    transform: translateY(-50%);
    width: 8vw;
    height: 8vw;
    background: rgba(255, 255, 255, 0.9);
    border: 1px solid #ddd;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    font-size: 3vw;
    color: #333;
    transition: all 0.3s ease;
    box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
    z-index: 16;
    opacity: 0;
    visibility: hidden;
}

.wrapper .top-businesses-carousel:hover .carousel-arrow {
    opacity: 1;
    visibility: visible;
}

.wrapper .top-businesses-carousel .carousel-arrow:hover {
    background: white;
    transform: translateY(-50%) scale(1.1);
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
}

.wrapper .top-businesses-carousel .carousel-arrow-left {
    left: 8vw;
    /* 调整到扩大的悬停区域内 */
}

.wrapper .top-businesses-carousel .carousel-arrow-right {
    right: 8vw;
    /* 调整到扩大的悬停区域内 */
}

.wrapper .top-businesses-carousel .carousel-indicators {
    display: flex;
    justify-content: center;
    gap: 1.5vw;
    margin-top: 1.5vw;
    /* 减少指示器上方间距 */
}

.wrapper .top-businesses-carousel .indicator {
    width: 2.5vw;
    height: 2.5vw;
    border-radius: 50%;
    background: #ddd;
    cursor: pointer;
    transition: all 0.3s ease;
}

.wrapper .top-businesses-carousel .indicator.active {
    background: var(--fwl-brand, #0097ff);
    transform: scale(1.2);
}

/****************** 超级会员部分 ******************/
.wrapper .supermember {
    /*这里也设置容器宽度95%，不能用padding，因为背景色也会充满padding*/
    width: 95%;
    margin: 0 auto;
    height: 11.5vw;
    background-color: #FEEDC1;
    margin-top: 1.3vw;
    border-radius: 2px;
    color: #644F1B;

    display: flex;
    justify-content: space-between;
    align-items: center;
}

.wrapper .supermember .left {
    display: flex;
    align-items: center;
    margin-left: 4vw;
    user-select: none;
}

.wrapper .supermember .left img {
    width: 6vw;
    height: 6vw;
    margin-right: 2vw;
}

.wrapper .supermember .left h3 {
    font-size: 4vw;
    margin-right: 2vw;
}

.wrapper .supermember .left p {
    font-size: 3vw;
}

.wrapper .supermember .right {
    font-size: 3vw;
    margin-right: 4vw;
    cursor: pointer;
}

/****************** 推荐商家部分 ******************/
.wrapper .recommend {
    width: 100%;
    height: 14vw;
    display: flex;
    justify-content: center;
    align-items: center;
}

.wrapper .recommend .recommend-line {
    width: 6vw;
    height: 0.2vw;
    background-color: #888;
}

.wrapper .recommend p {
    font-size: 4vw;
    margin: 0 4vw;
}

/****************** 推荐方式部分 ******************/
.wrapper .recommendtype {
    width: 100%;
    height: 5vw;
    margin-bottom: 5vw;

    display: flex;
    justify-content: space-around;
    align-items: center;
}

.wrapper .recommendtype li {
    font-size: 3.5vw;
    color: #555;
}

/****************** 推荐商家列表部分 ******************/
.wrapper .business-list {
    width: 100%;
    padding: 0;
    margin: 0 0 15vh 0;
    /* 添加底部边距，避免被 Footer 遮挡 */
    list-style: none;
}

.wrapper .business-list li {
    padding: 3vw;
    border-bottom: 1px solid #f0f0f0;
    cursor: pointer;
    transition: background-color 0.3s;
}

.wrapper .business-list li:hover {
    background-color: #f9f9f9;
}

.wrapper .business-list li .business-info {
    display: flex;
    align-items: flex-start;
}

.wrapper .business-list li .business-info img {
    width: 20vw;
    height: 20vw;
    object-fit: cover;
    border-radius: 4px;
}

.wrapper .business-list li .business-info .business-info-detail {
    flex: 1;
    margin-left: 3vw;
}

.wrapper .business-list li .business-info .business-info-detail h3 {
    font-size: 4vw;
    margin: 0 0 2vw 0;
    color: #333;
}

.wrapper .business-list li .business-info .business-info-rating {
    display: flex;
    align-items: center;
    gap: 3vw;
    margin-bottom: 2vw;
}

.wrapper .business-list li .business-info .business-info-rating .rating-score {
    font-size: 3.2vw;
    color: #FF6600;
    font-weight: 600;
}

.wrapper .business-list li .business-info .business-info-rating .monthly-sales {
    font-size: 2.8vw;
    color: #999;
}

.wrapper .business-list li .business-info .business-info-delivery {
    display: flex;
    gap: 2vw;
    margin-bottom: 2vw;
}

.wrapper .business-list li .business-info .business-info-delivery .start-price {
    font-size: 2.8vw;
    color: #666;
}

.wrapper .business-list li .business-info .business-info-delivery .delivery-fee {
    font-size: 2.8vw;
    color: #666;
}

.wrapper .business-list li .business-info .business-info-delivery .delivery-fee.free-delivery {
    color: #FF6600;
    font-weight: 500;
}

.wrapper .business-list li .business-info .business-info-promotion {
    display: flex;
    align-items: center;
}

.wrapper .business-list li .business-info .business-info-promotion .business-info-promotion-left {
    display: flex;
    align-items: center;
    gap: 1vw;
}

.wrapper .business-list li .business-info .business-info-promotion .business-info-promotion-left .business-info-promotion-left-incon {
    background-color: #ff4444;
    color: white;
    padding: 0.5vw 1vw;
    border-radius: 2px;
    font-size: 2.5vw;
}

.wrapper .business-list li .business-info .business-info-promotion .business-info-promotion-left p {
    color: #666;
    font-size: 3vw;
    margin: 0;
}

/* 位置显示样式 */
.location-text {
    cursor: pointer;
    transition: color 0.3s;
    display: flex;
    align-items: center;
    gap: 4px;
}

.location-text:hover {
    color: #e0e0e0;
}

.location-display {
    max-width: 180px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 模态框样式 */
.location-modal {
    position: fixed;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background-color: rgba(0, 0, 0, 0.5);
    display: flex;
    justify-content: center;
    align-items: center;
    z-index: 1000;
    padding: 20px;
}

.modal-container {
    background: white;
    border-radius: 12px;
    width: 100%;
    max-width: 400px;
    max-height: 80vh;
    display: flex;
    flex-direction: column;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.3);
    overflow: hidden;
}

.modal-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
    background: var(--fwl-brand, #0097ff);
    color: white;
}

.modal-header h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
}

.close-btn {
    background: none;
    border: none;
    color: white;
    font-size: 20px;
    cursor: pointer;
    padding: 5px;
    border-radius: 50%;
    transition: background-color 0.3s;
}

.close-btn:hover {
    background-color: rgba(255, 255, 255, 0.2);
}

.modal-content {
    display: flex;
    flex-direction: column;
    padding: 20px;
    margin-left: 27px;
    margin-top: 10px;
    margin-bottom: 10px;
    overflow-y: auto;
}

/* 位置导航样式 */
.location-nav {
    display: flex;
    margin-bottom: 20px;
    border-bottom: 2px solid #f0f0f0;
}

.nav-item {
    padding: 12px 20px;
    cursor: pointer;
    border-bottom: 3px solid transparent;
    transition: all 0.3s;
    font-weight: 500;
    color: #666;
}

.nav-item.active {
    color: var(--fwl-brand, #0097ff);
    border-bottom-color: var(--fwl-brand, #0097ff);
}

.nav-item.disabled {
    color: #ccc;
    cursor: not-allowed;
}

.nav-item:not(.disabled):hover {
    color: var(--fwl-brand, #0097ff);
}

/* 位置列表样式 */
.location-list-container {
    min-height: 200px;
    max-height: 300px;
    overflow-y: auto;
    margin-bottom: 20px;
}

.loading-state,
.empty-state {
    display: flex;
    flex-direction: column;
    align-items: center;
    justify-content: center;
    height: 150px;
    color: #999;
}

.loading-state i,
.empty-state i {
    font-size: 24px;
    margin-bottom: 10px;
}

.location-items {
    display: grid;
    gap: 8px;
}

.location-item {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 12px 16px;
    border: 1px solid #e0e0e0;
    border-radius: 8px;
    cursor: pointer;
    transition: all 0.3s;
}

.location-item:hover {
    border-color: var(--fwl-brand, #0097ff);
    background-color: var(--fwl-surface, #f8f9ff);
}

.location-item.selected {
    border-color: var(--fwl-brand, #0097ff);
    background-color: var(--fwl-surface, #e6f3ff);
}

.item-name {
    font-weight: 500;
}

.selected-icon {
    color: var(--fwl-brand, #0097ff);
    font-size: 14px;
}

/* 当前选择显示 */
.current-selection {
    padding: 15px;
    background-color: var(--fwl-surface, #f8f9fa);
    border-radius: 8px;
    margin-top: 15px;
}

.selection-text {
    font-weight: 600;
    color: var(--fwl-brand, #0097ff);
    display: inline-block;
    max-width: 250px;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

/* 模态框底部 */
.modal-footer {
    display: flex;
    gap: 12px;
    padding: 20px;
    border-top: 1px solid #f0f0f0;
    background-color: #fafafa;
}

.btn-cancel,
.btn-confirm {
    flex: 1;
    padding: 12px;
    border: none;
    border-radius: 6px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
}

.btn-cancel {
    background-color: var(--fwl-surface, #f8f9fa);
    color: #666;
}

.btn-cancel:hover {
    background-color: var(--fwl-border, #e9ecef);
}

.btn-confirm {
    background: var(--fwl-brand, #0097ff);
    color: white;
}

.btn-confirm:hover {
    background: var(--fwl-brand, #087dcc);
    transform: translateY(-1px);
}

/* 动画效果 */
.fade-enter-active,
.fade-leave-active {
    transition: opacity 0.3s;
}

.fade-enter-from,
.fade-leave-to {
    opacity: 0;
}

/* 空状态样式 */
.empty-carousel,
.empty-business-list {
    display: flex;
    justify-content: center;
    align-items: center;
    min-height: 200px;
    padding: 40px 20px;
}

.empty-carousel .empty-state,
.empty-business-list .empty-state {
    text-align: center;
    color: #999;
}

.empty-carousel .empty-state i,
.empty-business-list .empty-state i {
    font-size: 48px;
    margin-bottom: 16px;
    opacity: 0.5;
}

.empty-carousel .empty-state p,
.empty-business-list .empty-state p {
    font-size: 16px;
    margin: 8px 0;
}

.empty-business-list .empty-state .empty-hint {
    font-size: 14px;
    color: #ccc;
}

/* 推荐方式样式 */
.wrapper .recommendtype {
    width: 100%;
    height: 5vw;
    margin-bottom: 5vw;
    display: flex;
    justify-content: space-around;
    align-items: center;
}

.wrapper .recommendtype li {
    font-size: 3.5vw;
    color: #555;
    cursor: pointer;
    transition: color 0.3s;
    padding: 1vw 2vw;
    border-radius: 1vw;
}

.wrapper .recommendtype li:hover {
    color: var(--fwl-brand, #0097ff);
}

.wrapper .recommendtype li.active {
    color: var(--fwl-brand, #0097ff);
    background-color: var(--fwl-surface, #f0f8ff);
}

/* 筛选面板继承的通用样式（结构见 .filter-sheet） */
.filter-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    padding: 20px;
    border-bottom: 1px solid #f0f0f0;
    background: var(--fwl-brand, #0097ff);
    color: white;
}

.filter-header h3 {
    margin: 0;
    font-size: 18px;
    font-weight: 600;
}

.close-btn {
    background: none;
    border: none;
    color: white;
    font-size: 20px;
    cursor: pointer;
    padding: 5px;
    border-radius: 50%;
    transition: background-color 0.3s;
}

.close-btn:hover {
    background-color: rgba(255, 255, 255, 0.2);
}

.filter-content {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
}

.filter-section {
    margin-bottom: 25px;
}

.filter-footer {
    display: flex;
    gap: 12px;
    padding: 20px;
    border-top: 1px solid #f0f0f0;
    background-color: #fafafa;
}

.btn-reset,
.btn-confirm {
    flex: 1;
    padding: 12px;
    border: none;
    border-radius: 6px;
    font-weight: 600;
    cursor: pointer;
    transition: all 0.3s;
}

.btn-reset {
    background-color: var(--fwl-surface, #f8f9fa);
    color: #666;
}

.btn-reset:hover {
    background-color: var(--fwl-border, #e9ecef);
}

.btn-confirm {
    background: var(--fwl-brand, #0097ff);
    color: white;
}

.btn-confirm:hover {
    background: var(--fwl-brand, #087dcc);
    transform: translateY(-1px);
}

.ai-chat {
    position: fixed;
  bottom: 100px; /* 距离底部40px */
  right: 20px; /* 距离右边20px */
  z-index: 9999;
}

/* 首页外壳：手机优先，桌面端保持居中 600px 的手机画布 */
.wrapper { max-width: 600px; margin: 0 auto; overflow-x: hidden; }

/* ── 首页最终稿（对照参考图实现）──────────────────────────────────
 * 插画头部 → 搜索 → 分类卡 → 猜你想吃 → 推荐商家。
 * 只有 CSS 画不出的校园插画被裁成图片，其余全部是活元素与真实数据。
 */
.home-page {
    --home-blue: var(--fwl-brand, #168fe4);
    --home-deep: var(--fwl-brand-strong, #0f3559);
    --home-title: var(--fwl-brand-strong, #103c6c);
    --home-muted: var(--fwl-muted, #8aa2b4);
    --home-border: var(--fwl-border, #dceaf4);
    position: relative;
    min-height: 100vh;
    background: var(--fwl-surface, #f4f9fd);
    color: var(--home-deep);
    font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", "Noto Sans CJK SC", sans-serif;
}

/* 头部保持与搜索区同色的纯色底，只保留插画色的浅蓝，不再叠任何背景图 */
.home-hero {
    position: relative;
    background: var(--fwl-border, #d9edfe);
}

.home-page .home-header {
    position: relative;
    z-index: 1;
    height: 112px;
    padding: 43px 16px 0;
    box-sizing: border-box;
    display: block;
    background: transparent;
}

.home-page .home-header .location-text {
    display: inline-flex;
    align-items: center;
    gap: 5px;
    max-width: 52%;
    color: var(--fwl-brand-strong, #0b2b54);
    font-size: 16px;
    font-weight: 700;
    line-height: 22px;
    cursor: pointer;
    user-select: none;
}

.home-page .home-header .location-text .fa-map-marker-alt {
    color: var(--fwl-brand, #1a8cff);
    font-size: 17px;
}

.home-page .home-header .location-text .fa-caret-down {
    margin-left: 1px;
    color: var(--fwl-brand, #1a8cff);
    font-size: 13px;
}

.home-page .home-header .location-display {
    max-width: 100%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.home-page .home-header .hero-actions {
    position: absolute;
    top: 39px;
    right: 16px;
    display: flex;
    align-items: center;
    gap: 6px;
}

.home-page .home-header .hero-pill,
.home-page .home-header .hero-user {
    max-width: 104px;
    padding: 4px 10px;
    box-sizing: border-box;
    overflow: hidden;
    border: 1px solid rgba(255, 255, 255, .9);
    border-radius: 13px;
    background: rgba(255, 255, 255, .72);
    color: var(--home-deep);
    font-size: 12px;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
    backdrop-filter: blur(8px);
}

.home-page .home-header .hero-pill {
    cursor: pointer;
}

.home-page .home-header .hero-pill:hover {
    background: #fff;
}

.home-page .home-header .hero-bell {
    position: relative;
    width: 26px;
    height: 26px;
    padding: 0;
    border: 0;
    background: transparent;
    color: var(--fwl-brand-strong, #0b2b54);
    font-size: 19px;
    line-height: 26px;
    cursor: pointer;
}

.home-page .home-header .hero-bell-dot {
    position: absolute;
    top: 2px;
    right: 3px;
    width: 7px;
    height: 7px;
    border: 1.5px solid #fff;
    border-radius: 50%;
    background: #ff4d4f;
}

.home-page .home-header .hero-bottom {
    position: absolute;
    left: 16px;
    right: 16px;
    top: 72px;
    display: flex;
    align-items: flex-start;
    justify-content: flex-end;
    gap: 12px;
}

.home-page .home-header .hero-slogan {
    margin: 0;
    padding-bottom: 3px;
    border-bottom: 2px solid rgba(var(--fwl-brand-rgb, 18, 129, 253), 0.32);
    color: var(--fwl-brand, #1281fd);
    font-size: 14px;
    font-style: italic;
    font-weight: 600;
    letter-spacing: .04em;
    line-height: 1.3;
    text-align: right;
}

.home-page .search {
    position: sticky;
    top: 0;
    z-index: 25;
    height: 72px;
}

.home-page .search .search-fixed-top {
    position: relative;
    inset: auto;
    transform: none;
    width: 100%;
    max-width: none;
    margin: 0;
    height: 72px;
    padding: 11px 14px 15px;
    box-sizing: border-box;
    background: var(--fwl-border, #d9edfe);
    backdrop-filter: none;
    /* 聚焦时两侧内边距收窄，搜索框展开到满宽 */
    transition: padding 300ms cubic-bezier(.22, 1, .36, 1);
}

.home-page .search .search-fixed-top:focus-within {
    padding-left: 5px;
    padding-right: 5px;
}

/* 位置弹窗保持在同一个手机画布内居中，并盖住底部导航 */
.home-page .location-modal {
    position: fixed;
    z-index: 12000;
    left: 50%;
    right: auto;
    width: min(100vw, 600px);
    max-width: 600px;
    transform: translateX(-50%);
    box-sizing: border-box;
}

.home-page .modal-container {
    width: calc(100% - 32px);
    max-width: 400px;
    margin-left: auto;
    margin-right: auto;
}

.home-page .search .search-fixed-top .search-box {
    width: 100%;
    height: 46px;
    padding: 0 6px 0 15px;
    box-sizing: border-box;
    border: 1px solid rgba(255, 255, 255, .95);
    border-radius: 23px;
    background: #fff;
    color: var(--fwl-muted, #8aa4bb);
    box-shadow: 0 7px 18px rgba(var(--fwl-brand-rgb, 35, 113, 166), 0.13);
    font-family: inherit;
    transform-origin: center;
    transition: transform 300ms cubic-bezier(.22, 1, .36, 1), box-shadow 300ms ease, border-color 300ms ease;
}

.home-page .search .search-fixed-top .search-box:focus-within {
    transform: translateY(-2px) scale(1.015);
    border-color: rgba(var(--fwl-brand-rgb, 22, 143, 228), 0.55);
    box-shadow: 0 13px 26px rgba(var(--fwl-brand-rgb, 35, 113, 166), 0.22), 0 0 0 3px rgba(var(--fwl-brand-rgb, 22, 143, 228), 0.12);
}

.home-page .search .search-fixed-top .search-box .fa-search {
    margin-right: 8px;
    color: var(--home-blue);
    font-size: 19px;
}

.home-page .search .search-fixed-top .search-box input {
    min-width: 0;
    margin: 0 8px;
    color: var(--home-deep);
    font-size: 14px;
}

.home-page .search .search-fixed-top .search-box input::placeholder {
    color: var(--fwl-muted, #8fa8be);
}

.home-page .search .search-fixed-top .search-box .search-btn {
    min-width: 66px;
    padding: 9px 14px;
    border-radius: 20px;
    background: linear-gradient(135deg, var(--fwl-brand, #2aa9f1), var(--fwl-brand, #0f83dc));
    color: #fff;
    font-size: 14px;
    font-weight: 700;
    box-shadow: 0 4px 10px rgba(var(--fwl-brand-rgb, 18, 126, 207), 0.2);
    transform-origin: center;
    transition: transform 160ms cubic-bezier(.22, 1, .36, 1);
}

.home-page .search .search-fixed-top .search-box .search-btn:active {
    transform: scale(.95);
}

/* 分类卡：白色圆角卡 + 浅蓝图标底，白色底图用 multiply 融进底色 */
.home-page .foodtype {
    position: relative;
    z-index: 2;
    width: calc(100% - 24px);
    height: auto;
    margin: 14px auto 0;
    padding: 14px 8px 12px;
    display: grid;
    grid-template-columns: repeat(5, 1fr);
    gap: 10px 2px;
    align-content: initial;
    box-sizing: border-box;
    border: 1px solid rgba(255, 255, 255, .9);
    border-radius: 18px;
    background: #fff;
    box-shadow: 0 10px 26px rgba(var(--fwl-brand-rgb, 58, 129, 177), 0.09);
    list-style: none;
}

.home-page .foodtype li {
    width: auto;
    height: auto;
    display: flex;
    flex-direction: column;
    align-items: center;
    gap: 6px;
    cursor: pointer;
    touch-action: manipulation;
}

.home-page .foodtype li .foodtype-icon {
    width: 46px;
    height: 46px;
    display: grid;
    place-items: center;
    border-radius: 15px;
    background: var(--fwl-surface, #eaf4fd);
    isolation: isolate;
    transform-origin: center;
    transition: transform 180ms cubic-bezier(.22, 1, .36, 1);
}

.home-page .foodtype li .foodtype-icon img {
    width: 34px;
    height: 30px;
    object-fit: contain;
    mix-blend-mode: multiply;
}

.home-page .foodtype li p {
    margin: 0;
    color: var(--fwl-brand-strong, #24476b);
    font-size: 12px;
    font-weight: 600;
    white-space: nowrap;
    transition: color 180ms ease;
}

.home-page .foodtype li:active .foodtype-icon {
    animation: category-press 150ms cubic-bezier(.22, 1, .36, 1);
}

.home-page .foodtype li:active p {
    color: var(--home-blue);
}

/* 分类点击反馈：放大到 1.15 并轻微上跳，再回弹归位 */
@keyframes category-press {
    0% { transform: scale(1) translateY(0); }
    45% { transform: scale(1.15) translateY(-4px); }
    100% { transform: scale(1) translateY(0); }
}

/* 猜你想吃：横向卡片 + 后端规则给出的角标 */
.home-page .guess-section {
    padding: 20px 14px 12px;
    background: transparent;
    border: 0;
}

.home-page .guess-section .section-heading,
.home-page .recommend .section-heading {
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: 0 1px 12px;
}

.home-page .guess-section .section-heading > div,
.home-page .recommend .section-heading > div {
    display: flex;
    align-items: baseline;
    gap: 8px;
}

.home-page .guess-section .section-heading h2,
.home-page .recommend .section-heading h2 {
    position: relative;
    margin: 0;
    color: var(--home-title);
    font-size: 21px;
    font-weight: 800;
    letter-spacing: 0;
}

.home-page .guess-section .section-heading h2 .section-spark {
    margin-left: 2px;
    color: #f7b731;
    font-size: 14px;
}

.home-page .guess-section .section-heading span,
.home-page .recommend .section-heading span {
    color: var(--fwl-subtle, #9aadb9);
    font-size: 12px;
}

.home-page .guess-section .section-heading button {
    border: 0;
    background: transparent;
    color: var(--fwl-muted, #8aa0af);
    font-size: 12px;
    cursor: pointer;
}

.home-page .guess-scroll {
    display: flex;
    gap: 10px;
    padding: 2px 1px 4px;
    overflow-x: auto;
    scrollbar-width: none;
}

.home-page .guess-scroll::-webkit-scrollbar {
    display: none;
}

.home-page .guess-card {
    flex: 0 0 152px;
    min-width: 0;
    padding: 8px;
    border: 1px solid var(--home-border);
    border-radius: 14px;
    background: #fff;
    box-shadow: 0 6px 16px rgba(var(--fwl-brand-rgb, 59, 120, 161), 0.07);
    text-align: left;
    cursor: pointer;
}

.home-page .guess-card-media {
    position: relative;
    display: block;
}

.home-page .guess-card-media img {
    display: block;
    width: 100%;
    height: 78px;
    object-fit: cover;
    border-radius: 10px;
    background: var(--fwl-surface, #eef5f9);
}

.home-page .guess-badge {
    position: absolute;
    top: 0;
    left: 0;
    max-width: calc(100% - 12px);
    overflow: hidden;
    padding: 3px 8px;
    border-radius: 9px 0 9px 0;
    color: #fff;
    font-size: 10px;
    font-style: normal;
    font-weight: 600;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.home-page .guess-badge-hot { background: linear-gradient(135deg, #ff7444, #f4452c); }
.home-page .guess-badge-morning { background: linear-gradient(135deg, #ffbb45, #f79009); }
.home-page .guess-badge-good { background: linear-gradient(135deg, var(--fwl-brand, #3aa8f2), var(--fwl-brand, #168fe4)); }
.home-page .guess-badge-fresh { background: linear-gradient(135deg, #45ce93, #22a866); }
.home-page .guess-badge-neutral { background: linear-gradient(135deg, var(--fwl-subtle, #9db2c4), var(--fwl-muted, #7d94a8)); }

.home-page .guess-card strong {
    display: block;
    margin-top: 7px;
    overflow: hidden;
    color: var(--home-title);
    font-size: 13px;
    font-weight: 700;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.home-page .guess-card-meta {
    display: flex;
    align-items: baseline;
    gap: 6px;
    margin-top: 5px;
}

.home-page .guess-card-meta b {
    color: #f27635;
    font-size: 12px;
    font-weight: 700;
}

.home-page .guess-card-meta span {
    overflow: hidden;
    color: var(--home-muted);
    font-size: 10px;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.home-page .guess-card-price {
    display: block;
    margin-top: 3px;
    color: var(--home-muted);
    font-size: 10px;
}

/* 推荐商家标题与排序胶囊 */
.home-page .recommend {
    display: block;
    width: 100%;
    height: auto;
    min-height: 0;
    margin: 0;
    padding: 18px 14px 0;
    box-sizing: border-box;
    background: transparent;
}

.home-page .recommend .recommend-line,
.home-page .recommend p {
    display: none;
}

.home-page .recommendtype {
    position: relative;
    z-index: 8;
    display: flex;
    align-items: center;
    justify-content: flex-start;
    gap: 8px;
    width: 100%;
    height: auto;
    margin: 12px 0 0;
    padding: 0 14px 12px;
    box-sizing: border-box;
    background: transparent;
    border-bottom: 0;
    backdrop-filter: none;
    list-style: none;
    overflow-x: auto;
    scrollbar-width: none;
}

.home-page .recommendtype::-webkit-scrollbar {
    display: none;
}

.home-page .recommendtype li {
    position: static;
    flex: 0 0 auto;
    width: auto;
    height: auto;
    padding: 7px 12px;
    border: 0;
    border-radius: 16px;
    background: var(--fwl-surface, #eef4f9);
    color: var(--fwl-muted, #70899c);
    font-size: 12px;
    font-weight: 500;
    cursor: pointer;
    transition: background-color 160ms ease, color 160ms ease;
}

.home-page .recommendtype li i {
    margin-left: 4px;
    font-size: 10px;
}

.home-page .recommendtype li.active {
    background: var(--fwl-surface, #e2f1fe);
    color: var(--home-blue);
    font-weight: 700;
    border-bottom: 0;
}

/* 推荐商家卡片：图片 + 信息 + 距离/送达时间/箭头 */
.home-page .business-list {
    width: 100%;
    margin: 0;
    padding: 2px 12px 92px;
    box-sizing: border-box;
    list-style: none;
}

.home-page .business-list li {
    width: 100%;
    min-height: 122px;
    margin: 0 0 11px;
    padding: 12px;
    box-sizing: border-box;
    border: 1px solid var(--home-border);
    border-radius: 15px;
    background: #fff;
    box-shadow: 0 7px 17px rgba(var(--fwl-brand-rgb, 51, 111, 151), 0.08);
    cursor: pointer;
}

.home-page .business-list li:hover {
    box-shadow: 0 7px 17px rgba(var(--fwl-brand-rgb, 51, 111, 151), 0.08);
}

.home-page .business-list li .business-info {
    display: flex;
    align-items: flex-start;
    gap: 10px;
}

.home-page .business-list li .business-info img {
    width: 104px;
    height: 104px;
    flex: 0 0 104px;
    margin: 0;
    border-radius: 10px;
    object-fit: cover;
    background: var(--fwl-surface, #eef5f9);
}

.home-page .business-list li .business-info .business-info-detail {
    min-width: 0;
    flex: 1;
}

.home-page .business-list li .business-info .business-info-detail h3 {
    margin: 0;
    overflow: hidden;
    color: var(--home-title);
    font-size: 16px;
    font-weight: 800;
    line-height: 1.3;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.home-page .business-list li .business-info .business-info-rating {
    display: flex;
    flex-wrap: wrap;
    align-items: baseline;
    gap: 4px 7px;
    margin-top: 7px;
}

.home-page .business-list li .business-info .business-info-rating .rating-score {
    color: #f27635;
    font-size: 15px;
    font-weight: 800;
}

.home-page .business-list li .business-info .business-info-rating .monthly-sales,
.home-page .business-list li .business-info .business-info-rating .average-price,
.home-page .business-list li .business-info .business-info-delivery .start-price,
.home-page .business-list li .business-info .business-info-delivery .delivery-fee {
    color: var(--fwl-muted, #7893ac);
    font-size: 11px;
    line-height: 1.4;
}

.home-page .business-list li .business-info .business-info-delivery {
    display: flex;
    flex-wrap: wrap;
    gap: 4px 10px;
    margin-top: 8px;
}

.home-page .business-list li .business-info .business-info-delivery .free-delivery {
    color: var(--home-blue);
}

.home-page .business-side {
    flex: 0 0 auto;
    max-width: 64px;
    display: flex;
    flex-direction: column;
    align-items: flex-end;
    gap: 5px;
    padding-top: 2px;
}

.home-page .business-distance {
    color: var(--home-muted);
    font-size: 11px;
    white-space: nowrap;
}

.home-page .business-eta {
    padding: 2px 6px;
    border-radius: 9px;
    background: var(--fwl-surface, #e8f4fe);
    color: var(--home-blue);
    font-size: 10px;
    white-space: nowrap;
}

.home-page .business-chevron {
    margin-top: 2px;
    color: var(--fwl-subtle, #c2d0da);
    font-size: 15px;
}

.home-page .business-tags {
    display: flex;
    flex-wrap: wrap;
    gap: 5px;
    margin-top: 8px;
}

.home-page .business-tag {
    display: inline-block;
    max-width: 100%;
    overflow: hidden;
    padding: 3px 6px;
    border: 1px solid var(--fwl-border, #d9e7ee);
    border-radius: 4px;
    font-size: 10px;
    line-height: 1.1;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.home-page .business-tag.tag-blue { border-color: var(--fwl-brand-soft, #b9def1); background: var(--fwl-surface, #f1faff); color: var(--fwl-brand, #168bd1); }
.home-page .business-tag.tag-orange { border-color: #f1d0b7; background: #fff8f2; color: #d97b43; }
.home-page .business-tag.tag-gold { border-color: #f0dfb0; background: #fffbef; color: #b48731; }
.home-page .business-tag.tag-green { border-color: #c5e5d2; background: #f2fbf5; color: #3d9b69; }
.home-page .business-tag.tag-neutral { border-color: var(--fwl-border, #dce7ed); background: var(--fwl-surface, #f8fbfc); color: var(--fwl-muted, #7591a0); }

.home-page .closed-shop-tag {
    margin-left: 5px;
    padding: 2px 6px;
    border-radius: 7px;
    background: var(--fwl-surface, #edf1f4);
    color: var(--fwl-muted, #80909c);
    font-size: 10px;
    font-weight: 500;
    vertical-align: 2px;
}

.home-page .empty-business-list {
    padding: 50px 16px;
}

.home-page .load-more {
    display: block;
    width: calc(100% - 24px);
    margin: 2px auto 90px;
    padding: 11px 0;
    border: 1px solid var(--fwl-border, #b9dff4);
    border-radius: 9px;
    background: #fff;
    color: var(--home-blue);
    font-size: 13px;
    cursor: pointer;
}

.home-page .load-more:active {
    background: var(--fwl-surface, #f1faff);
}

/* 筛选面板：底部弹出，5 个维度 + 查看 N 家商家。
 * 层级要高于底部导航(1000)和 AI 悬浮入口(9999)。 */
.filter-sheet-mask {
    position: fixed;
    inset: 0;
    z-index: 12000;
    display: flex;
    align-items: flex-end;
    justify-content: center;
    background: rgba(var(--fwl-brand-strong-rgb, 12, 52, 86), 0.42);
}

.filter-sheet {
    position: relative;
    width: 100%;
    max-width: 600px;
    max-height: 88vh;
    display: flex;
    flex-direction: column;
    padding-top: 8px;
    box-sizing: border-box;
    border-radius: 22px 22px 0 0;
    background: #fff;
    box-shadow: 0 -14px 40px rgba(var(--fwl-brand-strong-rgb, 12, 52, 86), 0.18);
}

.filter-sheet .sheet-handle {
    display: block;
    width: 42px;
    height: 4px;
    margin: 0 auto 6px;
    border-radius: 3px;
    background: var(--fwl-border, #dbe6ee);
}

.filter-sheet .filter-header {
    display: flex;
    align-items: flex-start;
    justify-content: space-between;
    padding: 6px 18px 12px;
    border: 0;
    background: #fff;
    color: inherit;
}

.filter-sheet .filter-header h3 {
    margin: 0;
    color: var(--home-title);
    font-size: 19px;
    font-weight: 800;
}

.filter-sheet .filter-header p {
    margin: 4px 0 0;
    color: var(--fwl-muted, #93a9ba);
    font-size: 12px;
}

.filter-sheet .close-btn {
    width: 28px;
    height: 28px;
    flex: 0 0 28px;
    padding: 0;
    border: 0;
    border-radius: 50%;
    background: var(--fwl-surface, #f2f6fa);
    color: var(--fwl-muted, #7b93a8);
    font-size: 14px;
    cursor: pointer;
}

.filter-sheet .close-btn:hover {
    background: var(--fwl-border, #e8eef4);
}

.filter-sheet .filter-content {
    flex: 1;
    padding: 2px 18px 8px;
    overflow-y: auto;
}

.filter-sheet .filter-section {
    margin-bottom: 18px;
}

.filter-sheet .filter-section h4 {
    display: flex;
    align-items: center;
    gap: 7px;
    margin: 0 0 11px;
    color: var(--home-title);
    font-size: 14px;
    font-weight: 700;
}

.filter-sheet .filter-section h4 i {
    width: 20px;
    height: 20px;
    display: grid;
    place-items: center;
    border-radius: 7px;
    background: var(--fwl-surface, #e8f4fe);
    color: var(--home-blue);
    font-size: 11px;
}

.filter-sheet .filter-options {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
}

.filter-sheet .filter-chip {
    padding: 8px 15px;
    border: 0;
    border-radius: 18px;
    background: var(--fwl-surface, #f1f5f9);
    color: var(--fwl-muted, #5d7488);
    font-size: 13px;
    cursor: pointer;
    transition: background-color 160ms ease, color 160ms ease;
}

.filter-sheet .filter-chip.active {
    background: linear-gradient(135deg, var(--fwl-brand, #2aa9f1), var(--fwl-brand, #0f83dc));
    color: #fff;
    font-weight: 600;
}

.filter-sheet .filter-footer {
    display: flex;
    gap: 12px;
    padding: 12px 18px 18px;
    border: 0;
    background: #fff;
}

.filter-sheet .filter-footer .btn-reset {
    flex: 0 0 34%;
    padding: 13px 0;
    border: 0;
    border-radius: 14px;
    background: var(--fwl-surface, #e8f4fe);
    color: var(--home-blue);
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
}

.filter-sheet .filter-footer .btn-confirm {
    flex: 1;
    padding: 13px 0;
    border: 0;
    border-radius: 14px;
    background: linear-gradient(135deg, var(--fwl-brand, #2aa9f1), var(--fwl-brand, #0f83dc));
    color: #fff;
    font-size: 15px;
    font-weight: 700;
    cursor: pointer;
}

.sheet-enter-active,
.sheet-leave-active {
    transition: opacity 240ms ease;
}

.sheet-enter-active .filter-sheet,
.sheet-leave-active .filter-sheet {
    transition: transform 350ms cubic-bezier(.22, 1, .36, 1);
}

.sheet-enter-from,
.sheet-leave-to {
    opacity: 0;
}

.sheet-enter-from .filter-sheet,
.sheet-leave-to .filter-sheet {
    transform: translateY(100%);
}

@media (max-width: 380px) {
    .home-page .home-header { padding-left: 14px; padding-right: 14px; }
    .home-page .home-header .location-text { max-width: 46%; font-size: 15px; }
    .home-page .home-header .hero-actions { right: 14px; }
    .home-page .home-header .hero-bottom { left: 14px; right: 14px; }
    .home-page .home-header .hero-slogan { font-size: 13px; }
    .home-page .business-list li .business-info img { width: 92px; height: 92px; flex-basis: 92px; }
}

@media (prefers-reduced-motion: reduce) {
    .home-page .search .search-fixed-top .search-box,
    .home-page .search .search-fixed-top .search-box .search-btn,
    .home-page .foodtype li .foodtype-icon,
    .home-page .foodtype li p,
    .home-page .guess-card,
    .home-page .recommendtype li,
    .filter-sheet .filter-chip {
        transition: none;
    }

    .home-page .foodtype li:active .foodtype-icon {
        animation: none;
    }

    .sheet-enter-active,
    .sheet-leave-active,
    .sheet-enter-active .filter-sheet,
    .sheet-leave-active .filter-sheet {
        transition: none;
    }
}
</style>

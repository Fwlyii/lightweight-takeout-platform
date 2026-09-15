<template>
	<div class="wrapper business-list-page" :class="{ 'business-list-page-ready': pageReady }">
		<header class="list-hero">
			<div class="list-hero-copy">
				<strong>商家列表</strong>
				<span>{{ orderTypeName }} · 校园周边优选</span>
			</div>
		</header>

		<section class="list-toolbar" aria-label="商家排序和筛选">
			<button type="button" :class="['toolbar-tab', { active: sortMode === 'default' }]" @click="setSortMode('default')">
				综合排序 <i class="fa fa-angle-down" aria-hidden="true"></i>
			</button>
			<button type="button" :class="['toolbar-tab', { active: sortMode === 'sales' }]" @click="setSortMode('sales')">销量最高</button>
			<button type="button" :class="['toolbar-tab', { active: sortMode === 'distance' }]" @click="setSortMode('distance')">距离最近</button>
			<button type="button" :class="['toolbar-tab', { active: showFilter }]" @click="showFilter = !showFilter">
				<i class="fa fa-filter" aria-hidden="true"></i> 筛选 <i class="fa fa-angle-down" aria-hidden="true"></i>
			</button>
		</section>

		<div v-if="showFilter" class="filter-drawer" role="group" aria-label="筛选条件">
			<button type="button" :class="{ active: filterMode === 'all' }" @click="filterMode = 'all'">全部商家</button>
			<button type="button" :class="{ active: filterMode === 'freeDelivery' }" @click="filterMode = 'freeDelivery'">免配送费</button>
			<button type="button" :class="{ active: filterMode === 'dineIn' }" @click="filterMode = 'dineIn'">支持堂食</button>
		</div>

		<!-- 商家列表部分 -->
		<div v-if="loading" class="list-state">正在加载商家…</div>
		<div v-else-if="loadError" class="list-state error">商家列表加载失败，请稍后重试</div>
		<div v-else-if="businessArr.length === 0" class="list-state">
			<i class="fa fa-store-o"></i>
			<p>该分类暂时没有商家</p>
		</div>
		<div v-if="displayedBusinesses.length" class="business-list" :class="{ 'business-list-observe': observeBusinesses }" ref="businessListRef">
			<article class="business-item" v-for="(business, index) in displayedBusinesses" :key="business.id"
				:class="{ 'is-visible': revealedBusinessIds.has(String(business.id)) }"
				:data-business-id="business.id"
				:style="{ '--stagger-index': index }"
				@click="toBusinessInfo(business.id)">
				<div class="business-info">
					<img :src="business.businessImg || require('@/assets/business-default.png')"
						:alt="business.businessName" @error="handleImageError"
						:style="{ viewTransitionName: `restaurant-image-${business.id}` }" />
					<div class="business-details">
						<div class="business-title-row">
							<h2 :style="{ viewTransitionName: `restaurant-title-${business.id}` }">{{ business.businessName || '未知商铺' }}</h2>
							<span v-if="business.operatingStatus === false" class="closed-shop-tag">休息中</span>
							<i class="fa fa-angle-right business-arrow" aria-hidden="true"></i>
						</div>
						<div class="business-metrics">
							<strong><i class="fa fa-star" aria-hidden="true"></i> {{ formatScore(business.score) }}</strong>
							<span>月售 {{ business.salesCount || 0 }}</span>
							<span>人均 ¥{{ averagePrice(business) }}</span>
						</div>
						<div class="business-delivery">
							<span>起送 ¥{{ money(business.startPrice) }}</span>
							<b>|</b>
							<span>配送费 ¥{{ money(business.deliveryPrice) }}</span>
						</div>
						<div class="business-tags">
							<span v-for="(tag, tagIndex) in businessTags(business)" :key="`${business.id}-${tagIndex}`" :class="['business-tag', `tag-${tagIndex % 3}`]">{{ tag }}</span>
						</div>
					</div>
					<div class="business-distance">
						<strong>{{ distanceText(business, index) }}km</strong>
						<span>约{{ deliveryMinutes(business, index) }}分钟送达</span>
					</div>
				</div>
			</article>
		</div>
		<div v-else-if="businessArr.length > 0 && !loading && !loadError" class="list-state filtered-empty">没有符合条件的商家</div>
	</div>
</template>

<script>
import { ref, computed, onMounted, onBeforeUnmount, nextTick, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import request from "@/utils/request";
import { pushWithViewTransition } from "@/utils/navigationMotion";
export default {
	name: "BusinessList",
	setup() {
		const businessArr = ref([]);
		const loading = ref(true);
		const loadError = ref(false);
		const pageReady = ref(false);
		const businessListRef = ref(null);
		const observeBusinesses = ref(false);
		const revealedBusinessIds = ref(new Set());
		const sortMode = ref('default');
		const filterMode = ref('all');
		const showFilter = ref(false);
		let businessObserver = null;
		const route = useRoute();
		const router = useRouter();
		const orderTypeName = computed(() => ({
			1: '美食', 2: '早餐', 3: '跑腿代购', 4: '汉堡披萨', 5: '甜品饮品',
			6: '速食简餐', 7: '地方小吃', 8: '米粉面馆', 9: '包子粥铺', 10: '炸鸡炸串'
		}[Number(route.query.orderTypeId)] || '附近美食'));
		const sortedBusinesses = computed(() => [...businessArr.value].sort((a, b) => {
			if (sortMode.value === 'sales') return Number(b.salesCount || 0) - Number(a.salesCount || 0);
			if (sortMode.value === 'distance') return Number(distanceText(a, 0)) - Number(distanceText(b, 0));
			return Number(b.recommendationScore || b.score || 0) - Number(a.recommendationScore || a.score || 0);
		}));
		const displayedBusinesses = computed(() => sortedBusinesses.value.filter((business) => {
			if (filterMode.value === 'freeDelivery') return Number(business.deliveryPrice || 0) === 0;
			if (filterMode.value === 'dineIn') return business.dineInAvailable === true;
			return true;
		}));
		const setSortMode = (mode) => { sortMode.value = mode; };
		const formatScore = (score) => score === null || score === undefined || score === '' ? '暂无' : Number(score).toFixed(1);
		const averagePrice = (business) => money(business.averagePrice || business.startPrice || 20);
		const distanceText = (business, index) => Number(business.distanceKm || business.distance || (0.6 + ((Number(business.id || index) % 4) * 0.2))).toFixed(1);
		const deliveryMinutes = (business, index) => Number(business.deliveryMinutes || (15 + ((Number(business.id || index) % 4) * 3)));
		const businessTags = (business) => {
			if (Array.isArray(business.recommendationTags) && business.recommendationTags.length) return business.recommendationTags.slice(0, 3);
			const tags = [];
			if (business.promotionThreshold && business.promotionDiscount) tags.push(`满${money(business.promotionThreshold)}减${money(business.promotionDiscount)}`);
			if (business.dineInAvailable) tags.push('支持堂食');
			tags.push(business.businessExplain || '校园优选');
			return tags.slice(0, 3);
		};
		const observeBusinessItems = () => {
			const elements = businessListRef.value?.querySelectorAll('[data-business-id]');
			if (!elements?.length) return;
			if (typeof window === 'undefined' || !('IntersectionObserver' in window)) {
				revealedBusinessIds.value = new Set([...elements].map(element => String(element.dataset.businessId)));
				observeBusinesses.value = false;
				return;
			}
			observeBusinesses.value = true;
			businessObserver ||= new IntersectionObserver((entries) => {
				entries.forEach((entry) => {
					if (!entry.isIntersecting) return;
					const next = new Set(revealedBusinessIds.value);
					next.add(String(entry.target.dataset.businessId));
					revealedBusinessIds.value = next;
					businessObserver?.unobserve(entry.target);
				});
			}, { root: document.querySelector('.content'), rootMargin: '0px 0px 24px', threshold: 0.08 });
			elements.forEach(element => {
				if (!revealedBusinessIds.value.has(String(element.dataset.businessId))) businessObserver.observe(element);
			});
		};
			onMounted(async () => {
			const orderTypeId = route.query.orderTypeId || 1; // 默认为类型1

			try {
				// 获取商家列表
				const response = await request.get(
					"/api/businesses/type/presentations",
					{
						params: {
							type: Number(orderTypeId) === 1 ? undefined : orderTypeId
						}
					}
				);

				if (response.success) {
					businessArr.value = Array.isArray(response.data) ? response.data : [];
				} else {
					loadError.value = true;
					console.error("获取商家列表失败:", response.message);
				}
			} catch (error) {
				loadError.value = true;
				console.error("请求商家列表出错:", error);
			} finally {
				loading.value = false;
				requestAnimationFrame(() => { pageReady.value = true; });
				nextTick(observeBusinessItems);
			}
		});
		onBeforeUnmount(() => businessObserver?.disconnect());
		watch(displayedBusinesses, () => nextTick(observeBusinessItems), { flush: 'post' });

		const money = (value) => Number(value || 0).toFixed(2);

		const toBusinessInfo = (businessId) => {
			pushWithViewTransition(router, {
				path: '/businessInfo',
				query: { businessId }
			});
		};
		const handleImageError = (event) => {
			const image = event?.target;
			if (!image || image.dataset.fallbackApplied === 'true') return;
			image.dataset.fallbackApplied = 'true';
			image.src = require('@/assets/business-default.png');
		};

		return {
			businessArr,
			displayedBusinesses,
			loading,
			loadError,
			pageReady,
			businessListRef,
			observeBusinesses,
			revealedBusinessIds,
			sortMode,
			filterMode,
			showFilter,
			setSortMode,
			orderTypeName,
			formatScore,
			averagePrice,
			distanceText,
			deliveryMinutes,
			businessTags,
			money,
			toBusinessInfo,
			handleImageError,
		};
	},
};
</script>

<style scoped>
/* 保持原有的样式不变 */
.wrapper {
	width: 100%;
	height: 100%;
	position: relative;
	padding-top: 0;
	background-color: #f5f5f5;
}
.header {
	width: 100%;
  height: 12vw;
  background-color: var(--skin-brand, #0097ff);
  color: #fff;
  font-size: 4.8vw;
  position: fixed;
  left: 0;
  top: 0;
  z-index: 1000;
  display: flex;
  justify-content: center;
  align-items: center;
}

.wrapper title {
	margin: 0;
	font-size: 24px;
	font-weight: 600;
	color: white;
}

.wrapper .business-list {
	padding: 2vw;
	margin-top: 12vw;
	margin-bottom: 15vw;
}
.list-state { min-height: 240px; padding: 120px 16px 40px; box-sizing: border-box; text-align: center; color: var(--skin-muted, #8aa0b2); font-size: 14px; }
.list-state i { display: block; margin-bottom: 10px; color: var(--skin-brand-soft, #8fc2e4); font-size: 28px; }
.list-state.error { color: #c87878; }

.wrapper .business-item {
	background: #fff;
	border-radius: 3vw;
	margin-bottom: 3vw;
	padding: 3vw;
	box-shadow: 0 0.5vw 2vw rgba(0, 0, 0, 0.05);
	transition: all 0.3s ease;
}

.wrapper .business-item:active {
	transform: scale(0.98);
}

.wrapper .business-info {
	display: flex;
	gap: 3vw;
	margin-bottom: 2vw;
	cursor: pointer;
}

.wrapper .business-info img {
	width: 20vw;
	height: 20vw;
	border-radius: 2vw;
	object-fit: cover;
	background-color: #eee;
	/* 图片加载前的背景色 */
}

.wrapper .business-details {
	flex: 1;
	display: flex;
	flex-direction: column;
	gap: 2vw;
}

.wrapper .business-header {
	display: flex;
	flex-direction: column;
	gap: 1vw;
}

.wrapper .business-details h3 {
	font-size: 4.2vw;
	margin: 0;
	color: #333;
	font-weight: 600;
}

.wrapper .description {
	font-size: 3.2vw;
	color: #666;
	line-height: 1.4;
	margin: 0;
}

.wrapper .business-info-bottom {
	margin-top: auto;
}

.wrapper .price-info {
	display: flex;
	flex-wrap: wrap;
	gap: 2vw 3vw;
	font-size: 3vw;
	color: #666;
}

.wrapper .business-tags {
	display: flex;
	flex-wrap: wrap;
	gap: 2vw;
	margin-top: 2vw;
}

.wrapper .tag {
	font-size: 2.8vw;
	padding: 0.5vw 2vw;
	border-radius: 3vw;
	background: var(--skin-surface, #f0f7ff);
	color: var(--skin-brand, #0097ff);
}
.closed-shop-tag{margin-left:5px;padding:2px 6px;border-radius:7px;background:var(--skin-surface, #edf1f4);color:var(--skin-muted, #80909c);font-size:10px;font-weight:500;vertical-align:2px}

/* Reference-led category list: the data remains live, while the shell follows
 * the supplied blue campus mockup and keeps every category on one visual path. */
.business-list-page {
	--list-blue: var(--skin-brand, #078fe8);
	--list-ink: var(--skin-brand-strong, #173b5e);
	--list-muted: var(--skin-muted, #7c8d9d);
	width: 100%;
	max-width: 600px;
	min-height: 100%;
	margin: 0 auto;
	background: var(--skin-surface, #f2f8fc);
	color: var(--list-ink);
	font-family: -apple-system, BlinkMacSystemFont, "PingFang SC", "Microsoft YaHei", sans-serif;
}

.business-list-page .list-hero {
	position: relative;
	height: 154px;
	overflow: hidden;
	display: flex;
	align-items: center;
	justify-content: center;
	background: var(--skin-brand, #119eef);
	color: #fff;
}

.business-list-page .list-hero::before {
	content: "";
	position: absolute;
	inset: -90px -50px 0;
	background:
		radial-gradient(circle at 0 0, rgba(255,255,255,.17) 0 120px, transparent 121px),
		radial-gradient(circle at 24% 112%, rgba(255,255,255,.12) 0 76px, transparent 77px),
		radial-gradient(circle at 100% 12%, rgba(255,255,255,.10) 0 112px, transparent 113px);
	}

.business-list-page .list-hero-copy {
	position: relative;
	z-index: 1;
	display: flex;
	flex-direction: column;
	align-items: center;
	gap: 8px;
	padding-top: 12px;
}

.business-list-page .list-hero-copy strong {
	font-size: clamp(31px, 8vw, 43px);
	font-weight: 800;
	line-height: 1;
	letter-spacing: 2px;
	text-shadow: 0 3px 9px rgba(var(--skin-brand-strong-rgb, 0, 92, 170), 0.18);
}

.business-list-page .list-hero-copy span {
	color: rgba(255,255,255,.85);
	font-size: 12px;
	letter-spacing: 1px;
}

.business-list-page .list-toolbar {
	position: sticky;
	top: 0;
	z-index: 8;
	min-height: 92px;
	margin-top: -1px;
	padding: 15px 12px;
	box-sizing: border-box;
	display: grid;
	grid-template-columns: 1.18fr .95fr .95fr 1.05fr;
	align-items: center;
	gap: 4px;
	border-radius: 27px 27px 20px 20px;
	background: rgba(255,255,255,.97);
	box-shadow: 0 8px 24px rgba(var(--skin-brand-rgb, 45, 113, 155), 0.08);
	backdrop-filter: blur(12px);
}

.business-list-page .toolbar-tab {
	min-width: 0;
	height: 54px;
	padding: 0 6px;
	border: 0;
	border-radius: 27px;
	background: transparent;
	color: var(--skin-ink, #34485b);
	font: inherit;
	font-size: 14px;
	font-weight: 600;
	white-space: nowrap;
	cursor: pointer;
	transition: color 180ms ease, background 180ms ease, transform 180ms ease;
}

.business-list-page .toolbar-tab.active {
	color: var(--list-blue);
	background: var(--skin-surface, #e9f6ff);
}

.business-list-page .toolbar-tab:active { transform: scale(.96); }
.business-list-page .toolbar-tab i { margin: 0 2px; font-size: 13px; }

.business-list-page .filter-drawer {
	display: flex;
	gap: 8px;
	padding: 10px 14px 12px;
	background: #fff;
	border-bottom: 1px solid var(--skin-border, #e4eef4);
}

.business-list-page .filter-drawer button {
	padding: 7px 12px;
	border: 1px solid var(--skin-border, #dceaf2);
	border-radius: 999px;
	background: var(--skin-surface, #f8fbfd);
	color: var(--skin-muted, #6f8494);
	font-size: 12px;
	cursor: pointer;
}

.business-list-page .filter-drawer button.active {
	border-color: var(--skin-brand-soft, #a9daf5);
	background: var(--skin-surface, #eaf7ff);
	color: var(--list-blue);
}

.business-list-page .business-list {
	width: 100%;
	margin: 0;
	padding: 16px 12px calc(92px + env(safe-area-inset-bottom));
	box-sizing: border-box;
}

.business-list-page .business-item {
	min-height: 150px;
	margin: 0 0 14px;
	padding: 16px;
	box-sizing: border-box;
	border: 1px solid rgba(var(--skin-border-rgb, 219, 233, 241), 0.85);
	border-radius: 22px;
	background: rgba(255,255,255,.98);
	box-shadow: 0 7px 18px rgba(var(--skin-brand-strong-rgb, 48, 105, 140), 0.08);
	transition: transform 180ms ease, box-shadow 180ms ease;
}

.business-list-page .business-item:active { transform: scale(.985); }

.business-list-page .business-info {
	width: 100%;
	min-width: 0;
	margin: 0;
	display: grid;
	grid-template-columns: clamp(88px, 24vw, 108px) minmax(0, 1fr) auto;
	align-items: start;
	gap: 14px;
}

.business-list-page .business-info > img {
	width: clamp(88px, 24vw, 108px);
	height: clamp(88px, 24vw, 108px);
	flex: none;
	border-radius: 16px;
	object-fit: cover;
	background: var(--skin-surface, #edf5f9);
	box-shadow: inset 0 0 0 1px rgba(var(--skin-subtle-rgb, 156, 195, 216), 0.18);
}

.business-list-page .business-details {
	min-width: 0;
	display: flex;
	flex-direction: column;
	gap: 0;
}

.business-list-page .business-title-row {
	position: relative;
	min-width: 0;
	padding-right: 19px;
}

.business-list-page .business-title-row h2 {
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	color: var(--skin-brand-strong, #132d49);
	font-size: clamp(16px, 4.5vw, 22px);
	font-weight: 800;
	line-height: 1.25;
}

.business-list-page .business-arrow {
	position: absolute;
	right: 0;
	top: 2px;
	color: var(--skin-muted, #7d8c99);
	font-size: 22px;
}

.business-list-page .business-metrics {
	display: flex;
	align-items: baseline;
	gap: 10px;
	min-width: 0;
	margin-top: 9px;
	white-space: nowrap;
}

.business-list-page .business-metrics strong {
	color: #f39400;
	font-size: clamp(17px, 4.4vw, 21px);
	font-weight: 800;
}

.business-list-page .business-metrics strong i { font-size: 16px; }
.business-list-page .business-metrics span { color: var(--skin-muted, #7c8995); font-size: 12px; }

.business-list-page .business-delivery {
	display: flex;
	align-items: center;
	gap: 8px;
	margin-top: 9px;
	color: var(--skin-muted, #80909e);
	font-size: 12px;
	white-space: nowrap;
}

.business-list-page .business-delivery b { color: var(--skin-subtle, #ccd5db); font-weight: 400; }

.business-list-page .business-tags {
	display: flex;
	gap: 6px;
	min-width: 0;
	overflow: hidden;
	margin-top: 10px;
}

.business-list-page .business-tag {
	max-width: 31%;
	overflow: hidden;
	text-overflow: ellipsis;
	white-space: nowrap;
	padding: 6px 8px;
	border: 0;
	border-radius: 8px;
	font-size: 11px;
	line-height: 1;
}

.business-list-page .business-tag.tag-0 { color: #f04f58; background: #fff0f1; }
.business-list-page .business-tag.tag-1 { color: #ef872b; background: #fff4e9; }
.business-list-page .business-tag.tag-2 { color: var(--skin-brand, #128fe3); background: var(--skin-surface, #eaf6ff); }

.business-list-page .business-distance {
	min-width: 60px;
	padding-top: 45px;
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	gap: 7px;
	color: var(--skin-muted, #7d8b98);
	white-space: nowrap;
	text-align: right;
}

.business-list-page .business-distance strong { color: var(--skin-muted, #6f7e8b); font-size: 13px; font-weight: 500; }
.business-list-page .business-distance span { font-size: 11px; }
.business-list-page .filtered-empty { padding: 56px 12px; }

@media (max-width: 380px) {
	.business-list-page .list-toolbar { padding-left: 8px; padding-right: 8px; }
	.business-list-page .toolbar-tab { font-size: 12px; }
	.business-list-page .business-info { gap: 9px; }
	.business-list-page .business-distance { min-width: 52px; }
	.business-list-page .business-metrics { gap: 6px; }
	.business-list-page .business-metrics span { font-size: 10px; }
	.business-list-page .business-delivery { gap: 5px; font-size: 10px; }
}

@media (min-width: 700px) {
	.business-list-page { max-width: 600px; }
	.business-list-page .business-list { padding-left: 18px; padding-right: 18px; }
}

@media (prefers-reduced-motion: reduce) {
	.business-list-page .toolbar-tab,
	.business-list-page .business-item { transition: none; }
}
</style>

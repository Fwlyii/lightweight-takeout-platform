<template>
	<div class="wrapper cart-page">
		<!-- header部分：插画只作背景，返回/标题/管理都是活元素 -->
		<header class="cart-header">
			<p>购物车</p>
			<button type="button" class="manage-toggle" @click="manageMode = !manageMode">{{ manageMode ? '完成' : '管理' }}</button>
		</header>

		<!-- 购物车为空提示 -->
		<div class="empty-cart" v-if="cartItems.length === 0">
			<img src="../assets/empty-cart.png" alt="购物车为空">
			<p>您的购物车空空如也</p>
			<button @click="goBack">返回商家</button>
		</div>

		<!-- 购物车列表部分 -->
			<div v-else class="cart-body">
			<!-- 商家分组卡片 -->
			<section class="merchant-card">
				<div class="merchant-head">
					<button type="button" class="merchant-name" @click="goToBusiness">
						<i class="fa fa-shopping-bag"></i>
						<strong>{{ businessName }}</strong>
						<i class="fa fa-angle-right"></i>
					</button>
					<button type="button" class="select-all" @click="toggleSelectAll">{{ allSelected ? '取消全选' : '全选' }}</button>
				</div>
				<p class="selection-hint">已选 {{ selectedItems.length }} 份商品，可分批结算</p>

				<ul class="cart">
					<li v-for="item in cartItems" :key="item.id">
						<label class="cart-select" :aria-label="`选择${item.foodName}`"><input type="checkbox" v-model="selectedFoodIds" :value="item.foodId"><span></span></label>
						<div class="cart-img">
							<!-- 这里假设您有食物图片的URL，如果没有可以移除或使用默认图片 -->
							<img :src="item.foodImg || require('../assets/food-default.png')" alt="食物图片" @error="handleImageError">
							<div class="cart-img-quantity" v-show="item.quantity > 0">{{ item.quantity }}</div>
						</div>
						<div class="cart-info">
							<h3>{{ item.foodName }}</h3>
							<small v-if="item.purchaseLimit" class="food-meta">每单最多 {{ item.purchaseLimit }} 份</small>
							<p>&#165;{{ Number(item.foodPrice || 0).toFixed(2) }} / 份</p>
							<small v-if="Number(item.stock || 0) <= 0" class="stock-hint">当前已售罄</small>
						</div>
						<div class="cart-item-side">
							<button v-if="manageMode" type="button" class="delete-item" :disabled="updatingCartIds.has(item.id)" :aria-label="`删除${item.foodName}`" @click="removeItem(item)"><i class="fa fa-trash-o"></i></button>
							<strong>￥{{ (Number(item.foodPrice || 0) * Number(item.quantity || 0)).toFixed(2) }}</strong>
							<div class="quantity-stepper" :aria-label="`${item.foodName}数量`">
								<button type="button" :disabled="updatingCartIds.has(item.id)" @click="changeQuantity(item, -1)">−</button>
								<span>{{ item.quantity }}</span>
								<button type="button" :disabled="updatingCartIds.has(item.id) || item.quantity >= maxQuantity(item)" @click="changeQuantity(item, 1)">+</button>
							</div>
						</div>
					</li>
				</ul>

				<!-- 口味备注 / 餐具：同一商家一条备注，下单时写入订单 -->
				<div class="remark-row">
					<div class="remark-label"><i class="fa fa-commenting-o"></i><span>口味备注 / 餐具</span></div>
					<input
						v-model="remarkDraft"
						type="text"
						maxlength="255"
						placeholder="如：少辣、不要香菜、需要一次性餐具等"
						aria-label="口味备注与餐具需求"
						@focus="remarkEditing = true"
						@blur="saveRemark"
						@keyup.enter="$event.target.blur()" />
					<button v-if="remarkEditing" type="button" class="remark-save" :disabled="savingRemark" @click="saveRemark">{{ savingRemark ? '保存中' : '保存' }}</button>
					<i v-else class="fa fa-angle-right remark-arrow"></i>
				</div>
			</section>

			<!-- 你可能还想加点 -->
			<section v-if="recommendFoods.length" class="recommend-block">
				<div class="recommend-head">
					<h2>你可能还想加点</h2>
					<button type="button" @click="rotateRecommend">换一批</button>
				</div>
				<div class="recommend-scroll">
					<article v-for="food in recommendFoods" :key="food.id" class="recommend-card">
						<img :src="food.foodImg || require('../assets/food-default.png')" :alt="food.foodName" @error="handleImageError">
						<strong>{{ food.foodName }}</strong>
						<span>{{ food.foodExplain || '店内热销' }}</span>
						<div class="recommend-foot">
							<b>¥{{ Number(food.foodPrice || 0).toFixed(2) }}</b>
							<button type="button" :disabled="addingFoodId === food.id" :aria-label="`加购${food.foodName}`" @click="addRecommended(food)">+</button>
						</div>
					</article>
				</div>
			</section>

			<!-- 促销插画（无文字，装饰用） -->
			<section class="cart-banner" aria-hidden="true">
				<img src="../assets/cart-banner-art.png" alt="">
			</section>

			<!-- 底部结算栏 -->
			<div class="checkout-bar">
				<button type="button" class="select-all-circle" :class="{ active: allSelected }" aria-label="全选" @click="toggleSelectAll"><i class="fa fa-check"></i></button>
				<span class="select-all-label" @click="toggleSelectAll">全选</span>
				<div class="total-price">
					<p>总计：<span>&#165; {{ totalPrice }}</span></p>
					<small><i class="fa fa-info-circle"></i>另需配送费 &#165; {{ deliveryFeeText }}</small>
				</div>
				<button class="checkout-btn" :disabled="selectedItems.length === 0" @click="checkout">去下单</button>
			</div>
		</div>

		<!-- 底部菜单部分 -->
		<!-- <Footer /> -->
	</div>
</template>

<script>
import { ref, onMounted, computed } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import { toast } from '../utils/toast';
import request from '../utils/request';
import { cartQuantityLimitMessage, maxCartQuantity } from '../utils/cartQuantityRules';
import { addCartItem, listCartItems, removeCartItem, setCartItemQuantity } from '../services/cartService';

export default {
	name: 'Cart',
	setup() {
		const cartItems = ref([]);
		const userInfo = ref(null);
		const route = useRoute();
		const router = useRouter();
		const businessId = ref(null);
		const selectedFoodIds = ref([]);
		const updatingCartIds = ref(new Set());
		// const businessId = ref(null);
		const businessName = ref('');
		const businessSummary = ref(null);
		const allFoods = ref([]);
		const recommendOffset = ref(0);
		const addingFoodId = ref(null);
		// 管理态下才展示删除按钮，与参考图的“管理/完成”切换一致。
		const manageMode = ref(false);
		const remarkDraft = ref('');
		const remarkEditing = ref(false);
		const savingRemark = ref(false);

		/** 另需配送费来自商家展示口径，自取时不计配送费。 */
		const deliveryFeeText = computed(() => {
			const mode = route.query.serviceMode || localStorage.getItem(`businessServiceMode:${businessId.value}`) || 'delivery';
			if (mode === 'pickup') return '0.00';
			return Number(businessSummary.value?.deliveryPrice ?? 0).toFixed(2);
		});

		/** 推荐菜品只用同店真实在售商品，已在本单里的不重复推荐。 */
		const recommendFoods = computed(() => {
			const inCart = new Set(cartItems.value.map(item => item.foodId));
			const pool = allFoods.value.filter(food => !inCart.has(food.id));
			if (!pool.length) return [];
			const start = recommendOffset.value % pool.length;
			return [...pool.slice(start), ...pool.slice(0, start)].slice(0, 3);
		});

		onMounted(() => {
			businessId.value = parseInt(route.query.businessId);
			const cachedUser = localStorage.getItem('userInfo') || sessionStorage.getItem('userInfo');
			try { userInfo.value = cachedUser ? JSON.parse(cachedUser) : null; } catch (_) { userInfo.value = null; }

			if (userInfo.value) {
				listCart();
				loadBusinessSummary();
				loadRecommendFoods();
			} else {
				toast.error("用户未登录，请先登录！");
				router.replace({ path: '/login', query: { role: 'user', redirect: route.fullPath } });
			}
		});

		const listCart = async () => {
			try {
				const items = await listCartItems(businessId.value);
				cartItems.value = Array.isArray(items) ? items : [];
				businessName.value = cartItems.value[0]?.businessName || '当前商家';
				selectedFoodIds.value = [...new Set(cartItems.value.map(item => item.foodId).filter(Boolean))];
				if (!remarkEditing.value) remarkDraft.value = cartItems.value[0]?.remarks || '';
			} catch (error) {
				console.error('获取购物车失败:', error);
			}
		};

		/** 备注只写一次、作用于同商家全部购物车行，下单时写入订单。 */
		const saveRemark = async () => {
			if (savingRemark.value) return;
			remarkEditing.value = false;
			const next = (remarkDraft.value || '').trim();
			const current = cartItems.value[0]?.remarks || '';
			if (next === current) return;
			savingRemark.value = true;
			try {
				const response = await request.put('/api/carts/remarks', null, { params: { businessId: businessId.value, remarks: next } });
				if (!response?.success) throw new Error(response?.message || '备注保存失败');
				cartItems.value = cartItems.value.map(item => ({ ...item, remarks: next || null }));
			} catch (error) {
				toast.error(error?.response?.data?.message || error?.message || '备注保存失败');
				remarkDraft.value = current;
			} finally {
				savingRemark.value = false;
			}
		};

		// 店铺展示指标（配送费）与同店在售菜品，用于底部配送费与推荐位。
		const loadBusinessSummary = async () => {
			if (!businessId.value) return;
			try {
				const response = await request.get(`/api/businesses/${businessId.value}/summary`);
				businessSummary.value = response?.success ? (response.data || null) : null;
			} catch (error) {
				console.error('获取商家展示指标失败:', error);
				businessSummary.value = null;
			}
		};

		const loadRecommendFoods = async () => {
			if (!businessId.value) return;
			try {
				const response = await request.get('/api/foods/list', { params: { businessId: businessId.value } });
				const foods = response?.success && Array.isArray(response.data) ? response.data : [];
				allFoods.value = foods.filter(food => food.shelveStatus === 1);
			} catch (error) {
				console.error('获取推荐菜品失败:', error);
				allFoods.value = [];
			}
		};

		const rotateRecommend = () => {
			recommendOffset.value += 3;
		};

		const addRecommended = async (food) => {
			if (addingFoodId.value) return;
			addingFoodId.value = food.id;
			try {
				await addCartItem(food.id, 1);
				await listCart();
				toast.success(`已加入购物车：${food.foodName}`);
			} catch (error) {
				toast.error(error?.response?.data?.message || error?.message || '加入购物车失败');
			} finally {
				addingFoodId.value = null;
			}
		};

		const goToBusiness = () => {
			if (businessId.value) router.push({ path: '/businessInfo', query: { businessId: businessId.value } });
		};

		// 计算总价
		const selectedItems = computed(() => cartItems.value.filter(item => selectedFoodIds.value.includes(item.foodId)));
		const allSelected = computed(() => cartItems.value.length > 0 && selectedItems.value.length === cartItems.value.length);
		const totalPrice = computed(() => {
			return selectedItems.value.reduce((total, item) => {
				return total + (item.foodPrice * item.quantity);
			}, 0).toFixed(2);
		});

		const toggleSelectAll = () => {
			selectedFoodIds.value = allSelected.value ? [] : [...new Set(cartItems.value.map(item => item.foodId).filter(Boolean))];
		};

		const markUpdating = (id, updating) => {
			const next = new Set(updatingCartIds.value);
			if (updating) next.add(id); else next.delete(id);
			updatingCartIds.value = next;
		};
		const maxQuantity = maxCartQuantity;
		const updateItemQuantity = async (item, quantity) => {
			if (updatingCartIds.value.has(item.id)) return;
			markUpdating(item.id, true);
			try {
				await setCartItemQuantity(item.id, quantity);
				if (quantity === 0) {
					cartItems.value = cartItems.value.filter(candidate => candidate.id !== item.id);
					selectedFoodIds.value = selectedFoodIds.value.filter(foodId => foodId !== item.foodId);
				} else {
					item.quantity = quantity;
				}
			} catch (error) {
				toast.error(error?.response?.data?.message || error?.message || '购物车更新失败');
			} finally {
				markUpdating(item.id, false);
			}
		};
		const removeItem = async (item) => {
			if (updatingCartIds.value.has(item.id)) return;
			markUpdating(item.id, true);
			try {
				await removeCartItem(item.id);
				cartItems.value = cartItems.value.filter(candidate => candidate.id !== item.id);
				selectedFoodIds.value = selectedFoodIds.value.filter(foodId => foodId !== item.foodId);
			} catch (error) {
				toast.error(error?.response?.data?.message || error?.message || '删除失败，请重试');
			} finally {
				markUpdating(item.id, false);
			}
		};
		const changeQuantity = (item, delta) => {
			const next = Number(item.quantity || 0) + delta;
			if (next <= 0) return removeItem(item);
			if (next > maxQuantity(item)) {
				toast.warning(cartQuantityLimitMessage(item));
				return;
			}
			return updateItemQuantity(item, next);
		};

		// 结算
		const checkout = () => {
			if (selectedItems.value.length === 0) {
				toast.warning('请先选择要结算的商品');
				return;
			}
			// 跳转到结算页面
				router.push({
					path: '/userAddress',
					query: {
						businessId: businessId.value,
						foodIds: selectedFoodIds.value.join(','),
						serviceMode: route.query.serviceMode || localStorage.getItem(`businessServiceMode:${businessId.value}`) || 'delivery',
					}
			});
		};

		// 返回商家页面
		const goBack = () => {
			router.go(-1);
		};

		const handleImageError = (event) => {
			const image = event?.target;
			if (!image || image.dataset.fallbackApplied === 'true') return;
			image.dataset.fallbackApplied = 'true';
			image.src = require('../assets/food-default.png');
		};

		return {
			cartItems,
			businessName,
			manageMode,
			remarkDraft,
			remarkEditing,
			savingRemark,
			saveRemark,
			deliveryFeeText,
			recommendFoods,
			addingFoodId,
			rotateRecommend,
			addRecommended,
			goToBusiness,
			selectedFoodIds,
			selectedItems,
			allSelected,
			toggleSelectAll,
			updatingCartIds,
			maxQuantity,
			changeQuantity,
			removeItem,
			totalPrice,
			checkout,
			goBack,
			handleImageError,
			businessId
		};
	}
}
</script>

<style scoped>
.stock-hint{display:block;color:#8aa0b2;font-size:12px;margin-top:4px}
.food-meta{display:block;color:#2384bd;font-size:12px;margin-top:4px}
.business-info-title{display:flex;align-items:center;justify-content:space-between;gap:12px}
.business-info-title h3{margin:0}
.selection-hint{margin:6px 0 0;color:#8aa0b2;font-size:12px}
.select-all{border:1px solid #a9d5ef;border-radius:14px;background:#f5fbff;color:#168bd1;padding:5px 10px;font-size:12px;cursor:pointer}
/****************** 总容器 ******************/
.wrapper {
	width: 100%;
	height: 100%;
	position: relative;
	top: -4vw;
}

/****************** header部分 ******************/
.wrapper header {
	width: 100%;
	height: 12vw;
	background-color: #0097FF;
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

/****************** 商家信息 ******************/
.business-info {
	padding: 3vw;
	background-color: #f8f8f8;
	border-bottom: 1px solid #eee;
	margin-top: 12vw;
}

.business-info h3 {
	font-size: 4vw;
	color: #333;
}

/****************** 购物车列表部分 ******************/
.wrapper .cart {
	width: 100%;
	margin-bottom: 60px;
}

.wrapper .cart li {
	width: 100%;
	box-sizing: border-box;
	padding: 2.5vw;
	border-bottom: solid 1px #DDD;
	user-select: none;
	cursor: pointer;
	display: flex;
	align-items: center;
}
.cart-select{width:24px;flex:0 0 24px;display:flex;align-items:center;justify-content:center;margin-right:6px;cursor:pointer}
.cart-select input{position:absolute;opacity:0;pointer-events:none}
.cart-select span{width:18px;height:18px;border:1px solid #b5c9d8;border-radius:50%;background:#fff;position:relative}
.cart-select input:checked + span{border-color:#168bd1;background:#168bd1}
.cart-select input:checked + span::after{content:'✓';position:absolute;left:3px;top:-1px;color:#fff;font-size:14px;line-height:18px}

.wrapper .cart li .cart-img {
	/*这里设置为相当定位，成为cart-img-quantity元素的父元素*/
	position: relative;
}

.wrapper .cart li .cart-img img {
	width: 20vw;
	height: 20vw;
	object-fit: cover;
	border-radius: 2vw;
}

.cart-item-side {
	width: 25vw;
	max-width: 116px;
	align-self: stretch;
	display: flex;
	flex-direction: column;
	align-items: flex-end;
	justify-content: space-between;
	gap: 8px;
}

.cart-item-side strong { font-size: 3.5vw; color: #202d3d; white-space: nowrap; }
.delete-item { border: 0; background: transparent; color: #9aabb8; font-size: 18px; padding: 2px 4px; cursor: pointer; }
.quantity-stepper { height: 28px; display: flex; align-items: center; border: 1px solid #dce8f0; border-radius: 15px; overflow: hidden; background: #fff; }
.quantity-stepper button { width: 28px; height: 28px; border: 0; background: #f2f8fc; color: #168bd1; font-size: 18px; line-height: 1; cursor: pointer; }
.quantity-stepper button:disabled,
.delete-item:disabled { opacity: .4; cursor: not-allowed; }
.quantity-stepper span { min-width: 28px; text-align: center; color: #24384a; font-size: 13px; font-weight: 600; }

.wrapper .cart li .cart-img .cart-img-quantity {
	width: 5vw;
	height: 5vw;
	background-color: red;
	color: #fff;
	font-size: 3.6vw;
	border-radius: 2.5vw;
	display: flex;
	justify-content: center;
	align-items: center;
	/*设置成绝对定位，不占文档流空间*/
	position: absolute;
	right: -1.5vw;
	top: -1.5vw;
}

.wrapper .cart li .cart-info {
	margin-left: 3vw;
	flex: 1;
}

.wrapper .cart li .cart-info h3 {
	font-size: 3.8vw;
	color: #555;
}

.wrapper .cart li .cart-info p {
	font-size: 3vw;
	color: #888;
	margin-top: 2vw;
}

.wrapper .cart li .cart-actions {
	display: flex;
	align-items: center;
	gap: 2vw;
}

.wrapper .cart li .cart-actions button {
	width: 6vw;
	height: 6vw;
	border: none;
	border-radius: 50%;
	background-color: #0097FF;
	color: white;
	font-size: 3.5vw;
	display: flex;
	justify-content: center;
	align-items: center;
	cursor: pointer;
}

.wrapper .cart li .cart-actions .delete-btn {
	width: auto;
	padding: 0 2vw;
	border-radius: 1vw;
	font-size: 2.8vw;
	background-color: #ff4d4f;
	margin-left: 2vw;
}

.wrapper .cart li .cart-actions span {
	font-size: 3.5vw;
	color: #555;
}

/****************** 结算栏 ******************/
.checkout-bar {
	position: fixed;
	bottom: 0;
	left: 0;
	width: 100%;
	height: 14vw;
	background-color: #fff;
	border-top: 1px solid #ddd;
	display: flex;
	justify-content: space-between;
	align-items: center;
	padding: 0 4vw;
	box-sizing: border-box;
	z-index: 1100;
}

.checkout-bar .total-price {
	font-size: 4.5vw;
	font-weight: bold;
	color: #ff4d4f;
}

.checkout-bar .checkout-btn {
	background-color: #0097FF;
	color: white;
	border: none;
	padding: 2.5vw 5vw;
	border-radius: 2vw;
	font-size: 4vw;
	cursor: pointer;
}
.checkout-bar .checkout-btn:disabled{background:#b8cbd7;cursor:not-allowed;box-shadow:none}

/****************** 空购物车 ******************/
.empty-cart {
	display: flex;
	flex-direction: column;
	align-items: center;
	justify-content: center;
	padding-top: 30vw;
}

.empty-cart img {
	width: 40vw;
	height: 40vw;
	margin-bottom: 5vw;
}

.empty-cart p {
	font-size: 4vw;
	color: #999;
	margin-bottom: 5vw;
}

.empty-cart button {
	background-color: #0097FF;
	color: white;
	border: none;
	padding: 3vw 6vw;
	border-radius: 2vw;
	font-size: 4vw;
	cursor: pointer;
}
.back-btn-container {
  position: fixed; /* 固定定位，不随滚动移动 */
  left: 0vw; /* 距离左侧的距离，可根据需求调整 */
  top: 1vw; /* 距离顶部的距离，与 header 高度（12vw）适配，确保垂直居中 */
  z-index: 1001; /* 比 header 的 z-index:1000 高，避免被遮挡 */
}

/* 桌面端也保持移动端外卖页面的窄栏比例，避免 vw 字号随窗口放大造成拥挤。 */
.wrapper{width:100%;max-width:600px;min-height:100vh;height:auto;margin:0 auto;top:0;background:#f7fafc;color:#29455f;overflow-x:hidden;box-sizing:border-box}
.wrapper header{width:100%;height:56px;position:fixed;left:50%;top:0;transform:translateX(-50%);max-width:600px;font-size:20px;z-index:1000}
.business-info{margin-top:56px;padding:14px 16px;background:#fff;border-bottom:1px solid #e5edf2}
.business-info h3{font-size:18px;color:#31556d}
.business-info-title h3{min-width:0;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.selection-hint{font-size:12px}
.wrapper .cart{margin:0 0 82px;padding:0;list-style:none;background:#f7fafc}
.wrapper .cart li{padding:14px 12px;gap:8px;align-items:center;background:#fff;border-bottom:1px solid #e7eef3;min-height:104px}
.wrapper .cart li .cart-img img{width:76px;height:76px;object-fit:cover;border-radius:6px}
.wrapper .cart li .cart-img .cart-img-quantity{width:22px;height:22px;right:-7px;top:-7px;border-radius:50%;font-size:12px}
.wrapper .cart li .cart-info{margin-left:4px;min-width:0}
.wrapper .cart li .cart-info h3{font-size:15px;color:#31556d;overflow:hidden;text-overflow:ellipsis;white-space:nowrap}
.wrapper .cart li .cart-info p{font-size:13px;color:#708797;margin-top:7px}
.wrapper .cart li .cart-info .quantity-mark{font-size:13px;color:#b33e48}
.wrapper .cart li .cart-info .stock-hint{white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.cart-item-side{width:108px;flex:0 0 108px}
.cart-item-side strong{font-size:14px}
.checkout-bar{left:50%;transform:translateX(-50%);max-width:600px;width:100%;height:64px;padding:0 14px;background:#fff;border-top:1px solid #dfeaf1;box-shadow:0 -2px 8px rgba(40,84,110,.08)}
.checkout-bar .total-price{font-size:15px;color:#31556d}
.checkout-bar .total-price p{font-size:15px!important}
.checkout-bar .checkout-btn{padding:10px 24px;border-radius:6px;font-size:15px;background:#168bd1}
.empty-cart{padding-top:120px}
.empty-cart img{width:160px;height:160px;margin-bottom:18px}
.empty-cart p{font-size:15px;margin-bottom:18px}
.empty-cart button{padding:10px 22px;border-radius:6px;font-size:14px}
.back-btn-container{left:max(12px, calc(50% - 288px));top:8px}
:deep(.back-button){left:max(16px, calc(50% - 284px));top:10px}
@media (max-width:600px){.wrapper header,.checkout-bar{left:0;transform:none}.back-btn-container{left:8px}}
@media (max-width:600px){:deep(.back-button){left:16px}}

/* ── 购物车（对照参考图）：蓝色插画头部 → 商家分组卡 → 推荐位 → 海报 → 结算栏 ── */
.cart-page { background: #f4f9fd; }

.cart-page .cart-header {
	position: relative;
	height: 96px;
	padding: 0 16px;
	box-sizing: border-box;
	display: flex;
	align-items: center;
	justify-content: center;
	background-color: #2f9df1;
	background-image: url('../assets/cart-header-art.png');
	background-repeat: no-repeat;
	background-position: center top;
	background-size: 100% auto;
}

.cart-page .cart-header p {
	margin: 0;
	color: #fff;
	font-size: 17px;
	font-weight: 700;
	letter-spacing: .02em;
	text-shadow: 0 1px 2px rgba(12, 78, 133, .28);
}

.cart-page .cart-header .manage-toggle {
	position: absolute;
	right: 16px;
	top: 50%;
	transform: translateY(-50%);
	border: 0;
	background: transparent;
	color: #fff;
	font-size: 14px;
	font-weight: 600;
	cursor: pointer;
}

.cart-page .cart-body { padding: 12px 14px 92px; }

/* 商家分组卡 */
.merchant-card {
	padding: 12px;
	border-radius: 16px;
	background: #fff;
	box-shadow: 0 8px 22px rgba(39, 86, 114, .07);
}

.cart-page .merchant-head {
	display: flex;
	align-items: center;
	justify-content: space-between;
	gap: 10px;
}

.cart-page .merchant-name {
	min-width: 0;
	display: flex;
	align-items: center;
	gap: 6px;
	border: 0;
	background: transparent;
	color: #103c6c;
	font-size: 16px;
	font-weight: 800;
	cursor: pointer;
}

.cart-page .merchant-name strong { overflow: hidden; text-overflow: ellipsis; white-space: nowrap; }
.cart-page .merchant-name .fa-shopping-bag { color: #168bd1; font-size: 15px; }
.cart-page .merchant-name .fa-angle-right { color: #a9bccb; font-size: 14px; }
.cart-page .selection-hint { margin: 5px 0 8px; color: #8aa0b2; font-size: 12px; }

.cart-page .cart { margin: 0; padding: 0; }

.cart-page .cart li {
	position: relative;
	display: flex;
	align-items: flex-start;
	gap: 10px;
	padding: 12px 0;
	border-bottom: 1px solid #f0f5f9;
}

.cart-page .cart li:last-child { border-bottom: 0; }
.cart-page .cart li .cart-info { min-width: 0; flex: 1; }
.cart-page .cart li .cart-info h3 { margin: 0; color: #103c6c; font-size: 15px; font-weight: 700; }
.cart-page .cart li .cart-info p { margin: 5px 0 0; color: #e2604b; font-size: 13px; font-weight: 700; }
.cart-page .cart li .cart-img img { width: 68px; height: 68px; border-radius: 10px; object-fit: cover; }
.cart-page .cart li .cart-img .cart-img-quantity {
	position: absolute;
	left: 54px;
	top: 0;
	min-width: 16px;
	height: 16px;
	padding: 0 4px;
	box-sizing: border-box;
	border-radius: 8px;
	background: #f4483b;
	color: #fff;
	font-size: 10px;
	line-height: 16px;
	text-align: center;
}
.cart-page .cart-item-side { display: flex; flex-direction: column; align-items: flex-end; gap: 8px; }
.cart-page .cart-item-side strong { color: #103c6c; font-size: 15px; font-weight: 700; }
.cart-page .quantity-stepper { border-color: #dbe9f4; }
.cart-page .quantity-stepper button { background: #eef6fd; }

/* 你可能还想加点 */
.recommend-block { margin-top: 14px; padding: 12px; border-radius: 16px; background: #fff; box-shadow: 0 8px 22px rgba(39, 86, 114, .06); }
.recommend-head { display: flex; align-items: center; justify-content: space-between; margin-bottom: 10px; }
.recommend-head h2 { margin: 0; color: #103c6c; font-size: 16px; font-weight: 800; }
.recommend-head button { border: 0; background: transparent; color: #8aa0b2; font-size: 12px; cursor: pointer; }
.recommend-scroll { display: flex; gap: 10px; overflow-x: auto; scrollbar-width: none; }
.recommend-scroll::-webkit-scrollbar { display: none; }
.recommend-card { flex: 0 0 118px; min-width: 0; padding: 8px; box-sizing: border-box; border: 1px solid #e3eef6; border-radius: 12px; background: #fff; }
.recommend-card img { width: 100%; height: 66px; border-radius: 8px; object-fit: cover; background: #eef5f9; }
.recommend-card strong { display: block; margin-top: 6px; overflow: hidden; color: #103c6c; font-size: 13px; font-weight: 700; text-overflow: ellipsis; white-space: nowrap; }
.recommend-card span { display: block; margin-top: 3px; overflow: hidden; color: #93a7b6; font-size: 10px; text-overflow: ellipsis; white-space: nowrap; }
.recommend-foot { display: flex; align-items: center; justify-content: space-between; margin-top: 6px; }
.recommend-foot b { color: #e2604b; font-size: 13px; }
.recommend-foot button { width: 24px; height: 24px; border: 0; border-radius: 50%; background: #168bd1; color: #fff; font-size: 16px; line-height: 1; cursor: pointer; }
.recommend-foot button:disabled { background: #b8cbd7; cursor: not-allowed; }

/* 海报插画 */
.cart-banner { margin-top: 14px; border-radius: 16px; overflow: hidden; }
.cart-banner img { display: block; width: 100%; }

/* 底部结算栏 */
.cart-page .checkout-bar {
	height: 64px;
	padding: 0 12px;
	gap: 8px;
	justify-content: flex-start;
	border-top: 1px solid #e7f0f7;
	background: #fff;
	box-shadow: 0 -6px 18px rgba(58, 112, 150, .07);
}

.cart-page .select-all-circle {
	width: 22px;
	height: 22px;
	flex: 0 0 22px;
	display: grid;
	place-items: center;
	border: 1px solid #b5c9d8;
	border-radius: 50%;
	background: #fff;
	color: transparent;
	font-size: 12px;
	cursor: pointer;
}

.cart-page .select-all-circle.active { border-color: #168bd1; background: #168bd1; color: #fff; }
.cart-page .select-all-label { color: #6f8ba0; font-size: 12px; cursor: pointer; }

.cart-page .checkout-bar .total-price { flex: 1; min-width: 0; margin-left: 6px; text-align: left; }
.cart-page .checkout-bar .total-price p { margin: 0; color: #24405c; font-size: 13px; }
.cart-page .checkout-bar .total-price p span { color: #f4483b; font-size: 19px; font-weight: 800; }
.cart-page .checkout-bar .total-price small { display: block; margin-top: 2px; color: #93a7b6; font-size: 11px; }
.cart-page .checkout-bar .total-price small i { margin-right: 3px; }

.cart-page .checkout-bar .checkout-btn {
	width: auto;
	height: 40px;
	padding: 0 26px;
	border-radius: 20px;
	background: linear-gradient(135deg, #2aa9f1, #0f83dc);
	color: #fff;
	font-size: 15px;
	font-weight: 700;
}

.cart-page .checkout-bar .checkout-btn:disabled { background: #b8cbd7; }
/* 口味备注 / 餐具 */
.cart-page .remark-row {
	margin-top: 10px;
	padding-top: 10px;
	border-top: 1px dashed #e6eff6;
	display: flex;
	align-items: center;
	gap: 8px;
}

.cart-page .remark-row .remark-label { display: flex; align-items: center; gap: 5px; flex: 0 0 auto; color: #24405c; font-size: 12px; font-weight: 600; }
.cart-page .remark-row .remark-label i { color: #168bd1; font-size: 13px; }
.cart-page .remark-row input { flex: 1; min-width: 0; border: 0; background: transparent; color: #4e7fa6; font-size: 12px; outline: none; }
.cart-page .remark-row input::placeholder { color: #a9bccb; }
.cart-page .remark-save { flex: 0 0 auto; border: 0; border-radius: 9px; padding: 4px 10px; background: #e9f6ff; color: #168bd1; font-size: 12px; font-weight: 600; cursor: pointer; }
.cart-page .remark-save:disabled { opacity: .6; cursor: not-allowed; }
.cart-page .remark-arrow { color: #c2d2de; font-size: 14px; }
</style>

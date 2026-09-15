<template>
  <main class="payment-page">
    <header class="payment-masthead"><button type="button" aria-label="返回" @click="router.push('/orderList')">‹</button><h1>在线支付</h1></header>
    <div v-if="loading" class="payment-unavailable" role="status">正在核对订单…</div>
    <div v-else-if="loadError" class="payment-unavailable" role="alert">
      <h2>暂时无法加载订单</h2><p>{{ loadError }}</p>
      <button @click="fetchOrderDetails">重试</button><button @click="router.replace('/orderList')">我的订单</button>
    </div>
    <div v-else-if="!canPay" class="payment-unavailable">
      <h2>{{ Number(orderDetail.orderState) === ORDER_STATUS.CANCELLED ? '订单已取消' : '该订单无需再次支付' }}</h2>
      <p>订单状态已更新，可在详情中查看最新进度。</p>
      <button @click="router.replace({path:'/listDetail',query:{orderId}})">查看订单</button>
    </div>
    <div v-else class="payment-content" :aria-busy="paying">
      <section class="payment-hero"><div class="payment-summary"><h2>{{ orderDetail.businessName || '我的订单' }}</h2><div><small>待支付金额</small><strong>¥{{ payableAmount }}</strong></div></div>
      <div class="fulfilment-note"><i :class="orderDetail.serviceMode === 'PICKUP' ? 'fa fa-shopping-bag' : 'fa fa-map-marker'" aria-hidden="true"></i>
        <span v-if="orderDetail.serviceMode === 'PICKUP'"><strong>到店自取</strong><small>商家备餐完成后到店取餐，无需配送地址</small></span>
        <span v-else><strong>{{ orderDetail.address || '配送地址以订单信息为准' }}</strong><small>{{ orderDetail.contactName }} · {{ orderDetail.contactTel }}</small></span>
      </div>
      </section>
      <section class="section"><h3>订单详情</h3>
        <button type="button" class="payment-merchant" @click="router.push({path:'/listDetail',query:{orderId}})"><img :src="orderDetail.businessImg || require('../assets/business-default.png')" alt="商家图片" @error="handleImageError"><strong>{{ orderDetail.businessName || '我的订单' }}</strong><span aria-hidden="true">›</span></button>
        <div v-for="(item,index) in orderDetail.foodList || []" :key="item.id || index" class="detail-item">
          <span class="item-name">{{ item.foodName }} <small>×{{ item.quantity }}</small></span><span class="item-price">¥{{ (Number(item.foodPrice || 0)*Number(item.quantity || 0)).toFixed(2) }}</span>
        </div>
        <div v-if="orderDetail.serviceMode !== 'PICKUP'" class="detail-item"><span>配送费</span><span>¥{{ Number(orderDetail.deliveryPrice || 0).toFixed(2) }}</span></div>
        <div v-if="merchantDiscount > 0" class="detail-item coupon-discount"><span>商家及会员优惠</span><span>−¥{{ merchantDiscount.toFixed(2) }}</span></div>
        <div v-if="couponDiscount > 0" class="detail-item coupon-discount"><span>{{ selectedCoupon.name || '红包抵扣' }}</span><span>−¥{{ couponDiscount.toFixed(2) }}</span></div>
        <div v-if="appliedPoints > 0" class="detail-item coupon-discount"><span>积分抵扣</span><span>−¥{{ (appliedPoints/100).toFixed(2) }}</span></div>
        <div class="detail-item payment-subtotal"><strong>应付金额</strong><strong>¥{{ payableAmount }}</strong></div>
      </section>
      <section class="section" aria-label="红包优惠"><div class="coupon-panel-header"><h3>红包 / 优惠券</h3></div>
        <div v-if="couponLoading" class="coupon-empty">正在查找可用红包…</div>
        <template v-else-if="usableCoupons.length">
          <button class="coupon-option" :class="{active:selectedCouponId === null}" :disabled="paying" :aria-pressed="selectedCouponId === null" @click="selectCoupon(null)"><i class="coupon-radio" aria-hidden="true"></i><span><b>不使用红包</b><small>保留本次红包</small></span></button>
          <button v-for="coupon in usableCoupons" :key="coupon.id" class="coupon-option" :class="{active:selectedCouponId === coupon.id}" :disabled="paying" :aria-pressed="selectedCouponId === coupon.id" @click="selectCoupon(coupon.id)"><i class="coupon-radio" aria-hidden="true"></i><span><b>{{ coupon.name || '红包' }} - 减 <em>¥{{ Number(coupon.discountAmount || 0).toFixed(2) }}</em></b><small>满 ¥{{ Number(coupon.minOrderAmount || 0).toFixed(2) }} 可用 · {{ formatCouponExpiry(coupon.expiresAt) }}到期</small></span></button>
        </template><p v-else class="coupon-empty">本单暂无可用红包</p>
      </section>
      <section class="section"><h3>选择支付方式</h3>
        <div class="payment-options"><label v-for="method in [{id:'alipay',label:'支付宝'},{id:'wechat',label:'微信支付'},{id:'wallet',label:'钱包余额'}]" :key="method.id" class="payment-option" :class="{active:selectedPayment === method.id}"><span v-if="method.id !== 'wallet'" class="payment-provider"><span class="brand-mark"><img :src="method.id === 'alipay' ? require('../assets/alipay.png') : require('../assets/wechat.png')" alt=""></span><span>{{ method.label }}</span></span><span v-else class="wallet-brand"><i class="fa fa-credit-card" aria-hidden="true"></i>钱包余额</span><input type="radio" name="payment-method" :aria-label="method.label" :value="method.id" v-model="selectedPayment" :disabled="paying || (method.id === 'wallet' && !assetInfo)"></label></div>
        <p class="payment-channel-note"><i class="fa fa-info-circle" aria-hidden="true"></i> 未接入真实支付，支付结果仅供体验</p>
        <div v-if="assetInfo" class="asset-pay-hint"><p>钱包余额 ¥{{ Number(assetInfo.balance || 0).toFixed(2) }} · 可用积分 {{ assetInfo.points || 0 }}</p><div v-if="maxPoints > 0" class="points-row"><label>积分抵扣<input v-model.number="pointsToUse" aria-label="抵扣积分" type="number" min="0" step="100" :max="maxPoints" :disabled="paying" @change="normalizePoints" @blur="normalizePoints"></label><small>本单最多可抵 {{ maxPoints }} 积分（应付金额的20%）</small></div></div>
      </section>
      <div class="payment-action"><div class="pay-total"><div>待支付 <strong>¥{{ payableAmount }}</strong></div><small>已优惠 ¥{{ (merchantDiscount + couponDiscount + appliedPoints / 100).toFixed(2) }}</small></div><button class="pay-button" :disabled="paying || couponLoading" @click="handlePayment">{{ paying ? '正在支付…' : '确认支付' }}</button></div>
    </div>
  </main>
</template>

<script>
import { ref, onMounted, onBeforeUnmount, computed } from 'vue';
import '../assets/styles/payment.css';
import { useRoute, useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { ORDER_STATUS } from '../utils/orderPresentation';
import { positiveId } from '../utils/checkout';

export default {
	name: 'Payment',
	setup() {
		let active = true;
		onBeforeUnmount(() => { active = false; });
		const orderDetail = ref({});
		const isShowDetailet = ref(true);
		const route = useRoute();
		const router = useRouter();
		const orderId = ref();
		const loading = ref(true);
		const loadError = ref('');
		const selectedPayment = ref('alipay');
		const paying = ref(false);
		const assetInfo = ref(null);
		const pointsToUse = ref(0);
		const availableCoupons = ref([]);
		const selectedCouponId = ref(null);
		const couponLoading = ref(false);
		const canPay = computed(() => Number(orderDetail.value?.orderState) === ORDER_STATUS.WAITING_PAYMENT);
		const foodSubtotal = computed(() => (orderDetail.value?.foodList || []).reduce((sum, item) => sum
			+ Number(item.foodPrice || 0) * Number(item.quantity || 0), 0));
		const merchantDiscount = computed(() => Math.max(0, foodSubtotal.value
			+ Number(orderDetail.value?.deliveryPrice || 0) - Number(orderDetail.value?.orderTotal || 0)));
		const couponBaseAmount = computed(() => Math.max(0, Number(orderDetail.value?.orderTotal || 0) - Number(orderDetail.value?.deliveryPrice || 0)));
		const usableCoupons = computed(() => availableCoupons.value
			.filter(coupon => Number(coupon.minOrderAmount || 0) <= couponBaseAmount.value)
			.sort((a, b) => Number(b.discountAmount || 0) - Number(a.discountAmount || 0)));
		const selectedCoupon = computed(() => usableCoupons.value.find(coupon => coupon.id === selectedCouponId.value) || {});
		const couponDiscount = computed(() => {
			const discount = Number(selectedCoupon.value.discountAmount || 0);
			return Math.min(Math.max(0, discount), couponBaseAmount.value);
		});
		const maxPoints = computed(() => {
			const total = Math.max(0, Number(orderDetail.value?.orderTotal || 0) - couponDiscount.value);
			const orderCap = Math.floor(total * 0.2 * 100);
			const available = Math.max(0, Number(assetInfo.value?.points || 0));
			return Math.floor(Math.min(orderCap, available) / 100) * 100;
		});
		const appliedPoints = computed(() => Math.min(maxPoints.value, Math.max(0, Math.floor((Number(pointsToUse.value) || 0) / 100) * 100)));
		const payableAmount = computed(() => (Math.max(0, Number(orderDetail.value?.orderTotal || 0) - couponDiscount.value - appliedPoints.value / 100)).toFixed(2));

		// 获取订单详情
		const fetchOrderDetails = async () => {
			loading.value = true; loadError.value = '';
			try {
				if (!positiveId(orderId.value)) throw new Error('缺少有效订单编号，请从订单列表重新进入');
				// 使用动态的orderId，而不是硬编码的24
				const response = await request.get("/api/orders/detail", {
					params: { orderId: orderId.value }
				});
				
					if (response.success && response.data) {
					// 正确的数据访问方式
					orderDetail.value = response.data;
					if (canPay.value) await fetchCoupons();
				} else {
					throw new Error(response?.message || '获取订单信息失败，请重试');
				}
			} catch (error) {
				console.error('请求错误:', error);
				loadError.value = error?.response?.data?.message || error.message || '订单信息加载失败';
			} finally {
				loading.value = false;
			}
		};
		const fetchCoupons = async () => {
			couponLoading.value = true;
			try {
				const response = await request.get('/api/v1/assets/coupons');
				availableCoupons.value = response?.success && Array.isArray(response.data) ? response.data : [];
				// 默认勾选优惠力度最大的可用红包，用户仍可手动取消或改选。
				selectedCouponId.value = usableCoupons.value[0]?.id ?? null;
			} catch (error) {
				availableCoupons.value = [];
				selectedCouponId.value = null;
			} finally {
				couponLoading.value = false;
			}
		};
		const selectCoupon = (couponId) => {
			if (paying.value) return;
			selectedCouponId.value = couponId;
			normalizePoints();
		};
		const formatCouponExpiry = (value) => value ? new Date(value).toLocaleDateString('zh-CN') : '近期';

		// 支付处理
		const handlePayment = async () => {
			if (!canPay.value) {
				toast.warning('订单状态已变化，无需再次支付');
				return;
			}
			if (paying.value || couponLoading.value) return;
            normalizePoints();
            if (selectedPayment.value === 'wallet' && (!assetInfo.value || Number(assetInfo.value.balance || 0) < Number(payableAmount.value))) { toast.warning('钱包余额不足，请选择其他方式'); return; }
			paying.value = true;
			try {
				const response = await request.put('/api/orders/status', null, { params: {
					orderState: ORDER_STATUS.WAITING_MERCHANT_ACCEPT,
					orderId: orderId.value,
					paymentMethod: selectedPayment.value === 'wallet' ? 'wallet' : 'simulated',
					pointsToUse: Number(pointsToUse.value || 0),
					couponId: selectedCouponId.value || undefined
				} });
				if (response.success && active) {
					// 支付成功，跳转到成功页面
					router.replace({
						path: '/successfulPayment',
						query: { orderId: orderId.value }
					});
				} else if (active) {
					toast.error(response?.message || '支付失败，请重试');
				}
			} catch (error) {
				console.error('支付失败:', error);
				toast.error(error?.response?.data?.message || error.message || '支付失败，请重试');
			} finally {
				paying.value = false;
			}
		};

		const detailetShow = () => {
			isShowDetailet.value = !isShowDetailet.value;
		};

		const selectPayment = (type) => {
			selectedPayment.value = type;
		};
		const normalizePoints = () => {
			pointsToUse.value = appliedPoints.value;
		};
		const handleImageError = (event) => {
			// 远程图片失效时回退到随前端一起部署的本地占位图，避免只显示 alt 文本。
			if (event.target.dataset.fallbackApplied) return;
			event.target.dataset.fallbackApplied = 'true';
			event.target.src = require('../assets/business-default.png');
		};

			onMounted(() => {
			orderId.value = route.query.orderId;
				fetchOrderDetails();
			request.get('/api/v1/assets/me').then(response => {
				if (response?.success) assetInfo.value = response.data;
			}).catch(() => { /* 未登录或资产接口不可用时仍可使用模拟支付 */ });
		});

		return {
			orderId,
			orderDetail,
			isShowDetailet,
			detailetShow,
			handlePayment,
			loading,
			loadError, fetchOrderDetails,
			selectedPayment,
			selectPayment,
			paying,
			canPay,
			router,
			assetInfo,
			pointsToUse,
			appliedPoints,
			maxPoints,
			payableAmount,
			normalizePoints,
			availableCoupons,
			usableCoupons,
			selectedCouponId,
			selectedCoupon,
			couponDiscount,
			merchantDiscount,
			couponLoading,
			selectCoupon,
			formatCouponExpiry,
			handleImageError,
			ORDER_STATUS,
		};
	}
}
</script>

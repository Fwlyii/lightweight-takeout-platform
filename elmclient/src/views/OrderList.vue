<template>
  <main class="order-page">
    <header class="order-head">
      <h1>订单</h1>
      <label class="order-search">
        <i class="fa fa-search" aria-hidden="true"></i>
        <input v-model.trim="searchKeyword" type="search" placeholder="搜索商家或订单号" aria-label="搜索商家或订单号">
        <button v-if="searchKeyword" type="button" class="clear-search" aria-label="清空搜索" @click="searchKeyword = ''">×</button>
      </label>
    </header>

    <nav class="order-tabs" aria-label="订单状态">
      <button
        v-for="(tab, index) in tabs"
        :key="tab.key"
        type="button"
        :class="{ active: activeTab === index }"
        @click="changeTab(index)"
      >
        {{ tab.label }}<span>({{ orderCounts[index] }})</span>
      </button>
    </nav>

    <section v-if="loading" class="order-loading" aria-live="polite">
      <i class="fa fa-spinner fa-spin"></i>
      <span>正在加载订单</span>
    </section>

    <template v-else-if="activeTab === 4">
      <section v-if="displayedOrders.length" :key="`orders-${activeTab}`" class="orders-content">
        <div v-for="group in groupedOrders" :key="group.key" class="month-block">
          <div class="month-summary">
            <strong>{{ group.label }}</strong>
            <span>共{{ group.orders.length }}笔 · 售后订单</span>
          </div>
          <div class="orders-stack">
            <article v-for="order in group.orders" :key="order.id" class="order-card" @click="goDetail(order.id)">
              <OrderMain :order="order" :detail="orderDetail(order)" :delivery="deliveryFor(order)" />
              <div class="card-actions single-action">
                <button type="button" class="outline-button" @click.stop="goDetail(order.id)">查看详情</button>
              </div>
            </article>
          </div>
        </div>
      </section>

      <section v-else class="after-sale-empty">
        <div class="after-sale-art">
          <i class="fa fa-file-text-o"></i>
          <i class="fa fa-heart"></i>
          <i class="fa fa-paper-plane"></i>
        </div>
        <h2>暂无售后订单</h2>
        <p>退款、赔付或售后进度会显示在这里</p>
        <button type="button" class="primary-button" @click="showAfterSaleHelp">查看售后说明</button>
        <button type="button" class="outline-button" @click="contactSupport">联系客服</button>
      </section>

      <section class="after-sale-faq">
        <h2><span>?</span>常见售后问题</h2>
        <button v-for="faq in afterSaleFaqs" :key="faq.title" type="button" @click="showFaq(faq)">
          <i class="fa" :class="faq.icon"></i><strong>{{ faq.title }}</strong><i class="fa fa-angle-right"></i>
        </button>
      </section>
    </template>

    <section v-else-if="displayedOrders.length" :key="`orders-${activeTab}`" class="orders-content">
      <div v-for="group in groupedOrders" :key="group.key" class="month-block">
        <div class="month-summary">
          <strong>{{ group.label }}</strong>
          <span>共{{ group.orders.length }}笔 · {{ groupSummaryLabel }} ¥{{ group.total }}</span>
        </div>

        <div class="orders-stack">
          <article v-for="order in group.orders" :key="order.id" class="order-card" @click="goDetail(order.id)">
            <OrderMain :order="order" :detail="orderDetail(order)" :delivery="deliveryFor(order)" />

            <div v-if="isOngoing(order)" class="rider-row">
              <div class="rider-avatar"><i class="fa fa-user"></i></div>
              <span>骑手 {{ deliveryFor(order)?.riderName || '信息更新中' }}<b v-if="deliveryFor(order)?.riderName"> · 配送中</b></span>
              <button v-if="deliveryFor(order)?.riderPhone" type="button" class="call-rider" aria-label="联系骑手" @click.stop="callRider(deliveryFor(order).riderPhone)"><i class="fa fa-phone"></i></button>
              <i v-else class="fa fa-angle-right rider-arrow"></i>
            </div>

            <div v-if="isOngoing(order)" class="progress-track" aria-label="配送进度">
              <div
                v-for="(step, index) in progressSteps(order)"
                :key="step.label"
                class="progress-step"
                :class="{ completed: index < progressIndex(order), current: index === progressIndex(order) }"
              >
                <span class="progress-dot"><i v-if="index < progressIndex(order)" class="fa fa-check"></i></span>
                <span>{{ step.label }}</span>
                <small>{{ step.time }}</small>
              </div>
            </div>

            <div v-if="isWaitingPayment(order)" class="payment-notice">
              <i class="fa fa-exclamation-circle"></i>
              <span>下单后请尽快完成支付，以免商家库存变化</span>
            </div>

            <div class="card-actions">
              <template v-if="isWaitingPayment(order)">
                <button type="button" class="outline-button" @click.stop="cancelOrder(order.id)">取消订单</button>
                <button type="button" class="primary-button" @click.stop="payOrder(order.id)">去支付</button>
              </template>
              <template v-else-if="isDelivered(order)">
                <button type="button" class="outline-button" @click.stop="goDetail(order.id)">查看详情</button>
                <button type="button" class="primary-button" @click.stop="confirmOrder(order.id)">确认收货</button>
              </template>
              <template v-else-if="isOngoing(order)">
                <button type="button" class="outline-button" @click.stop="goDetail(order.id)">查看详情</button>
                <button type="button" class="primary-button" @click.stop="contactRider(order)">联系骑手</button>
              </template>
              <template v-else-if="isCompleted(order)">
                <button type="button" class="outline-button" @click.stop="goDetail(order.id)">订单详情</button>
                <button type="button" class="primary-button" @click.stop="reorder(order)">再来一单</button>
                <button type="button" class="outline-button" @click.stop="reviewOrder(order.id)">评价</button>
              </template>
              <template v-else>
                <button type="button" class="outline-button" @click.stop="goDetail(order.id)">查看详情</button>
              </template>
            </div>
          </article>
        </div>
      </div>

      <section v-if="activeTab === 3" class="recommend-section">
        <div class="recommend-heading">
          <h2><i class="fa fa-cutlery"></i>你可能还想吃</h2>
          <button type="button" @click="refreshRecommendations">换一换 <i class="fa fa-refresh"></i></button>
        </div>
        <div class="recommend-grid">
          <article v-for="item in recommendations" :key="item.key" class="recommend-card">
            <img :src="businessImage(item.order)" :alt="businessName(item.order)" @error="handleImageError">
            <div>
              <strong>{{ businessName(item.order) }}</strong>
              <p>{{ orderSummary(item.order) }}</p>
              <b>¥{{ money(item.order.orderTotal) }}</b>
              <button type="button" @click="reorder(item.order)">去下单</button>
            </div>
          </article>
        </div>
      </section>

      <section v-if="activeTab === 2" class="ongoing-help">
        <button type="button" @click="urgeOrder">
          <i class="fa fa-bell-o"></i><span><strong>催单</strong><small>加快配送进度</small></span><i class="fa fa-angle-right"></i>
        </button>
        <button type="button" @click="contactSupport">
          <i class="fa fa-headphones"></i><span><strong>联系客服</strong><small>订单问题咨询</small></span><i class="fa fa-angle-right"></i>
        </button>
      </section>
    </section>

    <section v-else :key="`empty-${activeTab}`" class="empty-orders">
      <div class="empty-icon"><i class="fa fa-file-text-o"></i></div>
      <h2>暂无{{ tabs[activeTab].label }}订单</h2>
      <p>{{ activeTab === 1 ? '当前没有需要支付的订单' : '去首页逛逛，发现更多美味' }}</p>
      <button type="button" class="primary-button" @click="goIndex">去首页点餐</button>
    </section>

    <section v-if="activeTab === 0 || activeTab === 1" class="help-section">
      <div class="help-title">
        <span class="help-icon"><i class="fa fa-question"></i></span>
        <div><h2>需要帮助?</h2><p>遇到问题，可以通过以下方式获取帮助</p></div>
      </div>
      <div class="help-actions">
        <button type="button" @click="showPaymentHelp"><span><i class="fa fa-credit-card"></i></span><b>支付遇到问题<small>查看支付问题解答</small></b><i class="fa fa-angle-right"></i></button>
        <button type="button" @click="contactSupport"><span><i class="fa fa-headphones"></i></span><b>联系客服<small>在线客服为您服务</small></b><i class="fa fa-angle-right"></i></button>
      </div>
    </section>

    <div v-if="showConfirmFinishedModal || showConfirmCanceledModal" class="modal-overlay" @click.self="closeModal">
      <div class="confirm-modal">
        <button type="button" class="modal-close" aria-label="关闭" @click="closeModal">×</button>
        <div class="modal-icon"><i class="fa" :class="showConfirmCanceledModal ? 'fa-trash-o' : 'fa-check'"></i></div>
        <h3>{{ showConfirmCanceledModal ? '确认取消订单？' : '确认已经收到餐品？' }}</h3>
        <p>{{ showConfirmCanceledModal ? '取消后订单将无法恢复' : '确认收货后可以对本次订单进行评价' }}</p>
        <div class="modal-actions">
          <button type="button" class="outline-button" @click="closeModal">再想想</button>
          <button type="button" class="primary-button" @click="showConfirmCanceledModal ? confirmCanceled() : confirmFinished()">确认</button>
        </div>
      </div>
    </div>
  </main>
</template>

<script>
import { computed, defineComponent, h, onMounted, onUnmounted, ref } from 'vue';
import { useRouter } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { createRealtimeConnection } from '../services/realtimeService';
import { CUSTOMER_ORDER_GROUPS, ORDER_STATUS } from '../utils/orderPresentation';
import { formatDateTime } from '../utils/formatters';

const TABS = [
  { key: 'all', label: '全部' },
  { key: 'payment', label: '待付款' },
  { key: 'ongoing', label: '进行中' },
  { key: 'completed', label: '已完成' },
  { key: 'afterSale', label: '售后' }
];

const ONGOING_STATES = CUSTOMER_ORDER_GROUPS.fulfilling;
const AFTER_SALE_STATES = [...CUSTOMER_ORDER_GROUPS.cancelled, ORDER_STATUS.DELIVERY_EXCEPTION];
const fallbackImage = require('@/assets/business-default.png');

const getBusinessName = order => order?.businessName || order?.business?.businessName || '附近好店';
const getBusinessImage = order => order?.businessImg || order?.business?.businessImg || fallbackImage;
const getOrderState = order => Number(order?.orderState);
const getMoney = value => Number(value || 0).toFixed(2);
const getOrderSummary = (order, detail) => {
  const foods = detail?.foodList;
  if (Array.isArray(foods) && foods.length) {
    return foods.map(item => `${item.foodName || '商品'}${Number(item.quantity || 0) > 1 ? ` ×${item.quantity}` : ''}`).join(' + ');
  }
  return String(order?.serviceMode || '').toUpperCase() === 'PICKUP' ? '到店自取订单' : '订单商品明细';
};

const OrderMain = defineComponent({
  name: 'OrderMain',
  props: { order: { type: Object, required: true }, detail: { type: Object, default: () => ({}) }, delivery: { type: Object, default: null } },
  setup(props) {
    const isPayment = computed(() => getOrderState(props.order) === ORDER_STATUS.WAITING_PAYMENT);
    const isCompleted = computed(() => getOrderState(props.order) === ORDER_STATUS.COMPLETED);
    const isAfterSale = computed(() => AFTER_SALE_STATES.includes(getOrderState(props.order)));
    const isOngoing = computed(() => ONGOING_STATES.includes(getOrderState(props.order)) || getOrderState(props.order) === ORDER_STATUS.WAITING_MERCHANT_ACCEPT);
    const label = computed(() => {
      if (isPayment.value) return '待付款';
      if (isAfterSale.value) return getOrderState(props.order) === ORDER_STATUS.DELIVERY_EXCEPTION ? '配送异常' : '售后处理中';
      if (isCompleted.value) return '已完成';
      if (isOngoing.value) return '配送中';
      return '订单处理中';
    });
    const stateClass = computed(() => isPayment.value ? 'state-payment' : isAfterSale.value ? 'state-cancelled' : isCompleted.value ? 'state-completed' : 'state-ongoing');
    const stateIcon = computed(() => isPayment.value ? 'fa-clock-o' : isAfterSale.value ? 'fa-info-circle' : isCompleted.value ? 'fa-check-circle' : 'fa-truck');
    const paymentCountdown = computed(() => {
      const date = new Date(props.order.orderDate || props.order.createTime);
      if (!Number.isFinite(date.getTime())) return '15:00';
      const remaining = Math.max(0, 15 * 60 - Math.floor((Date.now() - date.getTime()) / 1000));
      return `${String(Math.floor(remaining / 60)).padStart(2, '0')}:${String(remaining % 60).padStart(2, '0')}`;
    });
    const sideText = computed(() => {
      if (isPayment.value) return `剩余 ${paymentCountdown.value} 自动取消`;
      if (isCompleted.value) return '已送达';
      if (isOngoing.value) return props.delivery?.deliveredTime ? '已送达待确认' : '预计尽快送达';
      return '';
    });
    return () => h('div', { class: 'card-main' }, [
      h('img', { src: getBusinessImage(props.order), alt: getBusinessName(props.order), onError: event => { event.target.src = fallbackImage; } }),
      h('div', { class: 'card-info' }, [
        h('div', { class: 'merchant-line' }, [h('strong', getBusinessName(props.order)), h('i', { class: 'fa fa-angle-right' })]),
        h('time', formatDateTime(props.order.orderDate || props.order.createTime, '时间待更新')),
        h('span', { class: 'service-tag' }, String(props.order.serviceMode || '').toUpperCase() === 'PICKUP' ? '到店自取' : '外送'),
        h('p', getOrderSummary(props.order, props.detail))
      ]),
      h('div', { class: 'card-side' }, [
        h('span', { class: ['state-pill', stateClass.value] }, [h('i', { class: ['fa', stateIcon.value] }), label.value]),
        sideText.value ? h('small', sideText.value) : null,
        h('strong', `¥${getMoney(props.order.orderTotal)}`)
      ])
    ]);
  }
});

export default defineComponent({
  name: 'OrderList',
  components: { OrderMain },
  setup() {
    const router = useRouter();
    const orderArr = ref([]);
    const detailCache = ref({});
    const deliveryCache = ref({});
    const loading = ref(false);
    const searchKeyword = ref('');
    const activeTab = ref(0);
    const showConfirmFinishedModal = ref(false);
    const showConfirmCanceledModal = ref(false);
    const selectId = ref(0);
    const recommendationOffset = ref(0);
    let realtimeConnection = null;

    const tabs = TABS;
    const state = getOrderState;
    const businessName = getBusinessName;
    const businessImage = getBusinessImage;
    const money = getMoney;
    const businessId = order => order?.businessId || order?.business?.id;
    const isWaitingPayment = order => state(order) === ORDER_STATUS.WAITING_PAYMENT;
    const isOngoing = order => ONGOING_STATES.includes(state(order)) || state(order) === ORDER_STATUS.WAITING_MERCHANT_ACCEPT;
    const isDelivered = order => state(order) === ORDER_STATUS.DELIVERED || (state(order) === ORDER_STATUS.WAITING_PICKUP && String(order?.serviceMode || '').toUpperCase() === 'PICKUP');
    const isCompleted = order => state(order) === ORDER_STATUS.COMPLETED;
    const isAfterSale = order => AFTER_SALE_STATES.includes(state(order));
    const orderDetail = order => detailCache.value[order?.id] || {};
    const deliveryFor = order => deliveryCache.value[order?.id] || null;
    const orderSummary = order => getOrderSummary(order, orderDetail(order));
    const formatTime = value => formatDateTime(value, '时间待更新');

    const tabOrders = computed(() => {
      if (activeTab.value === 0) return orderArr.value;
      if (activeTab.value === 1) return orderArr.value.filter(isWaitingPayment);
      if (activeTab.value === 2) return orderArr.value.filter(isOngoing);
      if (activeTab.value === 3) return orderArr.value.filter(isCompleted);
      return orderArr.value.filter(isAfterSale);
    });
    const displayedOrders = computed(() => {
      const keyword = searchKeyword.value.toLowerCase();
      if (!keyword) return tabOrders.value;
      return tabOrders.value.filter(order => [order.id, businessName(order), orderSummary(order)].some(value => String(value || '').toLowerCase().includes(keyword)));
    });
    const groupedOrders = computed(() => {
      const groups = new Map();
      displayedOrders.value.forEach(order => {
        const date = new Date(order.orderDate || order.createTime);
        const valid = Number.isFinite(date.getTime());
        const key = valid ? `${date.getFullYear()}-${date.getMonth() + 1}` : 'unknown';
        if (!groups.has(key)) groups.set(key, { key, label: valid ? `${date.getFullYear()}年${date.getMonth() + 1}月` : '日期未知', timestamp: valid ? new Date(date.getFullYear(), date.getMonth(), 1).getTime() : 0, orders: [], amount: 0 });
        const group = groups.get(key);
        group.orders.push(order);
        if (!isWaitingPayment(order) && !isAfterSale(order)) group.amount += Number(order.orderTotal || 0);
      });
      return [...groups.values()].sort((a, b) => b.timestamp - a.timestamp).map(group => ({ ...group, total: money(group.amount) }));
    });
    const orderCounts = computed(() => [
      orderArr.value.length,
      orderArr.value.filter(isWaitingPayment).length,
      orderArr.value.filter(isOngoing).length,
      orderArr.value.filter(isCompleted).length,
      orderArr.value.filter(isAfterSale).length
    ]);
    const groupSummaryLabel = computed(() => activeTab.value === 1 ? '待支付' : activeTab.value === 2 ? '进行中' : '支出');
    const recommendations = computed(() => {
      const source = orderArr.value.filter(order => !isAfterSale(order));
      if (!source.length) return [];
      const offset = recommendationOffset.value % source.length;
      return source.slice(offset).concat(source.slice(0, offset)).slice(0, 2).map(order => ({ order, key: `${order.id}-${offset}` }));
    });

    const loadOrderExtras = async orders => {
      await Promise.all(orders.slice(0, 15).map(async order => {
        if (!order?.id || detailCache.value[order.id]) return;
        try {
          const response = await request.get('/api/orders/detail', { params: { orderId: order.id } });
          if (response?.success && response.data) detailCache.value[order.id] = response.data;
        } catch (_) { /* 列表仍可使用，商品摘要稍后刷新 */ }
      }));
      await Promise.all(orders.filter(isOngoing).slice(0, 10).map(async order => {
        if (!order?.id || deliveryCache.value[order.id]) return;
        try {
          const response = await request.get(`/api/v1/orders/${order.id}/delivery`);
          if (response?.success && response.data) deliveryCache.value[order.id] = response.data;
        } catch (_) { /* 未生成配送任务时保留基础进度 */ }
      }));
    };
    const fetchOrders = async ({ silent = false } = {}) => {
      if (!silent) loading.value = true;
      try {
        const response = await request.get('/api/orders/list/user');
        if (response?.success) {
          orderArr.value = Array.isArray(response.data) ? response.data : [];
          loadOrderExtras(orderArr.value);
        } else if (!silent) toast.error(response?.message || '获取订单失败');
      } catch (error) {
        console.error('请求订单列表失败:', error);
        if (!silent) toast.error('获取订单失败，请稍后重试');
      } finally {
        if (!silent) loading.value = false;
      }
    };
    const progressIndex = order => {
      const delivery = deliveryFor(order);
      if (state(order) === ORDER_STATUS.DELIVERED || delivery?.deliveredTime) return 3;
      if (state(order) === ORDER_STATUS.DELIVERING || delivery?.pickupTime) return 2;
      if ([ORDER_STATUS.WAITING_PICKUP, ORDER_STATUS.WAITING_RIDER_ACCEPT].includes(state(order)) || delivery?.acceptedTime) return 1;
      return 0;
    };
    const toClock = value => value ? formatDateTime(value, '').split(' ')[1]?.slice(0, 5) || '' : '';
    const progressSteps = order => {
      const delivery = deliveryFor(order) || {};
      return [
        { label: '商家接单', time: toClock(delivery.acceptedTime) },
        { label: '制作中', time: toClock(delivery.arrivedStoreTime) },
        { label: '骑手配送', time: toClock(delivery.pickupTime) },
        { label: '已送达', time: toClock(delivery.deliveredTime) }
      ];
    };

    const changeTab = index => { activeTab.value = index; };
    const cancelOrder = id => { selectId.value = id; showConfirmCanceledModal.value = true; };
    const confirmOrder = id => { selectId.value = id; showConfirmFinishedModal.value = true; };
    const closeModal = () => { showConfirmFinishedModal.value = false; showConfirmCanceledModal.value = false; selectId.value = 0; };
    const confirmCanceled = async () => {
      if (!selectId.value) return;
      try {
        const response = await request.put('/api/orders/status', null, { params: { orderState: ORDER_STATUS.CANCELLED, orderId: selectId.value } });
        if (response?.success) { toast.success('订单取消成功'); await fetchOrders(); } else toast.error(response?.message || '取消失败，请重试');
      } catch (_) { toast.error('取消失败，请重试'); } finally { closeModal(); }
    };
    const confirmFinished = async () => {
      if (!selectId.value) return;
      try {
        const response = await request.post(`/api/v1/orders/${selectId.value}/confirm-receipt`);
        if (response?.success) { toast.success('订单完成'); await fetchOrders(); } else toast.error(response?.message || '确认收货失败');
      } catch (_) { toast.error('确认收货失败，请重试'); } finally { closeModal(); }
    };
    const payOrder = orderId => router.push({ path: '/payment', query: { orderId } });
    const goDetail = orderId => router.push({ path: '/listDetail', query: { orderId } });
    const reviewOrder = orderId => router.push({ path: '/listDetail', query: { orderId, focus: 'review' } });
    const reorder = order => businessId(order) ? router.push({ path: '/businessInfo', query: { businessId: businessId(order) } }) : toast.info('暂时无法定位该商家');
    const callRider = tel => { window.location.href = `tel:${tel}`; };
    const contactRider = order => deliveryFor(order)?.riderPhone ? callRider(deliveryFor(order).riderPhone) : toast.info('骑手联系方式正在更新');
    const contactSupport = () => toast.info('客服通道将在订单详情中为你提供帮助');
    const urgeOrder = () => toast.success('已为你催单，商家会尽快处理');
    const showPaymentHelp = () => toast.info('请确认网络和支付账户状态，也可以进入订单详情联系客服');
    const showAfterSaleHelp = () => toast.info('订单详情中可以查看退款、赔付和售后进度');
    const showFaq = faq => toast.info(faq.answer, 3500);
    const refreshRecommendations = () => { recommendationOffset.value += 1; };
    const goIndex = () => router.push('/index');
    const handleImageError = event => { event.target.src = fallbackImage; };
    const afterSaleFaqs = [
      { title: '如何申请退款', icon: 'fa-file-text-o', answer: '请进入对应订单详情，联系客服说明退款原因。' },
      { title: '商家漏餐怎么办', icon: 'fa-cutlery', answer: '保留餐品照片并联系商家或客服，平台会协助核实处理。' },
      { title: '配送超时如何处理', icon: 'fa-clock-o', answer: '可先在进行中订单催单，仍未解决时请联系客服。' }
    ];

    onMounted(() => {
      const userData = sessionStorage.getItem('userInfo') || localStorage.getItem('userInfo');
      if (!userData) {
        toast.error('用户未登录，请先登录');
        router.push({ path: '/login', query: { role: 'user' } });
        return;
      }
      realtimeConnection = createRealtimeConnection({ onMessage: () => fetchOrders({ silent: true }), onFallbackRefresh: () => fetchOrders({ silent: true }) });
      realtimeConnection.start();
      fetchOrders();
    });
    onUnmounted(() => realtimeConnection?.stop());

    return {
      tabs, activeTab, searchKeyword, loading, displayedOrders, groupedOrders, orderCounts, groupSummaryLabel,
      recommendations, afterSaleFaqs, isWaitingPayment, isOngoing, isDelivered, isCompleted, orderDetail, deliveryFor,
      businessName, businessImage, orderSummary, money, progressIndex, progressSteps, changeTab, cancelOrder, confirmOrder,
      confirmCanceled, confirmFinished, closeModal, showConfirmFinishedModal, showConfirmCanceledModal, payOrder, goDetail,
      reviewOrder, reorder, callRider, contactRider, contactSupport, urgeOrder, showPaymentHelp, showAfterSaleHelp, showFaq,
      refreshRecommendations, goIndex, handleImageError, formatTime
    };
  }
});
</script>

<style scoped>
:global(.content) { background: #f3f9ff; }
.order-page { min-height: 100vh; padding: 0 16px 96px; box-sizing: border-box; color: #102b57; background: radial-gradient(circle at 50% -10%, #d9efff 0, #f3f9ff 34%, #f3f9ff 100%); font-family: "PingFang SC", "Microsoft YaHei", sans-serif; }
.order-head { padding: 26px 6px 18px; }.order-head h1 { text-align: center; font-size: 26px; line-height: 1; font-weight: 800; letter-spacing: 1px; }
.order-search { display: flex; align-items: center; height: 56px; margin-top: 26px; padding: 0 20px; border-radius: 30px; background: rgba(255,255,255,.88); box-shadow: 0 8px 24px rgba(60,130,190,.1); color: #7c91ae; }.order-search .fa { margin-right: 16px; font-size: 23px; }.order-search input { min-width: 0; flex: 1; border: 0; outline: 0; background: transparent; color: #213a61; font: inherit; font-size: 16px; }.order-search input::placeholder { color: #7d91ad; }.clear-search { border: 0; color: #9aacc2; background: transparent; font-size: 24px; cursor: pointer; }
.order-tabs { display: grid; grid-template-columns: repeat(5, minmax(0, 1fr)); gap: 3px; margin: 0 -5px 18px; }.order-tabs button { min-width: 0; height: 48px; border: 0; border-radius: 25px; color: #506887; background: transparent; font: inherit; font-size: 14px; font-weight: 600; white-space: nowrap; cursor: pointer; }.order-tabs button span { font-weight: 500; }.order-tabs button.active { color: #fff; background: linear-gradient(135deg, #1b9dff, #087df0); box-shadow: 0 7px 15px rgba(11,139,241,.19); }
.order-loading { display: flex; justify-content: center; align-items: center; gap: 10px; min-height: 160px; color: #7290b1; font-size: 14px; }.month-block + .month-block { margin-top: 18px; }.month-summary { display: flex; align-items: center; justify-content: space-between; min-height: 50px; padding: 0 16px; border-radius: 13px; background: rgba(232,242,251,.82); }.month-summary strong { font-size: 20px; }.month-summary span { color: #7d91ad; font-size: 13px; text-align: right; }.orders-stack { display: grid; gap: 16px; margin-top: 16px; }
.order-card { overflow: hidden; border-radius: 20px; background: rgba(255,255,255,.96); box-shadow: 0 7px 22px rgba(59,123,177,.08); cursor: pointer; }
:deep(.card-main) { display: grid; grid-template-columns: 92px minmax(0, 1fr) auto; gap: 12px; padding: 16px 14px 14px; }:deep(.card-main > img) { width: 92px; height: 92px; border-radius: 13px; object-fit: cover; background: #e8f3fb; }:deep(.card-info) { min-width: 0; padding-top: 2px; }:deep(.merchant-line) { display: flex; align-items: center; gap: 6px; min-width: 0; }:deep(.merchant-line strong) { overflow: hidden; color: #0e2a55; font-size: 18px; line-height: 1.3; text-overflow: ellipsis; white-space: nowrap; }:deep(.merchant-line .fa) { flex: 0 0 auto; color: #7186a3; font-size: 19px; }:deep(.card-info time) { display: block; margin-top: 8px; color: #7890ad; font-size: 13px; white-space: nowrap; }:deep(.service-tag) { display: inline-block; margin-top: 7px; padding: 5px 10px; border-radius: 8px; color: #0788ef; background: #e6f4ff; font-size: 12px; }:deep(.card-info p) { overflow: hidden; margin-top: 9px; color: #415b7c; font-size: 14px; line-height: 1.3; text-overflow: ellipsis; white-space: nowrap; }
:deep(.card-side) { display: flex; flex-direction: column; align-items: flex-end; min-width: 88px; gap: 8px; text-align: right; }:deep(.state-pill) { display: inline-flex; align-items: center; gap: 6px; min-height: 34px; padding: 0 10px; border-radius: 11px; font-size: 13px; font-weight: 700; white-space: nowrap; }:deep(.state-pill .fa) { font-size: 15px; }:deep(.state-payment) { color: #f18b00; background: #fff5e5; }:deep(.state-ongoing) { color: #0585e8; background: #e7f5ff; }:deep(.state-completed) { color: #1caf58; background: #e7f9ec; }:deep(.state-cancelled) { color: #ef8b19; background: #fff3e5; }:deep(.card-side small) { color: #8195ae; font-size: 12px; white-space: nowrap; }:deep(.card-side > strong) { margin-top: auto; color: #0b2853; font-size: 21px; white-space: nowrap; }
.payment-notice { display: flex; align-items: center; gap: 10px; margin: 0 14px 14px; padding: 12px 13px; border-radius: 12px; color: #e47b0c; background: #fff6e9; font-size: 13px; line-height: 1.35; }.payment-notice .fa { font-size: 18px; }.card-actions { display: flex; justify-content: flex-end; gap: 9px; margin: 0 14px; padding: 14px 0 16px; border-top: 1px solid #edf3f8; }.card-actions button, .after-sale-empty button { height: 40px; padding: 0 22px; border-radius: 11px; font: inherit; font-size: 14px; font-weight: 700; cursor: pointer; }.primary-button { border: 1px solid #128ef5; color: white; background: linear-gradient(135deg, #1c9dff, #087ff0); box-shadow: 0 6px 13px rgba(15,143,245,.16); }.outline-button { border: 1px solid #8bc6fb; color: #087fe5; background: #fff; }.card-actions .primary-button, .card-actions .outline-button { min-width: 104px; }.single-action .outline-button { min-width: 124px; }
.rider-row { display: flex; align-items: center; gap: 10px; margin: 0 14px; padding: 13px 0; border-top: 1px solid #edf3f8; border-bottom: 1px solid #edf3f8; color: #3d587a; font-size: 14px; }.rider-row b { font-weight: 500; }.rider-avatar { display: grid; place-items: center; width: 40px; height: 40px; border-radius: 50%; color: #0788ef; background: #e8f5ff; font-size: 20px; }.call-rider { display: grid; place-items: center; width: 38px; height: 38px; margin-left: auto; border: 0; border-radius: 50%; color: #0788ef; background: #eef8ff; font-size: 17px; cursor: pointer; }.rider-arrow { margin-left: auto; color: #8aa0ba; font-size: 20px; }
.progress-track { display: grid; grid-template-columns: repeat(4, 1fr); margin: 15px 14px 4px; }.progress-step { position: relative; display: flex; flex-direction: column; align-items: center; gap: 6px; color: #8297b0; font-size: 11px; text-align: center; }.progress-step:not(:last-child)::after { content: ''; position: absolute; top: 8px; left: 61%; width: 78%; border-top: 3px dotted #ccd8e5; }.progress-step.completed, .progress-step.current { color: #18345d; }.progress-step.completed:not(:last-child)::after { border-top-style: solid; border-top-color: #168ff3; }.progress-dot { z-index: 1; position: relative; display: grid; place-items: center; width: 16px; height: 16px; border: 3px solid #d8e2ee; border-radius: 50%; background: #fff; box-sizing: border-box; }.progress-step.completed .progress-dot, .progress-step.current .progress-dot { border-color: #138ef4; color: white; background: #138ef4; }.progress-dot .fa { font-size: 8px; }.progress-step small { min-height: 14px; color: #8195ad; font-size: 11px; }
.ongoing-help { display: grid; grid-template-columns: 1fr 1fr; margin-top: 16px; border-radius: 19px; background: rgba(255,255,255,.95); box-shadow: 0 7px 22px rgba(59,123,177,.07); }.ongoing-help button { display: flex; align-items: center; gap: 11px; min-width: 0; padding: 18px 14px; border: 0; color: #138be9; background: transparent; font: inherit; text-align: left; cursor: pointer; }.ongoing-help button + button { border-left: 1px solid #e5eef6; }.ongoing-help button > .fa:first-child { font-size: 25px; }.ongoing-help span { display: flex; flex-direction: column; min-width: 0; }.ongoing-help strong { color: #173560; font-size: 15px; }.ongoing-help small { margin-top: 4px; color: #8598b1; font-size: 11px; white-space: nowrap; }.ongoing-help button > .fa:last-child { margin-left: auto; color: #8aa0ba; }
.recommend-section, .help-section, .after-sale-faq { margin-top: 18px; padding: 17px 14px; border-radius: 20px; background: rgba(255,255,255,.95); box-shadow: 0 7px 22px rgba(59,123,177,.07); }.recommend-heading { display: flex; align-items: center; justify-content: space-between; }.recommend-heading h2 { color: #112d59; font-size: 19px; }.recommend-heading h2 .fa { margin-right: 10px; color: #0e8bed; }.recommend-heading button { border: 0; color: #7d91ad; background: transparent; font: inherit; font-size: 13px; cursor: pointer; }.recommend-heading button .fa { margin-left: 4px; font-size: 15px; }.recommend-grid { display: grid; grid-template-columns: repeat(2, minmax(0, 1fr)); gap: 10px; margin-top: 14px; }.recommend-card { display: grid; grid-template-columns: 78px minmax(0, 1fr); gap: 9px; min-width: 0; padding: 9px; border-radius: 15px; background: #f8fcff; }.recommend-card > img { width: 78px; height: 104px; border-radius: 11px; object-fit: cover; }.recommend-card > div { min-width: 0; }.recommend-card strong { display: block; overflow: hidden; color: #172f56; font-size: 14px; text-overflow: ellipsis; white-space: nowrap; }.recommend-card p { overflow: hidden; margin-top: 7px; color: #8195ae; font-size: 12px; text-overflow: ellipsis; white-space: nowrap; }.recommend-card b { display: block; margin-top: 9px; color: #132f5b; font-size: 16px; }.recommend-card button { width: 100%; height: 32px; margin-top: 9px; border: 0; border-radius: 9px; color: #0787ec; background: #e5f4ff; font: inherit; font-size: 13px; font-weight: 700; cursor: pointer; }
.help-title { display: flex; align-items: center; gap: 12px; }.help-icon { display: grid; place-items: center; width: 42px; height: 42px; border-radius: 50%; color: #0a88ef; background: #e2f2ff; font-size: 22px; }.help-title h2 { font-size: 19px; }.help-title p { margin-top: 4px; color: #8195ae; font-size: 13px; }.help-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; margin-top: 16px; }.help-actions button { display: flex; align-items: center; gap: 10px; min-width: 0; padding: 13px 10px; border: 0; border-radius: 14px; color: #8498b1; background: #f2f8fd; font: inherit; text-align: left; cursor: pointer; }.help-actions button > span { display: grid; place-items: center; flex: 0 0 auto; width: 37px; height: 37px; border-radius: 50%; color: #0788ef; background: #e0f1ff; font-size: 18px; }.help-actions b { min-width: 0; color: #1b355c; font-size: 14px; }.help-actions b small { display: block; overflow: hidden; margin-top: 4px; color: #879ab2; font-size: 11px; font-weight: 500; text-overflow: ellipsis; white-space: nowrap; }.help-actions > button > .fa:last-child { margin-left: auto; font-size: 20px; }
.empty-orders { display: flex; flex-direction: column; align-items: center; margin-top: 50px; padding: 38px 20px; border-radius: 20px; background: rgba(255,255,255,.92); text-align: center; }.empty-icon { display: grid; place-items: center; width: 64px; height: 64px; border-radius: 50%; color: #148ff2; background: #e7f5ff; font-size: 30px; }.empty-orders h2 { margin-top: 16px; font-size: 20px; }.empty-orders p { margin: 8px 0 20px; color: #8397af; font-size: 13px; }.empty-orders .primary-button { height: 42px; padding: 0 29px; border-radius: 13px; font: inherit; font-size: 14px; font-weight: 700; cursor: pointer; }

/* ── 任务7/9：订单状态与空态动效（全部为 transform / opacity，移动端开销极低）── */
/* 切换状态标签时，列表淡入 + 上滑 */
.orders-content, .after-sale-empty, .empty-orders { animation: order-switch-in 320ms cubic-bezier(.22, 1, .36, 1) both; }
/* 时间线当前节点：蓝色圆点循环呼吸 */
.progress-step.current .progress-dot { animation: dot-breathe 2s ease-in-out infinite; }
.progress-step.current .progress-dot::after { content: ''; position: absolute; inset: -6px; border-radius: 50%; border: 2px solid rgba(19, 142, 244, .4); animation: dot-ring 2s ease-out infinite; }
/* 状态牌（待付款 / 配送中 / 已完成）出现时轻微上浮 */
:deep(.state-pill) { animation: state-pill-in 280ms cubic-bezier(.22, 1, .36, 1) both; }
/* 空状态插画缓慢浮动，替代 Lottie，不引入额外依赖 */
.empty-orders .empty-icon { animation: empty-float 2.4s ease-in-out infinite; }
.after-sale-art { animation: empty-float 2.8s ease-in-out infinite; }

@keyframes order-switch-in {
  from { opacity: 0; transform: translateY(14px); }
  to { opacity: 1; transform: translateY(0); }
}

@keyframes dot-breathe {
  0%, 100% { transform: scale(1); }
  50% { transform: scale(1.2); }
}

@keyframes dot-ring {
  0% { opacity: .55; transform: scale(.8); }
  100% { opacity: 0; transform: scale(1.35); }
}

@keyframes state-pill-in {
  from { opacity: 0; transform: translateY(4px) scale(.96); }
  to { opacity: 1; transform: translateY(0) scale(1); }
}

@keyframes empty-float {
  0%, 100% { transform: translateY(0); }
  50% { transform: translateY(-6px); }
}

@media (prefers-reduced-motion: reduce) {
  .orders-content, .after-sale-empty, .empty-orders,
  .progress-step.current .progress-dot,
  .progress-step.current .progress-dot::after,
  .empty-orders .empty-icon, .after-sale-art,
  :deep(.state-pill) {
    animation: none;
  }
}
.after-sale-empty { display: flex; flex-direction: column; align-items: center; padding: 62px 18px 38px; border-radius: 20px; background: rgba(255,255,255,.95); text-align: center; }.after-sale-art { position: relative; width: 180px; height: 150px; margin-bottom: 17px; border-radius: 50%; color: #1693f4; background: linear-gradient(145deg, #eef7ff, #dcefff); }.after-sale-art .fa-file-text-o { position: absolute; left: 64px; top: 43px; padding: 19px 22px; border-radius: 8px; background: white; box-shadow: 5px 7px 0 #c5e2fb; font-size: 35px; transform: rotate(-8deg); }.after-sale-art .fa-heart { position: absolute; right: 23px; bottom: 25px; color: #fff; font-size: 40px; }.after-sale-art .fa-paper-plane { position: absolute; right: 4px; top: 13px; color: #8fc7fa; font-size: 23px; transform: rotate(-18deg); }.after-sale-empty h2 { font-size: 21px; }.after-sale-empty p { margin: 9px 0 23px; color: #8195ae; font-size: 14px; }.after-sale-empty button { width: min(100%, 290px); }.after-sale-empty .outline-button { margin-top: 11px; }
.after-sale-faq h2 { display: flex; align-items: center; gap: 9px; padding-bottom: 8px; font-size: 19px; }.after-sale-faq h2 span { display: grid; place-items: center; width: 24px; height: 24px; border-radius: 50%; color: #fff; background: #1590f3; font-size: 16px; }.after-sale-faq button { display: flex; align-items: center; gap: 14px; width: 100%; height: 62px; padding: 0 7px; border: 0; border-bottom: 1px solid #edf3f8; color: #17345d; background: transparent; font: inherit; text-align: left; cursor: pointer; }.after-sale-faq button:last-child { border-bottom: 0; }.after-sale-faq button > .fa:first-child { display: grid; place-items: center; width: 38px; height: 38px; border-radius: 50%; color: #128ef3; background: #e6f4ff; font-size: 17px; }.after-sale-faq button:nth-of-type(2) > .fa:first-child { color: #f58a00; background: #fff3e4; }.after-sale-faq button:nth-of-type(3) > .fa:first-child { color: #15a956; background: #e7f8ed; }.after-sale-faq button strong { flex: 1; font-size: 15px; font-weight: 500; }.after-sale-faq button > .fa:last-child { color: #879bb4; font-size: 20px; }
.modal-overlay { position: fixed; inset: 0; z-index: 2000; display: grid; place-items: center; padding: 20px; background: rgba(15,39,74,.35); }.confirm-modal { position: relative; width: min(100%, 320px); padding: 26px 22px 20px; border-radius: 19px; background: #fff; box-shadow: 0 18px 50px rgba(22,56,97,.2); text-align: center; box-sizing: border-box; }.modal-close { position: absolute; top: 10px; right: 13px; border: 0; color: #9aaabd; background: transparent; font-size: 25px; cursor: pointer; }.modal-icon { display: grid; place-items: center; width: 48px; height: 48px; margin: 0 auto 13px; border-radius: 50%; color: #138ef4; background: #e9f6ff; font-size: 20px; }.confirm-modal h3 { font-size: 18px; }.confirm-modal p { margin: 9px 0 20px; color: #8296af; font-size: 13px; }.modal-actions { display: grid; grid-template-columns: 1fr 1fr; gap: 10px; }.modal-actions button { height: 40px; border-radius: 10px; font: inherit; font-size: 14px; font-weight: 700; cursor: pointer; }
@media (min-width: 601px) { .order-page { max-width: 600px; margin: 0 auto; } }
@media (max-width: 390px) { .order-page { padding-left: 12px; padding-right: 12px; }.order-tabs { margin-left: -7px; margin-right: -7px; }.order-tabs button { font-size: 12px; }:deep(.card-main) { grid-template-columns: 78px minmax(0, 1fr) auto; gap: 9px; padding-left: 11px; padding-right: 11px; }:deep(.card-main > img) { width: 78px; height: 78px; }:deep(.merchant-line strong) { font-size: 16px; }:deep(.card-side) { min-width: 75px; }:deep(.state-pill) { padding: 0 7px; font-size: 11px; }:deep(.card-side > strong) { font-size: 18px; }.card-actions { margin-left: 11px; margin-right: 11px; }.card-actions button { min-width: 0; padding: 0 12px; }.help-actions { gap: 6px; }.help-actions button { padding-left: 7px; padding-right: 7px; }.help-actions b { font-size: 12px; }.help-actions b small { font-size: 10px; }.recommend-card { grid-template-columns: 62px minmax(0, 1fr); gap: 7px; padding: 7px; }.recommend-card > img { width: 62px; height: 88px; } }
</style>

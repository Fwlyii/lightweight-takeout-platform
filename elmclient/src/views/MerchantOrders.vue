<template>
  <div class="wrapper merchant-orders-page merchant-ui">
    <header class="top-background"><h1>商家订单管理</h1><MerchantLogoutButton /></header>
    <div class="merchant-selector"><div class="merchant-tabs" aria-label="选择店铺">
      <p v-if="merchantList.length === 0" class="merchant-no-business">{{ loadingMerchants ? '正在加载店铺…' : '您还没有商铺哦' }}</p>
      <button v-for="merchant in merchantList" :key="merchant.merchantId" :class="{ active: selectedMerchantId === merchant.merchantId }" @click="selectMerchant(merchant.merchantId)">{{ merchant.merchantName }}</button>
    </div></div>
    <nav class="tabs" aria-label="订单状态"><button v-for="(t, i) in statusTabs" :key="t" :class="{ active: activeStatusTab === i }" @click="changeStatusTab(i)">{{ t }}<span v-if="orderCounts[i] > 0">({{ orderCounts[i] }})</span></button></nav>
    <div v-if="loading" class="loading"><p>正在加载订单…</p></div>
    <div v-else-if="filteredOrders.length === 0" class="empty-state"><img src="../assets/empty-order.png" alt=""><p>暂无订单</p></div>
    <ul v-else class="order-list">
      <li v-for="order in filteredOrders" :key="order.id" class="order-item">
        <div class="order-header">
          <i class="fas fa-clipboard-list order-icon"></i>
          <div><h2 class="order-id">订单号 <strong>{{ order.id }}</strong></h2><span class="order-time">下单时间：{{ formatTime(order.orderDate) }}</span></div>
          <span class="status-badge" :class="getStatusClass(order.orderState)">{{ getStatusText(order.orderState, order) }}</span>
        </div>
        <div class="order-content">
          <div class="customer-info">
            <p><i class="fas fa-user"></i><span>顾客</span><b>{{ order.customerName || '-' }}</b></p>
            <template v-if="order.serviceMode !== 'PICKUP'">
              <p><i class="fas fa-phone"></i><span>联系电话</span><b>{{ order.contactTel || '-' }}</b></p>
              <p><i class="fas fa-map-marker-alt"></i><span>配送地址</span><b>{{ order.address || '-' }}</b></p>
            </template>
            <p v-else class="pickup-label"><i class="fas fa-shopping-bag"></i><span>履约方式</span><b>到店自取</b></p>
            <p v-if="order.remarks" class="remark-line"><i class="fas fa-file-alt"></i><span>备注</span><b>{{ order.remarks }}</b></p>
          </div>
          <div class="order-items">
            <h3 class="items-title"><i class="fas fa-utensils"></i>商品明细</h3>
            <div v-for="(item, index) in order.foodList" :key="index" class="item-row">
              <span class="item-name">{{ item.foodName || '未知商品' }}</span><span class="item-quantity">¥{{ Number(item.foodPrice || 0).toFixed(2) }} × {{ item.quantity || 0 }}</span>
              <span class="item-price">¥ {{ (Number(item.foodPrice || 0) * Number(item.quantity || 0)).toFixed(2) }}</span>
            </div>
          </div>
          <div class="price-row"><i class="fas fa-truck"></i><span>{{ order.serviceMode === 'PICKUP' ? '无需配送费 · 到店自取' : '配送费' }}</span><b v-if="order.serviceMode !== 'PICKUP'">¥ {{ Number(order.deliveryPrice || 0).toFixed(2) }}</b></div>
          <div class="order-footer"><span><i class="fas fa-yen-sign"></i> 总计</span><strong class="order-total">¥ {{ Number(order.orderTotal || 0).toFixed(2) }}</strong></div>
        </div>
        <div class="actions">
          <template v-if="order.orderState === ORDER_STATUS.WAITING_MERCHANT_ACCEPT"><button class="cancel-btn" :disabled="submitting" @click.stop="rejectOrder(order.id)">拒单</button><button class="confirm-btn" :disabled="submitting" @click.stop="acceptOrder(order.id)">接单</button></template>
          <button v-else-if="order.orderState === ORDER_STATUS.WAITING_DISPATCH" class="confirm-btn" :disabled="submitting" @click.stop="readyOrder(order.id)">确认出餐</button>
          <div v-else-if="![ORDER_STATUS.COMPLETED, ORDER_STATUS.CANCELLED].includes(order.orderState)" class="fulfillment-tip"><i class="fas fa-route"></i> {{ fulfillmentTip(order.orderState, order) }}</div>
        </div>
      </li>
    </ul>
    <Teleport to="body">
      <div v-if="showAcceptModal || showRejectModal" class="merchant-order-modal-overlay" @click.self="closeModal">
        <section class="merchant-order-dialog" role="dialog" aria-modal="true" aria-labelledby="merchant-confirm-title">
          <header class="dialog-heading"><span class="dialog-icon"><i class="fas fa-clipboard-list"></i><i class="fas fa-check-circle"></i></span><div><h2 id="merchant-confirm-title">{{ showAcceptModal ? '确认接单' : '确认拒单' }}</h2><p>{{ showAcceptModal ? '请确认订单信息，是否接单？' : '请确认订单信息，是否拒绝此订单？' }}</p></div><button class="dialog-close" :disabled="submitting" aria-label="关闭确认弹窗" @click="closeModal">×</button></header>
          <dl v-if="selectedOrder" class="confirmation-details">
            <div><dt><i class="fas fa-file-alt"></i>订单号</dt><dd>{{ selectedOrder.id }}</dd></div>
            <div><dt><i class="fas fa-shopping-bag"></i>履约方式</dt><dd>{{ selectedOrder.serviceMode === 'PICKUP' ? '到店自取' : '配送到家' }}</dd></div>
            <div><dt><i class="fas fa-file-alt"></i>顾客备注</dt><dd>{{ selectedOrder.remarks || '无备注' }}</dd></div>
          </dl>
          <p class="confirmation-note"><i class="fas fa-info-circle"></i>{{ showAcceptModal ? '请确认可正常备餐后接单' : '拒单后订单将取消，请谨慎确认' }}</p>
          <div class="dialog-actions"><button class="cancel-btn" :disabled="submitting" @click="closeModal">{{ showAcceptModal ? '暂不接单' : '暂不拒单' }}</button><button class="confirm-btn" :class="{ 'reject-confirm': showRejectModal }" :disabled="submitting" @click="showAcceptModal ? confirmAccept() : confirmReject()">{{ submitting ? '提交中…' : showAcceptModal ? '确认接单' : '确认拒单' }}</button></div>
        </section>
      </div>
    </Teleport>
  </div>
</template>
<script>
import { ref, onMounted, computed,onUnmounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import request from '../utils/request';
import { toast } from '../utils/toast';
import { createRealtimeConnection } from '../services/realtimeService';
import { MERCHANT_ORDER_GROUPS, ORDER_STATUS, orderStatusClass, orderStatusText } from '../utils/orderPresentation';
import { formatDateTime } from '../utils/formatters';
import MerchantLogoutButton from '../components/MerchantLogoutButton.vue';

export default {
  name: 'BusinessOrderManage',
  components: { MerchantLogoutButton },
  setup() {
    const router = useRouter();
    const route = useRoute();
    const loading = ref(false);
    const orders = ref([]);
    const businessId = ref(1);

    // 新增：商铺列表和选中状态
    const merchantList = ref([]);
    const selectedMerchantId = ref(null);
    const loadingMerchants = ref(false);

    // 订单状态标签
    const statusTabs = ref(['全部', '待接单', '待骑手', '配送中', '已完成', '已取消']);
    const activeStatusTab = ref(0);

    // 弹窗相关状态
    const showAcceptModal = ref(false);
    const showRejectModal = ref(false);
    const selectId = ref(0);
    const submitting = ref(false);
    const selectedOrder = computed(() => orders.value.find(order => order.id === selectId.value));

    let realtimeConnection = null;

    // 获取商铺列表
    const fetchMerchantList = async () => {
      loadingMerchants.value = true;
      try {
        const response = await request.get("/api/businesses/id_list");
        if (response.success) {
          merchantList.value = response.data || [];
          // 默认选择第一个商铺
          if (merchantList.value.length > 0) {
            selectedMerchantId.value = merchantList.value[0].merchantId;
            businessId.value = selectedMerchantId.value;
          }
        } else {
          console.error('获取商铺列表失败:', response.message);
        }
      } catch (error) {
        console.error("请求商铺列表失败:", error);
      } finally {
        loadingMerchants.value = false;
      }
    };

    // 选择商铺
    const selectMerchant = (merchantId) => {
      selectedMerchantId.value = merchantId;
      businessId.value = merchantId;
      fetchOrders(); // 重新加载该商铺的订单
    };

    // 获取订单列表
    const fetchOrders = async ({ silent = false } = {}) => {
      if (!selectedMerchantId.value) return;

      if (!silent) loading.value = true;
      try {
        const response = await request.get("/api/orders/list/business", {
          params: { businessId: businessId.value }
        });

        if (response.success) {
          orders.value = response.data || [];
        } else {
          if (!silent) toast.error("获取订单列表失败");
        }
      } catch (error) {
        if (!silent) toast.error("获取订单列表失败");
      } finally {
        if (!silent) loading.value = false;
      }
    };

    // 切换状态标签
    const changeStatusTab = (index) => {
      activeStatusTab.value = index;
    };

    // 计算各状态订单数量
    const orderCounts = computed(() => {
      const counts = [0, 0, 0, 0, 0, 0];

      orders.value.forEach(order => {
        counts[0]++; // 全部
        if (MERCHANT_ORDER_GROUPS.waitingAccept.includes(order.orderState)) counts[1]++;
        else if (MERCHANT_ORDER_GROUPS.waitingRider.includes(order.orderState)) counts[2]++;
        else if (MERCHANT_ORDER_GROUPS.fulfilling.includes(order.orderState)) counts[3]++;
        else if (MERCHANT_ORDER_GROUPS.completed.includes(order.orderState)) counts[4]++;
        else if (MERCHANT_ORDER_GROUPS.cancelled.includes(order.orderState)) counts[5]++;
      });

      return counts;
    });

    // 过滤订单
    const filteredOrders = computed(() => {
      if (activeStatusTab.value === 0) return orders.value; // 全部

      const groups = {
        1: MERCHANT_ORDER_GROUPS.waitingAccept,
        2: MERCHANT_ORDER_GROUPS.waitingRider,
        3: MERCHANT_ORDER_GROUPS.fulfilling,
        4: MERCHANT_ORDER_GROUPS.completed,
        5: MERCHANT_ORDER_GROUPS.cancelled
      };
      return orders.value.filter(order => groups[activeStatusTab.value].includes(order.orderState));
    });

    // 获取状态文本
    const getStatusText = (state, order = {}) => {
      return orderStatusText(state, order, 'merchant');
    };

    // 获取状态样式类
    const getStatusClass = (state) => {
      return orderStatusClass(state);
    };

    // 格式化时间
    const formatTime = (timeString) => {
      return formatDateTime(timeString);
    };

    // 接单
    const acceptOrder = (id) => {
      selectId.value = id;
      showAcceptModal.value = true;
    };

    // 确认接单
    const confirmAccept = async () => {
      if (selectId.value === 0 || submitting.value) return;
      submitting.value = true;

      try {
        const response = await request.post(`/api/v1/orders/${selectId.value}/merchant-accept`);
        if (response.success) {
          toast.success("接单成功");
          fetchOrders(); // 重新加载订单
        } else {
          toast.error("接单失败: " + response.message);
        }
      } catch (error) {
        toast.error("接单失败，请稍后重试");
      } finally {
        submitting.value = false;
        closeModal();
      }
    };

    const readyOrder = async (id) => {
      if (submitting.value) return;
      submitting.value = true;
      try {
        const response = await request.post(`/api/v1/orders/${id}/merchant-ready`);
        if (response.success) { toast.success(orderById(id)?.serviceMode === 'PICKUP' ? '已确认出餐，等待顾客到店取餐' : '已确认出餐，配送任务已发布'); fetchOrders(); }
        else toast.error(response.message || '确认出餐失败');
      } catch (error) { toast.error(error.response?.data?.message || '确认出餐失败，请稍后重试'); }
      finally { submitting.value = false; }
    };

    // 拒单
    const rejectOrder = (id) => {
      selectId.value = id;
      showRejectModal.value = true;
    };

    // 确认拒单
    const confirmReject = async () => {
      if (selectId.value === 0 || submitting.value) return;
      submitting.value = true;

      try {
        const response = await request.post(`/api/v1/orders/${selectId.value}/merchant-reject`);
        if (response.success) {
          toast.success("拒绝成功");
          fetchOrders(); // 重新加载订单
        } else {
          toast.error(response.message);
        }
      } catch (error) {
        toast.error("拒绝失败，请重试");
      } finally {
        submitting.value = false;
        closeModal();
      }
    };

    const orderById = id => orders.value.find(item => item.id === id);
    const fulfillmentTips = {
      [ORDER_STATUS.WAITING_DISPATCH]: '餐品制作中，完成后请确认出餐',
      [ORDER_STATUS.WAITING_RIDER_ACCEPT]: '任务已进入骑手大厅',
      [ORDER_STATUS.WAITING_PICKUP]: '骑手正在前往商家',
      [ORDER_STATUS.DELIVERING]: '骑手配送中',
      [ORDER_STATUS.DELIVERED]: '等待顾客确认',
      [ORDER_STATUS.COMPLETED]: '订单已闭环',
      [ORDER_STATUS.CANCELLED]: '订单已取消',
      [ORDER_STATUS.DELIVERY_EXCEPTION]: '调度员正在处理'
    };
    const fulfillmentTip = (state, order = {}) => {
      if (state === ORDER_STATUS.WAITING_PICKUP && order.serviceMode === 'PICKUP') return '等待顾客到店取餐';
      return fulfillmentTips[state] || '履约中';
    };

    // 关闭弹窗
    const closeModal = () => {
      if (submitting.value) return;
      showAcceptModal.value = false;
      showRejectModal.value = false;
      selectId.value = 0;
    };

    // 预留订单详情入口，当前卡片操作不调用。
    const goDetail = (order) => {
      router.push({
        path: '/orderDetail',
        query: { orderId: order.id }
      });
    };

    onMounted(() => {
      realtimeConnection = createRealtimeConnection({
        onMessage: () => fetchOrders({ silent: true }),
        onFallbackRefresh: () => fetchOrders({ silent: true })
      });
      realtimeConnection.start();
      // 先获取商铺列表，然后自动加载第一个商铺的订单
      fetchMerchantList().then(() => {
        if (selectedMerchantId.value) {
          fetchOrders();
        }
      });
    });
    onUnmounted(() => {
      realtimeConnection?.stop();
    });

    return {
      loading,
      loadingMerchants,
      selectedOrder,
      submitting,
      orders,
      merchantList,
      selectedMerchantId,
      statusTabs,
      activeStatusTab,
      orderCounts,
      filteredOrders,
      fetchOrders,
      selectMerchant,
      changeStatusTab,
      getStatusText,
      getStatusClass,
      formatTime,
      acceptOrder,
      rejectOrder,
      showAcceptModal,
      showRejectModal,
      closeModal,
      confirmAccept,
      confirmReject,
      ORDER_STATUS,
      fulfillmentTip,
      readyOrder,
      goDetail
    };
  }
};
</script>

<template>
  <div class="container">
    <div class="card">
      <div class="header-section">
        <div v-if="receiptState === 'paid'" class="icon-section">
          <svg
            xmlns="http://www.w3.org/2000/svg"
            viewBox="0 0 24 24"
            fill="none"
            stroke="currentColor"
            stroke-width="2"
            stroke-linecap="round"
            stroke-linejoin="round"
            class="check-icon"
          >
            <path d="M22 11.08V12a10 10 0 1 1-5.93-8.82"></path>
            <polyline points="22 4 12 14.01 9 11.01"></polyline>
          </svg>
        </div>
        <h2 class="title">{{ loading ? '正在核实订单…' : receiptState === 'paid' ? '支付成功' : receiptState === 'unpaid' ? '订单尚未支付' : receiptState === 'cancelled' ? '订单已取消' : '无法确认支付结果' }}</h2>
        <p v-if="error" role="alert">{{ error }}</p>
      </div>
      <div v-if="!loading && !error" class="details">
        <div class="detail-item">
          <span class="label">商家名称</span>
          <span class="value">{{ paymentDetails.businessName || '未知商家' }}</span>
        </div>
        <div class="detail-item">
          <span class="label">{{ receiptState === 'paid' ? '支付金额' : '订单金额' }}</span>
          <span class="value amount">¥{{ Number(paymentDetails.orderTotal || 0).toFixed(2) }}</span>
        </div>
        <div class="detail-item">
          <span class="label">下单时间</span>
          <span class="value">{{ paymentDetails.orderDate }}</span>
        </div>
      </div>
      <div class="actions">
        <button v-if="error" @click="load" class="btn-back" :disabled="loading">重新查询</button>
        <button v-if="receiptState === 'unpaid'" @click="router.replace({ path: '/payment', query: { orderId } })" class="btn-back">继续支付</button>
        <button @click="goBack" class="btn-back">去查看订单</button>
      </div>
    </div>
  </div>
</template>

<script>
import { onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import request from '@/utils/request';
import { loadPaymentReceipt } from '../utils/paymentReceipt';

export default {
  setup() {
    const route = useRoute();
    const router = useRouter();
    const paymentDetails = ref({});
    const loading = ref(true), error = ref(''), receiptState = ref('');
    
    const orderId = ref(route.query.orderId);

    const load = async () => {
      loading.value = true; error.value = ''; receiptState.value = '';
      try {
        const receipt = await loadPaymentReceipt(request, orderId.value);
        paymentDetails.value = receipt.order;
        receiptState.value = receipt.state;
      } catch (e) { error.value = e.response?.data?.message || e.message || '订单查询失败'; }
      finally { loading.value = false; }
    };
    onMounted(load);

    const goBack = () => {
      router.replace('/orderList');
    };

    return {
      paymentDetails,
      loading, error, receiptState, load, router, orderId,
      goBack,
    };
  }
};
</script>

<style scoped>
/* 全局和基础容器 */
:root {
  --primary-color: #2e7d32;
  --secondary-color: #f5f7fa;
  --text-color-primary: #1a202c;
  --text-color-secondary: #4a5568;
  --card-bg-color: #ffffff;
  --button-color: #2563eb;
  --button-hover-color: #1e40af;
}

html, body {
  margin: 0;
  padding: 0;
  height: 100%;
  font-family: -apple-system, BlinkMacSystemFont, "Segoe UI", "Roboto", "Oxygen", "Ubuntu", "Cantarell", "Fira Sans", "Droid Sans", "Helvetica Neue", "PingFang SC", "Microsoft YaHei", sans-serif;
  background-color: var(--secondary-color);
}

.container {
  display: flex;
  justify-content: center;
  align-items: center;
  min-height: 100vh;
  padding: 1.5rem;
  box-sizing: border-box;
}

/* 核心卡片样式 */
.card {
  background-color: var(--card-bg-color);
  border-radius: 20px;
  box-shadow: 0 12px 24px rgba(0, 0, 0, 0.05), 0 4px 8px rgba(0, 0, 0, 0.02);
  padding: 2.5rem 2rem;
  width: 100%;
  max-width: 420px;
  text-align: center;
  display: flex;
  flex-direction: column;
  gap: 2rem;
}

/* 头部：图标和标题 */
.header-section {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 1.25rem;
}

/* 修改后的图标样式 */
.icon-section {
  display: flex;
  justify-content: center;
  align-items: center;
  width: 90px;
  height: 90px;
  border-radius: 50%;
  background-color: #e6f6e8;
  animation: scale-in 0.5s cubic-bezier(0.25, 0.46, 0.45, 0.94) both;
}

.check-icon {
  width: 55px;
  height: 55px;
  color: #2e7d32;
  animation: fade-in 0.8s ease-out 0.2s both;
}

.title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-color-primary);
  margin: 0;
  white-space: nowrap;
  animation: slide-up 0.6s ease-out 0.3s both;
}

/* 交易详情 */
.details {
  display: flex;
  flex-direction: column;
  gap: 1rem;
  text-align: left;
  border-top: 1px solid #e2e8f0;
  padding-top: 1.5rem;
}

.detail-item {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 1rem;
}

.label {
  color: var(--text-color-secondary);
  font-weight: 500;
}

.value {
  color: var(--text-color-primary);
  font-weight: 600;
}

.value.amount {
  font-size: 1.25rem;
  color: var(--primary-color);
  font-weight: 700;
}

/* 按钮 */
.actions {
  margin-top: 0.5rem;
}

.btn-back {
  width: 100%;
  padding: 1rem;
  background-color:  #0493f2da;
  color: #e2e8f0;
  border: none;
  border-radius: 12px;
  font-size: 1rem;
  font-weight: 600;
  cursor: pointer;
  transition: background-color 0.3s ease, box-shadow 0.3s ease;
  box-shadow: 0 4px 12px rgba(37, 99, 235, 0.2);
}

.btn-back:hover {
  background-color: var(--button-hover-color);
  box-shadow: 0 6px 16px rgba(37, 99, 235, 0.3);
}

/* 动画效果 */
@keyframes scale-in {
  from {
    transform: scale(0.8);
    opacity: 0;
  }
  to {
    transform: scale(1);
    opacity: 1;
  }
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes slide-up {
  from {
    transform: translateY(20px);
    opacity: 0;
  }
  to {
    transform: translateY(0);
    opacity: 1;
  }
}

/* 媒体查询：适配大屏幕设备 */
@media (min-width: 600px) {
  .card {
    padding: 3rem;
  }
  .title {
    font-size: 2.5rem;
  }
}
</style>

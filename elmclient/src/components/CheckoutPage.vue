<template>
  <ProfilePage title="确认订单" :back-to="backTo">
    <p v-if="loading" role="status">正在核对待结算商品…</p>
    <div v-if="error" class="notice error" role="alert">{{ error }}
      <button v-if="!submitting" type="button" @click="load">重新加载</button>
    </div>
    <template v-if="ready">
      <section class="panel">
        <h2>{{ business.businessName }}</h2>
        <p>{{ context.serviceMode === 'pickup' ? '到店自取 · 无需收货地址' : '商家外送' }}</p>
        <p v-if="context.serviceMode === 'pickup'" class="muted">取餐地点：{{ business.businessAddress }}</p>
        <ul class="checkout-items">
          <li v-for="item in items" :key="item.id"><span>{{ item.foodName }} × {{ item.quantity }}</span>
            <strong>¥{{ money(Number(item.foodPrice) * Number(item.quantity)) }}</strong></li>
        </ul>
        <p>商品小计：¥{{ money(subtotal) }}</p>
        <p>配送费：¥{{ money(context.serviceMode === 'pickup' ? 0 : business.deliveryPrice) }}</p>
        <p class="muted">商家优惠、会员折扣及最终金额由服务器核算，提交后在支付页确认。</p>
      </section>
      <section v-if="context.serviceMode === 'delivery'">
        <h2>选择收货地址</h2>
        <p v-if="selectedAddress" class="notice" role="status">已选：{{ selectedAddress.contactName }} · {{ selectedAddress.address }}</p>
        <AddressManager selectable :disabled="submitting" :selected-id="selectedAddress?.id"
          :checkout-query="checkoutQuery(context)" @select="selectAddress" @loaded="syncAddresses" />
      </section>
      <p v-if="submitError" class="notice error" role="alert">{{ submitError }}</p>
      <button class="primary wide" type="button" :disabled="submitting || (context.serviceMode === 'delivery' && !selectedAddress)"
        @click="submit">{{ submitting ? '正在提交…' : '提交订单，去支付' }}</button>
      <p v-if="context.serviceMode === 'delivery' && !selectedAddress" class="muted">请选择上方地址；没有地址时可先新增。</p>
    </template>
  </ProfilePage>
</template>
<script setup>
import { computed, onMounted, ref } from 'vue';
import { useRoute, useRouter } from 'vue-router';
import ProfilePage from './ProfilePage.vue';
import AddressManager from './AddressManager.vue';
import request from '../utils/request';
import { listCartItems } from '../services/cartService';
import { checkoutContext, checkoutQuery, checkoutItems, createOrderSubmitter } from '../utils/checkout';
const route = useRoute(), router = useRouter();
const context = ref(null), business = ref({}), items = ref([]), selectedAddress = ref(null);
const loading = ref(true), ready = ref(false), error = ref(''), submitError = ref(''), submitting = ref(false);
const submitOrder = createOrderSubmitter(request);
const subtotal = computed(() => items.value.reduce((sum, item) => sum + Number(item.foodPrice) * Number(item.quantity), 0));
const backTo = computed(() => context.value ? { path: '/cart', query: checkoutQuery(context.value) } : '/cart');
const money = value => Number(value || 0).toFixed(2);
const selectAddress = address => { selectedAddress.value = address; submitError.value = ''; };
const syncAddresses = addresses => {
  selectedAddress.value = addresses.find(address => address.id === selectedAddress.value?.id)
    || addresses.find(address => address.isDefault) || null;
};
async function load() {
  loading.value = true; ready.value = false; error.value = '';
  try {
    context.value = checkoutContext(route.query);
    if (!context.value) throw new Error('缺少结算信息，请返回购物车重新选择');
    const [response, cart] = await Promise.all([
      request.get(`/api/businesses/public/${context.value.businessId}`), listCartItems(context.value.businessId)
    ]);
    if (!response?.success || !response.data) throw new Error(response?.message || '商家信息加载失败');
    business.value = response.data;
    if (business.value.operatingStatus === false) throw new Error('商家当前休息中，暂时无法下单');
    items.value = checkoutItems(cart || [], context.value);
    if (context.value.serviceMode === 'delivery' && subtotal.value < Number(business.value.startPrice || 0)) {
      throw new Error(`未达到起送价 ¥${money(business.value.startPrice)}，请返回购物车继续选购`);
    }
    ready.value = true;
  } catch (e) { error.value = e.response?.data?.message || e.message || '结算信息加载失败'; }
  finally { loading.value = false; }
}
async function submit() {
  if (submitting.value || !ready.value) return;
  submitting.value = true; submitError.value = '';
  try {
    const id = await submitOrder(context.value, selectedAddress.value?.id);
    await router.replace({ path: '/payment', query: { orderId: id } });
  } catch (e) { submitError.value = e.response?.data?.message || e.message || '下单失败，请重试'; }
  finally { submitting.value = false; }
}
onMounted(load);
</script>
<style scoped>
.checkout-items { list-style:none; padding:0; }
.checkout-items li { display:flex; justify-content:space-between; gap:16px; padding:8px 0; border-bottom:1px solid #eef2f5; }
</style>

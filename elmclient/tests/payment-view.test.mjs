import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { ref, computed } from 'vue';
import { positiveId } from '../src/utils/checkout.js';
import { ORDER_STATUS } from '../src/utils/orderPresentation.js';

// Exercise the production setup, including reactive quote and submit guards.
const source = (await readFile(new URL('../src/views/Payment.vue', import.meta.url), 'utf8'))
  .match(/<script>([\s\S]*?)<\/script>/)[1].replace(/^import .*;\s*$/gm, '').replace('export default', 'return');
function view(overrides = {}) {
  const mounted = [], unmounted = [], calls = [], navigations = [];
  const request = {
    get: async path => ({ success: true, data: path === '/api/orders/detail'
      ? { orderState: ORDER_STATUS.WAITING_PAYMENT, orderTotal: 50, deliveryPrice: 2, foodList: [] }
      : path === '/api/v1/assets/me' ? { balance: 100, points: 2000 }
        : [{ id: 1, minOrderAmount: 20, discountAmount: 5 }] }),
    put: async (...args) => { calls.push(args); return { success: true }; }, ...overrides
  };
  const api = new Function('ref','computed','onMounted','onBeforeUnmount','useRoute','useRouter','request','toast','ORDER_STATUS','positiveId','PageHeader',source)(
    ref, computed, fn => mounted.push(fn), fn => unmounted.push(fn), () => ({query:{orderId:'9'}}),
    () => ({replace: location => navigations.push(location)}), request,
    {warning(){},error(){}}, ORDER_STATUS, positiveId, {}
  ).setup();
  return { api, request, calls, navigations, unmount: () => unmounted.forEach(fn => fn()),
    async load() { mounted.forEach(fn => fn()); await new Promise(resolve => setImmediate(resolve)); } };
}
test('payment quote clamps negative, non-numeric, excessive and fractional point inputs', async () => {
  const v = view(); await v.load();
  assert.equal(v.api.payableAmount.value, '45.00');
  for (const [input, expected] of [[-100,'45.00'], ['bad','45.00'], [199,'44.00'], [99999,'36.00']]) {
    v.api.pointsToUse.value = input;
    assert.equal(v.api.payableAmount.value, expected);
  }
  v.api.normalizePoints(); assert.equal(v.api.pointsToUse.value, 900);
});
test('retrying an order load also reloads usable coupons', async () => {
  const v = view(); await v.load(); v.api.availableCoupons.value = [];
  await v.api.fetchOrderDetails();
  assert.equal(v.api.selectedCouponId.value, 1); assert.equal(v.api.payableAmount.value, '45.00');
});
test('payment is single-flight and coupon selection freezes while submitting', async () => {
  const v = view(); await v.load(); let finish;
  v.request.put = async (...args) => { v.calls.push(args); return new Promise(resolve => { finish = resolve; }); };
  const first = v.api.handlePayment(); await v.api.handlePayment();
  v.api.selectCoupon(null); assert.equal(v.api.selectedCouponId.value, 1);
  assert.equal(v.calls.length, 1); finish({success:true}); await first;
  assert.equal(v.navigations.length, 1);
});
test('insufficient wallet balance and paid orders cannot submit a payment', async () => {
  const v = view(); await v.load(); v.api.selectedPayment.value = 'wallet'; v.api.assetInfo.value.balance = 1;
  await v.api.handlePayment(); assert.equal(v.calls.length, 0);
  v.api.selectedPayment.value = 'alipay'; v.api.orderDetail.value.orderState = ORDER_STATUS.WAITING_MERCHANT_ACCEPT;
  await v.api.handlePayment(); assert.equal(v.calls.length, 0);
});
test('a late payment response cannot navigate away from a newer page', async () => {
  const v = view(); await v.load(); let finish;
  v.request.put = () => new Promise(resolve => { finish = resolve; });
  const pending = v.api.handlePayment(); v.unmount(); finish({success:true}); await pending;
  assert.deepEqual(v.navigations, []);
});

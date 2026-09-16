import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { ref, computed } from 'vue';
import { parse, compileTemplate } from '@vue/compiler-sfc';
import { MERCHANT_ORDER_GROUPS, ORDER_STATUS, orderStatusClass, orderStatusText } from '../src/utils/orderPresentation.js';
const read = name => readFile(new URL('../src/views/' + name + '.vue', import.meta.url), 'utf8');
const sources = Object.fromEntries(await Promise.all(['MerchantBusiness', 'MerchantOrders', 'MerchantProfile'].map(async name => [name, await read(name)])));
function mount(name, extra = {}) {
  const mounts = [];
  const bindings = { ref, computed, onMounted: fn => mounts.push(fn), onUnmounted() {},
    useRouter: () => ({ push() {}, replace() {} }), useRoute: () => ({ query: {} }),
    MerchantLogoutButton: {}, MerchantActionLabel: {}, merchantIndicator: {}, pinMerchantCard() {}, releaseMerchantCard() {}, getToken: () => 'fixture', clearAuth() {}, DEFAULT_AVATAR_URL: '/avatar.png',
    toast: { warning() {}, error() {}, success() {} },
    createRealtimeConnection: () => ({ start() {}, stop() {} }),
    MERCHANT_ORDER_GROUPS, ORDER_STATUS, orderStatusClass, orderStatusText, formatDateTime: String,
    Swal: { fire: async () => ({}) }, listMyBusinesses: async () => [],
    request: { get: async () => ({ success: true, data: [] }) }, ...extra };
  const script = parse(sources[name]).descriptor.script.content.replace(/^import .*;\r?\n/gm, '').replace('export default', 'return');
  const view = new Function(...Object.keys(bindings), script)(...Object.values(bindings)).setup();
  return { view, mounts };
}
test('merchant templates compile and referenced cropped assets are shipped', async () => {
  for (const [name, source] of Object.entries(sources)) {
    const { descriptor, errors } = parse(source); assert.deepEqual(errors, []);
    assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: name, id: name }).errors, []);
  }
  for (const asset of ['profile-shop', 'empty-shop']) {
    const data = await readFile(new URL('../public/images/merchant/' + asset + '.jpg', import.meta.url));
    assert.equal(data.subarray(0, 2).toString('hex'), 'ffd8');
  }
});
test('workbench fetches existing stores on first entry, then filters without falsifying summary counts', async () => {
  let reads = 0;
  const { view, mounts } = mount('MerchantBusiness', { listMyBusinesses: async () => { reads++; return [{ id: 1, status: 1 }, { id: 2, status: 0 }, { id: 3, status: 2 }]; } });
  mounts.forEach(fn => fn()); await Promise.resolve();
  assert.equal(reads, 1); assert.equal(view.shops.value.length, 3);
  view.changeTab(0); assert.equal(view.filteredShops.value.length, 1);
  assert.equal(view.approvedCount.value, 1); assert.equal(view.pendingCount.value, 1);
});
test('store-load failures surface a retryable error instead of a false empty success', async () => {
  const { view } = mount('MerchantBusiness', { listMyBusinesses: async () => { throw Error('offline'); } });
  await view.loadShops(); assert.match(view.errorMessage.value, /获取商铺列表失败/); assert.equal(view.loading.value, false);
});
test('successful application refreshes all stores rather than narrowing the stored summary to the current tab', async () => {
  let config, requestedStatus, payload;
  const values = { businessName: '测试店铺', businessAddress: '天津', businessExplain: '', deliveryPrice: 0, startPrice: 0, orderTypeId: 1 };
  const { view } = mount('MerchantBusiness', {
    Swal: { fire: async options => { config = options; return { value: values }; } },
    request: { post: async (path, body) => { assert.equal(path, '/api/permission/apply-shop'); payload = body; return { success: true }; } },
    listMyBusinesses: async status => { requestedStatus = status; return [{ id: 1, status: 1 }, { id: 2, status: 0 }]; }
  });
  view.changeTab(0); await view.applyNewShop();
  assert.deepEqual(payload, values); assert.equal(requestedStatus, null); assert.equal(view.shops.value.length, 2);
  assert.equal(config.customClass.popup, 'merchant-shop-popup');
  assert.match(config.html, /maxlength="10"/); assert.match(config.html, /maxlength="15"/);
  assert.doesNotMatch(config.html + config.footer, /当前位置|1个工作日|30字|200字/);
});
test('merchant profile counts only approved and operating stores as open', () => {
  const { view } = mount('MerchantProfile');
  view.stores.value = [{ status: 1, operatingStatus: true }, { status: 1, operatingStatus: false }, { status: 0, operatingStatus: true }];
  assert.equal(view.operatingStoreCount.value, 1); assert.equal(view.pendingStoreCount.value, 1);
});
test('acceptance dialog derives the selected order and cancellation sends no mutation', () => {
  const { view } = mount('MerchantOrders');
  view.orders.value = [{ id: 1034, orderState: 1, remarks: '少辣', serviceMode: 'PICKUP' }];
  view.acceptOrder(1034); assert.equal(view.selectedOrder.value.remarks, '少辣'); assert.equal(view.showAcceptModal.value, true);
  view.closeModal(); assert.equal(view.showAcceptModal.value, false); assert.equal(view.selectedOrder.value, undefined);
  assert.doesNotMatch(sources.MerchantOrders, /约 15 分钟|预计制作时间/);
});
test('merchant acceptance remains single-flight, rejects close while pending, and retains the existing endpoint', async () => {
  let release, calls = 0;
  const { view } = mount('MerchantOrders', { request: { post: async path => { assert.equal(path, '/api/v1/orders/1034/merchant-accept'); calls++; await new Promise(resolve => { release = resolve; }); return { success: true }; } } });
  view.acceptOrder(1034); const pending = view.confirmAccept();
  await view.confirmAccept(); view.closeModal(); assert.equal(calls, 1); assert.equal(view.showAcceptModal.value, true);
  release(); await pending; assert.equal(view.submitting.value, false); assert.equal(view.showAcceptModal.value, false);
});
test('failed accept or reject releases controls; ready still uses its original contract', async () => {
  const calls = [];
  const { view } = mount('MerchantOrders', { request: { post: async path => { calls.push(path); throw Error('offline'); } } });
  view.acceptOrder(3); await view.confirmAccept(); assert.equal(view.submitting.value, false);
  view.rejectOrder(4); await view.confirmReject(); assert.equal(view.showRejectModal.value, false);
  await view.readyOrder(5); assert.equal(view.submitting.value, false);
  assert.deepEqual(calls, ['/api/v1/orders/3/merchant-accept', '/api/v1/orders/4/merchant-reject', '/api/v1/orders/5/merchant-ready']);
});
test('ready button feedback follows the actual request and preserves single-flight behavior', async () => {
  let release, count = 0;
  const { view } = mount('MerchantOrders', { request: { post: async path => {
    assert.equal(path, '/api/v1/orders/5/merchant-ready'); count++;
    return new Promise(resolve => { release = resolve; });
  } } });
  const pending = view.readyOrder(5);
  assert.equal(view.readyId.value, 5);
  await view.readyOrder(6); assert.equal(count, 1);
  release({ success: false, message: 'retry' }); await pending;
  assert.equal(view.readyId.value, null); assert.equal(view.submitting.value, false);
});
test('new order highlight is limited to unseen pending orders, not every refresh', async () => {
  const data = [{ id: 11, orderState: 1 }, { id: 12, orderState: 2 }];
  const { view } = mount('MerchantOrders', { request: { get: async () => ({ success: true, data }) } });
  view.selectMerchant(1); await view.fetchOrders();
  assert.deepEqual([...view.freshOrderIds.value], [11]);
  view.finishOrderAttention(11, { animationName: 'merchant-order-attention' });
  await view.fetchOrders(); assert.equal(view.freshOrderIds.value.size, 0);
});

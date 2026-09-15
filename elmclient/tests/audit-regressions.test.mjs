import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { ref, computed } from 'vue';
import { parse } from '@vue/compiler-sfc';
import * as presentation from '../src/utils/orderPresentation.js';
import { getBusinessDistanceKm, getBusinessDeliveryMinutes } from '../src/utils/businessPresentation.js';
const read = name => readFileSync(new URL(`../src/views/${name}.vue`, import.meta.url), 'utf8');
const strip = source => source.replace(/import\s+[\s\S]*?from\s+['"][^'"]+['"];?/g, '');
const tick = () => new Promise(setImmediate);
function orders() {
  const pending = [], unmount = [];
  const bindings = { MerchantLogoutButton: {}, ref, computed, onMounted() {}, onUnmounted: fn => unmount.push(fn),
    useRouter: () => ({}), useRoute: () => ({ query: {} }), toast: { error() {}, success() {}, warning() {} },
    createRealtimeConnection: () => ({}), formatDateTime: String, ...presentation,
    request: { get: (url, config) => new Promise(resolve => pending.push({ resolve, id: config.params.businessId })) } };
  const component = new Function(...Object.keys(bindings), strip(parse(read('MerchantOrders')).descriptor.script.content).replace('export default', 'return'))(...Object.values(bindings));
  return { view: component.setup(), pending, unmount };
}
test('merchant order responses cannot cross shop switches', async () => {
  const { view, pending } = orders(); view.selectMerchant(1); view.selectMerchant(2);
  pending[1].resolve({ success: true, data: [{ id: 202, businessId: 2 }] }); await tick();
  pending[0].resolve({ success: true, data: [{ id: 101, businessId: 1 }] }); await tick();
  assert.equal(view.orders.value[0].businessId, 2);
});
test('a failed shop switch never displays previous shop orders', async () => {
  const { view, pending } = orders(); view.selectMerchant(1);
  pending[0].resolve({ success: true, data: [{ id: 101 }] }); await tick();
  view.selectMerchant(2); pending[1].resolve({ success: false }); await tick();
  assert.deepEqual(view.orders.value, []);
});
test('merchant can load reviews of the second owned shop', async () => {
  const calls = [], bindings = { ref, computed, onMounted() {}, onUnmounted() {}, toast: { warning() {}, success() {}, error() {} },
    request: { get: async url => { calls.push(url); return { success: true, data: url.endsWith('id_list') ? [{ merchantId: 1 }, { merchantId: 2 }] : [{ id: 22 }] }; } } };
  const source = strip(parse(read('MerchantReviews')).descriptor.scriptSetup.content);
  const view = new Function(...Object.keys(bindings), source + ';return {load, selectShop: typeof selectShop === "undefined" ? null : selectShop, reviews};')(...Object.values(bindings));
  await view.load(); assert.equal(typeof view.selectShop, 'function');
  await view.selectShop(2); assert.ok(calls.includes('/api/v1/reviews/business/2'));
  assert.equal(view.reviews.value[0].id, 22);
});
test('missing or invalid distance and ETA remain unknown, while zero distance is valid', () => {
  assert.equal(getBusinessDistanceKm({ id: 1 }), null);
  assert.equal(getBusinessDeliveryMinutes({ id: 1 }), null);
  assert.equal(getBusinessDistanceKm({ distanceKm: 0 }), 0);
  assert.equal(getBusinessDistanceKm({ distanceKm: -1 }), null);
  assert.equal(getBusinessDeliveryMinutes({ deliveryMinutes: 'bad' }), null);
  assert.equal(getBusinessDistanceKm({ distanceKm: 1.5 }), 1.5);
});
test('catalog pages label unavailable distance and delivery estimates honestly', () => {
  for (const name of ['Index', 'BusinessList']) {
    assert.match(read(name), /距离暂无/);
    assert.match(read(name), /送达时间暂无/);
  }
});

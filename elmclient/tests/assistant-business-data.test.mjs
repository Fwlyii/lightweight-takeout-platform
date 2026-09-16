import test from 'node:test';
import assert from 'node:assert/strict';
import { createAssistantBusinessLookup } from '../src/utils/assistantBusinessData.js';
import { withTianjinDelivery } from '../src/utils/businessDistance.js';
const breakfast = { id: 2, businessName: '海棠早餐铺', businessAddress: '天津大学卫津路校区', score: 4.5, salesCount: 3 };
const rice = { id: 1, businessName: '北洋食堂·现炒', businessAddress: '天津大学北洋园校区', score: 4.9, salesCount: 7 };
const food = { foodId: 4, businessId: 2, businessName: '海棠早餐铺', foodName: '热拿铁', price: 12 };
const response = { success: true, data: [breakfast, rice] };
function fixture(request = { get: async () => response }) {
  const saved = new Map([['userLocationSource', 'manual'], ['userLocationVersion', '2']]);
  const setRegion = province => saved.set('userLocation', JSON.stringify(province === '天津市'
    ? { province, city: province, district: '津南区' } : { province, city: province, district: '黄浦区' }));
  setRegion('天津市');
  const lookup = createAssistantBusinessLookup(request, { storage: { getItem: key => saved.get(key) } });
  return { lookup, setRegion };
}
test('breakfast recommendations join the real shop address, score and homepage distance', async () => {
  const { lookup } = fixture(); await lookup.load();
  const result = lookup.enrich(food);
  assert.equal(result.distanceKm, withTianjinDelivery(breakfast, { province: '天津市', district: '津南区' }).distanceKm);
  assert.ok(result.distanceKm > 15); assert.equal(result.businessScore, 4.5); assert.equal(result.businessSalesCount, 3);
  assert.equal(result.foodId, 4); assert.equal(result.price, 12); assert.equal(result.foodName, '热拿铁');
  assert.equal(food.distanceKm, undefined);
});
test('AI campus recommendations reuse the per-shop points rather than the shared campus center', async () => {
  const { lookup } = fixture(); await lookup.load();
  const result = lookup.enrich({ foodId: 27, businessId: 1, businessName: rice.businessName });
  assert.equal(result.distanceKm, withTianjinDelivery(rice, {}).distanceKm);
  assert.ok(result.distanceKm < .4);
});
test('reopening AI after changing provinces clears both stale values', async () => {
  const { lookup, setRegion } = fixture(); await lookup.load(); assert.ok(lookup.enrich(food).distanceKm > 0);
  setRegion('上海市'); await lookup.load();
  const result = lookup.enrich({ ...food, distanceKm: 2, deliveryMinutes: 25 });
  assert.equal(result.distanceKm, null); assert.equal(result.deliveryMinutes, null);
});
test('metadata requests are single-flight and recover after failure', async () => {
  let reject, count = 0;
  const { lookup } = fixture({ get: () => { count++; return count === 1 ? new Promise((_, fail) => { reject = fail; }) : Promise.resolve(response); } });
  const first = lookup.load(), duplicate = lookup.load(); await Promise.resolve(); assert.equal(count, 1);
  reject(Error('offline')); await Promise.all([first, duplicate]);
  assert.equal(lookup.enrich(food).distanceKm, null);
  await lookup.load(); assert.equal(count, 2); assert.ok(lookup.enrich(food).distanceKm > 0);
});
test('disposing ignores late metadata, unknown shops do not borrow another shop coordinates', async () => {
  let resolve;
  const { lookup } = fixture({ get: () => new Promise(done => { resolve = done; }) });
  const pending = lookup.load(); await Promise.resolve(); lookup.dispose(); resolve(response); await pending;
  assert.equal(lookup.enrich(food).distanceKm, null);
  const fresh = fixture().lookup; await fresh.load();
  assert.equal(fresh.enrich({ ...food, businessId: 999 }).distanceKm, null);
});

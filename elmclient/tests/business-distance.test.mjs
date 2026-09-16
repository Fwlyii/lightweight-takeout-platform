import test from 'node:test';
import assert from 'node:assert/strict';
import { readFileSync } from 'node:fs';
import { distanceKm, validCoordinates, JINNAN_REFERENCE, TIANJIN_DISTRICTS, tianjinOrigin,
  businessLocation, withTianjinDelivery, estimatedDeliveryMinutes, businessDistanceText } from '../src/utils/businessDistance.js';
import { compareBusinessDistance } from '../src/utils/businessPresentation.js';

const jinnan = { province: '天津市', city: '天津市', district: '津南区' };
const elsewhere = { province: '北京市', city: '北京市', district: '东城区' };
const campus = { id: 1, businessAddress: '天津大学北洋园校区' };
const weijin = { id: 2, businessAddress: '天津大学卫津路校区' };
const heping = { id: 3, businessAddress: '天津市和平区' };

test('Jinnan campus distances are local, original Weijin/Heping stores stay distant', () => {
  const local = withTianjinDelivery(campus, jinnan);
  assert.ok(local.distanceKm > .5 && local.distanceKm < 2);
  assert.ok(local.deliveryMinutes >= 25 && local.deliveryMinutes <= 35);
  for (const city of [weijin, heping]) {
    const result = withTianjinDelivery(city, jinnan);
    assert.ok(result.distanceKm > 15 && result.distanceKm < 22);
    assert.ok(result.deliveryMinutes > local.deliveryMinutes);
    assert.equal(result.businessAddress, city.businessAddress);
  }
});
test('all 16 Tianjin district selections calculate finite distance and ETA', () => {
  assert.equal(Object.keys(TIANJIN_DISTRICTS).length, 16);
  for (const district of Object.keys(TIANJIN_DISTRICTS)) {
    const result = withTianjinDelivery(campus, { ...jinnan, district });
    assert.ok(Number.isFinite(result.distanceKm)); assert.ok(Number.isFinite(result.deliveryMinutes));
  }
});
test('all current demo address forms produce numeric values without changing shop data', () => {
  for (const businessAddress of ['天津大学北洋园校区', '天津大学卫津路校区', '天津市和平区', '天津大学津南校区', '天津大学']) {
    const original = { businessAddress }, result = withTianjinDelivery(original, jinnan);
    assert.match(businessDistanceText(result), /^约\d+\.\dkm$/);
    assert.ok(result.deliveryMinutes > 0); assert.deepEqual(original, { businessAddress });
  }
});
test('changing outside Tianjin clears BOTH values even when the API or previous selection supplied them', () => {
  const tianjin = withTianjinDelivery(campus, jinnan);
  const changed = withTianjinDelivery({ ...tianjin, distance: 2 }, elsewhere);
  assert.equal(changed.distanceKm, null); assert.equal(changed.distance, null); assert.equal(changed.deliveryMinutes, null);
  assert.equal(businessDistanceText(changed), '距离暂无');
  assert.equal(withTianjinDelivery(changed, jinnan).distanceKm, tianjin.distanceKm);
});
test('default is Jinnan and invalid Tianjin district is not silently treated as Jinnan', () => {
  assert.deepEqual(tianjinOrigin({}), JINNAN_REFERENCE);
  assert.equal(tianjinOrigin({ ...jinnan, district: '不存在' }), null);
  assert.equal(tianjinOrigin({ province: '河北省', district: '和平区' }), null);
  assert.equal(businessLocation({ businessAddress: '沈阳市和平区' }), null);
  assert.equal(businessLocation({ businessAddress: '未知地址' }), null);
});
test('ETA is monotonic, rounded up and does not clamp distant stores to a fake short time', () => {
  let previous = 0;
  for (const km of [0, .3, 1, 3, 8, 20, 100]) {
    const minutes = estimatedDeliveryMinutes(km);
    assert.ok(minutes >= previous); assert.equal(minutes % 5, 0); previous = minutes;
  }
  assert.equal(estimatedDeliveryMinutes(1), 25); assert.equal(estimatedDeliveryMinutes(20), 110);
  for (const value of [null, undefined, '2', NaN, Infinity, -1]) assert.equal(estimatedDeliveryMinutes(value), null);
});
test('missing/invalid coordinates are never turned into zero', () => {
  for (const latitude of [null, undefined, '', ' ', true, [], Infinity, NaN, 91]) {
    assert.equal(validCoordinates({ latitude, longitude: 117 }), false);
    assert.equal(distanceKm(JINNAN_REFERENCE, { latitude, longitude: 117 }), null);
  }
  assert.equal(validCoordinates({ latitude: 0, longitude: 0 }), true);
  assert.equal(distanceKm(JINNAN_REFERENCE, JINNAN_REFERENCE), 0);
});
test('distance sort matches displayed distance and unknowns remain last', () => {
  const stores = [weijin, { id: 4 }, campus].map(b => withTianjinDelivery(b, jinnan)).sort(compareBusinessDistance);
  assert.deepEqual(stores.map(v => v.id), [1, 2, 4]);
  assert.equal(stores[2].deliveryMinutes, null);
});
test('campus demo shops have distinct stable point distances without moving their addresses', () => {
  const shops = [
    { id: 1, businessName: '北洋食堂·现炒', businessAddress: '天津大学北洋园校区' },
    { id: 4, businessName: '津南麻辣香锅', businessAddress: '天津大学北洋园校区' },
    { id: 6, businessName: '清真兰州牛肉面', businessAddress: '天津大学北洋园校区' },
    { id: 7, businessName: '轻食研究所', businessAddress: '天津大学津南校区' },
    { id: 8, businessName: '泰合炸鸡·夜宵', businessAddress: '天津大学北洋园校区' },
    { id: 9, businessName: '肯德基', businessAddress: '天津大学' }
  ];
  const values = shops.map(shop => withTianjinDelivery(shop, jinnan));
  assert.equal(new Set(values.map(businessDistanceText)).size, 6);
  assert.ok(values.every(b => b.distanceKm > .1 && b.distanceKm < 2));
  assert.deepEqual(values, shops.map(shop => withTianjinDelivery(shop, jinnan)));
  assert.deepEqual(values.map(v => v.businessAddress), shops.map(v => v.businessAddress));
  const renamed = { ...shops[0], businessName: '另一家店' };
  assert.notDeepEqual(businessLocation(renamed), businessLocation(shops[0]));
});
test('home/category keep their existing layout with no added location panel or explanatory labels', () => {
  for (const name of ['Index', 'BusinessList']) {
    const source = readFileSync(new URL(`../src/views/${name}.vue`, import.meta.url), 'utf8');
    assert.match(source, /withTianjinDelivery/);
    assert.doesNotMatch(source, /DistanceLocationBar|地址参考点估算|geolocation/);
  }
});

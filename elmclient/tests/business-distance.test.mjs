import test from 'node:test';
import assert from 'node:assert/strict';
import { distanceKm, validCoordinates, tianjinOrigin, businessLocation, withTianjinDelivery, estimatedDeliveryMinutes, businessDistanceText } from '../src/utils/businessDistance.js';
import { compareBusinessDistance } from '../src/utils/businessPresentation.js';
const origin = { longitude: 117.31, latitude: 39 };
const near = { id: 1, longitude: 117.312, latitude: 39.002 };
const far = { id: 2, longitude: 117.164, latitude: 39.11 };
test('distance follows selected coordinates instead of a district or shop ID', () => {
  const first = withTianjinDelivery(near, origin), next = withTianjinDelivery(near, far);
  assert.ok(first.distanceKm > 0 && first.distanceKm < 1);
  assert.ok(next.distanceKm > 10); assert.ok(next.deliveryMinutes > first.deliveryMinutes);
  assert.equal(withTianjinDelivery({ ...near, id: 999 }, origin).distanceKm, first.distanceKm);
  assert.match(businessDistanceText(first), /^直线\d+\.\dkm$/);
});
test('legacy demo identities and region strings never fabricate coordinates', () => {
  for (const businessAddress of ['天津大学北洋园校区', '天津大学卫津路校区', '天津市津南区', '津南区咸水沽西']) {
    assert.equal(businessLocation({ id: 1, businessName: '北洋食堂·现炒', businessAddress }), null);
  }
  assert.equal(tianjinOrigin({ province: '天津市', district: '津南区' }), null);
  assert.equal(tianjinOrigin({}), null);
});
test('unknown coordinates clear stale distance and ETA instead of showing zero', () => {
  for (const location of [null, {}, { longitude: 117 }]) {
    const value = withTianjinDelivery({ ...near, distanceKm: 2, distance: 2, deliveryMinutes: 25 }, location);
    assert.equal(value.distanceKm, null); assert.equal(value.deliveryMinutes, null); assert.equal(value.distance, null);
    assert.equal(businessDistanceText(value), '距离暂无');
  }
});
test('invalid coordinates are rejected', () => {
  for (const latitude of [null, undefined, '', ' ', true, [], Infinity, NaN, 91]) {
    assert.equal(validCoordinates({ latitude, longitude: 117 }), false);
    assert.equal(distanceKm(origin, { latitude, longitude: 117 }), null);
  }
  assert.equal(validCoordinates({ latitude: 0, longitude: 0 }), true);
  assert.equal(validCoordinates({ latitude: '39.0', longitude: '117.3' }), true);
  assert.equal(distanceKm(origin, origin), 0);
});
test('distance sorting matches display and unknowns remain last', () => {
  const values = [far, { id: 3 }, near].map(b=>withTianjinDelivery(b, origin)).sort(compareBusinessDistance);
  assert.deepEqual(values.map(v=>v.id), [1,2,3]);
});
test('ETA remains a monotonic estimate and never clamps long trips', () => {
  let previous = 0;
  for(const km of [0,.3,1,3,8,20,100]) { const next=estimatedDeliveryMinutes(km); assert.ok(next>=previous); assert.equal(next%5,0); previous=next; }
  for(const value of [null,undefined,'2',NaN,Infinity,-1]) assert.equal(estimatedDeliveryMinutes(value),null);
});

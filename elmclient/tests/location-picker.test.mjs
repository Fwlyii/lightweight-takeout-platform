import test from 'node:test';
import assert from 'node:assert/strict';
import { useLocationPicker } from '../src/composables/useLocationPicker.js';

const province = { name: '天津市', adcode: '120000' };
const city = { name: '天津市', adcode: '120100' };
const district = { name: '津南区', adcode: '120112' };
const savedLocation = { province: '天津市', city: '天津市', district: '津南区' };

function fixture(overrides = {}) {
  const values = new Map();
  const calls = [];
  const storage = {
    getItem: key => values.get(key) ?? null,
    setItem: (key, value) => values.set(key, value)
  };
  const http = {
    async get(url, options) {
      calls.push({ url, options });
      const children = { '中国': [province], '120000': [city], '天津市': [city], '120100': [district] };
      return { data: { status: '1', districts: [{ districts: children[options.params.keywords] ?? [] }] } };
    }
  };
  return { values, calls, picker: useLocationPicker({ http, storage, apiKey: 'test-key', ...overrides }) };
}

async function chooseDistrict(picker) {
  await picker.showLocationPicker();
  await picker.selectLocation(province);
  await picker.selectLocation(city);
  await picker.selectLocation(district);
}

test('selection changes the displayed location only after confirmation', async () => {
  const { picker, calls, values } = fixture();
  const initial = picker.displayLocation.value;
  await chooseDistrict(picker);
  assert.equal(picker.displayLocation.value, initial);
  assert.equal(picker.currentLevel.value, 2);
  assert.equal(picker.isSelected(district), true);
  assert.equal(picker.confirmLocation(), true);
  assert.equal(picker.showPicker.value, false);
  assert.equal(picker.displayLocation.value, '天津市 津南区');
  assert.deepEqual(JSON.parse(values.get('userLocation')), savedLocation);
  assert.equal(values.get('userLocationSource'), 'manual');
  assert.equal(values.get('userLocationVersion'), '2');
  assert.equal(calls.length, 3);
  assert.equal(calls[0].url, 'https://restapi.amap.com/v3/config/district');
  assert.equal(calls[0].options.params.key, 'test-key');
  assert.equal(calls[0].options.headers?.Authorization, undefined);
  assert.ok(calls[0].options.timeout > 0);
});

test('cancel discards the pending selection and keeps the saved location', async () => {
  const { picker } = fixture();
  await chooseDistrict(picker);
  picker.confirmLocation();
  await picker.showLocationPicker();
  await picker.selectLocation({ name: '河北省', adcode: '130000' });
  picker.hideLocationPicker();
  assert.deepEqual(picker.selectedLocation.value, savedLocation);
  assert.equal(picker.displayLocation.value, '天津市 津南区');
});

test('incomplete selections cannot be persisted', async () => {
  const { picker, values } = fixture();
  await picker.showLocationPicker();
  assert.equal(picker.confirmLocation(), false);
  assert.match(picker.error.value, /省份/);
  await picker.selectLocation(province);
  assert.equal(picker.confirmLocation(), false);
  assert.match(picker.error.value, /城市/);
  await picker.selectLocation(city);
  assert.equal(picker.confirmLocation(), false);
  assert.match(picker.error.value, /区域/);
  assert.equal(values.size, 0);
});

test('returning to the province level invalidates the previously selected city and district', async () => {
  const { picker } = fixture();
  await chooseDistrict(picker);
  await picker.switchLevel(0);
  assert.equal(picker.currentLevel.value, 0);
  assert.equal(picker.confirmLocation(), false);
});

test('missing map configuration does not send a request with an empty key', async () => {
  const { picker, calls } = fixture({ apiKey: '' });
  await picker.showLocationPicker();
  assert.equal(calls.length, 0);
  assert.equal(picker.loading.value, false);
  assert.match(picker.error.value, /配置/);
});

test('network failures and malformed map responses leave the picker usable', async () => {
  for (const get of [
    async () => { throw new Error('network unavailable'); },
    async () => ({ data: { status: '0', info: 'INVALID_USER_KEY' } }),
    async () => ({ data: { status: '1', districts: [] } })
  ]) {
    const { picker } = fixture({ http: { get } });
    await picker.showLocationPicker();
    assert.equal(picker.loading.value, false);
    assert.ok(picker.error.value);
    assert.deepEqual(picker.locationData.value, []);
  }
});

test('closing the picker invalidates an in-flight request', async () => {
  let resolve;
  const { picker } = fixture({ http: { get: () => new Promise(done => { resolve = done; }) } });
  const pending = picker.showLocationPicker();
  assert.equal(picker.loading.value, true);
  picker.hideLocationPicker();
  resolve({ data: { status: '1', districts: [{ districts: [province] }] } });
  await pending;
  assert.equal(picker.showPicker.value, false);
  assert.equal(picker.loading.value, false);
  assert.deepEqual(picker.locationData.value, []);
});

test('out-of-order responses cannot overwrite the latest picker request', async () => {
  const resolvers = [];
  const { picker } = fixture({ http: { get: () => new Promise(done => resolvers.push(done)) } });
  const first = picker.showLocationPicker();
  const second = picker.showLocationPicker();
  resolvers[1]({ data: { status: '1', districts: [{ districts: [province] }] } });
  await second;
  resolvers[0]({ data: { status: '1', districts: [{ districts: [city] }] } });
  await first;
  assert.deepEqual(picker.locationData.value, [province]);
});

test('restoration validates stored data and recomputes its display text', () => {
  const { picker, values } = fixture();
  values.set('userLocationSource', 'manual');
  values.set('userLocationVersion', '2');
  values.set('userLocation', JSON.stringify({ ...savedLocation, userId: 99 }));
  values.set('userLocationDisplay', 'untrusted stale label');
  picker.restoreSavedLocation();
  assert.deepEqual(picker.selectedLocation.value, savedLocation);
  assert.equal(picker.displayLocation.value, '天津市 津南区');
});

test('old versions, corrupt JSON and incomplete locations are ignored', () => {
  for (const value of ['broken', 'null', '{}', '{"province":"","city":"","district":""}']) {
    const { picker, values } = fixture();
    const initial = picker.displayLocation.value;
    values.set('userLocationSource', 'manual');
    values.set('userLocationVersion', '2');
    values.set('userLocation', value);
    assert.doesNotThrow(() => picker.restoreSavedLocation());
    assert.equal(picker.displayLocation.value, initial);
  }
  const { picker, values } = fixture();
  values.set('userLocationSource', 'manual');
  values.set('userLocationVersion', '1');
  values.set('userLocation', JSON.stringify(savedLocation));
  picker.restoreSavedLocation();
  assert.notEqual(picker.displayLocation.value, '天津市 津南区');
});

test('blocked storage does not crash restoration or an otherwise valid selection', async () => {
  const { picker } = fixture({ storage: {
    getItem() { throw new Error('storage disabled'); },
    setItem() { throw new Error('storage disabled'); }
  } });
  assert.doesNotThrow(() => picker.restoreSavedLocation());
  await chooseDistrict(picker);
  assert.equal(picker.confirmLocation(), true);
  assert.equal(picker.displayLocation.value, '天津市 津南区');
});

test('invalid selection values and levels do not trigger network requests', async () => {
  const { picker, calls } = fixture();
  await picker.showLocationPicker();
  const count = calls.length;
  for (const item of [null, {}, { name: '', adcode: '1' }]) await picker.selectLocation(item);
  for (const level of [-1, 1, 20, '0', NaN]) await picker.switchLevel(level);
  assert.equal(calls.length, count);
});

import test from 'node:test';
import assert from 'node:assert/strict';
import { useLocationPicker } from '../src/composables/useLocationPicker.js';

function fixture(options = {}) {
  const values = new Map();
  const storage = { getItem: key => values.get(key), setItem: (key, value) => values.set(key, value) };
  return { values, storage, picker: useLocationPicker({ storage, ...options }) };
}
function choose(picker, names = ['浙江省', '杭州市', '西湖区']) {
  picker.showLocationPicker();
  for (const name of names) {
    const option = picker.locationData.value.find(item => item.name === name);
    assert.ok(option, `missing ${name}`);
    picker.selectLocation(option);
  }
}
test('default is Tianjin Jinnan and all province entries are available without a map key', () => {
  const { picker } = fixture({ apiKey: '', http: { get() { assert.fail('must not request a map API'); } } });
  assert.equal(picker.displayLocation.value, '天津市津南区');
  picker.showLocationPicker();
  assert.equal(picker.locationData.value.length, 34);
  assert.equal(picker.loading.value, false);
  assert.equal(picker.error.value, '');
});
test('selection only changes the header after confirmation and persists across reload', () => {
  const { picker, storage, values } = fixture();
  choose(picker);
  assert.equal(picker.displayLocation.value, '天津市津南区');
  assert.equal(picker.confirmLocation(), true);
  assert.equal(picker.displayLocation.value, '浙江省杭州市西湖区');
  assert.equal(picker.showPicker.value, false);
  assert.equal(values.get('userLocationSource'), 'manual');
  const fresh = useLocationPicker({ storage });
  fresh.restoreSavedLocation();
  assert.equal(fresh.displayLocation.value, '浙江省杭州市西湖区');
});
test('municipalities are not repeated in the displayed region', () => {
  const { picker } = fixture();
  choose(picker, ['天津市', '天津市', '津南区']);
  assert.equal(picker.confirmLocation(), true);
  assert.equal(picker.displayLocation.value, '天津市津南区');
});
test('cancel discards pending changes', () => {
  const { picker, values } = fixture();
  choose(picker);
  picker.hideLocationPicker();
  assert.equal(picker.displayLocation.value, '天津市津南区');
  assert.equal(values.size, 0);
  assert.equal(picker.pendingLocation.value.province, '');
});
test('each missing level prevents confirmation and selecting clears validation errors', () => {
  const { picker, values } = fixture();
  picker.showLocationPicker();
  for (const [label, name] of [['省份', '浙江省'], ['城市', '杭州市'], ['区域', '西湖区']]) {
    assert.equal(picker.confirmLocation(), false);
    assert.ok(picker.error.value.includes(label));
    picker.selectLocation(picker.locationData.value.find(item => item.name === name));
    assert.equal(picker.error.value, '');
  }
  assert.equal(values.size, 0);
});
test('switching parents resets descendants and filters options', () => {
  const { picker } = fixture();
  choose(picker);
  picker.switchLevel(1);
  assert.deepEqual({ ...picker.pendingLocation.value }, { province: '浙江省', city: '', district: '' });
  assert.ok(picker.locationData.value.every(item => item.adcode.startsWith('33')));
  picker.switchLevel(0);
  picker.selectLocation(picker.locationData.value.find(item => item.name === '天津市'));
  assert.equal(picker.pendingLocation.value.district, '');
  assert.ok(picker.locationData.value.every(item => item.adcode.startsWith('12')));
});
test('province-direct counties can be selected', () => {
  const { picker } = fixture();
  choose(picker, ['海南省', '省直辖县', '琼海市']);
  assert.equal(picker.confirmLocation(), true);
  assert.equal(picker.displayLocation.value, '海南省琼海市');
});
test('closed dialog ignores selections and confirmations', () => {
  const { picker } = fixture();
  choose(picker);
  picker.hideLocationPicker();
  picker.selectLocation({ name: '津南区', adcode: '120112' });
  assert.equal(picker.confirmLocation(), false);
  assert.deepEqual(picker.locationData.value, []);
});
test('invalid and unrelated options or levels are ignored', () => {
  const { picker } = fixture();
  picker.showLocationPicker();
  for (const option of [null, {}, { name: '伪造地区', adcode: '120000' }, { name: '津南区', adcode: '120112' }]) picker.selectLocation(option);
  for (const level of [-1, 1, 20, '0', NaN]) picker.switchLevel(level);
  assert.equal(picker.currentLevel.value, 0);
  assert.equal(picker.pendingLocation.value.province, '');
});
test('restoration rejects corrupt, incomplete and mismatched administrative paths', () => {
  for (const saved of ['broken', 'null', '{}', JSON.stringify({ province: '天津市', city: '杭州市', district: '西湖区' })]) {
    const { picker, values } = fixture();
    values.set('userLocationSource', 'manual');
    values.set('userLocationVersion', '2');
    values.set('userLocation', saved);
    picker.restoreSavedLocation();
    assert.equal(picker.selectedLocation.value.province, '');
  }
});
test('old location versions are ignored and stored labels are recomputed', () => {
  const { picker, values } = fixture();
  choose(picker);
  picker.confirmLocation();
  values.set('userLocationDisplay', 'stale label');
  picker.restoreSavedLocation();
  assert.equal(picker.displayLocation.value, '浙江省杭州市西湖区');
  values.set('userLocationVersion', '1');
  const fresh = useLocationPicker({ storage: { getItem: key => values.get(key) } });
  fresh.restoreSavedLocation();
  assert.equal(fresh.selectedLocation.value.province, '');
});
test('disabled local storage does not prevent selection', () => {
  const { picker } = fixture({ storage: { getItem() { throw Error('disabled'); }, setItem() { throw Error('disabled'); } } });
  assert.doesNotThrow(() => picker.restoreSavedLocation());
  choose(picker);
  assert.equal(picker.confirmLocation(), true);
  assert.equal(picker.displayLocation.value, '浙江省杭州市西湖区');
});

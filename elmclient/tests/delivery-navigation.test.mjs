import test from 'node:test';
import assert from 'node:assert/strict';
import { openDeliveryNavigation } from '../src/utils/deliveryNavigation.js';

function dependencies(url = 'https://uri.amap.com/search?keyword=test') {
  const calls = [], notices = [], locations = [];
  const target = { opener: {}, location: { replace: value => locations.push(value) }, close() { this.closed = true; } };
  return { target, calls, notices, locations, request: { get: async path => {
    calls.push(path); return { success: true, data: { navigationUrl: url } };
  } }, openWindow: () => target, notify: value => notices.push(value) };
}
test('rider navigation uses the owned task endpoint and opens the validated map URL', async () => {
  const d = dependencies(); await openDeliveryNavigation(42, d);
  assert.deepEqual(d.calls, ['/api/v1/delivery-tasks/42/navigation']);
  assert.equal(d.locations.length, 1); assert.equal(d.target.opener, null);
});
test('blocked popup prevents a navigation request', async () => {
  const d = dependencies(); d.openWindow = () => null; await openDeliveryNavigation(42, d);
  assert.equal(d.calls.length, 0); assert.equal(d.notices.length, 1);
});
for (const url of ['javascript:alert(1)', 'https://evil.example/search', 'https://uri.amap.com.evil.example/search']) {
  test(`rejects an unsafe navigation URL: ${url}`, async () => {
    const d = dependencies(url); await openDeliveryNavigation(42, d);
    assert.equal(d.locations.length, 0); assert.equal(d.target.closed, true); assert.equal(d.notices.length, 1);
  });
}
test('authorization failure closes the blank window without navigation', async () => {
  const d = dependencies(); d.request.get = async () => { throw Error('forbidden'); };
  await openDeliveryNavigation(42, d);
  assert.equal(d.target.closed, true); assert.equal(d.locations.length, 0);
});

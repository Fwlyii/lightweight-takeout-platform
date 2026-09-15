import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { checkoutContext, checkoutQuery, addressReturnLocation, checkoutItems, createOrderSubmitter, positiveId } from '../src/utils/checkout.js';
import { loadPaymentReceipt } from '../src/utils/paymentReceipt.js';

const context = { businessId: 7, foodIds: [21, 22], serviceMode: 'delivery' };
test('checkout context survives new/edit address navigation without unrelated parameters', () => {
  const query = { ...checkoutQuery(context), id: '4', redirect: '//bad.example' };
  assert.deepEqual(checkoutContext(query), context);
  assert.deepEqual(addressReturnLocation(query), { path: '/userAddress', query: checkoutQuery(context) });
  assert.deepEqual(addressReturnLocation({}), { path: '/userAddress', query: {} });
});
for (const businessId of ['NaN', '-1', '0', '7x', ['7'], '9007199254740992']) {
  test(`reject invalid checkout business ${JSON.stringify(businessId)}`, () => {
    assert.throws(() => checkoutContext({ businessId }));
  });
}
for (const foodIds of ['', '21,', '21,no', '21,0', ['21'], '9007199254740992']) {
  test(`reject invalid selected food ${JSON.stringify(foodIds)}`, () => {
    assert.throws(() => checkoutContext({ businessId: '7', foodIds }));
  });
}
test('pickup is explicit and missing mode defaults to delivery', () => {
  assert.equal(checkoutContext({ businessId: '7' }).serviceMode, 'delivery');
  assert.equal(checkoutContext({ businessId: '7', serviceMode: 'pickup' }).serviceMode, 'pickup');
  assert.throws(() => checkoutContext({ businessId: '7', serviceMode: 'unknown' }));
  assert.equal(positiveId(undefined), null);
});
test('checkout never silently includes another merchant or unselected food', () => {
  const rows = [{ businessId: 7, foodId: 21 }, { businessId: 7, foodId: 22 }, { businessId: 7, foodId: 23 }, { businessId: 8, foodId: 24 }];
  assert.deepEqual(checkoutItems(rows, context), rows.slice(0, 2));
  assert.throws(() => checkoutItems(rows.slice(1), context), /部分/);
  assert.throws(() => checkoutItems([], context), /购物车/);
});
test('delivery requires an address and never sends user IDs or client prices', async () => {
  const calls = [];
  const submit = createOrderSubmitter({ post: async (...args) => { calls.push(args); return { success: true, data: 41 }; } }, () => 'request-1');
  await assert.rejects(submit(context), /地址/);
  assert.equal(calls.length, 0);
  assert.equal(await submit(context, 5), 41);
  assert.deepEqual(calls[0], ['/api/orders/submit', null, { headers: { 'Idempotency-Key': 'request-1' },
    params: { businessId: 7, addressId: 5, serviceMode: 'delivery', foodIds: '21,22' } }]);
});
test('pickup submits without an address even if a previous address was selected', async () => {
  let params;
  const submit = createOrderSubmitter({ post: async (_, body, options) => { params = options.params; return { success: true, data: 42 }; } }, () => 'pickup-1');
  await submit({ ...context, serviceMode: 'pickup' }, 5);
  assert.equal(params.addressId, undefined);
  assert.equal(params.serviceMode, 'pickup');
});
test('double click and successful resubmission share a single order', async () => {
  let resolve, calls = 0;
  const submit = createOrderSubmitter({ post: () => { calls++; return new Promise(r => { resolve = r; }); } }, () => 'one-key');
  const first = submit(context, 5), second = submit(context, 5);
  assert.equal(first, second);
  resolve({ success: true, data: 43 });
  assert.equal(await first, 43);
  assert.equal(await submit(context, 5), 43);
  assert.equal(calls, 1);
});
test('retry after network failure retains the idempotency key', async () => {
  const keys = [];
  const submit = createOrderSubmitter({ post: async (_, body, options) => {
    keys.push(options.headers['Idempotency-Key']);
    if (keys.length === 1) throw new Error('timeout');
    return { success: true, data: 44 };
  } }, () => 'retry-key');
  await assert.rejects(submit(context, 5), /timeout/);
  assert.equal(await submit(context, 5), 44);
  assert.deepEqual(keys, ['retry-key', 'retry-key']);
});
test('failed business response cannot be mistaken for a created order', async () => {
  const submit = createOrderSubmitter({ post: async () => ({ success: false, message: '库存不足' }) }, () => 'failure');
  await assert.rejects(submit(context, 5), /库存不足/);
});
for (const [state, expected] of [[0, 'unpaid'], [1, 'paid'], [7, 'paid'], [8, 'cancelled'], [9, 'paid']]) {
  test(`payment receipt checks actual server state ${state}`, async () => {
    const receipt = await loadPaymentReceipt({ get: async () => ({ success: true, data: { id: 1, orderState: state } }) }, '1');
    assert.equal(receipt.state, expected);
  });
}
test('missing order, failed API and unknown state cannot display success', async () => {
  await assert.rejects(loadPaymentReceipt({ get: () => assert.fail('must not request') }, undefined));
  await assert.rejects(loadPaymentReceipt({ get: async () => ({ success: false, message: '无权访问' }) }, 1), /无权/);
  await assert.rejects(loadPaymentReceipt({ get: async () => ({ success: true, data: { orderState: 999 } }) }, 1), /状态/);
});
test('production pages connect selectable addresses to checkout and preserve return context', async () => {
  const read = path => readFile(new URL('../src/' + path, import.meta.url), 'utf8');
  const [page, checkout, editor, manager] = await Promise.all([
    read('views/UserAddress.vue'), read('components/CheckoutPage.vue'), read('components/AddressEditor.vue'), read('components/AddressManager.vue')]);
  assert.match(page, /<CheckoutPage v-if="route\.query\.businessId !== undefined"/);
  assert.match(checkout, /<AddressManager selectable/);
  assert.match(checkout, /@select="selectAddress"/);
  assert.match(checkout, /submitOrder\(context.value, selectedAddress.value\?\.id\)/);
  assert.match(editor, /router\.replace\(returnTo.value\)/);
  assert.match(manager, /checkoutQuery, id: address.id/);
});
test('account pages use implemented current-profile and admin-only endpoints', async () => {
  const read = path => readFile(new URL('../src/views/' + path, import.meta.url), 'utf8');
  const [merchant, admin, users] = await Promise.all([read('MerchantProfile.vue'), read('AdminHome.vue'), read('AdminUser.vue')]);
  assert.match(merchant, /request.get\('\/api\/user'\)/);
  assert.match(admin, /request.get\('\/api\/user'\)/);
  assert.match(users, /useAdminAccounts/);
  assert.doesNotMatch(merchant + admin + users, /\/api\/persons|\/api\/personInfo|\/api\/person'/);
});

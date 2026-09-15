import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { ref, computed } from 'vue';
import { positiveId } from '../src/utils/checkout.js';

const source = await readFile(new URL('../src/views/Cart.vue', import.meta.url), 'utf8');
const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
  .replace(/^import .*\r?\n/gm, '').replace('export default', 'return');
function page(write) {
  const navigations = [];
  const bindings = {
    ref, computed, positiveId, onMounted() {},
    useRoute: () => ({ path: '/cart', query: { businessId: '7' } }),
    useRouter: () => ({ push: value => navigations.push(value), replace() {} }),
    localStorage: { getItem: () => null },
    toast: { error() {}, warning() {}, success() {} },
    request: { put: write },
    maxCartQuantity: () => 99, cartQuantityLimitMessage: () => '',
  };
  const component = new Function(...Object.keys(bindings), script)(...Object.values(bindings)).setup();
  component.businessId.value = 7;
  component.cartItems.value = [{ id: 1, foodId: 21, businessId: 7, quantity: 1, foodPrice: 10, remarks: '' }];
  component.selectedFoodIds.value = [21];
  component.remarkDraft.value = '少辣';
  return { component, navigations };
}

test('checkout awaits an in-flight blur save before navigating', async () => {
  let finish;
  const p = page(() => new Promise(resolve => { finish = resolve; }));
  const blur = p.component.saveRemark();
  const checkout = p.component.checkout();
  assert.equal(p.navigations.length, 0);
  finish({ success: true, data: 1 });
  await Promise.all([blur, checkout]);
  assert.equal(p.navigations.length, 1);
  assert.equal(p.component.cartItems.value[0].remarks, '少辣');
});

test('failed remark save retains the draft and blocks checkout', async () => {
  const p = page(async () => { throw new Error('network unavailable'); });
  await p.component.checkout();
  assert.equal(p.navigations.length, 0);
  assert.equal(p.component.remarkDraft.value, '少辣');
  assert.equal(p.component.remarkEditing.value, true);
});

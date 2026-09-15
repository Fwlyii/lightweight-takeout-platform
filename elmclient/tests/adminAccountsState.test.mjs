import test from 'node:test';
import assert from 'node:assert/strict';
import { createSSRApp } from 'vue';
import { renderToString } from '@vue/server-renderer';
import { useAdminAccounts } from '../src/composables/useAdminAccounts.js';

async function setup(api) {
  let state;
  const messages = [];
  // Set up the real composable within Vue; drive requests explicitly without a browser.
  await renderToString(createSSRApp({ setup() {
    state = useAdminAccounts(api, { error: value => messages.push(['error', value]),
      success: value => messages.push(['success', value]) });
    return () => null;
  } }));
  return { state, messages };
}
const deferred = () => { let resolve, reject; const promise = new Promise((a, b) => { resolve = a; reject = b; });
  return { promise, resolve, reject }; };

test('latest search wins when older requests complete later', async () => {
  const first = deferred(), second = deferred();
  let calls = 0;
  const { state } = await setup({ list: () => (++calls === 1 ? first.promise : second.promise) });
  state.searchKeyword.value = 'first'; const a = state.handleSearch();
  state.searchKeyword.value = 'second'; const b = state.handleSearch();
  second.resolve([{ id: 2, username: 'second', activated: true }]); await b;
  first.resolve([{ id: 1, username: 'first', activated: true }]); await a;
  assert.equal(state.users.value[0].userId, 2);
  assert.equal(state.loading.value, false);
});
test('status update is single-flight and failure does not flip the displayed account', async () => {
  const pending = deferred(); let calls = 0;
  const { state, messages } = await setup({ setActivated: () => { calls++; return pending.promise; } });
  const user = { userId: 7, disabled: false };
  state.toggleUserStatus(user);
  const a = state.confirmToggle(); await state.confirmToggle();
  assert.equal(calls, 1);
  pending.reject(new Error('禁止操作')); await a;
  assert.equal(user.disabled, false);
  assert.equal(state.updating.value, false);
  assert.deepEqual(messages, [['error', '禁止操作']]);
});
test('successful status change refreshes the current server-side filter', async () => {
  const calls = [];
  const { state } = await setup({ setActivated: async (...args) => calls.push(args),
    list: async query => { calls.push(query); return []; } });
  state.activeFilter.value = 'enabled';
  state.searchKeyword.value = ' demo ';
  state.toggleUserStatus({ userId: 7, disabled: false });
  await state.confirmToggle();
  assert.deepEqual(calls, [[7, false], { status: 1, keyword: 'demo' }]);
  assert.equal(state.showConfirmModal.value, false);
  assert.deepEqual(state.users.value, []);
});

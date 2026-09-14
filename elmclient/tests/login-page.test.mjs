import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { ref, computed } from 'vue';
import { ROLE_DEFINITIONS, roleCanEnter } from '../src/utils/roles.js';
import { createAuthenticationClient, selectLoginDestination } from '../src/utils/authenticationClient.js';

const source = await readFile(new URL('../src/views/Login.vue', import.meta.url), 'utf8');
const script = source.match(/<script setup>([\s\S]*?)<\/script>/)[1].replace(/^import .*;\r?\n/gm, '');
function page({ authRole = 'user', redirect = '/orderList', storageUnavailable = false } = {}) {
  const saved = [], navigated = [], observed = [];
  const profile = { username: 'test-user', authorities: [{ name: 'USER' }] };
  const request = {
    async post() { return { id_token: 'new-token', role: authRole }; },
    async get(path, options) { observed.push({ savedCount: saved.length, options }); return profile; }
  };
  const router = { push: async target => navigated.push(target), replace() {}, resolve: path => ({
    name: path === '/orderList' ? 'OrderList' : 'AdminHome', meta: { role: path === '/orderList' ? 'user' : 'admin' }
  }) };
  const storage = { getItem() { if (storageUnavailable) throw Error('blocked storage'); return null; },
    setItem() { if (storageUnavailable) throw Error('blocked storage'); }, removeItem() {} };
  const bindings = { ref, computed, watch() {}, useRouter: () => router, useRoute: () => ({ query: { role: 'user', redirect } }),
    request, ROLE_DEFINITIONS, roleCanEnter, createAuthenticationClient, selectLoginDestination,
    clearAuth() {}, saveAuth: (...args) => saved.push(args), updateStoredUser() {},
    toast: { info() {}, success() {}, error() {} }, settleRouteWipe() {}, startRouteWipe() {},
    localStorage: storage, window: { matchMedia: () => ({ matches: true }), setTimeout: () => 0 },
    document: {}, requestAnimationFrame: callback => callback() };
  const component = new Function(...Object.keys(bindings), script + '\nreturn { login, userName, password };')(...Object.values(bindings));
  component.userName.value = 'test-user';
  component.password.value = 'test-only';
  return { component, saved, navigated, observed };
}

test('the redesigned login page does not publish identity before profile verification', async () => {
  const p = page(); await p.component.login();
  assert.equal(p.observed[0].savedCount, 0);
  assert.equal(p.observed[0].options.headers.Authorization, 'Bearer new-token');
  assert.equal(p.saved.length, 1);
  assert.equal(p.saved[0][1].username, 'test-user');
  assert.deepEqual(p.navigated, ['/orderList']);
});
test('the redesigned login page rejects a different server role', async () => {
  const p = page({ authRole: 'merchant' }); await p.component.login();
  assert.equal(p.saved.length, 0);
  assert.equal(p.navigated.length, 0);
});
for (const redirect of ['/admin/home', '/\\evil.example']) {
  test(`the redesigned login page validates the actual redirect: ${redirect}`, async () => {
    const p = page({ redirect }); await p.component.login();
    assert.deepEqual(p.navigated, ['/index']);
  });
}
test('unavailable remembered-name storage cannot break login', async () => {
  const p = page({ storageUnavailable: true }); await p.component.login();
  assert.equal(p.saved.length, 1);
  assert.deepEqual(p.navigated, ['/orderList']);
});

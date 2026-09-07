import test from 'node:test';
import assert from 'node:assert/strict';
import { createAuthenticationClient, selectLoginDestination } from '../src/utils/authenticationClient.js';

const customer = { username: 'student', authorities: [{ name: 'USER' }] };
function environment({ auth = { id_token: 'signed', role: 'user', application_only: false }, profile = customer, failure } = {}) {
  const saved = [];
  let cleared = 0;
  const transport = {
    async post(path, body, config) { assert.equal(path, '/api/auth'); assert.equal(config.skipAuthRedirect, true); return auth; },
    async get(path, config) {
      assert.equal(path, '/api/user');
      assert.equal(saved.length, 0, 'Do not publish a half-finished session');
      assert.equal(config.headers.Authorization, 'Bearer signed');
      if (failure) throw failure;
      return profile;
    }
  };
  const session = { saveAuth: (...args) => saved.push(args), clearAuth: () => cleared++ };
  return { client: createAuthenticationClient(transport, session), saved, cleared: () => cleared };
}

test('successful login stores the verified profile and selected portal together', async () => {
  const e = environment();
  await e.client.login({ username: 'student', password: 'Study2026!', role: 'user', rememberMe: true });
  assert.deepEqual(e.saved, [['signed', customer, true, 'user']]);
});

test('failed profile request cannot leave a partial session', async () => {
  const e = environment({ failure: new Error('network unavailable') });
  await assert.rejects(e.client.login({ role: 'user' }), /network unavailable/);
  assert.equal(e.saved.length, 0);
  assert.equal(e.cleared(), 1);
});

test('server role must match the submitted role', async () => {
  const e = environment({ auth: { id_token: 'signed', role: 'rider' } });
  await assert.rejects(e.client.login({ role: 'user' }));
  assert.equal(e.saved.length, 0);
});

test('role changed between login and profile retrieval asks for a new login', async () => {
  const e = environment({ profile: { authorities: [{ name: 'RIDER' }, { name: 'USER' }] } });
  await assert.rejects(e.client.login({ role: 'user' }));
  assert.equal(e.saved.length, 0);
});

test('customer can retain an applicant session but cannot turn it into approved rider access', async () => {
  const e = environment({ auth: { id_token: 'signed', role: 'rider', application_only: true } });
  const result = await e.client.login({ role: 'rider' });
  assert.equal(result.applicationOnly, true);
  assert.equal(e.saved[0][3], 'rider');
});

test('missing signed token is a failed login', async () => {
  const e = environment({ auth: { role: 'user' } });
  await assert.rejects(e.client.login({ role: 'user' }));
  assert.equal(e.saved.length, 0);
});

const resolve = path => ({ name: path === '/orderList' ? 'OrderList' : 'Unknown', meta: { role: path === '/admin/user' ? 'admin' : 'user' } });
test('valid customer redirect is retained', () => {
  assert.equal(selectLoginDestination({ role: 'user' }, '/orderList', resolve), '/orderList');
});
for (const redirect of ['//evil.test', '/\\evil.test', 'https://evil.test', '/admin/user', '/missing', '/login']) {
  test(`unsafe or unavailable redirect is rejected: ${redirect}`, () => {
    assert.equal(selectLoginDestination({ role: 'user' }, redirect, resolve), '/index');
  });
}
test('applicants always go to their application route', () => {
  assert.equal(selectLoginDestination({ role: 'rider', applicationOnly: true }, '/orderList', resolve), '/rider/apply');
});
test('approved operational account goes to its own workspace', () => {
  assert.equal(selectLoginDestination({ role: 'merchant' }, '/orderList', resolve), '/merchant/business');
});

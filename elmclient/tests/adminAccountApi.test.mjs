import test from 'node:test';
import assert from 'node:assert/strict';
import { createAdminAccountApi } from '../src/services/adminAccountApi.js';

test('account API preserves query and status endpoints after extraction', async () => {
  const calls = [];
  const api = createAdminAccountApi({
    get: async (...args) => { calls.push(args); return { success: true, data: [{ id: 1 }] }; },
    put: async (...args) => { calls.push(args); return { success: true }; }
  });
  assert.deepEqual(await api.list({ status: 2, keyword: 'demo' }), [{ id: 1 }]);
  await api.setActivated(1, true);
  assert.deepEqual(calls, [
    ['/api/admin/users', { params: { status: 2, keyword: 'demo' } }],
    ['/api/admin/users/1/status', null, { params: { activated: true } }]
  ]);
});
test('account business failures remain failures', async () => {
  const api = createAdminAccountApi({
    get: async () => ({ success: false, message: '禁止访问' }),
    put: async () => ({ success: false, message: '不能禁用管理员' })
  });
  await assert.rejects(api.list(), /禁止访问/);
  await assert.rejects(api.setActivated(1, false), /不能禁用管理员/);
});

import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile, readdir } from 'node:fs/promises';

const root = new URL('../src/', import.meta.url);
async function sources(dir = root) {
  const files = await readdir(dir, { withFileTypes: true });
  const children = await Promise.all(files.map(entry => entry.isDirectory()
    ? sources(new URL(entry.name + '/', dir))
    : /\.(vue|js)$/.test(entry.name) ? readFile(new URL(entry.name, dir), 'utf8') : ''));
  return children.join('\n');
}
test('production source cannot call retired write-via-GET or nonexistent cancel endpoints', async () => {
  const source = await sources();
  assert.doesNotMatch(source, /\/api\/carts\/(?:add|quantity|clear|remove)(?:[?'"`]|\b)/);
  assert.doesNotMatch(source, /\/api\/foods\/(?:status|delete)(?:[?'"`]|\b)/);
  assert.doesNotMatch(source, /\/api\/orders\/cancel/);
});
test('merchant catalog mutations use PATCH and DELETE with failure handling', async () => {
  const source = await readFile(new URL('views/MerchantBusinessInfo.vue', root), 'utf8');
  assert.match(source, /request\.patch\(`\/api\/foods\/\$\{id\}\/status`/);
  assert.match(source, /request\.delete\(`\/api\/foods\/\$\{id\}`/);
  assert.match(source, /throw new Error\(response.message \|\| '商品状态更新失败'\)/);
  assert.match(source, /throw new Error\(response.message \|\| '删除商品失败'\)/);
});
test('order list reads the shared response envelope, not an extra data wrapper', async () => {
  const source = await readFile(new URL('views/OrderList.vue', root), 'utf8');
  assert.doesNotMatch(source, /response\.data\.message/);
});

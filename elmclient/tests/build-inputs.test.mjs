import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { compileTemplate, parse } from '@vue/compiler-sfc';
import * as profileDefaults from '../src/utils/profileDefaults.js';

test('role pages import an existing shared avatar default', async () => {
  for (const page of ['AdminHome', 'AdminBusiness', 'MerchantProfile']) {
    const source = await readFile(new URL(`../src/views/${page}.vue`, import.meta.url), 'utf8');
    const imported = source.match(/import \{ (\w+) \} from ['"][^'"]*profileDefaults['"]/)[1];
    assert.equal(typeof profileDefaults[imported], 'string', `${page}: ${imported}`);
  }
});

test('all ten category icons referenced by the home page are shipped as PNG files', async () => {
  for (let number = 1; number <= 10; number += 1) {
    const file = `dcfl${String(number).padStart(2, '0')}.png`;
    const bytes = await readFile(new URL(`../src/assets/${file}`, import.meta.url));
    assert.equal(bytes.subarray(0, 8).toString('hex'), '89504e470d0a1a0a', file);
  }
});

test('the rider dashboard template contains compilable JavaScript expressions', async () => {
  const filename = new URL('../src/views/RiderDashboard.vue', import.meta.url);
  const { descriptor, errors } = parse(await readFile(filename, 'utf8'));
  assert.deepEqual(errors, []);
  const result = compileTemplate({ source: descriptor.template.content, filename: filename.pathname, id: 'rider-dashboard' });
  assert.deepEqual(result.errors, []);
});

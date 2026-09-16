import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { parse, compileTemplate } from '@vue/compiler-sfc';

const read = file => readFile(new URL('../src/' + file, import.meta.url), 'utf8');

test('personal services retain wallet, appearance, notifications and rider shortcuts', async () => {
  const source = await read('views/MyInformation.vue');
  for (const path of ['/assets', '/preferences', '/notifications', '/rider/dashboard?tab=active', '/rider/dashboard?tab=history']) {
    assert.ok(source.includes(`to="${path}"`), path);
  }
  assert.match(source, /updateMyAvatar/);
  assert.match(source, /getAuthRole\(\) === 'user'/);
});

test('layout-container animations cannot replace fixed element positioning transforms', async () => {
  const css = await read('assets/styles/interaction-motion.css');
  for (const name of ['home-content-in', 'detail-section-in']) {
    const keyframes = css.split(`@keyframes ${name}`)[1].split('\n}')[0];
    assert.doesNotMatch(keyframes, /transform\s*:/);
  }
});

test('location dialog escapes the animated header and surfaces configuration errors', async () => {
  const source = await read('views/Index.vue');
  assert.match(source, /<Teleport to="body">[\s\S]*role="dialog"[\s\S]*locationError[\s\S]*<\/Teleport>/);
  assert.match(source, /MapLocationPicker[\s\S]*@select="acceptMapLocation"/);
});

test('pickup is not incorrectly disabled by dine-in seating metadata', async () => {
  for (const file of ['views/BusinessInfo.vue', 'components/CheckoutPage.vue']) {
    assert.doesNotMatch(await read(file), /dineInAvailable\s*(?:!==|===)\s*(?:true|false)/);
  }
});

test('sticky store header is not translated like a viewport-fixed legacy header', async () => {
  assert.match(await read('assets/styles/fwl-alignment.css'), /\.business-detail-page > \.store-header \{ left: auto; right: auto; transform: none;/);
});

for (const name of ['Index', 'MyInformation', 'AdminHome', 'AdminUser', 'AdminBusiness', 'AdminShop']) {
  test(`${name} updated template compiles`, async () => {
    const source = await read(`views/${name}.vue`);
    const { descriptor, errors } = parse(source);
    assert.deepEqual(errors, []);
    assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: name + '.vue', id: 'regression-' + name }).errors, []);
  });
}

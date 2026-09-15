import test from 'node:test';
import assert from 'node:assert/strict';
import { readdir, readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { parse } from '@vue/compiler-sfc';
const require = createRequire(import.meta.url);
const postcss = require('postcss');
const values = require('postcss-value-parser');
const { skinColor } = require('../scripts/skin-colors.cjs');
const root = new URL('../src/', import.meta.url);

async function files(dir) {
  const entries = await readdir(dir, { withFileTypes: true });
  return (await Promise.all(entries.map(entry => entry.isDirectory()
    ? files(new URL(entry.name + '/', dir)) : new URL(entry.name, dir)))).flat();
}

test('production component CSS has no hard-coded platform blue outside theme swatches or variable fallbacks', async () => {
  const failures = [];
  for (const url of await files(root)) {
    if (!/\.(vue|css)$/.test(url.pathname) || /auth-preview|global\.css/.test(url.pathname)) continue;
    const source = await readFile(url, 'utf8');
    const styles = url.pathname.endsWith('.vue') ? parse(source).descriptor.styles.map(s => s.content) : [source];
    for (const css of styles) postcss.parse(css).walkDecls(decl => {
      if (decl.parent.selector?.includes('theme-preview')) return;
      values(decl.value).walk(node => {
        if (node.type === 'function' && ['var', 'url'].includes(node.value)) return false;
        if ((node.type === 'word' && /^#(?:[\da-f]{3,4}|[\da-f]{6}|[\da-f]{8})$/i.test(node.value)) ||
            (node.type === 'function' && /^rgba?$/.test(node.value))) {
          if (skinColor(values.stringify(node))) failures.push(`${url.pathname}: ${decl.prop}: ${decl.value}`);
          return false;
        }
      });
    });
  }
  assert.deepEqual(failures, []);
});

test('theme classification preserves semantic success, warning and error colors', () => {
  for (const color of ['#f00', '#28a745', '#ffc107', '#fff', '#666']) assert.equal(skinColor(color), null);
  assert.equal(skinColor('#0097ff').token, 'brand');
  assert.equal(skinColor('#f7fbff').token, 'surface');
  assert.equal(skinColor('#10283980').channels[3], 128 / 255);
  assert.equal(skinColor('rgba(var(--skin-brand-rgb, 0, 151, 255), .2)'), null);
});

test('route rendering does not wait for fragment-root leave transitions', async () => {
  const app = await readFile(new URL('App.vue', root), 'utf8');
  const template = parse(app).descriptor.template.content;
  assert.doesNotMatch(template, /<transition\b/i);
  assert.match(template, /<component :is="Component"/);
});

test('saved theme applies before mounting and warm/mint do not paint every header', async () => {
  const main = await readFile(new URL('main.js', root), 'utf8');
  const themeSetup = main.indexOf('applyTheme(themeForRoute(window.location.pathname, getStoredTheme()), { persist: false })');
  assert.ok(themeSetup >= 0 && themeSetup < main.indexOf('createApp(App)'));
  const css = await readFile(new URL('assets/styles/global.css', root), 'utf8');
  assert.doesNotMatch(css, /html\[data-theme="(?:warm|mint)"\] header/);
  assert.doesNotMatch(css, /html[^\n]*data-theme[^\n]*\.login-page/);
  for (const theme of ['warm', 'mint']) {
    assert.match(css, new RegExp(`html\\[data-theme="${theme}"\\] \\{[\\s\\S]*?--skin-brand:`));
  }
});

test('home search stays in its sticky parent without viewport-relative inline positioning', async () => {
  const source = await readFile(new URL('views/Index.vue', root), 'utf8');
  const { descriptor } = parse(source);
  assert.doesNotMatch(descriptor.script.content, /search\.style\.(?:position|left|top|transform)/);
  const styles = descriptor.styles.map(style => style.content).join('\n');
  assert.match(styles, /\.home-page \.search \{\s*position: sticky;\s*top: 0;/);
  assert.match(styles, /\.home-page \.search \.search-fixed-top \{\s*position: relative;\s*inset: auto;\s*transform: none;/);
});

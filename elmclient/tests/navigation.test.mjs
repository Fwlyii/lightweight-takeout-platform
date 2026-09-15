import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { createRequire } from 'node:module';
import { createRouter, createMemoryHistory } from 'vue-router';

const require = createRequire(import.meta.url);
const { parse } = require('@babel/parser');
const root = new URL('../src/', import.meta.url);
const read = path => readFile(new URL(path, root), 'utf8');
function find(node, predicate) {
  if (!node || typeof node !== 'object') return null;
  if (predicate(node)) return node;
  for (const child of Object.values(node)) {
    if (Array.isArray(child)) { for (const item of child) { const result = find(item, predicate); if (result) return result; } }
    else if (child && typeof child === 'object') { const result = find(child, predicate); if (result) return result; }
  }
  return null;
}

// Use the application's real route table to validate paths and portal metadata.
const routerSource = await read('router/index.js');
const routeTree = parse(routerSource, { sourceType: 'module' });
const routesNode = find(routeTree, node => node.type === 'VariableDeclarator' && node.id.name === 'routes');
const routeExpression = routerSource.slice(routesNode.init.start, routesNode.init.end);
const components = [...new Set([...routeExpression.matchAll(/component:\s*(\w+)/g)].map(match => match[1]))];
const routes = new Function(...components, `return ${routeExpression}`)(...components.map(() => ({})));
const resolver = createRouter({ history: createMemoryHistory(), routes });

let navigation = {};
try { navigation = await import('../src/utils/backNavigation.js'); }
catch (error) { if (error.code !== 'ERR_MODULE_NOT_FOUND') throw error; }

function journey(initial = ['/index', '/ai-chat']) {
  const entries = [...initial], calls = [];
  let position = entries.length - 1;
  const resolve = value => resolver.resolve(value);
  const route = new Proxy({}, { get: (_, key) => resolve(entries[position])[key] });
  const router = {
    resolve,
    options: { history: { get state() { return { back: entries[position - 1] || null }; } } },
    push(value) { calls.push(['push', value]); entries.splice(position + 1); entries.push(resolve(value).fullPath); position++; },
    replace(value) { calls.push(['replace', value]); entries[position] = resolve(value).fullPath; },
    back() { calls.push(['back']); if (position > 0) position--; },
    go(delta) { calls.push(['go', delta]); position = Math.max(0, Math.min(entries.length - 1, position + delta)); }
  };
  return { router, route, entries, calls, current: () => entries[position] };
}

async function namedHandler(file, name, j) {
  const source = (await read(file)).match(/<script(?: setup)?>([\s\S]*?)<\/script>/)[1];
  const tree = parse(source, { sourceType: 'module' });
  const node = find(tree, value => value.type === 'VariableDeclarator' && value.id.name === name);
  assert.ok(node, `${file} must expose its actual ${name} handler`);
  const bindings = { ...navigation, router: j.router, route: j.route,
    activeTool: { get value() { return j.route.meta.aiTool || 'chat'; }, set value(_) {} },
    smartQuery: { value: '' }, findSmartFoods() {} };
  return new Function(...Object.keys(bindings), `return (${source.slice(node.init.start, node.init.end)})`)(...Object.values(bindings));
}

async function returnButton(file, j) {
  const source = await read(file);
  const pageHeader = source.match(/<PageHeader\b([^>]*)>/);
  if (pageHeader) {
    const action = pageHeader[1].match(/:back-action="([^"]+)"/)?.[1];
    if (action) return (await namedHandler(file, action, j))();
    const backTo = pageHeader[1].match(/back-to="([^"]+)"/)?.[1];
    assert.ok((await read('components/PageHeader.vue')).includes('navigateBack(router, route)'));
    return backTo ? j.router.push(backTo) : navigation.navigateBack(j.router, j.route);
  }
  const adminHeader = source.match(/<AdminPageHeader\b([^>]*)\/>/);
  if (adminHeader) {
    const component = await read('components/AdminPageHeader.vue');
    const backTo = adminHeader[1].match(/back-to="([^"]+)"/)?.[1]
      || component.match(/backTo:.*?default: '([^']+)'/)?.[1];
    const click = component.match(/@click="([^"]+)"/)[1];
    assert.ok(backTo && resolver.resolve(backTo).name);
    return new Function('router', 'backTo', `return (${click})`)(j.router, backTo);
  }
  if (/const goBack\s*=/.test(source)) return (await namedHandler(file, 'goBack', j))();
  const headerEnd = source.indexOf('</header>');
  const header = source.slice(0, headerEnd >= 0 ? headerEnd + 9 : source.indexOf('</template>'));
  const click = [...header.matchAll(/@click="([^"]+)"/g)][0]?.[1];
  assert.ok(click, `${file} must contain a return control`);
  const bindings = { ...navigation, router: j.router, route: j.route, $router: j.router, $route: j.route };
  return new Function(...Object.keys(bindings), `return (${click})`)(...Object.values(bindings));
}

test('AI main -> tool -> return -> return exits to the original page', async () => {
  const j = journey();
  const tool = await namedHandler('views/AiChat.vue', 'openTool', j);
  const back = await namedHandler('views/AiChat.vue', 'goBack', j);
  tool({ key: 'recommend', path: '/ai-chat/recommend' }); back(); back();
  assert.equal(j.current(), '/index');
});

for (const path of ['/ai-chat/recommend', '/ai-chat/voice', '/ai-chat/image']) {
  test(`AI tab changes do not grow browser history: ${path}`, async () => {
    const j = journey(); const open = await namedHandler('views/AiChat.vue', 'openTool', j);
    open({ key: 'voice', path });
    assert.equal(j.entries.length, 2);
    assert.equal(j.current(), path);
    j.router.back(); assert.equal(j.current(), '/index');
  });
  test(`directly opened AI subpage can exit in two clicks: ${path}`, async () => {
    const j = journey([path]); const back = await namedHandler('views/AiChat.vue', 'goBack', j);
    back(); assert.equal(j.current(), '/ai-chat'); back(); assert.equal(j.current(), '/index');
  });
}
test('image keyword -> recommendation uses the same AI history entry', async () => {
  const j = journey(['/index', '/ai-chat/image']);
  (await namedHandler('views/AiChat.vue', 'searchKeyword', j))('牛肉面');
  assert.equal(j.current(), '/ai-chat/recommend'); assert.equal(j.entries.length, 2);
});
test('the chat tab itself replaces the current AI subpage', async () => {
  const j = journey(['/index', '/ai-chat/voice']);
  const source = await read('views/AiChat.vue');
  const click = source.match(/@click="([^"]+)"[^>]*><i class="fa fa-comments-o"/)[1];
  const bindings = { ...navigation, router: j.router, route: j.route };
  new Function(...Object.keys(bindings), click)(...Object.values(bindings));
  assert.equal(j.current(), '/ai-chat'); assert.equal(j.entries.length, 2);
});
for (const previous of [null, '/ai-chat/voice', '/ai-chat?old=1', '/login', '/register', '/missing',
  '/admin/home', '//evil.example', '/\\evil.example', 'https://evil.example']) {
  test(`AI exit falls back safely for absent or invalid history: ${previous}`, async () => {
    const j = journey(previous ? [previous, '/ai-chat'] : ['/ai-chat']);
    (await namedHandler('views/AiChat.vue', 'goBack', j))();
    assert.equal(j.current(), '/index');
  });
}
test('a refreshed AI entry retains a valid business-list origin and query', async () => {
  const source = '/businessList?orderTypeId=2';
  const j = journey([source, '/ai-chat']);
  (await namedHandler('views/AiChat.vue', 'goBack', j))();
  assert.equal(j.current(), source);
});
test('a late second AI back click cannot re-enter AI after leaving', async () => {
  const j = journey(['/businessList', '/index', '/ai-chat']);
  const back = await namedHandler('views/AiChat.vue', 'goBack', j);
  back(); back(); assert.equal(j.current(), '/index');
});

const directPages = [
  ['components/BackButton.vue', '/search', '/index'],
  ['views/BusinessInfo.vue', '/businessInfo?businessId=7', '/index'],
  ['views/ListDetail.vue', '/listDetail?orderId=1', '/orderList'],
  ['views/Assets.vue', '/assets', '/myInformation'],
  ['views/Notifications.vue', '/notifications', '/myInformation'],
  ['views/Notifications.vue', '/notifications?role=rider', '/rider/dashboard'],
  ['views/MerchantApply.vue', '/merchant/apply', '/index'],
  ['views/MerchantReviews.vue', '/merchant/reviews', '/merchant/business'],
  ['views/MerchantBusinessInfo.vue', '/merchant/businessInfo?businessId=1', '/merchant/business'],
  ['views/AdminUser.vue', '/admin/user', '/admin/home'],
  ['views/AdminBusiness.vue', '/admin/business', '/admin/home'],
  ['views/AdminShop.vue', '/admin/shop?ownerId=2', '/admin/business']
];
for (const [file, path, expected] of directPages) {
  test(`${path}: directly opened detail has an in-app fallback`, async () => {
    const j = journey([path]); await returnButton(file, j); assert.equal(j.current(), expected);
  });
}
for (const query of ['businessId=7', 'businessId=0', 'businessId=-2', 'businessId=1.5', 'businessId=oops', '']) {
  test(`cart's return-to-merchant button uses a validated merchant id: ${query}`, async () => {
    const j = journey(['/ai-chat', `/cart${query ? '?' + query : ''}`]);
    await returnButton('views/Cart.vue', j);
    assert.equal(j.current(), query === 'businessId=7' ? '/businessInfo?businessId=7' : '/index');
  });
}
test('valid same-portal previous page is preserved by the shared return control', async () => {
  const j = journey(['/businessList?orderTypeId=3', '/search']);
  await returnButton('components/BackButton.vue', j);
  assert.equal(j.current(), '/businessList?orderTypeId=3');
});
test('a stale login entry cannot trap the shared return control', async () => {
  const j = journey(['/login?redirect=/search', '/search']);
  await returnButton('components/BackButton.vue', j); assert.equal(j.current(), '/index');
});
test('shared return helper rejects cross-portal and payment-completion origins', () => {
  assert.equal(typeof navigation.navigateBack, 'function');
  for (const previous of ['/admin/home', '/successfulPayment', '/payment', '/search', '/missing']) {
    const j = journey([previous, '/search']); navigation.navigateBack(j.router, j.route);
    assert.equal(j.current(), '/index');
  }
});

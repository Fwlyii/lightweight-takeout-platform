import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { ref, computed, reactive, watch, nextTick, effectScope } from 'vue';
import { parse, compileTemplate } from '@vue/compiler-sfc';

const source = await readFile(new URL('../src/views/RiderDashboard.vue', import.meta.url), 'utf8');
const script = parse(source).descriptor.scriptSetup.content.replace(/^import .*;\r?\n/gm, '');
function dashboard() {
  const scope = effectScope(), route = reactive({ query: { tab: 'available' } });
  const calls = [], notices = [], state = { status: 'WAITING_RIDER', online: true };
  const task = { id: 2, taskStatus: 'WAITING_RIDER' };
  let mounted, unmounted, dismissFeedback, fail = false, release;
  const request = {
    async get(path) {
      if (path === '/api/v1/riders/me') return { data: { auditStatus: 1, online: state.online } };
      if (path.includes('available-tasks')) return { data: state.status === 'WAITING_RIDER' ? [task] : [] };
      return { data: path.endsWith('active=true') && state.status !== 'WAITING_RIDER' ? [{ ...task, taskStatus: state.status }] : [] };
    },
    async post(path) {
      calls.push(path);
      if (release) await new Promise(resolve => { release = resolve; });
      if (fail) throw Error('unavailable');
      const action = path.split('/').pop();
      state.status = { accept: 'ACCEPTED', 'arrive-store': 'ARRIVED_STORE', pickup: 'DELIVERING', deliver: 'DELIVERED', exceptions: 'EXCEPTION' }[action];
    },
    async patch(path, body) { calls.push(path); state.online = body.online; return { data: { auditStatus: 1, online: state.online } }; }
  };
  const bindings = {
    ref, computed, reactive, watch, onMounted: fn => { mounted = fn; }, onUnmounted: fn => { unmounted = fn; },
    useRiderIndicator() {}, setTimeout: callback => { dismissFeedback = callback; return 1; }, clearTimeout: () => { dismissFeedback = null; },
    useRoute: () => route, useRouter: () => ({ replace: async value => { route.query = value.query || {}; } }),
    request, toast: Object.fromEntries(['error', 'success', 'warning'].map(key => [key, text => notices.push([key, text])])),
    createRealtimeConnection: () => ({ start() {}, stop() {} }),
    taskStatusText: value => value, openDeliveryNavigation() {}
  };
  const view = scope.run(() => new Function(...Object.keys(bindings), script + ';return {act, selectTab, toggleOnline, activeTab, selectedTaskId, successNotice, visibleTasks, displayedTasks, actingId, profile, progressIndex, openException, submitException, exceptionForm, exceptionModal, actionFeedback, actionError, isActionPending, isActionSuccess, newOrderIds, finishNewOrder, loadTasks};')(...Object.values(bindings)));
  return { view, route, calls, notices, state, mounted: () => mounted(), dispose: () => { unmounted?.(); scope.stop(); }, dismiss: () => dismissFeedback?.(), setFailure: () => { fail = true; }, hold: () => { release = true; }, release: () => { const fn = release; release = null; fn(); } };
}
test('accept opens the active task details and preserves the route through each delivery action', async () => {
  const d = dashboard(); await d.mounted();
  for (const [action, status, step] of [['accept', 'ACCEPTED', 0], ['arrive-store', 'ARRIVED_STORE', 1], ['pickup', 'DELIVERING', 2], ['deliver', 'DELIVERED', 2]]) {
    await d.view.act({ id: 2 }, action); await nextTick();
    assert.equal(d.route.query.tab, 'active');
    assert.equal(d.view.selectedTaskId.value, 2);
    assert.equal(d.view.displayedTasks.value[0].taskStatus, status);
    assert.equal(d.view.progressIndex({ taskStatus: status }), step);
    assert.equal(d.view.actingId.value, null);
  }
  assert.equal(d.calls.length, 4); d.dispose();
});
test('failed acceptance retains the available list and never shows success details', async () => {
  const d = dashboard(); await d.mounted(); d.setFailure();
  await d.view.act({ id: 2 }, 'accept');
  assert.equal(d.view.activeTab.value, 'available'); assert.equal(d.view.selectedTaskId.value, null);
  assert.equal(d.view.successNotice.value, false); assert.equal(d.view.actingId.value, null);
  assert.match(d.view.actionError.value, /操作失败/);
  assert.equal(d.view.actionFeedback.value, null); d.dispose();
});
test('pending delivery mutations are single-flight', async () => {
  const d = dashboard(); await d.mounted(); d.hold();
  const pending = d.view.act({ id: 2 }, 'accept');
  assert.equal(d.view.isActionPending({ id: 2 }), true);
  assert.equal(d.view.isActionPending({ id: 3 }), false);
  assert.equal(d.view.isActionSuccess({ id: 2 }, 'accept'), false);
  await d.view.act({ id: 2 }, 'accept'); assert.equal(d.calls.length, 1);
  d.release(); await pending; assert.equal(d.view.actingId.value, null); d.dispose();
});
test('successful feedback never delays progression and dismissing it cannot mutate delivery state', async () => {
  const d = dashboard(); await d.mounted();
  await d.view.act({ id: 2, riderFee: 3 }, 'accept');
  assert.equal(d.view.actionFeedback.value.phase, 'success');
  assert.equal(d.view.activeTab.value, 'active');
  assert.equal(d.view.actingId.value, null);
  const before = [...d.calls];
  d.dismiss();
  assert.equal(d.view.actionFeedback.value, null);
  assert.equal(d.state.status, 'ACCEPTED');
  assert.deepEqual(d.calls, before); d.dispose();
});
test('new order attention runs once and repeated polling does not re-arm it', async () => {
  const d = dashboard(); await d.mounted(); await nextTick();
  assert.equal(d.view.newOrderIds.value.has(2), true);
  d.view.finishNewOrder(2, { animationName: 'rider-order-glow' });
  await d.view.loadTasks(); await nextTick();
  assert.equal(d.view.newOrderIds.value.has(2), false); d.dispose();
});
test('footer query changes cannot strand a selected task from a different list', async () => {
  const d = dashboard(); await d.mounted(); await d.view.act({ id: 2 }, 'accept');
  d.route.query = { tab: 'available' }; await nextTick();
  assert.equal(d.view.activeTab.value, 'available'); assert.equal(d.view.selectedTaskId.value, null); d.dispose();
});
test('offline mode clears available tasks and uses the existing online endpoint', async () => {
  const d = dashboard(); await d.mounted(); await d.view.toggleOnline();
  assert.equal(d.view.profile.value.online, false); assert.equal(d.view.visibleTasks.value.length, 0);
  assert.deepEqual(d.calls, ['/api/v1/riders/me/online']); d.dispose();
});
test('exception form keeps the existing delivery exception contract', async () => {
  const d = dashboard(); await d.mounted(); await d.view.act({ id: 2 }, 'accept');
  d.view.openException({ id: 2 }); d.view.exceptionForm.description = '商家暂未出餐';
  await d.view.submitException(); assert.equal(d.calls.at(-1), '/api/v1/delivery-tasks/2/exceptions');
  assert.equal(d.view.exceptionModal.value, null); assert.equal(d.state.status, 'EXCEPTION'); d.dispose();
});
test('rider profile and footer compile, and all illustration assets are shipped', async () => {
  for (const file of ['views/RiderDashboard.vue', 'views/MyInformation.vue', 'components/RiderFooter.vue', 'components/RiderActionFeedback.vue']) {
    const { descriptor } = parse(await readFile(new URL('../src/' + file, import.meta.url), 'utf8'));
    assert.deepEqual(compileTemplate({ source: descriptor.template.content, filename: file, id: 'rider-ui' }).errors, []);
  }
  for (const asset of ['profile-banner', 'dispatch-hero', 'orders-hero']) {
    const bytes = await readFile(new URL('../public/images/rider/' + asset + '.jpg', import.meta.url));
    assert.equal(bytes.subarray(0, 2).toString('hex'), 'ffd8');
  }
  assert.doesNotMatch(source, /预计送达|3分钟内接单|已实名认证|联系商家/);
});

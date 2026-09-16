import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { ref, computed } from 'vue';
import { getBusinessDistanceKm } from '../src/utils/businessPresentation.js';
import * as assistantData from '../src/utils/assistantData.js';
import { createAssistantBusinessLookup } from '../src/utils/assistantBusinessData.js';

const source = await readFile(new URL('../src/components/AiAssistantDrawer.vue', import.meta.url), 'utf8');
const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
  .replace(/^import .*\r?\n/gm, '').replace('export default', 'return');

function drawer(overrides = {}) {
  let unmount, grant, recorder;
  const track = { stopped: false, stop() { this.stopped = true; } };
  class Recorder {
    constructor() { recorder = this; this.state = 'inactive'; }
    start() { this.state = 'recording'; }
    stop() { this.state = 'inactive'; queueMicrotask(() => this.onstop?.()); }
  }
  const props = { open: true };
  const bindings = {
    createAssistantBusinessLookup, request: {},
    ref, computed, nextTick: fn => fn(), watch() {}, onBeforeUnmount: fn => { unmount = fn; },
    useRouter: () => ({}), formatMoney: String, getBusinessDistanceKm, ...assistantData,
    navigator: { mediaDevices: { getUserMedia: () => new Promise(resolve => { grant = () => resolve({ getTracks: () => [track] }); }) } },
    window: { MediaRecorder: Recorder, setInterval: () => 1, clearInterval() {} },
    MediaRecorder: Recorder, ...overrides,
  };
  const component = new Function(...Object.keys(bindings), script)(...Object.values(bindings)).setup(props, { emit() {} });
  component.capabilities.value.speechRecognition = true;
  return { component, props, track, grant: () => grant(), unmount: () => unmount(), recorder: () => recorder };
}

test('closing and reopening ignores an old chat result and does not unlock a newer request', async () => {
  const resolvers = [];
  const d = drawer({ aiChatService: { sendMessage: () => new Promise(resolve => resolvers.push(resolve)) } });
  const first = d.component.send('旧问题'); d.component.close();
  const second = d.component.send('新问题');
  resolvers[0]({ success: true, data: { message: '旧回复', candidates: [] } }); await first;
  assert.equal(d.component.loading.value, true);
  assert.ok(!d.component.messages.value.some(item => item.text === '旧回复'));
  resolvers[1]({ success: true, data: { message: '新回复', candidates: [] } }); await second;
  assert.equal(d.component.loading.value, false);
  assert.ok(d.component.messages.value.some(item => item.text === '新回复'));
});
test('failed chat is shown as an error and does not silently request recommendations', async () => {
  let calls = 0;
  const d = drawer({ aiChatService: { sendMessage: async () => ({ success: false, error: '连接超时' }) }, request: { post: async () => { calls++; } } });
  await d.component.send('推荐饭');
  assert.equal(d.component.messages.value.at(-1).text, '连接超时'); assert.equal(calls, 0);
});
test('rapid quick-chip clicks are single-flight and malformed candidates cannot crash rendering', async () => {
  let resolve, count = 0;
  const d = drawer({ request: { post: () => { count++; return new Promise(done => { resolve = done; }); } } });
  const first = d.component.runChip({ label: '午饭', query: '饭' });
  await d.component.runChip({ label: '午饭', query: '饭' }); assert.equal(count, 1);
  resolve({ success: true, data: [null, {}, { foodId: 1, foodName: '饭' }, { foodId: 1 }] }); await first;
  assert.equal(d.component.candidates.value.length, 1);
});
test('recorder startup failure releases acquired microphone tracks', async () => {
  const d = drawer({ MediaRecorder: class { constructor() { throw Error('unsupported codec'); } } });
  const pending = d.component.startRecording(); d.grant(); await pending;
  assert.equal(d.track.stopped, true); assert.equal(d.component.recording.value, false);
  assert.match(d.component.messages.value.at(-1).text, /无法启动录音/);
});
test('Chinese composition Enter never submits the drawer draft', () => {
  const d = drawer({ aiChatService: { sendMessage() { assert.fail('must not send'); } } });
  d.component.draft.value = '牛肉';
  d.component.handleEnter({ isComposing: true, preventDefault() { assert.fail('must not cancel composition'); } });
  assert.equal(d.component.messages.value.length, 0);
});

test('recommendation cards remain renderable without distance data', () => {
  const { component } = drawer();
  for (const candidate of [{ foodId: 27 }, { distanceKm: null }, { distanceKm: '' }, { distanceKm: -1 }, { distanceKm: 'invalid' }, { distanceKm: Infinity }]) {
    assert.equal(component.distanceText(candidate), '距离暂无');
  }
});
test('known distances keep their value and unit, including zero and legacy distance fields', () => {
  const { component } = drawer();
  assert.equal(component.distanceText({ distanceKm: 0 }), '0.0km');
  assert.equal(component.distanceText({ distanceKm: 2.34 }), '2.3km');
  assert.equal(component.distanceText({ distance: '1.8' }), '1.8km');
  assert.equal(component.distanceText({ distanceKm: 0, distance: 8 }), '0.0km');
  assert.doesNotMatch(source, /\{\{\s*distanceText\(candidate\)\s*\}\}km/);
});

for (const action of ['close', 'unmount']) {
  test(`drawer releases a late microphone permission after ${action}`, async () => {
    const d = drawer();
    const pending = d.component.startRecording();
    if (action === 'unmount') d.unmount(); else { d.props.open = false; d.component.cancelRecording(); }
    d.grant(); await pending;
    assert.equal(d.track.stopped, true);
    assert.equal(d.recorder(), undefined);
  });
}

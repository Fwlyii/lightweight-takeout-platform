import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { ref, computed } from 'vue';
import * as assistantData from '../src/utils/assistantData.js';
import { createAssistantBusinessLookup } from '../src/utils/assistantBusinessData.js';
import { businessDistanceText } from '../src/utils/businessDistance.js';

const source = await readFile(new URL('../src/views/AiChat.vue', import.meta.url), 'utf8');
const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
  .replace(/^import .*\r?\n/gm, '').replace('export default', 'return');
function page({ delayedPermission = false, overrides = {} } = {}) {
  let unmount, recorder, resolvePermission;
  const posts = [], cart = [], tracks = { stopped: false, stop() { this.stopped = true; } };
  const stream = { getTracks: () => [tracks] };
  class Recorder {
    static isTypeSupported() { return true; }
    constructor() { recorder = this; this.state = 'inactive'; this.mimeType = 'audio/webm'; }
    start() { this.state = 'recording'; }
    stop() {
      this.state = 'inactive';
      queueMicrotask(() => { this.ondataavailable?.({ data: new Blob(['test-audio']) }); this.onstop?.(); });
    }
  }
  const bindings = { createAssistantBusinessLookup, businessDistanceText, ...assistantData, ref, computed, defineComponent: v => v, h() {}, nextTick: async () => {},
    onMounted() {}, onBeforeUnmount: fn => { unmount = fn; }, watch() {},
    useRouter: () => ({ push() {}, back() {} }), useRoute: () => ({ meta: {} }),
    aiChatService: {}, formatSafeMessage: v => v,
    addCartItem: async (...args) => cart.push(args),
    request: { post: async (...args) => { posts.push(args); return { success: true, data: [] }; } },
    window: { MediaRecorder: Recorder, setInterval: () => 1, clearInterval() {} }, MediaRecorder: Recorder,
    navigator: { mediaDevices: { getUserMedia: () => delayedPermission
      ? new Promise(resolve => { resolvePermission = resolve; }) : Promise.resolve(stream) } }, ...overrides };
  const component = new Function(...Object.keys(bindings), script)(...Object.values(bindings)).setup();
  return { component, posts, cart, tracks, unmount: () => unmount(), recorder: () => recorder,
    grant: () => resolvePermission(stream) };
}

test('clearing a pending chat prevents a late reply from resurrecting it', async () => {
  let resolve;
  const p = page({ overrides: { window: { confirm: () => true }, aiChatService: { detectChatType: () => 'general', sendMessage: () => new Promise(done => { resolve = done; }) } } });
  p.component.inputMessage.value = '你好'; const pending = p.component.sendMessage();
  await Promise.resolve(); p.component.clearChat();
  resolve({ success: true, data: { message: '旧回复', sessionId: 'old' } }); await pending;
  assert.deepEqual(p.component.messages.value, []); assert.equal(p.component.isTyping.value, false);
});
test('candidate add-to-cart is single-flight per food and releases the button on failure', async () => {
  let reject, count = 0;
  const p = page({ overrides: { addCartItem: () => { count++; return new Promise((_, fail) => { reject = fail; }); } } });
  const first = p.component.addFoodToCart({ foodId: 1, foodName: '饭' });
  await p.component.addFoodToCart({ foodId: 1, foodName: '饭' }); assert.equal(count, 1);
  assert.equal(p.component.addingFoods.value.has(1), true);
  reject(Error('库存不足')); await first;
  assert.equal(p.component.addingFoods.value.size, 0); assert.match(p.component.messages.value.at(-1).content, /库存不足/);
});
test('history endpoint failures are visible instead of appearing as an empty history', async () => {
  const p = page({ overrides: { aiChatService: { getChatHistory: async () => ({ success: false, error: '会话读取失败' }) } } });
  await p.component.loadChatHistory(); assert.equal(p.component.historyError.value, '会话读取失败');
  assert.equal(p.component.loadingHistory.value, false);
});
test('only the last clicked history session can replace the conversation', async () => {
  const resolves = [];
  const p = page({ overrides: { aiChatService: { getChatHistoryBySession: () => new Promise(resolve => resolves.push(resolve)) } } });
  const first = p.component.loadHistorySession('old'), second = p.component.loadHistorySession('new');
  resolves[1]({ success: true, data: [{ userMessage: 'new', aiResponse: 'new response' }] }); await second;
  resolves[0]({ success: true, data: [{ userMessage: 'old', aiResponse: 'old response' }] }); await first;
  assert.equal(p.component.messages.value[0].content, 'new');
});
test('invalid budgets never send recommendation requests and are visible on tool pages', async () => {
  const p = page(); p.component.activeTool.value = 'recommend';
  for (const value of [-1, 0, Infinity, 'invalid', 10000]) {
    p.component.smartBudget.value = value; await p.component.findSmartFoods();
    assert.match(p.component.toolFeedback.value, /预算/);
  }
  assert.equal(p.posts.length, 0);
});
test('recommendation payloads tolerate null candidate entries', async () => {
  const p = page({ overrides: { request: { post: async () => ({ success: true, data: [null, {}, { foodId: 3, foodName: '饭' }] }) } } });
  await p.component.findSmartFoods(); assert.equal(p.component.smartFoods.value.length, 1);
});
test('Chinese composition Enter never sends the full-page draft', () => {
  const p = page(); p.component.inputMessage.value = '牛肉';
  p.component.handleKeyDown({ key: 'Enter', isComposing: true, preventDefault() { assert.fail('composition interrupted'); } });
  assert.equal(p.component.messages.value.length, 0);
});

test('voice rematching includes the edited quantity in the total budget query', async () => {
  const p = page(); p.component.voiceDraft.value = { query: '面', quantity: 3, budget: 50 };
  await p.component.refreshVoiceCandidates();
  assert.equal(p.posts[0][1].quantity, 3);
});
for (const quantity of [1.5, 0, -1, 100, NaN]) {
  test(`invalid cart quantity is rejected without a request: ${quantity}`, async () => {
    const p = page(); await p.component.addFoodToCart({ foodId: 1, foodName: '面' }, quantity);
    assert.equal(p.cart.length, 0);
  });
}
test('leaving while recording releases the microphone without uploading audio', async () => {
  const p = page(); await p.component.toggleRecording(); p.unmount();
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(p.tracks.stopped, true);
  assert.equal(p.posts.length, 0);
});
test('a microphone permission result arriving after unmount is immediately released', async () => {
  const p = page({ delayedPermission: true }); const pending = p.component.toggleRecording();
  p.unmount(); p.grant(); await pending;
  assert.equal(p.tracks.stopped, true);
  assert.equal(p.recorder(), undefined);
  assert.equal(p.posts.length, 0);
});
test('explicit stop still submits the recording once', async () => {
  const p = page(); await p.component.toggleRecording(); await p.component.toggleRecording();
  await new Promise(resolve => setImmediate(resolve));
  assert.equal(p.tracks.stopped, true);
  assert.equal(p.posts.length, 1);
  assert.equal(p.posts[0][0], '/api/v1/voice-order-drafts');
});

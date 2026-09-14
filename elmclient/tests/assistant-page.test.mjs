import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { ref, computed } from 'vue';

const source = await readFile(new URL('../src/views/AiChat.vue', import.meta.url), 'utf8');
const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
  .replace(/^import .*\r?\n/gm, '').replace('export default', 'return');
function page({ delayedPermission = false } = {}) {
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
  const bindings = { ref, computed, defineComponent: v => v, h() {}, nextTick: async () => {},
    onMounted() {}, onBeforeUnmount: fn => { unmount = fn; }, watch() {},
    useRouter: () => ({ push() {}, back() {} }), useRoute: () => ({ meta: {} }),
    aiChatService: {}, formatSafeMessage: v => v,
    addCartItem: async (...args) => cart.push(args),
    request: { post: async (...args) => { posts.push(args); return { success: true, data: [] }; } },
    window: { MediaRecorder: Recorder, setInterval: () => 1, clearInterval() {} }, MediaRecorder: Recorder,
    navigator: { mediaDevices: { getUserMedia: () => delayedPermission
      ? new Promise(resolve => { resolvePermission = resolve; }) : Promise.resolve(stream) } } };
  const component = new Function(...Object.keys(bindings), script)(...Object.values(bindings)).setup();
  return { component, posts, cart, tracks, unmount: () => unmount(), recorder: () => recorder,
    grant: () => resolvePermission(stream) };
}

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

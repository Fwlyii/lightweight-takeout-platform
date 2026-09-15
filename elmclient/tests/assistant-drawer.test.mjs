import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { ref, computed } from 'vue';

const source = await readFile(new URL('../src/components/AiAssistantDrawer.vue', import.meta.url), 'utf8');
const script = source.match(/<script>([\s\S]*?)<\/script>/)[1]
  .replace(/^import .*\r?\n/gm, '').replace('export default', 'return');

function drawer() {
  let unmount, grant, recorder;
  const track = { stopped: false, stop() { this.stopped = true; } };
  class Recorder {
    constructor() { recorder = this; this.state = 'inactive'; }
    start() { this.state = 'recording'; }
    stop() { this.state = 'inactive'; queueMicrotask(() => this.onstop?.()); }
  }
  const props = { open: true };
  const bindings = {
    ref, computed, nextTick: fn => fn(), watch() {}, onBeforeUnmount: fn => { unmount = fn; },
    useRouter: () => ({}), formatMoney: String,
    navigator: { mediaDevices: { getUserMedia: () => new Promise(resolve => { grant = () => resolve({ getTracks: () => [track] }); }) } },
    window: { MediaRecorder: Recorder, setInterval: () => 1, clearInterval() {} },
    MediaRecorder: Recorder,
  };
  const component = new Function(...Object.keys(bindings), script)(...Object.values(bindings)).setup(props, { emit() {} });
  component.capabilities.value.speechRecognition = true;
  return { component, props, track, grant: () => grant(), unmount: () => unmount(), recorder: () => recorder };
}

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

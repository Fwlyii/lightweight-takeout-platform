import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
import { parse } from '@vue/compiler-sfc';
import postcss from 'postcss';

test('AI launcher stays inset from the shared page edge at every breakpoint', async () => {
  const source = await readFile(new URL('../src/components/AiChatbot.vue', import.meta.url), 'utf8');
  const css = postcss.parse(parse(source).descriptor.styles[0].content);
  const rightRules = [];
  css.walkRules('.ai-chat-launcher', rule => rule.walkDecls('right', decl => rightRules.push(decl.value)));
  assert.deepEqual(rightRules, ['max(20px, calc((100vw - var(--app-width, 600px)) / 2 + 20px))']);
  assert.match(source, /transform-origin: right center;/);
  for (const viewport of [320, 375, 600, 601, 700, 760, 761, 1024, 1440, 1920]) {
    const pageWidth = Math.min(viewport, 600);
    const pageRight = (viewport + pageWidth) / 2;
    const rightInset = Math.max(20, (viewport - 600) / 2 + 20);
    assert.equal(pageRight - (viewport - rightInset), 20, `viewport ${viewport}`);
  }
});

import test from 'node:test';
import assert from 'node:assert/strict';
import { applyTheme, getStoredTheme, normalizeTheme, THEME_OPTIONS } from '../src/utils/theme.js';

test('every offered theme is accepted and unknown values fall back to light', () => {
  for (const { value } of THEME_OPTIONS) assert.equal(normalizeTheme(value), value);
  for (const value of [null, undefined, '', 'neon', {}, '__proto__']) {
    assert.equal(normalizeTheme(value), 'light');
  }
});

test('theme applies to the document and round-trips through storage', () => {
  const values = new Map();
  globalThis.document = { documentElement: { dataset: {} } };
  globalThis.localStorage = {
    getItem: key => values.get(key), setItem: (key, value) => values.set(key, value)
  };
  try {
    assert.equal(applyTheme('mint'), 'mint');
    assert.equal(document.documentElement.dataset.theme, 'mint');
    assert.equal(getStoredTheme(), 'mint');
    assert.equal(applyTheme('invalid'), 'light');
    assert.equal(getStoredTheme(), 'light');
  } finally {
    delete globalThis.document;
    delete globalThis.localStorage;
  }
});

test('unavailable storage does not prevent visual theme changes', () => {
  globalThis.document = { documentElement: { dataset: {} } };
  globalThis.localStorage = {
    getItem() { throw new Error('storage disabled'); },
    setItem() { throw new Error('quota exceeded'); }
  };
  try {
    assert.equal(applyTheme('dark'), 'dark');
    assert.equal(document.documentElement.dataset.theme, 'dark');
    assert.equal(getStoredTheme(), 'light');
  } finally {
    delete globalThis.document;
    delete globalThis.localStorage;
  }
});

test('theme utilities also work without browser globals', () => {
  assert.equal(getStoredTheme(), 'light');
  assert.equal(applyTheme('warm'), 'warm');
});

import test from 'node:test';
import assert from 'node:assert/strict';
import { resolveApiBaseUrl } from '../src/utils/resolveApiBaseUrl.js';

for (const origin of ['http://localhost:38081', 'http://127.0.0.1:38081', 'https://demo.example.org']) {
  test(`production requests stay on their own deployment: ${origin}`, () => {
    assert.equal(resolveApiBaseUrl('', new URL(origin), 'production'), origin);
  });
}

test('local development retains its separate backend port', () => {
  assert.equal(resolveApiBaseUrl('', new URL('http://localhost:8080'), 'development'), 'http://localhost:18080');
});

test('an explicit backend configuration takes priority', () => {
  assert.equal(resolveApiBaseUrl('https://api.example.org/', new URL('http://localhost:38081'), 'production'), 'https://api.example.org');
});

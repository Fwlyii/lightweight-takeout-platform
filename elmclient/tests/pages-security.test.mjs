import test from 'node:test';
import assert from 'node:assert/strict';
import { readFile } from 'node:fs/promises';
const source = await readFile(new URL('../../deploy/cloudflare-pages/_worker.template.js', import.meta.url), 'utf8');
const { default: worker } = await import('data:text/javascript;base64,' + Buffer.from(source).toString('base64'));
const env = { ASSETS: { fetch: async () => new Response('<html>app</html>', { headers: { 'Content-Type': 'text/html' } }) } };

test('Pages allows only first-party voice while retaining camera and location restrictions', async () => {
  const response = await worker.fetch(new Request('https://example.test/', { headers: { Accept: 'text/html' } }), env);
  assert.equal(response.headers.get('Permissions-Policy'), 'camera=(), microphone=(self), geolocation=()');
  assert.equal(response.headers.get('X-Frame-Options'), 'SAMEORIGIN');
  assert.match(response.headers.get('Cache-Control'), /no-store/);
});

test('missing assets still return 404 instead of the SPA document', async () => {
  const response = await worker.fetch(new Request('https://example.test/missing.png'), env);
  assert.equal(response.status, 404);
});

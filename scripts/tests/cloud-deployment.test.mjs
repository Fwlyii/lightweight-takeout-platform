import test from 'node:test';
import assert from 'node:assert/strict';
import fs from 'node:fs';
import os from 'node:os';
import path from 'node:path';
import { spawnSync } from 'node:child_process';

const source = new URL('../deploy-cloudflare-pages.sh', import.meta.url);
function runDeployment(origin, curlStatus = 0) {
  const root = fs.mkdtempSync(path.join(os.tmpdir(), 'elm-cloud-deploy-test-'));
  try {
    for (const dir of ['scripts', 'scripts/cloudflare-pages', 'bin']) {
      fs.mkdirSync(path.join(root, dir), { recursive: true });
    }
    fs.copyFileSync(source, path.join(root, 'scripts/deploy-cloudflare-pages.sh'));
    fs.writeFileSync(path.join(root, 'scripts/cloudflare-pages/_worker.template.js'),
      'const origin = "__BACKEND_ORIGIN__";');
    const log = path.join(root, 'commands.log');
    fs.writeFileSync(log, '');
    const stubs = {
      docker: '#!/bin/sh\nprintf "docker\\n" >> "$DEPLOY_TEST_LOG"\nexit 99\n',
      curl: '#!/bin/sh\nprintf "curl %s\\n" "$*" >> "$DEPLOY_TEST_LOG"\nexit "$DEPLOY_TEST_CURL_STATUS"\n',
      npm: '#!/bin/sh\nprintf "npm %s\\n" "$*" >> "$DEPLOY_TEST_LOG"\nmkdir -p elmclient/dist\nprintf "test" > elmclient/dist/index.html\n',
    };
    for (const [name, body] of Object.entries(stubs)) {
      fs.writeFileSync(path.join(root, 'bin', name), body, { mode: 0o700 });
    }
    const result = spawnSync('zsh', [path.join(root, 'scripts/deploy-cloudflare-pages.sh'), 'qa'], {
      cwd: root, encoding: 'utf8',
      env: { ...process.env, PATH: path.join(root, 'bin') + path.delimiter + process.env.PATH,
        PAGES_PROJECT: 'test-project', PUBLIC_DEMO_URL: '', BACKEND_ORIGIN: origin,
        DEPLOY_TEST_LOG: log, DEPLOY_TEST_CURL_STATUS: String(curlStatus) },
    });
    return { ...result, commands: fs.readFileSync(log, 'utf8') };
  } finally { fs.rmSync(root, { recursive: true, force: true }); }
}

test('cloud deployment checks its backend and never starts local Docker', () => {
  const result = runDeployment('https://backend.example.test');
  assert.equal(result.status, 0, result.stderr);
  assert.match(result.commands, /backend\.example\.test\/api\/businesses\/search/);
  assert.match(result.commands, /npm --prefix elmclient run build/);
  assert.match(result.commands, /pages deploy/);
  assert.doesNotMatch(result.commands, /docker/);
});

test('unreachable cloud backend stops deployment without local fallback', () => {
  const result = runDeployment('https://backend.example.test', 22);
  assert.notEqual(result.status, 0);
  assert.doesNotMatch(result.commands, /npm|docker/);
});

test('cloud backend rejects non-HTTPS URLs and injection characters', () => {
  for (const origin of ['http://backend.example.test', 'https://backend.example.test/path',
    'https://backend.example.test/','https://backend.example.test|bad']) {
    const result = runDeployment(origin);
    assert.notEqual(result.status, 0);
    assert.equal(result.commands, '');
  }
});

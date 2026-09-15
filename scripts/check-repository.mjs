#!/usr/bin/env node
// Read-only checks: no network calls, deployment or database mutation.
import fs from 'node:fs';
import path from 'node:path';
import { fileURLToPath } from 'node:url';
import { execFileSync } from 'node:child_process';

const root = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const candidates = execFileSync('git', ['ls-files', '--cached', '--others', '--exclude-standard', '-z'],
  { cwd: root, encoding: 'utf8' }).split('\0').filter(Boolean);
const files = [...new Set(candidates)].filter(file => fs.existsSync(path.join(root, file)));
const problems = [];
const markdown = files.filter(file => file.endsWith('.md'));
if (markdown.some(file => file !== 'README.md' && !file.startsWith('docs/'))) problems.push('Keep project documentation in README.md and docs/');
for (const file of ['README.md', 'docs/SRS.pdf', 'docs/DEPLOYMENT.md']) {
  if (!fs.existsSync(path.join(root, file))) problems.push(`Missing submission document: ${file}`);
}
if (files.some(file => file.endsWith('.command'))) problems.push('Use scripts/ commands instead of platform-specific launchers');
for (const file of files.filter(file => file.endsWith('.md'))) {
  const text = fs.readFileSync(path.join(root, file), 'utf8');
  for (const match of text.matchAll(/\[[^\]]*\]\(([^)]+)\)/g)) {
    const href = match[1].replace(/^<|>$/g, '').split('#')[0];
    if (!href || /^[a-z]+:|^\/\//i.test(href)) continue;
    const target = path.resolve(root, path.dirname(file), decodeURIComponent(href));
    if (!fs.existsSync(target)) problems.push(`${file}: broken link ${href}`);
  }
}
for (const file of files.filter(file => /\.(sh|command)$/.test(file))) {
  const text = fs.readFileSync(path.join(root, file), 'utf8');
  if (/\/Users\/[^/$\s]+\/|\/home\/[^/$\s]+\//.test(text)) problems.push(`${file}: personal absolute path`);
}
const deployScript = fs.readFileSync(path.join(root, 'scripts/deploy-cloudflare-pages.sh'), 'utf8');
if (/PAGES_PROJECT=["'][a-z0-9-]+["']/.test(deployScript)) problems.push('Deployment project must be configured, not hardcoded');
const entrypoints = ['scripts/deploy-cloudflare-pages.sh'];
for (const file of entrypoints) {
  const text = fs.readFileSync(path.join(root, file), 'utf8');
  if (/https:\/\/[a-z0-9-]+(?:\.[a-z0-9-]+)+/i.test(text)) problems.push(`${file}: literal public deployment URL`);
}
const compose = fs.readFileSync(path.join(root, 'docker-compose.demo.yml'), 'utf8');
for (const match of compose.matchAll(/- \.\/([^:\n]+):/g)) {
  if (match[1] === 'runtime-uploads') continue; // Docker creates this runtime directory.
  if (!fs.existsSync(path.join(root, match[1]))) problems.push(`Compose mount missing: ${match[1]}`);
}
if (problems.length) {
  console.error(problems.join('\n'));
  process.exitCode = 1;
} else console.log('Repository checks passed: documentation links, deployment configuration, portable paths, Compose mounts.');

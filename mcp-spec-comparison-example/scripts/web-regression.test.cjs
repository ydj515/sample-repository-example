'use strict';
const { test, before, after } = require('node:test');
const assert = require('node:assert/strict');
const { readFileSync } = require('node:fs');
const { spawn } = require('node:child_process');
const { once } = require('node:events');
const vm = require('node:vm');
const path = require('node:path');

const root = path.resolve(__dirname, '..');
const source = readFileSync(path.join(root, 'src/main/resources/static/comparison.js'), 'utf8');
let server;
let base;

before(async () => {
  server = spawn('java', ['-jar', path.join(root, 'build/libs/mcp-spec-comparison-example-0.0.1-SNAPSHOT.jar')], {
    env: { ...process.env, SERVER_PORT: '0', SERVER_ADDRESS: '127.0.0.1' },
    stdio: ['ignore', 'pipe', 'pipe']
  });
  server.stderr.resume();
  await new Promise((resolve, reject) => {
    let output = '';
    const timer = setTimeout(() => reject(new Error('Server startup timed out')), 20000);
    server.once('error', error => { clearTimeout(timer); reject(error); });
    server.once('exit', code => { clearTimeout(timer); reject(new Error(`Server exited: ${code}`)); });
    server.stdout.on('data', chunk => {
      output = (output + chunk.toString()).slice(-16000);
      const port = output.match(/Tomcat started on port (\d+)/);
      if (port) { clearTimeout(timer); base = `http://127.0.0.1:${port[1]}`; resolve(); }
    });
  });
});

after(async () => {
  if (!server || server.exitCode !== null) return;
  const exited = once(server, 'exit');
  server.kill('SIGTERM');
  const timer = setTimeout(() => server.kill('SIGKILL'), 5000);
  try { await exited; } finally { clearTimeout(timer); }
});

// Minimal DOM adapter for script behavior. This does not emulate browser rendering.
class Element {
  constructor(value = '') {
    this.value = value;
    this.textContent = '';
    this.children = [];
    this.disabled = false;
    this.listeners = {};
    const classes = new Set();
    this.classList = {
      add: value => classes.add(value),
      toggle: (value, enabled) => enabled ? classes.add(value) : classes.delete(value)
    };
  }
  append(...children) { this.children.push(...children); }
  replaceChildren(...children) { this.children = children; }
  addEventListener(event, callback) { this.listeners[event] = callback; }
  querySelector() { return this.decline || (this.decline = new Element()); }
  focus() { this.focused = true; }
}

function page(overrides = {}, failure = false) {
  const elements = new Map();
  const get = id => {
    if (!elements.has(id)) elements.set(id, new Element());
    return elements.get(id);
  };
  const values = { scenario: 'search', region: 'kr', query: 'deployment', scope: 'public', ...overrides };
  for (const [id, value] of Object.entries(values)) get(id).value = value;
  get('controls').elements = [...Object.keys(values).map(get), get('run')];
  const requests = [];
  const context = vm.createContext({
    document: { getElementById: get, createElement: () => new Element() },
    AbortController, TextDecoder, structuredClone, performance, setTimeout, clearTimeout,
    fetch: async (url, options) => {
      requests.push({ url, body: JSON.parse(options.body), headers: options.headers });
      if (failure) throw new Error('Network unavailable');
      return fetch(base + url, { ...options, headers: { ...options.headers, Origin: base } });
    }
  });
  vm.runInContext(source, context, { filename: 'comparison.js' });
  return { get, requests, submit: () => get('controls').listeners.submit({ preventDefault() {} }) };
}

for (const region of ['kr', 'us']) {
  test(`search routes both versions to ${region}`, async () => {
    const ui = page({ region });
    await ui.submit();
    for (const era of ['legacy', 'modern']) {
      assert.equal(ui.get(era + '-state').textContent, '완료');
      assert.match(ui.get(era + '-result').textContent, region === 'kr' ? /Korea/ : /US/);
    }
    assert.equal(ui.requests.filter(r => r.body.method === 'initialize').length, 1);
    assert.equal(ui.requests.find(r => r.url === '/modern/mcp').headers['Mcp-Param-Region'], region);
    assert.ok(ui.get('controls').elements.every(element => !element.disabled));
  });
}

test('SSE and MRTR accept scope and use the correct response IDs', async () => {
  const ui = page({ scenario: 'input', scope: 'team' });
  await ui.submit();
  for (const era of ['legacy', 'modern']) {
    assert.equal(ui.get(era + '-state').textContent, '완료');
    assert.match(ui.get(era + '-result').textContent, /team/);
  }
  const modern = ui.requests.filter(r => r.url === '/modern/mcp');
  assert.equal(modern.length, 2);
  assert.notEqual(modern[0].body.id, modern[1].body.id);
  assert.equal(modern[1].body.params.inputResponses.scope.content.scope, 'team');
  const reply = ui.requests.find(r => !r.body.method && r.body.result);
  assert.match(reply.body.id, /^scope-/);
  assert.equal(reply.body.result.action, 'accept');
});

test('both versions surface declined input', async () => {
  const ui = page({ scenario: 'input', scope: 'decline' });
  await ui.submit();
  for (const era of ['legacy', 'modern']) {
    assert.equal(ui.get(era + '-state').textContent, '입력 거절 확인');
    assert.match(ui.get(era + '-result').textContent, /declined/);
  }
});

test('node change shows legacy session loss and modern success', async () => {
  const ui = page({ scenario: 'node' });
  await ui.submit();
  assert.equal(ui.get('legacy-state').textContent, '예상 오류 확인');
  assert.match(ui.get('legacy-result').textContent, /Session not found/);
  assert.equal(ui.get('modern-state').textContent, '완료');
  assert.equal(ui.get('query').disabled, true);
});

test('header mismatch is rejected only for modern', async () => {
  const ui = page({ scenario: 'mismatch' });
  await ui.submit();
  assert.equal(ui.get('legacy-state').textContent, '완료');
  assert.equal(ui.get('modern-state').textContent, '예상 오류 확인');
  assert.match(ui.get('modern-result').textContent, /-32020/);
});

test('network failures are visible and controls are restored', async () => {
  const ui = page({}, true);
  await ui.submit();
  for (const era of ['legacy', 'modern']) {
    assert.equal(ui.get(era + '-state').textContent, '실행 실패');
    assert.match(ui.get(era + '-result').textContent, /Network unavailable/);
  }
  assert.ok(ui.get('controls').elements.every(element => !element.disabled));
});

test('blank query sends no HTTP requests', async () => {
  const ui = page({ query: '   ' });
  await ui.submit();
  assert.equal(ui.requests.length, 0);
  assert.equal(ui.get('query').focused, true);
});

'use strict';
const PREFIX = 'io.modelcontextprotocol/';
const versions = { legacy: '2025-11-25', modern: '2026-07-28' };
const descriptions = {
  search: '동일한 검색을 실행합니다. 이전 버전은 초기화 후 본문으로 리전을 선택하고, 최신 버전은 초기화 없이 헤더로 선택합니다.',
  input: '검색 범위를 빼고 호출합니다. 서버가 입력을 요청하면 위에서 선택한 범위로 자동 응답합니다. 이전 버전은 SSE의 서버 요청에 답하고, 최신 버전은 새 ID로 도구를 재호출합니다.',
  node: '선택한 리전에서 초기화한 뒤 다른 노드에 tools/list를 보냅니다. 이전 버전의 404와 최신 버전의 정상 목록 응답을 비교합니다. 검색어·범위는 사용하지 않습니다.',
  mismatch: '본문의 리전과 반대 값을 Mcp-Param-Region 헤더에 보냅니다. 이전 버전은 이 헤더 없이 본문을 사용합니다. 최신 버전의 HeaderMismatch 오류를 확인합니다.'
};
const $ = id => document.getElementById(id);
let nextId = 0;
function message(method, era, params = {}) {
  if (era === 'modern') params._meta = {
    [PREFIX + 'protocolVersion']: versions.modern,
    [PREFIX + 'clientCapabilities']: { elicitation: { form: {} } },
    [PREFIX + 'clientInfo']: { name: 'comparison-web', version: '1.0.0' }
  };
  return { jsonrpc: '2.0', id: ++nextId, method, params };
}
function detail(parent, title, value) {
  const details = document.createElement('details');
  const summary = document.createElement('summary');
  summary.textContent = title;
  const pre = document.createElement('pre');
  pre.textContent = typeof value === 'string' ? value : JSON.stringify(value, null, 2);
  details.append(summary, pre); parent.append(details);
}
function entry(era, title) {
  const li = document.createElement('li');
  const strong = document.createElement('strong'); strong.textContent = title;
  li.append(strong); $(era + '-events').append(li);
  return li;
}
function outcome(era, response) {
  const result = $(era + '-result');
  result.classList.toggle('error', Boolean(response.error || response.result?.isError));
  if (response.error) {
    result.textContent = `프로토콜 오류 ${response.error.code}\n${response.error.message}`;
  } else if (response.result?.structuredContent) {
    const docs = response.result.structuredContent.documents;
    result.textContent = docs.length ? docs.join('\n') : '검색 결과가 없습니다.';
  } else if (response.result?.isError) {
    result.textContent = response.result.content.map(c => c.text).join('\n');
  } else result.textContent = '도구 목록 조회 성공 · ' + response.result.tools.length + '개';
}
async function exchange(era, path, body, extra, signal, onServerRequest) {
  const headers = { 'Content-Type': 'application/json', Accept: 'application/json, text/event-stream', ...extra };
  const li = entry(era, body.method || 'elicitation 응답');
  detail(li, '요청 헤더·본문', { method: 'POST', path, headers, body });
  const start = performance.now();
  const response = await fetch(path, { method: 'POST', headers, body: JSON.stringify(body), signal });
  const meta = document.createElement('p'); meta.className = 'meta';
  meta.textContent = `HTTP ${response.status} · 노드 ${response.headers.get('X-Demo-Node') || '-'} · 라우팅 ${response.headers.get('X-Demo-Routing') || '직접 호출'} · 응답 헤더 ${(performance.now() - start).toFixed(0)} ms`;
  li.append(meta);
  detail(li, '응답 헤더', Object.fromEntries(response.headers));
  let data = null;
  if (response.headers.get('Content-Type')?.includes('text/event-stream')) {
    if (!onServerRequest) throw new Error('예상하지 않은 SSE 응답입니다.');
    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let pending = '';
    try {
      while (true) {
        const chunk = await reader.read();
        pending += decoder.decode(chunk.value, { stream: !chunk.done }).replace(/\r\n/g, '\n');
        let boundary;
        while ((boundary = pending.indexOf('\n\n')) >= 0) {
          const block = pending.slice(0, boundary); pending = pending.slice(boundary + 2);
          const raw = block.split('\n').filter(line => line.startsWith('data:')).map(line => line.slice(5).trimStart()).join('\n');
          if (!raw) continue;
          const event = JSON.parse(raw);
          const row = entry(era, event.method ? '서버 → 클라이언트: ' + event.method : 'SSE: 원래 호출 결과');
          detail(row, 'SSE 메시지', event);
          if (event.method) await onServerRequest(event);
          else { data = event; outcome(era, event); }
        }
        if (chunk.done) break;
      }
    } finally { await reader.cancel().catch(() => {}); reader.releaseLock(); }
    if (!data) throw new Error('최종 결과 전에 SSE 연결이 종료됐습니다.');
  } else {
    const raw = await response.text();
    data = raw ? JSON.parse(raw) : null;
    detail(li, '응답 본문', data ?? '(본문 없음)');
  }
  return { status: response.status, headers: response.headers, data };
}
async function runLane(era, options) {
  const { scenario, region, query, scope } = options;
  const other = region === 'kr' ? 'us' : 'kr';
  const controller = new AbortController();
  const timer = setTimeout(() => controller.abort(), 12000);
  const signal = controller.signal;
  const send = (path, body, headers, callback) => exchange(era, path, body, headers, signal, callback);
  $(era + '-state').textContent = '실행 중';
  try {
    let headers = {};
    if (era === 'legacy') {
      const init = message('initialize', era, { protocolVersion: versions.legacy, capabilities: { elicitation: { form: {} } }, clientInfo: { name: 'comparison-web', version: '1.0.0' } });
      const initialized = await send(`/legacy/${region}/mcp`, init, {});
      if (initialized.status !== 200 || !initialized.headers.get('MCP-Session-Id')) throw new Error('초기화에 실패했습니다.');
      headers = { 'MCP-Session-Id': initialized.headers.get('MCP-Session-Id'), 'MCP-Protocol-Version': versions.legacy };
      const ready = await send(`/legacy/${region}/mcp`, { jsonrpc: '2.0', method: 'notifications/initialized' }, headers);
      if (ready.status !== 202) throw new Error('초기화 완료 통지에 실패했습니다.');
    }
    const method = scenario === 'node' ? 'tools/list' : 'tools/call';
    let params = method === 'tools/list' ? {} : { name: 'search_documents', arguments: { region, query } };
    if (scenario !== 'input' && method === 'tools/call') params.arguments.scope = scope;
    let body = message(method, era, params);
    let path = scenario === 'node' ? `/${era}/${other}/mcp` : `/${era}/mcp`;
    if (era === 'modern') {
      headers = { 'MCP-Protocol-Version': versions.modern, 'Mcp-Method': method };
      if (method === 'tools/call') Object.assign(headers, { 'Mcp-Name': 'search_documents', 'Mcp-Param-Region': scenario === 'mismatch' ? other : region });
    }
    const answer = scope === 'decline' ? { action: 'decline' } : { action: 'accept', content: { scope } };
    let result = await send(path, body, headers, async event => {
      if (event.method !== 'elicitation/create') throw new Error('지원하지 않는 서버 요청입니다.');
      const replied = await send(`/legacy/${region}/mcp`, { jsonrpc: '2.0', id: event.id, result: answer }, headers);
      if (replied.status !== 202) throw new Error('추가 입력 응답에 실패했습니다.');
    });
    if (result.data?.result?.resultType === 'input_required') {
      const params = structuredClone(body.params);
      params.inputResponses = { scope: answer };
      if (result.data.result.requestState !== undefined) params.requestState = result.data.result.requestState;
      body = message(method, era, params);
      result = await send(path, body, headers);
    }
    if (!result.data) throw new Error('결과 본문이 없습니다.');
    outcome(era, result.data);
    const expectedError = (scenario === 'node' && era === 'legacy' && result.status === 404)
      || (scenario === 'mismatch' && era === 'modern' && result.data.error?.code === -32020);
    if (result.data.error && !expectedError) throw new Error(result.data.error.message);
    $(era + '-state').textContent = expectedError ? '예상 오류 확인' : result.data.result?.isError ? '입력 거절 확인' : '완료';
  } catch (error) {
    $(era + '-state').textContent = '실행 실패';
    $(era + '-result').classList.add('error');
    $(era + '-result').textContent = error.name === 'AbortError' ? '12초 대기 시간을 초과했습니다.' : error.message;
  } finally { clearTimeout(timer); }
}
function explain() {
  const scenario = $('scenario').value;
  $('explanation').textContent = descriptions[scenario];
  $('scope').querySelector('[value="decline"]').disabled = scenario !== 'input';
  if (scenario !== 'input' && $('scope').value === 'decline') $('scope').value = 'public';
  $('query').disabled = scenario === 'node';
  $('scope').disabled = scenario === 'node';
}
$('scenario').addEventListener('change', explain);
$('controls').addEventListener('submit', async event => {
  event.preventDefault();
  const options = Object.fromEntries(['scenario', 'query', 'region', 'scope'].map(id => [id, $(id).value.trim()]));
  if (options.scenario !== 'node' && !options.query) { $('query').focus(); return; }
  for (const era of ['legacy', 'modern']) { $(era + '-events').replaceChildren(); $(era + '-result').textContent = ''; }
  for (const control of $('controls').elements) control.disabled = true;
  $('status').textContent = '두 버전을 실제 호출하고 있습니다. 추가 입력은 선택한 값으로 자동 응답합니다.';
  try { await Promise.all(['legacy', 'modern'].map(era => runLane(era, options))); }
  finally {
    for (const control of $('controls').elements) control.disabled = false;
    explain();
    $('status').textContent = '실행을 마쳤습니다. 각 단계에서 요청·응답을 펼쳐볼 수 있습니다.';
  }
});
explain();

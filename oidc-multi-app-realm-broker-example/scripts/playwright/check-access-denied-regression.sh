#!/usr/bin/env bash
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/../.." && pwd)"

OUTPUT_ROOT="${OUTPUT_ROOT:-${PROJECT_ROOT}/output/playwright/access-denied}"
RUN_ID="${RUN_ID:-$(date +%Y%m%d-%H%M%S)}"
RUN_DIR="${OUTPUT_ROOT}/${RUN_ID}"

APP1_BASE_URL="${APP1_BASE_URL:-http://localhost:8081}"
APP2_BASE_URL="${APP2_BASE_URL:-http://localhost:8082}"
KEYCLOAK_BASE_URL="${KEYCLOAK_BASE_URL:-http://localhost:9000}"
HEADED="${HEADED:-false}"

export CODEX_HOME="${CODEX_HOME:-$HOME/.codex}"
PWCLI="${PWCLI:-${CODEX_HOME}/skills/playwright/scripts/playwright_cli.sh}"

mkdir -p "${RUN_DIR}"

require_command() {
    local command_name="$1"
    if ! command -v "${command_name}" >/dev/null 2>&1; then
        echo "필수 명령을 찾을 수 없습니다: ${command_name}" >&2
        exit 1
    fi
}

check_http_ready() {
    local url="$1"
    local label="$2"
    if ! curl -fsS "${url}" >/dev/null; then
        echo "${label} 준비 상태를 확인할 수 없습니다: ${url}" >&2
        exit 1
    fi
}

js_escape() {
    local value="$1"
    value="${value//\\/\\\\}"
    value="${value//\"/\\\"}"
    value="${value//$'\n'/\\n}"
    printf '%s' "${value}"
}

open_browser() {
    local session_name="$1"
    if [[ "${HEADED}" == "true" ]]; then
        "${PWCLI}" --session "${session_name}" open about:blank --headed >/dev/null
    else
        "${PWCLI}" --session "${session_name}" open about:blank >/dev/null
    fi
}

run_scenario() {
    local scenario_name="$1"
    local start_url="$2"
    local username="$3"
    local password="$4"
    local expected_title="$5"
    local expected_css_path="$6"

    local session_name="d${RANDOM}"
    local screenshot_path="${RUN_DIR}/${scenario_name}.png"
    local result_log_path="${RUN_DIR}/${scenario_name}.log"
    local js_start_url
    local js_username
    local js_password
    local js_expected_title
    local js_expected_css_url
    local js_screenshot_path
    local run_output

    export PLAYWRIGHT_CLI_SESSION="${session_name}"

    open_browser "${session_name}"

    js_start_url="$(js_escape "${start_url}")"
    js_username="$(js_escape "${username}")"
    js_password="$(js_escape "${password}")"
    js_expected_title="$(js_escape "${expected_title}")"
    js_expected_css_url="$(js_escape "${start_url}${expected_css_path}")"
    js_screenshot_path="$(js_escape "${screenshot_path}")"

    run_output="$("${PWCLI}" run-code "$(cat <<EOF
async (page) => {
  const startUrl = "${js_start_url}";
  const username = "${js_username}";
  const password = "${js_password}";
  const expectedTitle = "${js_expected_title}";
  const expectedCssUrl = "${js_expected_css_url}";
  const screenshotPath = "${js_screenshot_path}";

  const consoleEntries = [];
  const failedRequests = [];
  const errorResponses = [];

  page.on('console', (message) => {
    if (message.type() === 'error' || message.type() === 'warning') {
      consoleEntries.push({ type: message.type(), text: message.text() });
    }
  });

  page.on('requestfailed', (request) => {
    failedRequests.push({
      url: request.url(),
      errorText: request.failure()?.errorText ?? 'unknown',
    });
  });

  page.on('response', (response) => {
    if (response.status() >= 400 && !response.url().endsWith('/favicon.ico')) {
      errorResponses.push({
        url: response.url(),
        status: response.status(),
      });
    }
  });

  await page.goto(startUrl + '/oauth2/authorization/keycloak', { waitUntil: 'domcontentloaded' });
  await page.getByRole('textbox', { name: /Username or email/i }).fill(username);
  await page.getByRole('textbox', { name: /Password/i }).fill(password);
  await page.getByRole('button', { name: /Sign In/i }).click();

  await page.waitForLoadState('domcontentloaded');
  await page.getByRole('heading', { level: 1 }).waitFor({ timeout: 15000 });

  const heading = await page.getByRole('heading', { level: 1 }).innerText();
  const title = await page.title();
  const pageUrl = page.url();
  const stylesheetUrls = await page.evaluate(() =>
    Array.from(document.styleSheets)
      .map((sheet) => sheet.href || 'inline')
  );
  const computedStyleSummary = await page.evaluate(() => {
    const bodyStyle = getComputedStyle(document.body);
    const panelStyle = getComputedStyle(document.querySelector('.panel'));
    const titleStyle = getComputedStyle(document.querySelector('h1'));

    return {
      bodyBackground: bodyStyle.backgroundImage || bodyStyle.backgroundColor,
      bodyFont: bodyStyle.fontFamily,
      panelRadius: panelStyle.borderRadius,
      panelShadow: panelStyle.boxShadow,
      titleColor: titleStyle.color,
      titleFontSize: titleStyle.fontSize,
    };
  });

  const expectedDeniedResponse = {
    url: startUrl + '/',
    status: 403,
  };

  const unexpectedResponses = errorResponses.filter((entry) =>
    !(entry.url === expectedDeniedResponse.url && entry.status === expectedDeniedResponse.status)
  );

  const unexpectedFailedRequests = failedRequests.filter((entry) => !entry.url.endsWith('/favicon.ico'));
  const unexpectedConsoleEntries = consoleEntries.filter((entry) => {
    const text = entry.text || '';
    return !text.includes('favicon.ico') && !text.includes('status of 403');
  });

  if (title !== expectedTitle) {
    throw new Error('예상 title(' + expectedTitle + ')과 실제 title(' + title + ')이 다릅니다.');
  }

  if (!heading.includes('접근 권한이 없습니다')) {
    throw new Error('접근 거부 헤딩을 찾지 못했습니다: ' + heading);
  }

  if (!pageUrl.startsWith(startUrl)) {
    throw new Error('접근 거부 페이지가 예상 앱으로 돌아오지 않았습니다: ' + pageUrl);
  }

  if (!stylesheetUrls.includes(expectedCssUrl)) {
    throw new Error('예상 CSS가 로드되지 않았습니다: ' + expectedCssUrl);
  }

  if (!computedStyleSummary.bodyBackground.includes('gradient')) {
    throw new Error('body 배경에 gradient가 적용되지 않았습니다: ' + computedStyleSummary.bodyBackground);
  }

  if (computedStyleSummary.panelRadius === '0px') {
    throw new Error('패널 radius가 적용되지 않았습니다: ' + computedStyleSummary.panelRadius);
  }

  if (unexpectedResponses.length > 0) {
    throw new Error('예상하지 않은 오류 응답이 있습니다: ' + JSON.stringify(unexpectedResponses));
  }

  if (unexpectedFailedRequests.length > 0) {
    throw new Error('예상하지 않은 requestfailed가 있습니다: ' + JSON.stringify(unexpectedFailedRequests));
  }

  if (unexpectedConsoleEntries.length > 0) {
    throw new Error('예상하지 않은 콘솔 오류가 있습니다: ' + JSON.stringify(unexpectedConsoleEntries));
  }

  await page.screenshot({ path: screenshotPath, fullPage: true });

  return {
    pageUrl,
    title,
    heading,
    stylesheetUrls,
    computedStyleSummary,
    deniedResponse: expectedDeniedResponse,
    screenshotPath,
  };
}
EOF
    )")"

    printf '%s\n' "${run_output}" | tee "${result_log_path}"

    if grep -q '^### Error' "${result_log_path}"; then
        echo "Playwright 검증이 실패했습니다: ${result_log_path}" >&2
        exit 1
    fi

    "${PWCLI}" --session "${session_name}" close >/dev/null 2>&1 || true
}

require_command "curl"
require_command "npx"

if [[ ! -x "${PWCLI}" ]]; then
    echo "Playwright CLI wrapper를 찾을 수 없습니다: ${PWCLI}" >&2
    exit 1
fi

check_http_ready "${APP1_BASE_URL}/public" "app1"
check_http_ready "${APP2_BASE_URL}/public" "app2"
check_http_ready "${KEYCLOAK_BASE_URL}/realms/agency-a/.well-known/openid-configuration" "Keycloak agency-a realm"
check_http_ready "${KEYCLOAK_BASE_URL}/realms/agency-b/.well-known/openid-configuration" "Keycloak agency-b realm"
check_http_ready "${KEYCLOAK_BASE_URL}/realms/platform-broker/.well-known/openid-configuration" "Keycloak platform-broker realm"

echo "접근 거부 회귀 점검을 시작합니다."
echo "결과 폴더: ${RUN_DIR}"

run_scenario \
    "app1-denied-by-agency-b-user" \
    "${APP1_BASE_URL}" \
    "agency-b-user" \
    "agencybuser1234" \
    "App 1 Access Denied" \
    "/css/app1-access-denied.css"

run_scenario \
    "app2-denied-by-agency-a-user" \
    "${APP2_BASE_URL}" \
    "agency-a-user" \
    "agencyauser1234" \
    "App 2 Access Denied" \
    "/css/app2-access-denied.css"

echo "접근 거부 회귀 점검이 완료되었습니다."
echo "스크린샷과 로그는 ${RUN_DIR} 에 저장되었습니다."

#!/usr/bin/env bash
set -euo pipefail

########################################
# Config
########################################
GITLAB_URL="https://gitlab.com"   # GitLab 인스턴스 주소 (self-hosted면 변경)
PRIVATE_TOKEN="xxxxxx"           # 개인용 액세스 토큰
GROUP_ID="xx"                    # 대상 프로젝트들이 속한 그룹 ID

# 선택: 변경 결과를 팀에 알릴 Slack Webhook (없으면 빈 문자열로)
SLACK_WEBHOOK_URL=""

# .gitlab-ci.yml 안에 'master' 문자열이 남아 있는 프로젝트 목록 기록 파일
TMP_CI_LIST="projects_needing_ci_update.csv"

########################################
# 초기화
########################################

# CI 점검 파일 초기화
echo "project_id,project_name" > "$TMP_CI_LIST"

########################################
# 그룹 내 프로젝트 목록 조회
########################################
echo "Fetching projects from group ID: $GROUP_ID..."

API_RESPONSE=$(
  curl --silent \
    --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
    "$GITLAB_URL/api/v4/groups/$GROUP_ID/projects?per_page=100"
)

# 응답이 유효한 JSON 배열( '[' 로 시작)인지 확인
if [[ ! "$API_RESPONSE" == \[* ]]; then
  echo "Error: Failed to fetch projects. API returned an error:"
  echo "$API_RESPONSE"
  echo "Please check your PRIVATE_TOKEN, GITLAB_URL, and GROUP_ID permissions."
  exit 1
fi

PROJECT_IDS=$(echo "$API_RESPONSE" | jq '.[].id')

if [ -z "$PROJECT_IDS" ]; then
  echo "No projects found in group $GROUP_ID. (This is not an error)"
  exit 0
fi

echo "Target project IDs:"
echo "$PROJECT_IDS"
echo ""

########################################
# 각 프로젝트 처리
########################################

for PROJECT_ID in $PROJECT_IDS; do
  # 프로젝트 기본 정보 조회 (한 번만 호출해서 재사용)
  PROJECT_JSON=$(
    curl --silent \
      --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
      "$GITLAB_URL/api/v4/projects/$PROJECT_ID"
  )

  PROJECT_NAME=$(echo "$PROJECT_JSON" | jq -r '.name')
  echo "==============================================="
  echo "--- Processing project: $PROJECT_NAME (ID: $PROJECT_ID) ---"

  ########################################
  # 0. master 보호 브랜치 설정 조회
  ########################################
  echo "0. Checking if 'master' is a protected branch..."

  MASTER_PROTECTED_JSON=$(
    curl --silent \
      --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
      "$GITLAB_URL/api/v4/projects/$PROJECT_ID/protected_branches?search=master"
  )

  MASTER_PROTECTED_EXISTS=$(echo "$MASTER_PROTECTED_JSON" | jq 'length')

  if [ "$MASTER_PROTECTED_EXISTS" -gt 0 ]; then
    echo "   'master' is protected. Will copy settings to 'main' and then unprotect 'master'."
  else
    echo "   'master' is not protected or no specific rule found."
  fi

  ########################################
  # 1. master 브랜치를 기반으로 main 브랜치 생성
  ########################################
  echo "1. Creating 'main' branch from 'master' (if not exists)..."

  CREATE_MAIN_STATUS=$(
    curl --silent --show-error \
      --write-out "%{http_code}" \
      --output /dev/null \
      --request POST \
      --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
      "$GITLAB_URL/api/v4/projects/$PROJECT_ID/repository/branches?branch=main&ref=master"
  )

  case "$CREATE_MAIN_STATUS" in
    201)
      echo "   'main' branch created from 'master'."
      ;;
    400)
      echo "   Failed to create 'main'. Invalid request (400). Please check if 'master' exists."
      ;;
    409)
      echo "   'main' already exists (409). Skipping creation."
      ;;
    404)
      echo "   Project or ref 'master' not found (404). Skipping this project."
      continue
      ;;
    *)
      echo "   Unexpected HTTP status while creating 'main': $CREATE_MAIN_STATUS"
      ;;
  esac

  ########################################
  # 1-1. master 보호 설정을 main 으로 복사
  ########################################
  if [ "$MASTER_PROTECTED_EXISTS" -gt 0 ]; then
    echo "1-1. Copying protected branch settings from 'master' to 'main'..."

    # 단순화: 첫 번째 보호 설정만 복사
    MASTER_RULE=$(echo "$MASTER_PROTECTED_JSON" | jq '.[0]')

    PUSH_LEVEL=$(echo "$MASTER_RULE"  | jq '.push_access_levels[0].access_level // 40')
    MERGE_LEVEL=$(echo "$MASTER_RULE" | jq '.merge_access_levels[0].access_level // 40')
    ALLOW_FORCE_PUSH=$(echo "$MASTER_RULE" | jq '.allow_force_push')
    CODE_OWNER_REQUIRED=$(echo "$MASTER_RULE" | jq '.code_owner_approval_required')

    # 기존에 main 보호 규칙이 있다면 API가 409를 반환할 수 있음 → 그냥 시도만 하고 로그만 남김
    PROTECT_MAIN_STATUS=$(
      curl --silent --show-error \
        --write-out "%{http_code}" \
        --output /dev/null \
        --request POST \
        --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
        --data-urlencode "name=main" \
        --data "push_access_level=$PUSH_LEVEL" \
        --data "merge_access_level=$MERGE_LEVEL" \
        --data "allow_force_push=$ALLOW_FORCE_PUSH" \
        --data "code_owner_approval_required=$CODE_OWNER_REQUIRED" \
        "$GITLAB_URL/api/v4/projects/$PROJECT_ID/protected_branches"
    )

    case "$PROTECT_MAIN_STATUS" in
      201)
        echo "   'main' protected branch created with same rules as 'master'."
        ;;
      409)
        echo "   'main' is already protected. Skipping protection creation."
        ;;
      *)
        echo "   Unexpected HTTP status while protecting 'main': $PROTECT_MAIN_STATUS"
        ;;
    esac
  else
    echo "   No 'master' protected rule to copy. Skipping."
  fi

  ########################################
  # 2. 기본 브랜치를 main 으로 변경
  ########################################
  echo "2. Setting 'main' as the default branch..."

  SET_DEFAULT_STATUS=$(
    curl --silent --show-error \
      --write-out "%{http_code}" \
      --output /dev/null \
      --request PUT \
      --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
      --data "default_branch=main" \
      "$GITLAB_URL/api/v4/projects/$PROJECT_ID"
  )

  if [ "$SET_DEFAULT_STATUS" -eq 200 ]; then
    echo "   Default branch changed to 'main'."
  else
    echo "   Failed to set default branch to 'main'. HTTP status: $SET_DEFAULT_STATUS"
  fi

  ########################################
  # 2-1. master 보호 해제 (삭제를 위해)
  ########################################
  if [ "$MASTER_PROTECTED_EXISTS" -gt 0 ]; then
    echo "2-1. Unprotecting 'master' branch to allow deletion..."

    UNPROTECT_MASTER_STATUS=$(
      curl --silent --show-error \
        --write-out "%{http_code}" \
        --output /dev/null \
        --request DELETE \
        --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
        "$GITLAB_URL/api/v4/projects/$PROJECT_ID/protected_branches/master"
    )

    case "$UNPROTECT_MASTER_STATUS" in
      204)
        echo "   'master' is now unprotected."
        ;;
      404)
        echo "   'master' protected rule not found (already unprotected?)."
        ;;
      *)
        echo "   Unexpected HTTP status while unprotecting 'master': $UNPROTECT_MASTER_STATUS"
        ;;
    esac
  fi

  ########################################
  # 3. master 브랜치 삭제
  ########################################
  echo "3. Deleting 'master' branch..."

  DELETE_MASTER_STATUS=$(
    curl --silent --show-error \
      --write-out "%{http_code}" \
      --output /dev/null \
      --request DELETE \
      --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
      "$GITLAB_URL/api/v4/projects/$PROJECT_ID/repository/branches/master"
  )

  case "$DELETE_MASTER_STATUS" in
    204)
      echo "   'master' branch deleted successfully."
      ;;
    400)
      echo "   Failed to delete 'master'. It might still be protected or the default branch."
      ;;
    404)
      echo "   'master' branch not found (already deleted?)."
      ;;
    *)
      echo "   An error occurred while deleting 'master'. HTTP status: $DELETE_MASTER_STATUS"
      ;;
  esac

  ########################################
  # 4. .gitlab-ci.yml 에 'master' 사용 여부 확인
  ########################################
  echo "4. Checking .gitlab-ci.yml for 'master'..."

  CI_TMP_FILE=$(mktemp)
  CI_API_URL="$GITLAB_URL/api/v4/projects/$PROJECT_ID/repository/files/.gitlab-ci.yml/raw?ref=main"

  CI_HTTP_STATUS=$(
    curl --silent --show-error \
      --write-out "%{http_code}" \
      --output "$CI_TMP_FILE" \
      --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
      "$CI_API_URL"
  )

  if [ "$CI_HTTP_STATUS" -eq 200 ]; then
    if grep -q "master" "$CI_TMP_FILE"; then
      echo "   WARNING: .gitlab-ci.yml contains 'master'. Please update branch rules (only/rules) to 'main'."
      echo "$PROJECT_ID,$PROJECT_NAME" >> "$TMP_CI_LIST"
    else
      echo "   .gitlab-ci.yml has no 'master' mentions."
    fi
  elif [ "$CI_HTTP_STATUS" -eq 404 ]; then
    echo "   .gitlab-ci.yml not found on 'main'. Skipping."
  else
    echo "   Failed to fetch .gitlab-ci.yml. HTTP status: $CI_HTTP_STATUS"
  fi

  rm -f "$CI_TMP_FILE"

  echo "--- Finished processing $PROJECT_NAME ---"
  echo ""
done

########################################
# 결과 요약 및 팀 알림
########################################

TOTAL_LINES=$(wc -l < "$TMP_CI_LIST")

if [ "$TOTAL_LINES" -gt 1 ]; then
  echo ""
  echo "=================================================="
  echo "main 브랜치 전환 완료. 다만 아래 프로젝트의 .gitlab-ci.yml 에는"
  echo "'master' 문자열이 남아 있습니다. 수동으로 CI 규칙을 확인/수정해 주세요."
  echo "List: $TMP_CI_LIST"
  cat "$TMP_CI_LIST"
  echo "=================================================="

  # Slack 알림 (Webhook 이 설정된 경우)
  if [ -n "$SLACK_WEBHOOK_URL" ]; then
    echo "Sending Slack notification..."

    MESSAGE=$(cat <<EOF
[GitLab 브랜치 전환 보고]

그룹 ID: $GROUP_ID 의 프로젝트들에 대해
- 기본 브랜치를 master → main 으로 변경
- master 브랜치를 삭제 시도
- master 보호 브랜치 설정을 main 으로 복사

다음 프로젝트들의 .gitlab-ci.yml 에는 아직 'master' 문자열이 남아 있습니다.
CI only/rules 조건을 검토해 주세요.

$(cat "$TMP_CI_LIST")
EOF
)

    # jq 필요
    PAYLOAD=$(jq -n --arg text "$MESSAGE" '{text: $text}')

    curl --silent --show-error \
      -X POST \
      -H 'Content-type: application/json' \
      --data "$PAYLOAD" \
      "$SLACK_WEBHOOK_URL" || echo "Failed to send Slack notification."
  fi
else
  echo "All projects processed. No .gitlab-ci.yml contains 'master'."
fi

echo "All projects have been processed."
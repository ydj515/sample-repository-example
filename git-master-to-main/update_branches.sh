GITLAB_URL="https://gitlab.com"  # GitLab 인스턴스 주소 (self-hosted인 경우 변경)
PRIVATE_TOKEN="xxxxxx" # 발급받은 개인용 액세스 토큰
GROUP_ID="xx" # 변경을 원하는 프로젝트들이 속한 그룹 ID (특정 그룹 내 프로젝트만 대상일 경우)

# 그룹 내 모든 프로젝트 ID 가져오기
echo "Fetching projects from group ID: $GROUP_ID..."
API_RESPONSE=$(curl --silent --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" "$GITLAB_URL/api/v4/groups/$GROUP_ID/projects?per_page=100")

# 응답이 유효한 JSON 배열( '[' 로 시작)인지 확인
if [[ ! "$API_RESPONSE" == \[* ]]; then
    echo "Error: Failed to fetch projects. API returned an error:"
    echo "$API_RESPONSE"
    echo "Please check your PRIVATE_TOKEN, GITLAB_URL, and GROUP_ID permissions."
    exit 1
fi

# 유효한 경우에만 jq로 파싱
PROJECT_IDS=$(echo "$API_RESPONSE" | jq '.[].id')

if [ -z "$PROJECT_IDS" ]; then
    echo "No projects found in group $GROUP_ID. (This is not an error)"
    exit 0
fi

echo "Target project IDs: $PROJECT_IDS"

# 각 프로젝트에 대해 작업 반복
for PROJECT_ID in $PROJECT_IDS
do
  # URL 인코딩된 프로젝트 경로 가져오기
  PROJECT_PATH_ENCODED=$(curl --silent --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" "$GITLAB_URL/api/v4/projects/$PROJECT_ID" | jq -r '.path_with_namespace | @uri')
  PROJECT_NAME=$(curl --silent --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" "$GITLAB_URL/api/v4/projects/$PROJECT_ID" | jq -r '.name')
  
  echo "--- Processing project: $PROJECT_NAME (ID: $PROJECT_ID) ---"

  # 1. master 브랜치를 기반으로 main 브랜치 생성
  echo "1. Creating 'main' branch from 'master'..."
  curl --request POST --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
       "$GITLAB_URL/api/v4/projects/$PROJECT_ID/repository/branches?branch=main&ref=master"

  # 2. 기본 브랜치를 main으로 변경
  echo "2. Setting 'main' as the default branch..."
  curl --request PUT --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
       --data "default_branch=main" \
       "$GITLAB_URL/api/v4/projects/$PROJECT_ID"

  # 3. master 브랜치 삭제 (주의: 보호 브랜치 설정에 따라 실패할 수 있음)
  echo "3. Deleting 'master' branch..."
  DELETE_RESPONSE=$(curl --write-out "%{http_code}" --silent --output /dev/null --request DELETE --header "PRIVATE-TOKEN: $PRIVATE_TOKEN" \
       "$GITLAB_URL/api/v4/projects/$PROJECT_ID/repository/branches/master")
  
  if [ "$DELETE_RESPONSE" -eq 204 ]; then
      echo "   'master' branch deleted successfully."
  elif [ "$DELETE_RESPONSE" -eq 400 ]; then
      echo "   Failed to delete 'master'. It might be a protected branch. Please check project settings."
  else
      echo "   An error occurred while deleting 'master'. HTTP status: $DELETE_RESPONSE"
  fi
  
  echo "--- Finished processing $PROJECT_NAME ---"
  echo ""
done

echo "All projects have been processed."
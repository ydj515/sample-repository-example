#!/usr/bin/env bash

set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8080}"

# 1. Prompt Template
curl --get "${BASE_URL}/test/prompt/template" \
  --data-urlencode "location=서울 종로" \
  --data-urlencode "content=맛집" \
  --data-urlencode "language=영어"

printf '\n\n'

# 2. List Output
curl --get "${BASE_URL}/test/prompt/list" \
  --data-urlencode "location=서울 종로" \
  --data-urlencode "content=카페" \
  --data-urlencode "language=한국어"

printf '\n\n'

# 3. Map Output
curl --get "${BASE_URL}/test/prompt/map" \
  --data-urlencode "location=서울 종로" \
  --data-urlencode "content=숙박업소" \
  --data-urlencode "language=영어"

printf '\n\n'

# 4. Bean Output
curl --get "${BASE_URL}/test/prompt/bean" \
  --data-urlencode "location=서울 성수" \
  --data-urlencode "content=베이커리" \
  --data-urlencode "language=한국어"

printf '\n\n'

# 5. Parameterized Type Reference
curl --get "${BASE_URL}/test/prompt/shops" \
  --data-urlencode "location=서울 종로" \
  --data-urlencode "content=숙박업소" \
  --data-urlencode "language=한국어"

printf '\n\n'

# 6. Advisor Completion
curl --get "${BASE_URL}/test/advisor/completion" \
  --data-urlencode "prompt=서울 성수에서 저녁 먹기 좋은 식당 추천해줘"

printf '\n\n'

# 7. Advisor Stream
curl -N --get "${BASE_URL}/test/advisor/stream" \
  --data-urlencode "prompt=봄에 가기 좋은 서울 산책 코스를 알려줘"

printf '\n\n'

# 8. Advisor Structured Output
curl --get "${BASE_URL}/test/advisor/bean-output" \
  --data-urlencode "location=서울 종로" \
  --data-urlencode "content=전시회" \
  --data-urlencode "language=한국어"

printf '\n'

#!/usr/bin/env bash

set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
SAMPLES_DIR="${SCRIPT_DIR}/samples"
BASE_URL="${BASE_URL:-http://localhost:8080}"
IMAGE_FILE="${IMAGE_FILE:-${SAMPLES_DIR}/sample-image.png}"
AUDIO_FILE="${AUDIO_FILE:-${SAMPLES_DIR}/sample-audio.mp3}"
AUDIO_STREAM_OUTPUT="${AUDIO_STREAM_OUTPUT:-/tmp/text-to-speech-chat-stream.mp3}"

# 업로드 예제를 실행하려면 아래 환경변수를 지정하세요.
# 예시:
# IMAGE_FILE=/absolute/path/sample.png AUDIO_FILE=/absolute/path/sample.mp3 ./scripts/test-curl.sh
# 기본 경로:
# - ${SAMPLES_DIR}/sample-image.png
# - ${SAMPLES_DIR}/sample-audio.mp3

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

printf '\n\n'

# 9. Multimodal Generate Image URL
curl --get "${BASE_URL}/test/multimodal/generate-image-url" \
  --data-urlencode "prompt=한옥 마당에서 책 읽는 고양이를 그려줘"

printf '\n\n'

# 10. Multimodal Generate Image
curl --get "${BASE_URL}/test/multimodal/generate-image" \
  --data-urlencode "prompt=봄비가 내리는 서울 골목 풍경을 그려줘"

printf '\n\n'

# 11. Multimodal Image Analysis
if [[ -n "${IMAGE_FILE}" && -f "${IMAGE_FILE}" ]]; then
  curl -N -X POST "${BASE_URL}/test/multimodal/image-analysis" \
    -F "question=이 이미지에 보이는 장면을 자세히 설명해줘" \
    -F "attach=@${IMAGE_FILE}"
else
  printf 'Skip 11. Multimodal Image Analysis: IMAGE_FILE 경로를 지정하세요.\n'
fi

printf '\n\n'

# 12. Audio Text to Speech
curl --get "${BASE_URL}/test/audio/text-to-speech" \
  --data-urlencode "prompt=안녕하세요. 스프링 AI 오디오 기능 테스트입니다."

printf '\n\n'

# 13. Audio Text to Speech Chat
curl --get "${BASE_URL}/test/audio/text-to-speech-chat" \
  --data-urlencode "prompt=서울에서 주말에 가볍게 가기 좋은 곳을 추천해줘"

printf '\n\n'

# 14. Audio Text to Speech Chat Stream
curl --get "${BASE_URL}/test/audio/text-to-speech-chat-stream" \
  --data-urlencode "prompt=오늘 기분 좋게 시작할 수 있는 짧은 한마디를 들려줘" \
  --output "${AUDIO_STREAM_OUTPUT}"

printf 'Audio stream saved to %s\n\n' "${AUDIO_STREAM_OUTPUT}"

# 15. Audio Speech to Text
if [[ -n "${AUDIO_FILE}" && -f "${AUDIO_FILE}" ]]; then
  curl -X POST "${BASE_URL}/test/audio/speech-to-text" \
    -F "attach=@${AUDIO_FILE}"
else
  printf 'Skip 15. Audio Speech to Text: AUDIO_FILE 경로를 지정하세요.\n'
fi

printf '\n\n'

# 16. Audio Speech to Text Chat
if [[ -n "${AUDIO_FILE}" && -f "${AUDIO_FILE}" ]]; then
  curl -N -X POST "${BASE_URL}/test/audio/speech-to-text-chat" \
    -F "attach=@${AUDIO_FILE}"
else
  printf 'Skip 16. Audio Speech to Text Chat: AUDIO_FILE 경로를 지정하세요.\n'
fi

printf '\n\n'

# 17. Audio Speech to Text Chat Voice
if [[ -n "${AUDIO_FILE}" && -f "${AUDIO_FILE}" ]]; then
  curl -X POST "${BASE_URL}/test/audio/speech-to-text-chat-voice" \
    -F "attach=@${AUDIO_FILE}"
else
  printf 'Skip 17. Audio Speech to Text Chat Voice: AUDIO_FILE 경로를 지정하세요.\n'
fi

printf '\n\n'

# 18. Tools Date Time
curl --get "${BASE_URL}/test/tools/date-time" \
  --data-urlencode "prompt=지금 서울 시간과 오늘 날씨를 알려줘"

printf '\n\n'

# 19. Tools Customer Inquiry JSON
curl --get "${BASE_URL}/test/tools/customer-inquiry-json" \
  --data-urlencode "prompt=id01 고객 정보를 조회해줘"

printf '\n\n'

# 20. Tools Customer Inquiry String
curl --get "${BASE_URL}/test/tools/customer-inquiry-string" \
  --data-urlencode "prompt=id02 고객 정보를 자연어로 알려줘"

printf '\n\n'

# 21. Tools Recommendation
curl --get "${BASE_URL}/test/tools/recommendation" \
  --data-urlencode "prompt=이 고객이 좋아할 만한 상품을 추천해줘" \
  --data-urlencode "user_id=id01"

printf '\n\n'

# 22. Tools Access System
if [[ -n "${IMAGE_FILE}" && -f "${IMAGE_FILE}" ]]; then
  curl -X POST "${BASE_URL}/test/tools/access-system" \
    -F "attach=@${IMAGE_FILE}"
else
  printf 'Skip 22. Tools Access System: IMAGE_FILE 경로를 지정하세요.\n'
fi

printf '\n'
